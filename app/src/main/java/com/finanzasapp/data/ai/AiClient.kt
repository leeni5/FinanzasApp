package com.finanzasapp.data.ai

import com.finanzasapp.R
import com.finanzasapp.data.settings.Idioma
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import kotlin.coroutines.coroutineContext

/** Error que se le puede mostrar al usuario tal cual. */
class AiException(message: String, val badRequest: Boolean = false) : Exception(message)

/** Un turno de la conversación. role: "system", "user" o "assistant". */
data class AiMessage(val role: String, val content: String)

/**
 * Cliente mínimo para APIs compatibles con OpenAI, con HttpURLConnection:
 * sin librerías nuevas. Solo lee y escribe texto: la IA nunca toca la base.
 */
object AiClient {

    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 90_000

    /** Modelos de chat que ofrece el proveedor con esa clave. */
    suspend fun listModels(provider: AiProvider, apiKey: String): List<String> {
        val body = request(provider, apiKey, "GET", "/models", null)
        val data = JSONObject(body).optJSONArray("data") ?: JSONArray()
        return (0 until data.length())
            .mapNotNull { data.optJSONObject(it)?.optString("id")?.takeIf(String::isNotBlank) }
            .map { it.removePrefix("models/") }
            // Fuera los que no conversan: audio, imágenes, embeddings, moderación.
            .filterNot { id ->
                listOf(
                    "whisper", "tts", "embed", "moderation", "guard", "image", "vision-preview", "ocr",
                    "transcribe", "aqa", "deep-research", "imagen", "veo", "lyria", "audio", "computer-use", "robotics"
                )
                    .any { id.contains(it, ignoreCase = true) }
            }
            .distinct()
            .sorted()
    }

    /**
     * Envía la conversación y devuelve la respuesta completa. La respuesta llega
     * en streaming: [onDelta] recibe el texto acumulado a medida que el modelo
     * lo escribe, para mostrarlo sin esperar al final.
     */
    suspend fun chat(
        provider: AiProvider,
        apiKey: String,
        model: String,
        messages: List<AiMessage>,
        onDelta: (String) -> Unit = {}
    ): String {
        fun payload(lowReasoning: Boolean) = JSONObject()
            .put("model", model)
            .put("temperature", 0.2)
            .put("stream", true)
            .apply {
                // En Gemini los modelos «piensan» antes de responder; para sumar
                // y comparar cifras basta con poco, y la respuesta llega antes.
                if (lowReasoning) put("reasoning_effort", "low")
            }
            .put(
                "messages",
                JSONArray().apply {
                    messages.forEach { put(JSONObject().put("role", it.role).put("content", it.content)) }
                }
            )
            .toString()

        val answer = StringBuilder()
        val onLine: (String) -> Unit = { line ->
            val data = line.removePrefix("data:").trim()
            if (line.startsWith("data:") && data != "[DONE]" && data.isNotEmpty()) {
                val piece = runCatching {
                    JSONObject(data).optJSONArray("choices")?.optJSONObject(0)
                        ?.optJSONObject("delta")?.optString("content")
                }.getOrNull()
                if (!piece.isNullOrEmpty() && piece != "null") {
                    answer.append(piece)
                    onDelta(answer.toString())
                }
            }
        }
        val lowReasoning = provider == AiProvider.GEMINI
        try {
            request(provider, apiKey, "POST", "/chat/completions", payload(lowReasoning), onLine)
        } catch (error: AiException) {
            // Si el modelo no acepta el ajuste de razonamiento, se reintenta sin él.
            if (!lowReasoning || answer.isNotEmpty() || !error.badRequest) throw error
            request(provider, apiKey, "POST", "/chat/completions", payload(false), onLine)
        }
        val content = answer.toString().trim()
        if (content.isEmpty()) throw AiException(Idioma.text(R.string.rv_ai_no_text))
        return content
    }

    private suspend fun request(
        provider: AiProvider,
        apiKey: String,
        method: String,
        path: String,
        json: String?,
        onLine: ((String) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        val connection = URL(provider.baseUrl + path).openConnection() as HttpURLConnection
        // Cancelar desde la pantalla corta la conexión en vez de esperar la respuesta.
        val job = coroutineContext[kotlinx.coroutines.Job]
        val handle = job?.invokeOnCompletion { if (it != null) connection.disconnect() }
        try {
            connection.requestMethod = method
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Accept", "application/json")
            if (provider == AiProvider.OPENROUTER) connection.setRequestProperty("X-Title", "FinanzasApp")
            if (json != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            if (code in 200..299 && onLine != null) {
                // Streaming: una línea «data: {...}» por fragmento.
                connection.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                    for (line in lines) {
                        coroutineContext.ensureActive()
                        onLine(line)
                    }
                }
                return@withContext ""
            }
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            coroutineContext.ensureActive()
            if (code !in 200..299) throw AiException(describe(code, text), badRequest = code == 400)
            text
        } catch (error: AiException) {
            throw error
        } catch (error: UnknownHostException) {
            throw AiException(Idioma.text(R.string.rv_ai_offline))
        } catch (error: SocketTimeoutException) {
            throw AiException(Idioma.text(R.string.rv_ai_timeout, provider.label))
        } catch (error: IOException) {
            coroutineContext.ensureActive()
            throw AiException(Idioma.text(R.string.rv_ai_io, provider.label, error.message ?: Idioma.text(R.string.rv_ai_network_error)))
        } finally {
            handle?.dispose()
            connection.disconnect()
        }
    }

    private fun describe(code: Int, body: String): String {
        val detail = runCatching {
            val json = JSONObject(body)
            json.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
                ?: json.optString("message").takeIf { it.isNotBlank() }
                ?: json.optString("detail").takeIf { it.isNotBlank() }
        }.getOrNull()?.take(200)
        val base = when (code) {
            400 -> Idioma.text(R.string.rv_ai_400)
            401, 403 -> Idioma.text(R.string.rv_ai_401)
            404 -> Idioma.text(R.string.rv_ai_404)
            413 -> Idioma.text(R.string.rv_ai_413)
            429 -> Idioma.text(R.string.rv_ai_429)
            in 500..599 -> Idioma.text(R.string.rv_ai_5xx)
            else -> Idioma.text(R.string.rv_ai_error_code, code)
        }
        return if (detail != null) "$base. ($detail)" else "$base."
    }
}
