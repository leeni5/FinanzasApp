package com.finanzasapp.ui.screens.inicio

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.geometry.Offset
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
import com.finanzasapp.ui.components.TintedCard
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.Spacing
import com.finanzasapp.ui.theme.asColor
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
        eyebrow = "Tu resumen",
        title = Fechas.saludo(),
        subtitle = Fechas.fechaLarga(),
        accent = AppPalette.Indigo
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

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            MonthTile(
                label = "Ingresos",
                amount = state.monthIncome,
                accent = AppPalette.Green,
                soft = AppPalette.GreenSoft,
                deep = Color(0xFF1D7346),
                modifier = Modifier.weight(1f)
            )
            MonthTile(
                label = "Gastos",
                amount = state.monthExpenses,
                accent = AppPalette.Coral,
                soft = AppPalette.CoralSoft,
                deep = Color(0xFFA8432C),
                modifier = Modifier.weight(1f)
            )
        }

        SectionHeader(
            title = "Dónde se fue el dinero",
            trailing = Fechas.mesActual()
        )
        SpendingCard(categories = state.categories)

        SectionHeader(
            title = "Próximos pagos",
            accent = AppPalette.Amber
        )
        PaymentsCard(payments = state.payments)

        SectionHeader(
            title = "Metas",
            accent = AppPalette.Violet
        )
        GoalsCard(goals = state.goals)

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
            .clip(RoundedCornerShape(28.dp))
            .background(AppPalette.HeroGradient)
    ) {
        // Dos círculos casi transparentes: dan profundidad sin ruido.
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = size.minDimension * 0.55f,
                center = Offset(size.width * 0.94f, size.height * 0.02f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = size.minDimension * 0.40f,
                center = Offset(size.width * 0.05f, size.height * 1.02f)
            )
        }

        Column(modifier = Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Saldo disponible",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Spacer(Modifier.weight(1f))
                SoftPill(
                    text = "COP",
                    color = Color.White,
                    background = Color.White.copy(alpha = 0.16f)
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = Money.cop(balance),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White
            )
            Text(
                text = "Ahorro, débito y efectivo",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.62f)
            )

            Spacer(Modifier.height(Spacing.lg))
            HorizontalDivider(color = Color.White.copy(alpha = 0.16f))
            Spacer(Modifier.height(Spacing.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Deuda en tarjetas",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (cardDebt > 0L) Money.cop(cardDebt) else "Sin deuda",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (cardDebt > 0L) Color(0xFFFFC7B8) else Color(0xFFA9F0C6)
                )
            }
        }
    }
}

@Composable
private fun MonthTile(
    label: String,
    amount: Long,
    accent: Color,
    soft: Color,
    deep: Color,
    modifier: Modifier = Modifier
) {
    TintedCard(
        modifier = modifier,
        color = soft,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorDot(color = accent, size = 8.dp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = deep.copy(alpha = 0.8f)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = Money.cop(amount),
            style = MaterialTheme.typography.titleLarge,
            color = deep
        )
        Text(
            text = "este mes",
            style = MaterialTheme.typography.bodySmall,
            color = deep.copy(alpha = 0.62f)
        )
    }
}

// ------------------------------------------------------------- categorías

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
                Spacer(Modifier.height(Spacing.md))
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
    val color = item.color.asColor(AppPalette.Indigo)
    val progress = if (item.budget > 0L) {
        item.spent.toFloat() / item.budget
    } else {
        item.spent.toFloat() / maxSpent
    }
    val overBudget = item.budget > 0L && item.spent > item.budget
    val barColor = if (overBudget) AppPalette.Coral else color

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorDot(color = color, size = 9.dp)
            Spacer(Modifier.width(Spacing.sm))
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
            InlineEmpty(text = "No tienes pagos programados. Cuando agregues uno, " +
                "aquí aparecerá con su fecha y su aviso.")
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
    val accent = if (overdue) AppPalette.Coral else AppPalette.Amber
    val soft = if (overdue) AppPalette.CoralSoft else AppPalette.AmberSoft

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier
                .width(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(soft)
                .padding(vertical = Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = payment.nextDueDate.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = accent
            )
            Text(
                text = Fechas.mesCorto(payment.nextDueDate),
                style = MaterialTheme.typography.labelSmall,
                color = accent.copy(alpha = 0.85f)
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
            InlineEmpty(text = "Todavía no hay metas. En la fase 3 podrás crearlas " +
                "y ver cómo crece cada aporte.")
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
    val color = goal.color.asColor(AppPalette.Violet)
    val progress = (goal.saved.toFloat() / goal.targetAmount.coerceAtLeast(1L))
        .coerceIn(0f, 1f)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorDot(color = color, size = 9.dp)
            Spacer(Modifier.width(Spacing.sm))
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
                    color = color
                )
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        SoftProgressBar(progress = progress, color = color)
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
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppPalette.AmberSoft)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = AppPalette.Amber,
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
