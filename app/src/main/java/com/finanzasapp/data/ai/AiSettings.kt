package com.finanzasapp.data.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.StringRes
import com.finanzasapp.R
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Proveedores con API compatible con OpenAI (`/chat/completions` y `/models`).
 * Todos aceptan la misma petición, así que cambiar de proveedor es cambiar la
 * URL y la clave. Los modelos no se fijan en el código: se piden al proveedor,
 * porque sus nombres cambian a menudo.
 */
enum class AiProvider(
    val label: String,
    val baseUrl: String,
    val keyUrl: String,
    @StringRes val note: Int
) {
    GROQ(
        "Groq",
        "https://api.groq.com/openai/v1",
        "console.groq.com/keys",
        R.string.rv_note_groq
    ),
    MISTRAL(
        "Mistral",
        "https://api.mistral.ai/v1",
        "console.mistral.ai/api-keys",
        R.string.rv_note_mistral
    ),
    OPENROUTER(
        "OpenRouter",
        "https://openrouter.ai/api/v1",
        "openrouter.ai/keys",
        R.string.rv_note_openrouter
    ),
    GEMINI(
        "Gemini",
        "https://generativelanguage.googleapis.com/v1beta/openai",
        "aistudio.google.com/apikey",
        R.string.rv_note_gemini
    )
}

/**
 * Proveedor, modelo y clave de la IA.
 *
 * La clave se guarda envuelta con AES-256/GCM usando una llave propia del
 * Android Keystore (distinta de la de la base): en las preferencias solo hay
 * texto cifrado. Las copias de Android están desactivadas, así que tampoco
 * sale del teléfono por ahí, y no va en las copias de seguridad de la app.
 */
class AiSettings(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var provider: AiProvider
        get() = prefs.getString(KEY_PROVIDER, null)
            ?.let { saved -> AiProvider.entries.firstOrNull { it.name == saved } }
            ?: AiProvider.GROQ
        set(value) {
            prefs.edit().putString(KEY_PROVIDER, value.name).apply()
        }

    /** Modelo elegido para el proveedor actual; "" si todavía no se eligió. */
    fun model(provider: AiProvider = this.provider): String =
        prefs.getString(KEY_MODEL + provider.name, "").orEmpty()

    fun setModel(provider: AiProvider, model: String) {
        prefs.edit().putString(KEY_MODEL + provider.name, model).apply()
    }

    fun hasKey(provider: AiProvider = this.provider): Boolean =
        prefs.contains(KEY_SECRET + provider.name)

    fun apiKey(provider: AiProvider = this.provider): String? =
        prefs.getString(KEY_SECRET + provider.name, null)?.let { runCatching { unwrap(it) }.getOrNull() }

    fun setApiKey(provider: AiProvider, key: String) {
        prefs.edit().putString(KEY_SECRET + provider.name, wrap(key.trim())).apply()
    }

    fun clearKey(provider: AiProvider) {
        prefs.edit()
            .remove(KEY_SECRET + provider.name)
            .remove(KEY_MODEL + provider.name)
            .apply()
    }

    private fun wrap(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        return Base64.encodeToString(cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }

    private fun unwrap(wrapped: String): String {
        val payload = Base64.decode(wrapped, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, payload.copyOfRange(0, 12)))
        return String(cipher.doFinal(payload.copyOfRange(12, payload.size)), Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFS = "finanzas_ia"
        const val KEY_PROVIDER = "proveedor"
        const val KEY_MODEL = "modelo_"
        const val KEY_SECRET = "clave_"
        const val KEY_ALIAS = "finanzas_ia_key_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
