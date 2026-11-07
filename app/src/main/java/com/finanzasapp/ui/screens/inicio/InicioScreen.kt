package com.finanzasapp.ui.screens.inicio

import android.content.pm.ApplicationInfo
import androidx.compose.ui.res.stringResource
import com.finanzasapp.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finanzasapp.FinanzasApp
import com.finanzasapp.data.entity.ScheduledPayment
import com.finanzasapp.data.model.CategorySpending
import com.finanzasapp.data.model.GoalProgress
import com.finanzasapp.ui.components.AppCard
import com.finanzasapp.ui.components.AnimatedMoney
import com.finanzasapp.ui.components.CategoryBadge
import com.finanzasapp.ui.components.HeroSurface
import com.finanzasapp.ui.components.ColorDot
import com.finanzasapp.ui.components.TintedCard
import com.finanzasapp.ui.theme.asColor
import com.finanzasapp.ui.theme.identityColor
import com.finanzasapp.ui.theme.soft
import com.finanzasapp.ui.components.PageScaffold
import com.finanzasapp.ui.components.SectionHeader
import com.finanzasapp.ui.components.SoftPill
import com.finanzasapp.ui.components.SoftProgressBar
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.Spacing
import com.finanzasapp.ui.util.Fechas
import com.finanzasapp.ui.util.Money

@Composable
fun InicioScreen(
    onOpenPayments: () -> Unit = {},
    onOpenAccounts: () -> Unit = {},
    onOpenAccount: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val database = remember(context) {
        (context.applicationContext as FinanzasApp).database
    }
    val viewModel: InicioViewModel = viewModel(factory = InicioViewModel.factory(database))
    val state by viewModel.state.collectAsStateWithLifecycle()

    val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    PageScaffold(
        title = Fechas.saludo(),
        subtitle = Fechas.fechaLarga()
    ) {
        if (state.loading) {
            Text(
                text = stringResource(R.string.mv_home_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@PageScaffold
        }

        HeroBalanceCard(
            balance = state.availableBalance,
            cardDebt = state.cardDebt
        )

        MonthSummary(
            income = state.monthIncome,
            expenses = state.monthExpenses
        )

        Section(title = stringResource(R.string.mv_home_your_accounts), trailing = stringResource(R.string.mv_home_see_all), onTrailing = onOpenAccounts) {
            AccountsStrip(onOpenAccount = onOpenAccount, onOpenAccounts = onOpenAccounts)
        }

        Section(
            title = stringResource(R.string.mv_home_where_money),
            trailing = Fechas.mesActual()
        ) {
            SpendingCard(categories = state.categories)
        }

        Section(title = stringResource(R.string.mv_home_upcoming), trailing = stringResource(R.string.mv_home_see_all_m), onTrailing = onOpenPayments) {
            PaymentsCard(payments = state.payments, onClick = onOpenPayments)
        }

        Section(title = stringResource(R.string.mv_home_goals)) {
            GoalsCard(goals = state.goals)
        }

        if (isDebuggable) {
            DebugNote()
        }
    }
}

// ------------------------------------------------------------------ saldo

@Composable
private fun HeroBalanceCard(balance: Long, cardDebt: Long) {
    HeroSurface(contentPadding = PaddingValues(0.dp)) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            Text(
                text = stringResource(R.string.mv_home_available),
                style = MaterialTheme.typography.labelMedium,
                color = AppPalette.HeroInkSoft
            )
            Spacer(Modifier.height(Spacing.sm))
            AnimatedMoney(amount = balance, style = MaterialTheme.typography.displaySmall, color = AppPalette.HeroInk)
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = stringResource(R.string.mv_home_available_detail),
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.HeroInkFaint
            )

            Spacer(Modifier.height(Spacing.lg))
            HorizontalDivider(color = AppPalette.HeroLine)
            Spacer(Modifier.height(Spacing.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.mv_home_card_debt),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.HeroInkSoft
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (cardDebt > 0L) Money.cop(cardDebt) else stringResource(R.string.mv_home_no_debt),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (cardDebt > 0L) AppPalette.HeroDebt else AppPalette.HeroOk
                )
            }
        }
    }
}

/** Ingresos y gastos del mes, cada uno en su color: verde entra, coral sale. */
@Composable
private fun MonthSummary(income: Long, expenses: Long) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = stringResource(R.string.mv_home_this_month, Fechas.mesActual()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            SummaryTile(
                label = stringResource(R.string.mv_income),
                amount = income,
                color = AppPalette.Green,
                modifier = Modifier.weight(1f)
            )
            SummaryTile(
                label = stringResource(R.string.mv_expenses),
                amount = expenses,
                color = AppPalette.Coral,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryTile(
    label: String,
    amount: Long,
    color: Color,
    modifier: Modifier = Modifier
) {
    TintedCard(
        modifier = modifier,
        color = color.soft(0.10f),
        contentPadding = PaddingValues(Spacing.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorDot(color = color, size = 8.dp)
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        AnimatedMoney(amount = amount, style = MaterialTheme.typography.titleLarge, color = color)
    }
}

// ------------------------------------------------------------- categorías

@Composable
private fun Section(
    title: String,
    trailing: String? = null,
    onTrailing: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionHeader(title = title, trailing = trailing, onTrailing = onTrailing)
        content()
    }
}

@Composable
private fun SpendingCard(categories: List<CategorySpending>) {
    val top = categories.take(4)

    AppCard {
        if (top.isEmpty()) {
            InlineEmpty(
                text = stringResource(R.string.mv_home_no_expenses)
            )
            return@AppCard
        }

        val maxSpent = top.maxOf { it.spent }.coerceAtLeast(1L)
        top.forEachIndexed { index, item ->
            if (index > 0) {
                Spacer(Modifier.height(Spacing.lg))
            }
            CategoryRow(item = item, maxSpent = maxSpent)
        }

        if (categories.size > top.size) {
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = stringResource(R.string.mv_home_more_categories, categories.size - top.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryRow(item: CategorySpending, maxSpent: Long) {
    val progress = if (item.budget > 0L) {
        item.spent.toFloat() / item.budget
    } else {
        item.spent.toFloat() / maxSpent
    }
    val overBudget = item.budget > 0L && item.spent > item.budget
    val atLimit = item.budget > 0L && item.spent == item.budget
    val categoryColor = identityColor(item.color, item.name)
    // El color de la categoría le da identidad a cada barra; solo cambia
    // cuando hay algo que vigilar: naranja al llegar al límite, coral al pasarlo.
    val barColor = when {
        overBudget -> AppPalette.Coral
        atLimit -> AppPalette.Limit
        else -> categoryColor
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryBadge(
                categoryName = item.name,
                color = categoryColor,
                iconKey = item.icon,
                iconColor = item.iconColor.takeIf { it.isNotBlank() }?.asColor(),
                size = 36.dp
            )
            Spacer(Modifier.width(Spacing.md))
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = Money.cop(item.spent),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        SoftProgressBar(progress = progress, color = barColor)
        Spacer(Modifier.height(6.dp))
        Row {
            Text(
                text = if (item.budget > 0L) {
                    stringResource(R.string.mv_home_of_budget, Money.percent(progress))
                } else {
                    stringResource(R.string.mv_home_no_budget)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            if (item.budget > 0L) {
                Text(
                    text = when {
                        overBudget -> stringResource(R.string.mv_home_over, Money.cop(item.spent - item.budget))
                        atLimit -> stringResource(R.string.mv_home_limit)
                        else -> stringResource(R.string.mv_home_left, Money.cop(item.budget - item.spent))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        overBudget -> AppPalette.Coral
                        atLimit -> AppPalette.Limit
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

// ------------------------------------------------------------------ pagos

@Composable
private fun PaymentsCard(payments: List<ScheduledPayment>, onClick: () -> Unit) {
    val next = payments.take(3)

    AppCard(onClick = onClick) {
        if (next.isEmpty()) {
            InlineEmpty(
                text = stringResource(R.string.mv_home_no_payments)
            )
            return@AppCard
        }

        next.forEachIndexed { index, payment ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = Spacing.md),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            PaymentRow(payment)
        }
    }
}

@Composable
private fun PaymentRow(payment: ScheduledPayment) {
    val overdue = payment.nextDueDate.isBefore(Fechas.hoy())

    val dateColor = if (overdue) AppPalette.Coral else AppPalette.Accent

    Row(verticalAlignment = Alignment.CenterVertically) {
        // Fecha en un bloque de color: se lee de un vistazo cuál va primero.
        Column(
            modifier = Modifier
                .size(44.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(dateColor.soft()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = payment.nextDueDate.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = dateColor
            )
            Text(
                text = Fechas.mesCorto(payment.nextDueDate),
                style = MaterialTheme.typography.labelSmall,
                color = dateColor
            )
        }
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = payment.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = Fechas.vencimiento(payment.nextDueDate),
                style = MaterialTheme.typography.bodySmall,
                color = if (overdue) AppPalette.Coral else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = Money.cop(payment.amount),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ------------------------------------------------------------------ metas

@Composable
private fun GoalsCard(goals: List<GoalProgress>) {
    val visible = goals.take(2)

    AppCard {
        if (visible.isEmpty()) {
            InlineEmpty(
                text = stringResource(R.string.mv_home_no_goals)
            )
            return@AppCard
        }

        visible.forEachIndexed { index, goal ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = Spacing.md),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            GoalRow(goal)
        }
    }
}

@Composable
private fun GoalRow(goal: GoalProgress) {
    val progress = (goal.saved.toFloat() / goal.targetAmount.coerceAtLeast(1L))
        .coerceIn(0f, 1f)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = goal.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (goal.completed || goal.saved >= goal.targetAmount) {
                SoftPill(text = stringResource(R.string.mv_home_completed), color = AppPalette.Green)
            } else {
                Text(
                    text = Money.percent(progress),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        SoftProgressBar(
            progress = progress,
            color = if (goal.completed) AppPalette.Green else identityColor(goal.color, goal.name)
        )
        Spacer(Modifier.height(6.dp))
        Row {
            Text(
                text = stringResource(R.string.mv_home_saved_of, Money.cop(goal.saved), Money.cop(goal.targetAmount)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.mv_home_goal_date, Fechas.corta(goal.targetDate)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------- detalles

@Composable
private fun InlineEmpty(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun DebugNote() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = stringResource(R.string.mv_home_sample_data),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
