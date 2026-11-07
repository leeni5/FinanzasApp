package com.finanzasapp.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.finanzasapp.FinanzasApp
import com.finanzasapp.data.ai.AiClient
import com.finanzasapp.data.ai.AiException
import com.finanzasapp.data.ai.AiMessage
import com.finanzasapp.domain.ai.PeriodDetector
import com.finanzasapp.data.ai.AiProvider
import com.finanzasapp.data.ai.AiSettings
import com.finanzasapp.data.ai.FinanceContext
import com.finanzasapp.data.ai.FinanceSnapshot
import com.finanzasapp.data.entity.AuditLog
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.Spacing
import com.finanzasapp.ui.util.Fechas
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant

private data class ChatTurn(
    val fromUser: Boolean,
    val text: String,
    val error: Boolean = false
)

/**
 * Chat para preguntar sobre las propias finanzas.
 *
 * El período se deduce de la pregunta en el teléfono ([PeriodDetector]): solo
 * se envían los movimientos de ese lapso. Si la pregunta no nombra fechas se
 * mantiene el de la pregunta anterior, y en la primera, los últimos 12 meses.
 * El ojo muestra el texto exacto enviado. La conversación no se guarda y la
 * IA solo responde texto; nunca escribe en la base.
 */
@Composable
fun ChatScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as FinanzasApp }
    val settings = remember(context) { AiSettings(context) }
    val scope = rememberCoroutineScope()

    var provider by remember { mutableStateOf(settings.provider) }
    var model by remember { mutableStateOf(settings.model()) }
    var configured by remember { mutableStateOf(settings.hasKey() && settings.model().isNotBlank()) }
    var showSetup by remember { mutableStateOf(!configured) }

    // Lapso de los datos: se actualiza con cada pregunta que nombre fechas.
    var range by remember { mutableStateOf(PeriodDetector.default(Fechas.hoy())) }
    var snapshot by remember { mutableStateOf<FinanceSnapshot?>(null) }
    var showData by remember { mutableStateOf(false) }

    val turns = remember { mutableStateListOf<ChatTurn>() }
    var input by rememberSaveable { mutableStateOf("") }
    var sending by remember { mutableStateOf<Job?>(null) }
    // Respuesta que va llegando (streaming) y en qué paso va la consulta.
    var partial by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(range) {
        snapshot = FinanceContext.build(app.database, range, includeDetail = true, today = Fechas.hoy())
    }
    // Siempre a la vista lo último: la pregunta, el indicador o la respuesta que crece.
    LaunchedEffect(turns.size, sending != null, partial.length / 80) {
        val last = listState.layoutInfo.totalItemsCount - 1
        if (last >= 0) listState.scrollToItem(last)
    }

    fun send(question: String) {
        val key = settings.apiKey(provider) ?: run { showSetup = true; return }
        val today = Fechas.hoy()
        val questionRange = PeriodDetector.detect(question, today) ?: range
        range = questionRange
        turns += ChatTurn(fromUser = true, text = question)
        input = ""
        val history = turns.filter { !it.error }.map { AiMessage(if (it.fromUser) "user" else "assistant", it.text) }
        partial = ""
        status = "Revisando tus movimientos"
        sending = scope.launch {
            try {
                val data = FinanceContext.build(app.database, questionRange, includeDetail = true, today = today)
                snapshot = data
                status = "${provider.label} está pensando"
                val answer = AiClient.chat(
                    provider,
                    key,
                    model,
                    listOf(
                        AiMessage("system", FinanceContext.systemPrompt),
                        AiMessage("system", "DATOS DEL USUARIO:\n${data.text}")
                    ) + history,
                    onDelta = { text -> partial = text }
                )
                turns += ChatTurn(fromUser = false, text = answer)
                app.database.systemDao().insertAudit(
                    AuditLog(
                        entity = "ia",
                        entityId = 0L,
                        action = "CONSULTA",
                        afterJson = JSONObject()
                            .put("proveedor", provider.label)
                            .put("modelo", model)
                            .put("periodo", questionRange.label)
                            .put("movimientos", data.movements)
                            .toString(),
                        source = "usuario",
                        at = Instant.now()
                    )
                )
            } catch (e: AiException) {
                turns += ChatTurn(fromUser = false, text = e.message.orEmpty(), error = true)
            } finally {
                // Si se canceló a mitad, lo que alcanzó a llegar se conserva.
                if (sending?.isCancelled == true && partial.isNotBlank()) {
                    turns += ChatTurn(fromUser = false, text = partial.trim() + " …")
                }
                partial = ""
                sending = null
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Encabezado
        Row(
            Modifier.padding(start = Spacing.page, end = Spacing.page, top = Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(onClick = onBack, shape = CircleShape, color = AppPalette.SurfaceMuted) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", tint = AppPalette.Ink)
                }
            }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text("Pregúntale a la IA", style = MaterialTheme.typography.titleLarge, color = AppPalette.Ink)
                Text(
                    if (configured) "${provider.label} · $model" else "Sin configurar",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.InkSoft,
                    maxLines = 1
                )
            }
            IconButton(onClick = { showData = true }) {
                Icon(Icons.Rounded.Visibility, "Ver qué datos se envían", tint = AppPalette.InkSoft)
            }
            IconButton(onClick = { showSetup = true }) {
                Icon(Icons.Rounded.Tune, "Proveedor y modelo", tint = AppPalette.InkSoft)
            }
        }

        // Conversación
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = Spacing.page, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            items(turns) { turn -> Bubble(turn) }
            if (sending != null) {
                item {
                    if (partial.isBlank()) TypingBubble(status) else Bubble(ChatTurn(fromUser = false, text = partial))
                }
            }
        }

        // Entrada
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.page, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Escribe tu pregunta") },
                modifier = Modifier.weight(1f).heightIn(max = 140.dp),
                shape = MaterialTheme.shapes.large
            )
            Spacer(Modifier.width(Spacing.sm))
            val busy = sending != null
            val canSend = input.isNotBlank() && !busy
            Surface(
                onClick = {
                    if (busy) {
                        sending?.cancel()
                        return@Surface
                    }
                    val question = input.trim()
                    when {
                        !configured -> showSetup = true
                        else -> send(question)
                    }
                },
                enabled = canSend || busy,
                shape = CircleShape,
                color = when {
                    busy -> AppPalette.CoralSoft
                    canSend -> AppPalette.Accent
                    else -> AppPalette.SurfaceMuted
                }
            ) {
                Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    if (busy) {
                        Icon(Icons.Rounded.Stop, "Detener", tint = AppPalette.Coral)
                    } else {
                        Icon(Icons.AutoMirrored.Rounded.Send, "Enviar", tint = if (canSend) AppPalette.OnAccent else AppPalette.InkFaint)
                    }
                }
            }
        }
    }

    if (showData) {
        DataSheet(snapshot?.text.orEmpty(), provider) { showData = false }
    }

    if (showSetup) {
        AiSetupSheet(
            settings = settings,
            onDismiss = { showSetup = false },
            onSaved = { savedProvider, savedModel ->
                provider = savedProvider
                model = savedModel
                configured = true
                showSetup = false
            }
        )
    }
}

/** Burbuja de «escribiendo»: el paso actual y tres puntos que laten. */
@Composable
private fun TypingBubble(status: String) {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(
        Modifier
            .clip(MaterialTheme.shapes.large)
            .background(AppPalette.SurfaceMuted)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(status, style = MaterialTheme.typography.bodyMedium, color = AppPalette.InkSoft)
        Spacer(Modifier.width(Spacing.sm))
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 500, delayMillis = index * 160),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            Box(
                Modifier
                    .padding(horizontal = 2.dp)
                    .size(7.dp)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(AppPalette.Accent)
            )
        }
    }
}

@Composable
private fun Bubble(turn: ChatTurn) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (turn.fromUser) Alignment.End else Alignment.Start) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (turn.fromUser) Arrangement.End else Arrangement.Start) {
        SelectionContainer {
            Text(
                text = if (turn.fromUser || turn.error) AnnotatedString(turn.text) else simpleMarkdown(turn.text),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    turn.error -> AppPalette.Coral
                    turn.fromUser -> AppPalette.OnAccent
                    else -> AppPalette.Ink
                },
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(
                        when {
                            turn.error -> AppPalette.CoralSoft
                            turn.fromUser -> AppPalette.Accent
                            else -> AppPalette.SurfaceMuted
                        }
                    )
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm)
            )
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DataSheet(text: String, provider: AiProvider, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AppPalette.SurfaceStrong) }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.page)
                .padding(bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text("Qué se envía a ${provider.label}", style = MaterialTheme.typography.headlineSmall, color = AppPalette.Ink)
            Text(
                "Esto, más tus instrucciones fijas y la conversación. Nada más sale del teléfono: ni notas, ni metas, ni la base.",
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.InkSoft
            )
            SelectionContainer {
                Text(text, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = AppPalette.Ink)
            }
        }
    }
}

/** Elegir proveedor, pegar la clave y escoger un modelo de los que ofrece. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSetupSheet(
    settings: AiSettings,
    onDismiss: () -> Unit,
    onSaved: (AiProvider, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var provider by remember { mutableStateOf(settings.provider) }
    var key by remember { mutableStateOf("") }
    var models by remember { mutableStateOf<List<String>>(emptyList()) }
    var selected by remember { mutableStateOf(settings.model()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val hasSavedKey = settings.hasKey(provider)

    fun loadModels() {
        val apiKey = key.trim().ifBlank { settings.apiKey(provider).orEmpty() }
        if (apiKey.isBlank()) { error = "Pega la clave de ${provider.label}."; return }
        loading = true
        error = null
        scope.launch {
            try {
                models = AiClient.listModels(provider, apiKey)
                if (key.isNotBlank()) settings.setApiKey(provider, key)
                settings.provider = provider
                if (selected !in models) selected = models.firstOrNull().orEmpty()
                if (models.isEmpty()) error = "${provider.label} no devolvió modelos de chat para esta clave."
            } catch (e: AiException) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AppPalette.SurfaceStrong) }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = Spacing.page)
                .padding(bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text("Conectar una IA", style = MaterialTheme.typography.headlineSmall, color = AppPalette.Ink)
            Text(
                "Usa tu propia clave. Se guarda cifrada en este teléfono y solo se usa para hablar con el proveedor que elijas.",
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.InkSoft
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                AiProvider.entries.forEach { option ->
                    val isSelected = option == provider
                    Surface(
                        onClick = {
                            provider = option
                            models = emptyList()
                            selected = settings.model(option)
                            key = ""
                            error = null
                        },
                        shape = MaterialTheme.shapes.small,
                        color = if (isSelected) AppPalette.Accent else AppPalette.SurfaceMuted
                    ) {
                        Text(
                            option.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) AppPalette.OnAccent else AppPalette.InkSoft,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            Text(provider.note, style = MaterialTheme.typography.bodySmall, color = AppPalette.InkSoft)
            Text("La clave se saca en ${provider.keyUrl}", style = MaterialTheme.typography.bodySmall, color = AppPalette.Accent)

            OutlinedTextField(
                value = key,
                onValueChange = { key = it.trim() },
                label = { Text(if (hasSavedKey) "Clave guardada · pega otra para cambiarla" else "Clave de la API") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { loadModels() },
                enabled = !loading && (key.isNotBlank() || hasSavedKey),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Accent, contentColor = AppPalette.OnAccent)
            ) { Text(if (loading) "Comprobando…" else "Comprobar clave y ver modelos") }

            error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = AppPalette.Coral) }

            if (models.isNotEmpty()) {
                Text("Modelo", style = MaterialTheme.typography.titleSmall, color = AppPalette.Ink)
                models.forEach { id ->
                    val isSelected = id == selected
                    Surface(
                        onClick = { selected = id },
                        shape = MaterialTheme.shapes.small,
                        color = if (isSelected) AppPalette.AccentSoft else AppPalette.SurfaceMuted,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            id,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) AppPalette.Accent else AppPalette.Ink,
                            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
                        )
                    }
                }
                Button(
                    onClick = {
                        settings.provider = provider
                        settings.setModel(provider, selected)
                        onSaved(provider, selected)
                    },
                    enabled = selected.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Accent, contentColor = AppPalette.OnAccent)
                ) { Text("Usar $selected") }
            }

            if (hasSavedKey) {
                TextButton(onClick = {
                    settings.clearKey(provider)
                    models = emptyList()
                    selected = ""
                    error = "Clave de ${provider.label} borrada del teléfono."
                }) { Text("Borrar la clave de ${provider.label}", color = AppPalette.Coral) }
            }
        }
    }
}

/**
 * Markdown mínimo de las respuestas: **negrita**, viñetas con «*» o «-» y
 * títulos con «#». Lo demás se muestra como texto.
 */
private fun simpleMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    text.lines().forEachIndexed { index, raw ->
        if (index > 0) append('\n')
        var line = raw.trimEnd()
        val heading = line.trimStart().startsWith("#")
        if (heading) line = line.trimStart().trimStart('#').trim()
        val bullet = Regex("^(\\s*)[*-]\\s+").find(line)
        if (bullet != null) {
            append(bullet.groupValues[1] + "• ")
            line = line.substring(bullet.range.last + 1)
        }
        val parts = line.split("**")
        parts.forEachIndexed { i, part ->
            if (i % 2 == 1 || heading) withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(part) } else append(part)
        }
    }
}
