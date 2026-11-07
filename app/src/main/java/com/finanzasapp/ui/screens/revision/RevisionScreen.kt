package com.finanzasapp.ui.screens.revision

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finanzasapp.FinanzasApp
import com.finanzasapp.data.entity.Account
import com.finanzasapp.data.entity.AuditLog
import com.finanzasapp.data.entity.CategorizationRule
import com.finanzasapp.data.entity.Category
import com.finanzasapp.data.entity.Transaction
import com.finanzasapp.data.entity.TransactionType
import com.finanzasapp.data.model.displayName
import com.finanzasapp.data.rules.RuleActions
import com.finanzasapp.ui.components.AppCard
import com.finanzasapp.ui.components.EmptyState
import com.finanzasapp.ui.components.IconBubble
import com.finanzasapp.ui.screens.categorias.CategoryRowCard
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.Spacing
import com.finanzasapp.ui.util.Fechas
import com.finanzasapp.ui.util.Money
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class Tab(val label: String) { PENDIENTES("Por revisar"), REGLAS("Reglas"), HISTORIAL("Historial") }

/**
 * Bandeja «Por revisar», reglas de categorización e historial de cambios.
 * Lo que llega de una importación sin categoría o sin pagar cae aquí; al
 * corregirlo se puede crear una regla para que la próxima vez se resuelva solo.
 */
@Composable
fun RevisionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val database = remember(context) { (context.applicationContext as FinanzasApp).database }
    val scope = rememberCoroutineScope()

    val pending by database.transactionDao().observePendingReview()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val rules by database.systemDao().observeRules()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val audit by database.systemDao().observeRecentAudit(200)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by database.categoryDao().observeAll()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val accounts by database.accountDao().observeAll()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val categoriesById = remember(categories) { categories.associateBy { it.id } }
    val accountsById = remember(accounts) { accounts.associateBy { it.id } }
    val activeCategories = remember(categories) { categories.filter { !it.archived } }

    var tab by rememberSaveable { mutableStateOf(Tab.PENDIENTES) }
    var reviewing by remember { mutableStateOf<Transaction?>(null) }
    var creatingRule by remember { mutableStateOf(false) }
    var deletingRule by remember { mutableStateOf<CategorizationRule?>(null) }
    var confirmAll by remember { mutableStateOf(false) }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.page, end = Spacing.page, top = Spacing.lg, bottom = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(onClick = onBack, shape = CircleShape, color = AppPalette.SurfaceMuted) {
                    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", tint = AppPalette.Ink)
                    }
                }
                Spacer(Modifier.width(Spacing.md))
                Text("Revisión", style = MaterialTheme.typography.headlineMedium, color = AppPalette.Ink)
            }
            Spacer(Modifier.height(Spacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Tab.entries.forEach { option ->
                    val selected = option == tab
                    val count = when (option) {
                        Tab.PENDIENTES -> pending.size
                        Tab.REGLAS -> rules.size
                        Tab.HISTORIAL -> null
                    }
                    Surface(
                        onClick = { tab = option },
                        shape = MaterialTheme.shapes.small,
                        color = if (selected) AppPalette.Accent else AppPalette.SurfaceMuted
                    ) {
                        Text(
                            text = if (count != null && count > 0) "${option.label} · $count" else option.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) AppPalette.OnAccent else AppPalette.InkSoft,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(Spacing.sm))
        }

        when (tab) {
            Tab.PENDIENTES -> {
                if (pending.isEmpty()) {
                    item {
                        EmptyState(
                            title = "Nada por revisar",
                            message = "Aquí llegan los movimientos importados sin categoría o " +
                                "marcados como no pagados, para que los confirmes.",
                            icon = Icons.Rounded.Checklist
                        )
                    }
                } else {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Toca uno para elegir su categoría y confirmarlo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppPalette.InkSoft,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { confirmAll = true }) {
                                Text("Confirmar todos", color = AppPalette.Accent)
                            }
                        }
                    }
                    items(pending, key = { it.id }) { transaction ->
                        PendingRow(
                            transaction = transaction,
                            category = transaction.categoryId?.let(categoriesById::get),
                            account = accountsById[transaction.accountId],
                            onClick = { reviewing = transaction }
                        )
                    }
                }
            }

            Tab.REGLAS -> {
                item {
                    AppCard {
                        Text(
                            "Si la descripción de un movimiento contiene el texto de la regla, " +
                                "recibe esa categoría. Se usan al importar y solo rellenan " +
                                "movimientos que llegan sin categoría.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppPalette.InkSoft
                        )
                        Spacer(Modifier.height(Spacing.md))
                        Button(
                            onClick = { creatingRule = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Accent, contentColor = AppPalette.OnAccent)
                        ) { Text("Nueva regla") }
                    }
                }
                if (rules.isEmpty()) {
                    item {
                        EmptyState(
                            title = "Sin reglas todavía",
                            message = "Crea una aquí o al revisar un movimiento.",
                            icon = Icons.Rounded.AutoFixHigh
                        )
                    }
                }
                items(rules, key = { it.id }) { rule ->
                    RuleRow(rule, categoriesById[rule.categoryId]) { deletingRule = rule }
                }
            }

            Tab.HISTORIAL -> {
                if (audit.isEmpty()) {
                    item {
                        EmptyState(
                            title = "Sin cambios registrados",
                            message = "Cada alta, edición o borrado queda anotado aquí.",
                            icon = Icons.Rounded.History
                        )
                    }
                } else {
                    item {
                        Text(
                            "Los últimos ${audit.size} cambios. El historial no se puede editar ni borrar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppPalette.InkSoft
                        )
                    }
                    item {
                        AppCard(contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xs)) {
                            audit.forEachIndexed { index, entry ->
                                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                AuditRow(entry, categoriesById, accountsById)
                            }
                        }
                    }
                }
            }
        }
    }

    reviewing?.let { transaction ->
        ReviewSheet(
            transaction = transaction,
            categories = activeCategories,
            onDismiss = { reviewing = null },
            onConfirm = { categoryId, rulePattern ->
                reviewing = null
                scope.launch {
                    ReviewActions.confirm(database, transaction.id, categoryId)
                    if (rulePattern != null && categoryId != null) {
                        val applied = RuleActions.create(database, rulePattern, categoryId)
                        toast(if (applied > 0) "Regla creada; categorizó $applied movimientos más." else "Regla creada.")
                    }
                }
            }
        )
    }

    if (creatingRule) {
        RuleSheet(
            categories = activeCategories,
            onDismiss = { creatingRule = false },
            onSave = { pattern, categoryId ->
                creatingRule = false
                scope.launch {
                    val applied = RuleActions.create(database, pattern, categoryId)
                    toast(if (applied > 0) "Regla creada; categorizó $applied movimientos." else "Regla creada.")
                }
            }
        )
    }

    deletingRule?.let { rule ->
        AlertDialog(
            onDismissRequest = { deletingRule = null },
            title = { Text("¿Borrar la regla «${rule.pattern}»?") },
            text = { Text("Los movimientos que ya categorizó se quedan como están.") },
            confirmButton = {
                TextButton(onClick = {
                    deletingRule = null
                    scope.launch { RuleActions.delete(database, rule.id) }
                }) { Text("Borrar", color = AppPalette.Coral) }
            },
            dismissButton = { TextButton(onClick = { deletingRule = null }) { Text("Cancelar") } }
        )
    }

    if (confirmAll) {
        AlertDialog(
            onDismissRequest = { confirmAll = false },
            title = { Text("¿Confirmar los ${pending.size} movimientos?") },
            text = { Text("Salen de la bandeja con la categoría que tienen ahora; los que no tienen quedan «Sin categoría».") },
            confirmButton = {
                TextButton(onClick = {
                    confirmAll = false
                    val list = pending
                    scope.launch { ReviewActions.confirmAll(database, list) }
                }) { Text("Confirmar todos") }
            },
            dismissButton = { TextButton(onClick = { confirmAll = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun PendingRow(
    transaction: Transaction,
    category: Category?,
    account: Account?,
    onClick: () -> Unit
) {
    AppCard(onClick = onClick, contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    transaction.displayName(),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppPalette.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${Fechas.corta(transaction.date)} ${transaction.date.year} · ${account?.name ?: "Sin cuenta"} · " +
                        (category?.name ?: "Sin categoría"),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (category == null) AppPalette.Amber else AppPalette.InkSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            Text(
                (if (transaction.type == TransactionType.INGRESO) "+ " else "− ") + Money.cop(transaction.amount),
                style = MaterialTheme.typography.titleSmall,
                color = if (transaction.type == TransactionType.INGRESO) AppPalette.Green else AppPalette.Ink
            )
        }
    }
}

@Composable
private fun RuleRow(rule: CategorizationRule, category: Category?, onDelete: () -> Unit) {
    AppCard(onClick = onDelete, contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(icon = Icons.Rounded.AutoFixHigh, tint = AppPalette.Violet, size = 36.dp)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text("Contiene «${rule.pattern}»", style = MaterialTheme.typography.titleSmall, color = AppPalette.Ink)
                Text(
                    "→ ${category?.name ?: "Categoría borrada"} · usada ${rule.timesApplied} " +
                        if (rule.timesApplied == 1) "vez" else "veces",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.InkSoft
                )
            }
            Text("Borrar", style = MaterialTheme.typography.labelMedium, color = AppPalette.Coral)
        }
    }
}

private val timeFormat = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.forLanguageTag("es-CO"))

@Composable
private fun AuditRow(entry: AuditLog, categories: Map<Long, Category>, accounts: Map<Long, Account>) {
    val json = runCatching { JSONObject(entry.afterJson ?: entry.beforeJson ?: "{}") }.getOrNull() ?: JSONObject()
    val what = when (entry.entity) {
        "transaction" -> "Movimiento"
        "transfer" -> "Transferencia"
        "category" -> "Categoría"
        "account" -> "Cuenta"
        "goal" -> "Meta"
        "goal_contribution" -> "Aporte"
        "app" -> "App"
        "payment" -> "Pago programado"
        "budget" -> "Presupuesto"
        "rule" -> "Regla"
        "import" -> "Importación"
        "backup" -> "Datos"
        "ia" -> "Consulta a la IA"
        else -> entry.entity.replaceFirstChar { it.uppercase() }
    }
    // Concordancia: «Regla creada», «Movimiento creado».
    val feminine = entry.entity in setOf("transfer", "category", "account", "goal", "rule", "import")
    fun gender(word: String) = if (feminine) word.dropLast(1) + "a" else word
    val action = when (entry.action) {
        "CREADO" -> gender("creado")
        "EDITADO" -> gender("editado")
        "BORRADO" -> gender("borrado")
        "ARCHIVADO" -> gender("archivado")
        "RECUPERADO" -> gender("recuperado")
        "REVISADO" -> gender("revisado")
        "COMPLETADO" -> "completada"
        "COPIA" -> "copia de seguridad creada"
        "RESTAURADO" -> "restaurados desde una copia"
        "EXPORTADO" -> "exportados a CSV"
        "SEMBRADO" -> "preparada"
        "CONSULTA" -> "enviada"
        else -> entry.action.lowercase()
    }
    val detail = buildList {
        json.optString("descripcion").takeIf { it.isNotBlank() }?.let(::add)
        json.optString("name").takeIf { it.isNotBlank() }?.let(::add)
        json.optString("patron").takeIf { it.isNotBlank() }?.let { add("«$it»") }
        json.optString("archivo").takeIf { it.isNotBlank() }?.let(::add)
        if (json.has("desde") && json.has("hacia")) {
            add("${accounts[json.optLong("desde")]?.name ?: "?"} → ${accounts[json.optLong("hacia")]?.name ?: "?"}")
        }
        if (json.has("monto")) add(Money.cop(json.optLong("monto")))
        if (entry.entity == "transaction" && !json.isNull("categoria") && json.has("categoria")) {
            categories[json.optLong("categoria")]?.name?.let(::add)
        }
        json.optString("proveedor").takeIf { it.isNotBlank() }?.let { add("$it · ${json.optString("modelo")}") }
        json.optString("periodo").takeIf { it.isNotBlank() }?.let(::add)
        if (json.has("movimientos")) add("${json.optInt("movimientos")} movimientos")
        if (json.has("importados")) add("${json.optInt("importados")} importados")
    }.joinToString(" · ")
    val source = when (entry.source) {
        "usuario" -> "Tú"
        "importacion" -> "Importación"
        "seed" -> "Datos de ejemplo"
        "regla" -> "Regla"
        "ia" -> "IA"
        "pago_programado" -> "Pago programado"
        else -> entry.source
    }

    Column(Modifier.fillMaxWidth().padding(vertical = Spacing.md)) {
        Text("$what $action", style = MaterialTheme.typography.titleSmall, color = AppPalette.Ink)
        if (detail.isNotBlank()) {
            Text(detail, style = MaterialTheme.typography.bodySmall, color = AppPalette.InkSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text(
            "${entry.at.atZone(Fechas.Bogota).format(timeFormat)} · $source",
            style = MaterialTheme.typography.labelSmall,
            color = AppPalette.InkFaint
        )
    }
}

/** Elegir categoría para un pendiente y, si se quiere, crear una regla con su texto. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewSheet(
    transaction: Transaction,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (categoryId: Long?, rulePattern: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var categoryId by remember { mutableStateOf(transaction.categoryId) }
    var makeRule by remember { mutableStateOf(false) }
    var pattern by remember { mutableStateOf(transaction.displayName()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(transaction.displayName(), style = MaterialTheme.typography.headlineSmall, color = AppPalette.Ink)
            Text(
                "${Fechas.diaYMes(transaction.date)} de ${transaction.date.year} · ${Money.cop(transaction.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.InkSoft
            )
            if (transaction.notes != null) {
                Text(transaction.notes, style = MaterialTheme.typography.bodySmall, color = AppPalette.InkSoft)
            }
            Spacer(Modifier.height(Spacing.sm))
            Text("Categoría", style = MaterialTheme.typography.titleSmall, color = AppPalette.Ink)
            categories.forEach { category ->
                CategoryRowCard(category, selected = category.id == categoryId) { categoryId = category.id }
            }
            Spacer(Modifier.height(Spacing.sm))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = makeRule,
                        enabled = categoryId != null,
                        role = Role.Checkbox,
                        onValueChange = { makeRule = it }
                    )
            ) {
                Checkbox(checked = makeRule, onCheckedChange = null, enabled = categoryId != null)
                Text(
                    "Usar siempre esta categoría cuando la descripción contenga…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppPalette.Ink
                )
            }
            if (makeRule) {
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("Texto de la regla") },
                    supportingText = { Text("Deja solo la parte que se repite, por ejemplo «RAPPI» o «EXITO».") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            Button(
                onClick = { onConfirm(categoryId, if (makeRule && pattern.isNotBlank()) pattern else null) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Accent, contentColor = AppPalette.OnAccent)
            ) { Text("Confirmar") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleSheet(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (pattern: String, categoryId: Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pattern by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<Long?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text("Nueva regla", style = MaterialTheme.typography.headlineSmall, color = AppPalette.Ink)
            OutlinedTextField(
                value = pattern,
                onValueChange = { pattern = it },
                label = { Text("Si la descripción contiene") },
                supportingText = { Text("No distingue mayúsculas ni tildes.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text("va a la categoría", style = MaterialTheme.typography.titleSmall, color = AppPalette.Ink)
            categories.forEach { category ->
                CategoryRowCard(category, selected = category.id == categoryId) { categoryId = category.id }
            }
            Spacer(Modifier.height(Spacing.sm))
            Button(
                onClick = { categoryId?.let { onSave(pattern, it) } },
                enabled = pattern.isNotBlank() && categoryId != null,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Accent, contentColor = AppPalette.OnAccent)
            ) { Text("Guardar y aplicar") }
        }
    }
}
