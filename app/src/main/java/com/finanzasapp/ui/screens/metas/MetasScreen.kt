package com.finanzasapp.ui.screens.metas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finanzasapp.FinanzasApp
import com.finanzasapp.ui.components.AppCard
import com.finanzasapp.ui.components.AnimatedMoney
import com.finanzasapp.ui.components.ColorDot
import com.finanzasapp.ui.components.EmptyState
import com.finanzasapp.ui.components.ScreenHeader
import com.finanzasapp.ui.components.SectionHeader
import com.finanzasapp.ui.components.SoftPill
import com.finanzasapp.ui.components.SoftProgressBar
import com.finanzasapp.ui.components.TintedCard
import com.finanzasapp.ui.components.ScrollAwareFab
import com.finanzasapp.ui.components.isScrollingUp
import androidx.compose.foundation.lazy.rememberLazyListState
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.Spacing
import com.finanzasapp.ui.theme.asColor
import com.finanzasapp.ui.theme.identityColor
import com.finanzasapp.ui.util.Fechas
import com.finanzasapp.ui.util.Money
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val GoalColors = listOf(
    "#4C5BD4",
    "#148F84",
    "#7C5CE0",
    "#D9548C",
    "#0E9AB5",
    "#2E86C8"
)

@Composable
fun MetasScreen() {
    val context = LocalContext.current
    val database = remember(context) {
        (context.applicationContext as FinanzasApp).database
    }
    val viewModel: MetasViewModel = viewModel(
        factory = MetasViewModel.factory(database)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    var editor by remember { mutableStateOf<GoalDraft?>(null) }
    var detailGoalId by remember { mutableStateOf<Long?>(null) }
    var contributionGoalId by remember { mutableStateOf<Long?>(null) }

    val detailGoal = state.goals.firstOrNull { it.goalId == detailGoalId }
    val contributionGoal = state.goals.firstOrNull { it.goalId == contributionGoalId }

    val listState = rememberLazyListState()
    val fabVisible = listState.isScrollingUp()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.page,
                end = Spacing.page,
                top = Spacing.xl,
                bottom = 108.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            item {
                ScreenHeader(
                    title = "Metas",
                    subtitle = "Ahorrar también es parte del plan."
                )
            }

            when {
                state.loading -> item {
                    Text(
                        text = "Leyendo tus metas…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                state.goals.isEmpty() -> item {
                    EmptyState(
                        title = "Todavía no hay metas",
                        message = "Crea una meta, define cuánto necesitas y registra tus " +
                            "aportes. El avance y la proyección se calculan solos.",
                        icon = Icons.Filled.Star,
                        actionLabel = "Crear mi primera meta",
                        onAction = { editor = GoalDraft() }
                    )
                }

                else -> {
                    item { GoalsSummary(state) }

                    if (state.active.isNotEmpty()) {
                        item { SectionHeader(title = "En curso", trailing = "${state.activeCount}") }
                        items(state.active, key = { it.goalId }) { goal ->
                            GoalCard(
                                goal = goal,
                                onClick = { detailGoalId = goal.goalId }
                            )
                        }
                    }

                    if (state.completed.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Completadas",
                                trailing = "${state.completedCount}"
                            )
                        }
                        items(state.completed, key = { it.goalId }) { goal ->
                            GoalCard(
                                goal = goal,
                                onClick = { detailGoalId = goal.goalId }
                            )
                        }
                    }
                }
            }
        }

        ScrollAwareFab(
            visible = fabVisible,
            description = "Nueva meta",
            onClick = { editor = GoalDraft() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Spacing.page, bottom = Spacing.lg)
        )
    }

    detailGoal?.let { goal ->
        GoalDetailSheet(
            goal = goal,
            onDismiss = { detailGoalId = null },
            onAddContribution = {
                detailGoalId = null
                contributionGoalId = goal.goalId
            },
            onEdit = {
                detailGoalId = null
                editor = goal.toDraft()
            },
            onDeleteGoal = {
                viewModel.deleteGoal(goal.goalId)
                detailGoalId = null
            },
            onDeleteContribution = { contribution ->
                viewModel.deleteContribution(goal.goalId, contribution)
            }
        )
    }

    editor?.let { draft ->
        GoalEditorSheet(
            initial = draft,
            onDismiss = { editor = null },
            onSave = { updated ->
                viewModel.saveGoal(updated)
                editor = null
            }
        )
    }

    contributionGoal?.let { goal ->
        ContributionSheet(
            goal = goal,
            onDismiss = { contributionGoalId = null },
            onSave = { date, amount ->
                viewModel.addContribution(goal.goalId, date, amount)
                contributionGoalId = null
                detailGoalId = goal.goalId
            }
        )
    }
}

// --------------------------------------------------------------- resumen

@Composable
private fun GoalsSummary(state: MetasUiState) {
    AppCard {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ahorrado para tus metas",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Spacing.xs))
                AnimatedMoney(amount = state.totalSaved, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "de ${Money.cop(state.totalTarget)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (state.completedCount > 0) {
                SoftPill(
                    text = if (state.completedCount == 1) {
                        "1 completada"
                    } else {
                        "${state.completedCount} completadas"
                    },
                    color = AppPalette.Green
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))
        SoftProgressBar(progress = state.progress, color = AppPalette.Indigo)
        Spacer(Modifier.height(Spacing.sm))
        Row {
            Text(
                text = if (state.activeCount == 1) "1 en curso" else "${state.activeCount} en curso",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${Money.percent(state.progress)} ahorrado",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ------------------------------------------------------------------ meta

@Composable
private fun GoalCard(goal: GoalRow, onClick: () -> Unit) {
    AppCard(onClick = onClick, elevated = false) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorDot(color = identityColor(goal.color, goal.name), size = 8.dp)
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = goal.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            GoalStatusPill(goal.status)
        }

        Spacer(Modifier.height(Spacing.sm))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = Money.cop(goal.saved),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = " de ${Money.cop(goal.targetAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 1.dp)
            )
        }

        Spacer(Modifier.height(Spacing.sm))
        SoftProgressBar(
            progress = goal.progress,
            color = goalAccent(goal),
            height = 6.dp
        )
        Spacer(Modifier.height(6.dp))
        Row {
            Text(
                text = "${Money.percent(goal.progress)} ahorrado",
                style = MaterialTheme.typography.labelSmall,
                color = AppPalette.InkFaint
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (goal.completed) {
                    "Meta cumplida"
                } else {
                    "Faltan ${Money.cop(goal.remaining)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(Spacing.xs))
        Row {
            Text(
                text = "Meta: ${targetLabel(goal.targetDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            goalHint(goal)?.let { hint ->
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun GoalStatusPill(status: GoalStatus) {
    SoftPill(
        text = statusLabel(status),
        color = statusColor(status),
        background = statusBackground(status)
    )
}

private fun statusLabel(status: GoalStatus): String = when (status) {
    GoalStatus.COMPLETADA -> "Completada"
    GoalStatus.EN_RITMO -> "En ritmo"
    GoalStatus.ATRASADA -> "Atrasada"
    GoalStatus.VENCIDA -> "Vencida"
    GoalStatus.SIN_RITMO -> "Sin ritmo"
}

/**
 * El estado ya lo dice la etiqueta de arriba; esta línea dice qué hacer.
 * Así la tarjeta no repite «Atrasada» con otras palabras.
 */
private fun goalHint(goal: GoalRow): String? = when (goal.status) {
    GoalStatus.COMPLETADA -> null
    GoalStatus.EN_RITMO -> goal.projectedDate?.let { "Llegas el ${Fechas.corta(it)}" }
    GoalStatus.ATRASADA, GoalStatus.SIN_RITMO ->
        if (goal.requiredMonthly > 0L) "Ahorra ${Money.cop(goal.requiredMonthly)}/mes" else null
    GoalStatus.VENCIDA -> "Ajusta la fecha"
}

private fun statusColor(status: GoalStatus): Color = when (status) {
    GoalStatus.COMPLETADA, GoalStatus.EN_RITMO -> AppPalette.Green
    GoalStatus.ATRASADA -> AppPalette.Amber
    GoalStatus.VENCIDA -> AppPalette.Coral
    GoalStatus.SIN_RITMO -> AppPalette.InkSoft
}

private fun statusBackground(status: GoalStatus): Color = when (status) {
    GoalStatus.COMPLETADA, GoalStatus.EN_RITMO -> AppPalette.GreenSoft
    GoalStatus.ATRASADA -> AppPalette.AmberSoft
    GoalStatus.VENCIDA -> AppPalette.CoralSoft
    GoalStatus.SIN_RITMO -> AppPalette.SurfaceMuted
}

/** La barra conserva el color de la meta; el estado lo comunica la etiqueta. */
private fun goalAccent(goal: GoalRow): Color = when (goal.status) {
    GoalStatus.COMPLETADA -> AppPalette.Green
    else -> identityColor(goal.color, goal.name)
}

// ---------------------------------------------------------------- detalle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalDetailSheet(
    goal: GoalRow,
    onDismiss: () -> Unit,
    onAddContribution: () -> Unit,
    onEdit: () -> Unit,
    onDeleteGoal: () -> Unit,
    onDeleteContribution: (ContributionRow) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var contributionToDelete by remember { mutableStateOf<ContributionRow?>(null) }
    var showDeleteGoal by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = AppPalette.Border)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = Spacing.page)
                .padding(top = Spacing.xs, bottom = Spacing.xxl)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorDot(color = identityColor(goal.color, goal.name), size = 10.dp)
                Spacer(Modifier.width(Spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = statusLabel(goal.status),
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor(goal.status)
                    )
                }
                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = AppPalette.SurfaceMuted
                ) {
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cerrar",
                            tint = AppPalette.InkSoft,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            Text(
                text = "Ahorrado",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = Money.cop(goal.saved),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "de ${Money.cop(goal.targetAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(Spacing.md))
            SoftProgressBar(progress = goal.progress, color = goalAccent(goal))
            Spacer(Modifier.height(Spacing.sm))
            Row {
                Text(
                    text = "${Money.percent(goal.progress)} ahorrado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (goal.completed) {
                        "Meta cumplida"
                    } else {
                        "Faltan ${Money.cop(goal.remaining)}"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (goal.completed) {
                        AppPalette.Green
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            Spacer(Modifier.height(Spacing.lg))

            TintedCard(color = AppPalette.SurfaceMuted) {
                DetailRow(label = "Fecha objetivo", value = longTargetLabel(goal.targetDate))
                Spacer(Modifier.height(Spacing.sm))
                DetailRow(
                    label = "Ritmo de los últimos 3 meses",
                    value = if (goal.recentMonthly > 0L) {
                        "${Money.cop(goal.recentMonthly)}/mes"
                    } else {
                        "Sin aportes"
                    }
                )
                Spacer(Modifier.height(Spacing.sm))
                DetailRow(
                    label = "Necesitas",
                    value = when {
                        goal.completed -> "—"
                        goal.status == GoalStatus.VENCIDA -> "Fecha vencida"
                        else -> "${Money.cop(goal.requiredMonthly)}/mes"
                    },
                    valueColor = when {
                        goal.status == GoalStatus.VENCIDA -> AppPalette.Coral
                        goal.status == GoalStatus.ATRASADA -> AppPalette.Amber
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                if (!goal.completed) {
                    Spacer(Modifier.height(Spacing.sm))
                    DetailRow(
                        label = "A este ritmo",
                        value = goal.projectedDate?.let {
                            Fechas.mesDe(it).lowercase()
                        } ?: "Sin ritmo de ahorro"
                    )
                }
            }

            Spacer(Modifier.height(Spacing.lg))

            if (!goal.completed) {
                Button(
                    onClick = onAddContribution,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppPalette.Indigo,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        text = "Aportar a esta meta",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            TextButton(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    tint = AppPalette.Indigo,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    text = "Editar meta",
                    style = MaterialTheme.typography.labelLarge,
                    color = AppPalette.Indigo
                )
            }

            Spacer(Modifier.height(Spacing.md))

            SectionHeader(
                title = "Aportes",
                trailing = if (goal.contributions.isEmpty()) null else "${goal.contributions.size}"
            )
            Spacer(Modifier.height(Spacing.sm))

            if (goal.contributions.isEmpty()) {
                Text(
                    text = "Todavía no hay aportes. Cada aporte que registres suma de " +
                        "inmediato al avance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                AppCard(contentPadding = PaddingValues(0.dp)) {
                    goal.contributions.forEachIndexed { index, contribution ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = Spacing.lg),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = Spacing.lg, top = Spacing.xs, bottom = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = Fechas.diaYMes(contribution.date),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = Money.cop(contribution.amount),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = { contributionToDelete = contribution }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Eliminar aporte",
                                    tint = AppPalette.InkFaint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.lg))

            TextButton(
                onClick = { showDeleteGoal = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = AppPalette.Coral)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    text = "Eliminar meta",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    contributionToDelete?.let { contribution ->
        AlertDialog(
            onDismissRequest = { contributionToDelete = null },
            title = { Text("¿Eliminar este aporte?") },
            text = {
                Text(
                    "Se restarán ${Money.cop(contribution.amount)} del avance de la meta. " +
                        "La operación queda en el historial de la app."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteContribution(contribution)
                        contributionToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppPalette.Coral)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { contributionToDelete = null }) {
                    Text("Cancelar")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showDeleteGoal) {
        AlertDialog(
            onDismissRequest = { showDeleteGoal = false },
            title = { Text("¿Eliminar esta meta?") },
            text = {
                Text(
                    "Se eliminarán la meta y todos sus aportes. El historial de la app " +
                        "conserva el registro de lo que se borró."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteGoal = false
                        onDeleteGoal()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppPalette.Coral)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGoal = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = valueColor
        )
    }
}

// ---------------------------------------------------------------- editores

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalEditorSheet(
    initial: GoalDraft,
    onDismiss: () -> Unit,
    onSave: (GoalDraft) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(initial) { mutableStateOf(initial.name) }
    var amountText by remember(initial) {
        mutableStateOf(if (initial.targetAmount > 0L) initial.targetAmount.toString() else "")
    }
    var targetDate by remember(initial) { mutableStateOf(initial.targetDate) }
    var color by remember(initial) { mutableStateOf(initial.color) }
    var showDatePicker by remember { mutableStateOf(false) }

    val amount = amountText.toLongOrNull() ?: 0L
    val canSave = name.isNotBlank() && amount > 0L
    val willComplete = initial.id != null && initial.saved >= amount && amount > 0L

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = AppPalette.Border)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = Spacing.page)
                .padding(top = Spacing.xs, bottom = Spacing.xxl)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (initial.id == null) "Nueva meta" else "Editar meta",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "El avance se calcula con tus aportes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = AppPalette.SurfaceMuted
                ) {
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cerrar",
                            tint = AppPalette.InkSoft,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.lg))

            FieldLabel("Nombre de la meta")
            TextField(
                value = name,
                onValueChange = { name = it.take(60) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Ej. Viaje a Cartagena",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = metaFieldColors()
            )

            Spacer(Modifier.height(Spacing.md))

            FieldLabel("Cuánto necesitas")
            TextField(
                value = amountText,
                onValueChange = { value ->
                    amountText = value.filter { it.isDigit() }.take(12)
                },
                modifier = Modifier.fillMaxWidth(),
                prefix = {
                    Text(
                        text = "\$",
                        style = MaterialTheme.typography.headlineSmall,
                        color = AppPalette.InkFaint
                    )
                },
                placeholder = {
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.headlineSmall,
                        color = AppPalette.InkFaint
                    )
                },
                textStyle = MaterialTheme.typography.headlineSmall,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.medium,
                colors = metaFieldColors()
            )

            if (initial.id != null && initial.saved > 0L) {
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "Ya tienes ${Money.cop(initial.saved)} ahorrados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(Spacing.md))

            FieldLabel("Fecha objetivo")
            DateField(
                date = targetDate,
                onClick = { showDatePicker = true }
            )

            Spacer(Modifier.height(Spacing.md))

            FieldLabel("Color")
            ColorPicker(
                selected = color,
                onSelect = { color = it }
            )

            if (willComplete) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = "Tu ahorro actual ya supera este objetivo; la meta quedará " +
                        "completada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.Green
                )
            }

            Spacer(Modifier.height(Spacing.lg))

            Button(
                onClick = {
                    onSave(
                        GoalDraft(
                            id = initial.id,
                            name = name.trim(),
                            targetAmount = amount,
                            targetDate = targetDate,
                            color = color,
                            saved = initial.saved,
                            completed = initial.completed
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = canSave,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppPalette.Indigo,
                    contentColor = Color.White,
                    disabledContainerColor = AppPalette.SurfaceStrong,
                    disabledContentColor = AppPalette.InkFaint
                )
            ) {
                Text(
                    text = if (initial.id == null) "Crear meta" else "Guardar cambios",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    if (showDatePicker) {
        DatePickerModal(
            initial = targetDate,
            onDismiss = { showDatePicker = false },
            onSelect = {
                targetDate = it
                showDatePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContributionSheet(
    goal: GoalRow,
    onDismiss: () -> Unit,
    onSave: (LocalDate, Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Fechas.hoy()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val amount = amountText.toLongOrNull() ?: 0L
    val futureDate = date.isAfter(Fechas.hoy())
    val canSave = amount > 0L && !futureDate

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = AppPalette.Border)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = Spacing.page)
                .padding(top = Spacing.xs, bottom = Spacing.xxl)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Aportar a ${goal.name}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Llevas ${Money.cop(goal.saved)} de " +
                            "${Money.cop(goal.targetAmount)}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = AppPalette.SurfaceMuted
                ) {
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cerrar",
                            tint = AppPalette.InkSoft,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.lg))

            FieldLabel("Monto del aporte")
            TextField(
                value = amountText,
                onValueChange = { value ->
                    amountText = value.filter { it.isDigit() }.take(12)
                },
                modifier = Modifier.fillMaxWidth(),
                prefix = {
                    Text(
                        text = "\$",
                        style = MaterialTheme.typography.headlineSmall,
                        color = AppPalette.InkFaint
                    )
                },
                placeholder = {
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.headlineSmall,
                        color = AppPalette.InkFaint
                    )
                },
                textStyle = MaterialTheme.typography.headlineSmall,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.medium,
                colors = metaFieldColors()
            )

            Spacer(Modifier.height(Spacing.md))

            FieldLabel("Fecha del aporte")
            DateField(
                date = date,
                onClick = { showDatePicker = true }
            )

            if (futureDate) {
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "La fecha del aporte no puede ser futura.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.Coral
                )
            }

            Spacer(Modifier.height(Spacing.lg))

            Button(
                onClick = { onSave(date, amount) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = canSave,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppPalette.Indigo,
                    contentColor = Color.White,
                    disabledContainerColor = AppPalette.SurfaceStrong,
                    disabledContentColor = AppPalette.InkFaint
                )
            ) {
                Text(
                    text = "Guardar aporte",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    if (showDatePicker) {
        DatePickerModal(
            initial = date,
            onDismiss = { showDatePicker = false },
            onSelect = {
                date = it
                showDatePicker = false
            }
        )
    }
}

// --------------------------------------------------------------- helpers

@Composable
private fun DateField(date: LocalDate, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = AppPalette.SurfaceMuted
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.DateRange,
                contentDescription = null,
                tint = AppPalette.InkSoft,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(Spacing.md))
            Text(
                text = Fechas.fechaLarga(date),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Cambiar",
                style = MaterialTheme.typography.labelMedium,
                color = AppPalette.Indigo
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onSelect: (LocalDate) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onSelect(
                            Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        )
                    } ?: onDismiss()
                }
            ) {
                Text("Listo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            showModeToggle = false
        )
    }
}

@Composable
private fun ColorPicker(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        GoalColors.forEach { hex ->
            val isSelected = hex.equals(selected, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onSelect(hex) }
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) AppPalette.Ink else AppPalette.Border,
                        shape = CircleShape
                    )
                    .padding(5.dp)
                    .clip(CircleShape)
                    .background(hex.asColor())
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = Spacing.xs)
    )
}

@Composable
private fun metaFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = AppPalette.SurfaceMuted,
    unfocusedContainerColor = AppPalette.SurfaceMuted,
    disabledContainerColor = AppPalette.SurfaceMuted,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    cursorColor = AppPalette.Indigo,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
)

private fun targetLabel(date: LocalDate): String = "${Fechas.corta(date)} ${date.year}"

private fun longTargetLabel(date: LocalDate): String = "${Fechas.diaYMes(date)} de ${date.year}"

private fun GoalRow.toDraft(): GoalDraft = GoalDraft(
    id = goalId,
    name = name,
    targetAmount = targetAmount,
    targetDate = targetDate,
    color = color,
    saved = saved,
    completed = completed
)
