package com.finanzasapp.ui.screens.inicio

import android.content.pm.ApplicationInfo
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.finanzasapp.ui.components.ColorDot
import com.finanzasapp.ui.components.PageScaffold
import com.finanzasapp.ui.components.SectionHeader
import com.finanzasapp.ui.components.SoftPill
import com.finanzasapp.ui.components.SoftProgressBar
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.Spacing
import com.finanzasapp.ui.util.Fechas
import com.finanzasapp.ui.util.Money

@Composable
fun InicioScreen() {
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
                text = "Cargando tus datos…",
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

        Section(
            title = "Dónde se fue el dinero",
            trailing = Fechas.mesActual()
        ) {
            SpendingCard(categories = state.categories)
        }

        Section(title = "Próximos pagos") {
            PaymentsCard(payments = state.payments)
        }

        Section(title = "Metas") {
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AppPalette.Hero)
    ) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            Text(
                text = "Saldo disponible",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.64f)
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = Money.cop(balance),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "Ahorro, débito y efectivo",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.48f)
            )

            Spacer(Modifier.height(Spacing.lg))
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            Spacer(Modifier.height(Spacing.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Deuda en tarjetas",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.58f)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (cardDebt > 0L) Money.cop(cardDebt) else "Sin deuda",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (cardDebt > 0L) Color(0xFFFFB4A3) else Color(0xFF9BE3B8)
                )
            }
        }
    }
}

@Composable
private fun MonthSummary(income: Long, expenses: Long) {
    AppCard {
        Text(
            text = "Este mes · ${Fechas.mesActual()}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SummaryColumn(
                label = "Ingresos",
                amount = income,
                dot = AppPalette.Green,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(34.dp)
                    .background(MaterialTheme.colorScheme.outline)
            )
            Spacer(Modifier.width(Spacing.lg))
            SummaryColumn(
                label = "Gastos",
                amount = expenses,
                dot = AppPalette.Coral,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryColumn(
    label: String,
    amount: Long,
    dot: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorDot(color = dot, size = 7.dp)
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = Money.cop(amount),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ------------------------------------------------------------- categorías

@Composable
private fun Section(
    title: String,
    trailing: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionHeader(title = title, trailing = trailing)
        content()
    }
}

@Composable
private fun SpendingCard(categories: List<CategorySpending>) {
    val top = categories.take(4)

    AppCard {
        if (top.isEmpty()) {
            InlineEmpty(
                text = "Todavía no hay gastos este mes. Cuando registres el primero, " +
                    "aquí verás en qué se va el dinero."
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
                text = "+ ${categories.size - top.size} categorías más",
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
    val barColor = if (overBudget) AppPalette.Coral else AppPalette.Indigo

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                    "${Money.percent(progress)} del presupuesto"
                } else {
                    "Sin presupuesto"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            if (item.budget > 0L) {
                Text(
                    text = if (overBudget) {
                        "${Money.cop(item.spent - item.budget)} de más"
                    } else {
                        "${Money.cop(item.budget - item.spent)} disponibles"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overBudget) AppPalette.Coral else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ------------------------------------------------------------------ pagos

@Composable
private fun PaymentsCard(payments: List<ScheduledPayment>) {
    val next = payments.take(3)

    AppCard {
        if (next.isEmpty()) {
            InlineEmpty(
                text = "No tienes pagos programados. Cuando agregues uno, " +
                    "aquí aparecerá con su fecha y su aviso."
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

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.width(44.dp)) {
            Text(
                text = payment.nextDueDate.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = if (overdue) AppPalette.Coral else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = Fechas.mesCorto(payment.nextDueDate),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                text = "Todavía no hay metas. En la fase 3 podrás crearlas " +
                    "y ver cómo crece cada aporte."
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
            if (goal.completed) {
                SoftPill(text = "Completada", color = AppPalette.Green)
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
            color = if (goal.completed) AppPalette.Green else AppPalette.Indigo
        )
        Spacer(Modifier.height(6.dp))
        Row {
            Text(
                text = "${Money.cop(goal.saved)} de ${Money.cop(goal.targetAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "Meta: ${Fechas.corta(goal.targetDate)}",
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
            text = "Estás viendo datos de ejemplo. La versión final arranca vacía, " +
                "solo con tus categorías.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
