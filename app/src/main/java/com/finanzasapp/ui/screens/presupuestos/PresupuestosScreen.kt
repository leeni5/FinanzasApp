package com.finanzasapp.ui.screens.presupuestos

import com.finanzasapp.R
import androidx.compose.ui.res.stringResource
import com.finanzasapp.data.settings.Idioma
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finanzasapp.FinanzasApp
import com.finanzasapp.ui.components.AppCard
import com.finanzasapp.ui.components.CategoryBadge
import com.finanzasapp.ui.theme.asColor
import com.finanzasapp.ui.theme.identityColor
import com.finanzasapp.ui.components.ColorDot
import com.finanzasapp.ui.components.EmptyState
import com.finanzasapp.ui.components.PageScaffold
import com.finanzasapp.ui.components.SectionHeader
import com.finanzasapp.ui.components.SoftProgressBar
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.Spacing
import com.finanzasapp.ui.util.Fechas
import com.finanzasapp.ui.util.Money
import java.time.YearMonth

@Composable
fun PresupuestosScreen() {
    val context = LocalContext.current
    val database = remember(context) {
        (context.applicationContext as FinanzasApp).database
    }
    val viewModel: PresupuestosViewModel = viewModel(
        factory = PresupuestosViewModel.factory(database)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf<BudgetRow?>(null) }

    PageScaffold(
        title = stringResource(R.string.fz_pres_title),
        subtitle = stringResource(R.string.fz_pres_subtitle)
    ) {
        if (state.loading) {
            Text(
                text = stringResource(R.string.fz_pres_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@PageScaffold
        }

        MonthSelector(
            month = state.month,
            canGoForward = state.canGoForward,
            onPrevious = viewModel::previousMonth,
            onNext = viewModel::nextMonth
        )

        if (state.totalBudget == 0L) {
            NoLimitsCard()
        } else {
            BudgetSummary(state = state)
        }

        if (state.isCurrentMonth && state.atRisk.isNotEmpty()) {
            BudgetAlert(rows = state.atRisk)
        } else if (state.isCurrentMonth && state.totalBudget > 0L) {
            CalmNote()
        }

        if (state.budgeted.isNotEmpty()) {
            Section(title = stringResource(R.string.fz_pres_with_limit)) {
                BudgetList(
                    rows = state.budgeted,
                    onEdit = { editing = it }
                )
            }
        }

        if (state.unbudgeted.isNotEmpty()) {
            Section(title = stringResource(R.string.fz_pres_without_limit)) {
                BudgetList(
                    rows = state.unbudgeted,
                    onEdit = { editing = it }
                )
                Text(
                    text = stringResource(R.string.fz_pres_tap_to_assign),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (state.budgeted.isEmpty() && state.unbudgeted.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.fz_pres_no_categories_title),
                message = stringResource(R.string.fz_pres_no_categories_message)
            )
        }
    }

    editing?.let { row ->
        BudgetEditorSheet(
            row = row,
            month = state.month,
            onDismiss = { editing = null },
            onSave = { amount ->
                viewModel.setBudget(row, amount)
                editing = null
            }
        )
    }
}

// ------------------------------------------------------------------- mes

@Composable
private fun MonthSelector(
    month: YearMonth,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.fz_previous_month),
                tint = AppPalette.InkSoft,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = Fechas.mesDe(month.atDay(1)),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        IconButton(onClick = onNext, enabled = canGoForward) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.fz_next_month),
                tint = if (canGoForward) AppPalette.InkSoft else AppPalette.SurfaceStrong,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// --------------------------------------------------------------- resumen

@Composable
private fun NoLimitsCard() {
    AppCard {
        Text(
            text = stringResource(R.string.fz_pres_no_limits_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = stringResource(R.string.fz_pres_no_limits_message),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BudgetSummary(state: PresupuestosUiState) {
    val over = state.totalSpent > state.totalBudget

    AppCard {
        Text(
            text = if (state.isCurrentMonth) {
                stringResource(R.string.fz_this_month)
            } else {
                Fechas.mesDe(state.month.atDay(1))
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = if (over) stringResource(R.string.fz_pres_exceeded) else stringResource(R.string.fz_pres_available),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (over) Money.cop(state.totalSpent - state.totalBudget) else Money.cop(state.remaining),
            style = MaterialTheme.typography.displaySmall,
            color = if (over) AppPalette.Coral else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.fz_metas_of_amount, Money.cop(state.totalBudget)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(Spacing.md))
        SoftProgressBar(
            progress = state.progress,
            color = if (over) AppPalette.Coral else AppPalette.Accent
        )
        Spacer(Modifier.height(Spacing.sm))
        Row {
            Text(
                text = stringResource(R.string.fz_pres_spent, Money.cop(state.totalSpent)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.fz_pres_percent_used, Money.percent(state.progress)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (state.isCurrentMonth && !over && state.daysLeft > 0) {
            Spacer(Modifier.height(Spacing.md))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(Spacing.md))
            Row {
                Text(
                    text = stringResource(R.string.fz_pres_days_left, state.daysLeft),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.fz_pres_per_day, Money.cop(state.dailyAllowance)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun BudgetAlert(rows: List<BudgetRow>) {
    val exceeded = rows.any { it.status == BudgetStatus.EXCEDIDO }
    val accent = if (exceeded) AppPalette.Coral else AppPalette.Amber
    val background = if (exceeded) AppPalette.CoralSoft else AppPalette.AmberSoft
    val count = rows.size

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = background
    ) {
        Row(modifier = Modifier.padding(Spacing.lg)) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(Spacing.md))
            Column {
                Text(
                    text = if (exceeded) stringResource(R.string.fz_pres_budget_exceeded) else stringResource(R.string.fz_pres_high_pace),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (exceeded) {
                        if (count == 1) {
                            stringResource(R.string.fz_pres_alert_exceeded_one)
                        } else {
                            stringResource(R.string.fz_pres_alert_exceeded_many, count)
                        }
                    } else {
                        if (count == 1) {
                            stringResource(R.string.fz_pres_alert_risk_one)
                        } else {
                            stringResource(R.string.fz_pres_alert_risk_many, count)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CalmNote() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ColorDot(color = AppPalette.Green, size = 7.dp)
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = stringResource(R.string.fz_pres_within_budget),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------------------------------------------------------- lista

@Composable
private fun Section(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionHeader(title = title)
        content()
    }
}

@Composable
private fun BudgetList(
    rows: List<BudgetRow>,
    onEdit: (BudgetRow) -> Unit
) {
    AppCard(contentPadding = PaddingValues(0.dp)) {
        rows.forEachIndexed { index, row ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                    color = MaterialTheme.colorScheme.outline
                )
            }
            BudgetRowItem(row = row, onClick = { onEdit(row) })
        }
    }
}

@Composable
private fun BudgetRowItem(row: BudgetRow, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryBadge(
            categoryName = row.name,
            color = identityColor(row.color, row.name),
            iconKey = row.icon,
            iconColor = row.iconColor.takeIf { it.isNotBlank() }?.asColor(),
            size = 36.dp
        )
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = row.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = Money.cop(row.spent),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (row.budget > 0L) {
                Spacer(Modifier.height(Spacing.sm))
                SoftProgressBar(
                    progress = row.progress,
                    color = barColor(row),
                    height = 6.dp
                )
                Spacer(Modifier.height(6.dp))
                Row {
                    Text(
                        text = statusText(row),
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor(row.status)
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.fz_pres_percent_used, Money.percent(row.progress)),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppPalette.InkFaint
                    )
                }
            } else {
                Spacer(Modifier.height(Spacing.xs))
                Row {
                    Text(
                        text = stringResource(R.string.fz_pres_no_monthly_limit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.fz_pres_assign),
                        style = MaterialTheme.typography.labelMedium,
                        color = AppPalette.Accent
                    )
                }
            }
        }
    }
}

private fun statusText(row: BudgetRow): String = when (row.status) {
    BudgetStatus.EXCEDIDO -> Idioma.text(R.string.fz_pres_over_by, Money.cop(-row.remaining))
    BudgetStatus.RIESGO -> Idioma.text(R.string.fz_pres_high_pace)
    BudgetStatus.COMPLETO -> Idioma.text(R.string.fz_pres_limit_reached)
    BudgetStatus.AL_LIMITE -> Idioma.text(R.string.fz_pres_remaining_available, Money.cop(row.remaining))
    BudgetStatus.NORMAL -> Idioma.text(R.string.fz_pres_remaining_available, Money.cop(row.remaining))
    BudgetStatus.SIN_LIMITE -> Idioma.text(R.string.fz_pres_no_monthly_limit)
}

/**
 * Escala de atención, de menos a más: color de la categoría (todo bien),
 * ámbar (ritmo alto o cerca del límite), naranja (límite alcanzado) y
 * coral (excedido). Así un 100 % nunca pesa menos que un 19 % en riesgo.
 */
@Composable
private fun statusColor(status: BudgetStatus): Color = when (status) {
    BudgetStatus.EXCEDIDO -> AppPalette.Coral
    BudgetStatus.COMPLETO -> AppPalette.Limit
    BudgetStatus.RIESGO, BudgetStatus.AL_LIMITE -> AppPalette.Amber
    BudgetStatus.NORMAL, BudgetStatus.SIN_LIMITE -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun barColor(row: BudgetRow): Color = when (row.status) {
    BudgetStatus.EXCEDIDO -> AppPalette.Coral
    BudgetStatus.COMPLETO -> AppPalette.Limit
    BudgetStatus.RIESGO, BudgetStatus.AL_LIMITE -> AppPalette.Amber
    else -> identityColor(row.color, row.name)
}

// --------------------------------------------------------------- editor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetEditorSheet(
    row: BudgetRow,
    month: YearMonth,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember(row) {
        mutableStateOf(if (row.budget > 0L) row.budget.toString() else "")
    }
    val amount = amountText.toLongOrNull() ?: 0L
    val willExceed = amount > 0L && row.spent > amount

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
                        text = stringResource(R.string.fz_pres_budget_for, row.name),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.fz_pres_applies_all_months),
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
                            contentDescription = stringResource(R.string.fz_close),
                            tint = AppPalette.InkSoft,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.lg))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.fz_pres_spent_in, Fechas.mesDe(month.atDay(1))),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = Money.cop(row.spent),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(Spacing.lg))

            Text(
                text = stringResource(R.string.fz_pres_monthly_limit),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.xs)
            )
            TextField(
                value = amountText,
                onValueChange = { value ->
                    amountText = value.filter { it.isDigit() }.take(12)
                },
                visualTransformation = com.finanzasapp.ui.components.ThousandsTransformation,
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
                colors = budgetFieldColors()
            )

            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = stringResource(R.string.fz_pres_zero_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (willExceed) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = stringResource(R.string.fz_pres_already_over),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.Coral
                )
            }

            Spacer(Modifier.height(Spacing.lg))

            Button(
                onClick = { onSave(amount) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppPalette.Accent,
                    contentColor = AppPalette.OnAccent
                )
            ) {
                Text(
                    text = if (amount == 0L) stringResource(R.string.fz_pres_remove_limit) else stringResource(R.string.fz_pres_save_limit),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun budgetFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = AppPalette.SurfaceMuted,
    unfocusedContainerColor = AppPalette.SurfaceMuted,
    disabledContainerColor = AppPalette.SurfaceMuted,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    cursorColor = AppPalette.Accent,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
)
