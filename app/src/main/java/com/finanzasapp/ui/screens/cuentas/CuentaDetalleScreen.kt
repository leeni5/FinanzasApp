package com.finanzasapp.ui.screens.cuentas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finanzasapp.FinanzasApp
import com.finanzasapp.data.entity.AccountType
import com.finanzasapp.data.entity.Transaction
import com.finanzasapp.data.entity.TransactionType
import com.finanzasapp.data.model.displayName
import com.finanzasapp.ui.components.AccountLogo
import com.finanzasapp.ui.components.AnimatedMoney
import com.finanzasapp.ui.components.AppCard
import com.finanzasapp.ui.components.DonutChart
import com.finanzasapp.ui.components.DonutSlice
import com.finanzasapp.ui.components.EmptyState
import com.finanzasapp.ui.components.HeroSurface
import com.finanzasapp.ui.components.IconCatalog
import com.finanzasapp.ui.components.SectionHeader
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.Spacing
import com.finanzasapp.ui.theme.asColor
import com.finanzasapp.ui.theme.readableInk
import com.finanzasapp.ui.theme.readableInkSoft
import com.finanzasapp.ui.theme.soft
import com.finanzasapp.ui.util.Fechas
import com.finanzasapp.ui.util.Money
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class Filtro(val label: String) { TODOS("Todos"), INGRESOS("Ingresos"), GASTOS("Gastos") }
private enum class Rango(val label: String) { MES("Este mes"), TODO("Todo") }

/**
 * Una cuenta por dentro: su saldo, en qué se gasta (torta por categoría) y
 * todos sus movimientos, con filtro Todos / Ingresos / Gastos.
 */
@Composable
fun CuentaDetalleScreen(accountId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val database = remember(context) { (context.applicationContext as FinanzasApp).database }
    val scope = rememberCoroutineScope()

    val accounts by database.accountDao().observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val balances by database.accountDao().observeBalances().collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by database.categoryDao().observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val movements by remember(accountId) { database.transactionDao().observeByAccount(accountId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var filtro by remember { mutableStateOf(Filtro.TODOS) }
    var rango by remember { mutableStateOf(Rango.MES) }
    var editing by remember { mutableStateOf(false) }

    val today = Fechas.hoy()
    val from = if (rango == Rango.MES) today.withDayOfMonth(1) else LocalDate.of(1970, 1, 1)
    val spending by remember(accountId, rango) {
        database.transactionDao().observeAccountSpending(accountId, from, today.plusYears(50))
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val uncategorized by remember(accountId, rango) {
        database.transactionDao().observeAccountUncategorized(accountId, from, today.plusYears(50))
    }.collectAsStateWithLifecycle(initialValue = 0L)

    val account = accounts.firstOrNull { it.id == accountId }
    val balance = balances.firstOrNull { it.accountId == accountId }?.balance ?: account?.initialBalance ?: 0L
    val categoryById = remember(categories) { categories.associateBy { it.id } }

    val inRange = movements.filter { !it.date.isBefore(from) }
    val income = inRange.filter { it.type == TransactionType.INGRESO }.sumOf { it.amount }
    val expenses = inRange.filter { it.type == TransactionType.GASTO }.sumOf { it.amount }
    val shown = movements.filter {
        when (filtro) {
            Filtro.TODOS -> true
            Filtro.INGRESOS -> it.type == TransactionType.INGRESO
            Filtro.GASTOS -> it.type == TransactionType.GASTO
        }
    }
    val groups = shown.groupBy { it.date }.toList()

    val slices = spending.map { DonutSlice(it.name, it.spent, it.color.asColor()) } +
        if (uncategorized > 0) listOf(DonutSlice("Sin categoría", uncategorized, AppPalette.InkFaint)) else emptyList()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.page, end = Spacing.page, top = Spacing.lg, bottom = Spacing.xxxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        item {
            BackTitle(account?.name ?: "Cuenta", onBack) {
                Surface(onClick = { editing = true }, shape = MaterialTheme.shapes.medium, color = AppPalette.SurfaceMuted) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Edit, null, tint = AppPalette.Accent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Editar", style = MaterialTheme.typography.labelLarge, color = AppPalette.Accent)
                    }
                }
            }
            Spacer(Modifier.height(Spacing.md))
        }

        // Saldo de la cuenta.
        item {
            if (account != null) {
                val isCredit = account.type == AccountType.CREDITO
                HeroSurface {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AccountLogo(account.name, account.logo, account.type, account.color, size = 40.dp)
                        Spacer(Modifier.width(Spacing.md))
                        Column {
                            Text(account.type.label(), style = MaterialTheme.typography.labelMedium, color = AppPalette.HeroInkSoft)
                            Text(if (isCredit) "Deuda actual" else "Saldo", style = MaterialTheme.typography.bodySmall, color = AppPalette.HeroInkFaint)
                        }
                    }
                    Spacer(Modifier.height(Spacing.md))
                    AnimatedMoney(
                        amount = if (isCredit) (-balance).coerceAtLeast(0L) else balance,
                        style = MaterialTheme.typography.displaySmall,
                        color = if (isCredit && balance < 0) AppPalette.HeroDebt else AppPalette.HeroInk
                    )
                    if (isCredit && account.creditLimit != null) {
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            "Cupo disponible ${Money.cop((account.creditLimit + balance).coerceAtLeast(0L))} de ${Money.cop(account.creditLimit)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppPalette.HeroInkSoft
                        )
                    }
                }
            }
        }

        // Rango + entradas/salidas + torta.
        item {
            Spacer(Modifier.height(Spacing.sm))
            Pills(Rango.entries.map { it.label }, rango.ordinal) { rango = Rango.entries[it] }
            Spacer(Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Totals("Entró", income, AppPalette.Green, Modifier.weight(1f))
                Totals("Salió", expenses, AppPalette.Coral, Modifier.weight(1f))
            }
        }

        item {
            AppCard {
                Text("¿En qué se gasta?", style = MaterialTheme.typography.titleSmall, color = AppPalette.Ink)
                Spacer(Modifier.height(Spacing.md))
                if (slices.isEmpty()) {
                    Text(
                        if (rango == Rango.MES) "Este mes no hay gastos en esta cuenta." else "Esta cuenta no tiene gastos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.InkSoft
                    )
                } else {
                    DonutChart(slices = slices, centerLabel = "Gastos")
                }
            }
        }

        // Movimientos.
        item {
            SectionHeader("Movimientos", trailing = "${shown.size}", modifier = Modifier.padding(top = Spacing.md))
            Spacer(Modifier.height(Spacing.sm))
            Pills(Filtro.entries.map { it.label }, filtro.ordinal) { filtro = Filtro.entries[it] }
        }

        if (shown.isEmpty()) {
            item {
                EmptyState(
                    title = "Sin movimientos",
                    message = when (filtro) {
                        Filtro.TODOS -> "Todavía no hay movimientos en esta cuenta."
                        Filtro.INGRESOS -> "No hay ingresos en esta cuenta."
                        Filtro.GASTOS -> "No hay gastos en esta cuenta."
                    }
                )
            }
        }

        items(groups, key = { it.first.toEpochDay() }) { (date, rows) ->
            val total = rows.sumOf { if (it.type == TransactionType.INGRESO) it.amount else -it.amount }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Row(Modifier.fillMaxWidth().padding(top = Spacing.sm), verticalAlignment = Alignment.Bottom) {
                    Text(dayLabel(date), style = MaterialTheme.typography.titleSmall, color = AppPalette.Ink)
                    Spacer(Modifier.width(Spacing.sm))
                    Text(Fechas.diaSemana(date), style = MaterialTheme.typography.bodySmall, color = AppPalette.InkFaint, modifier = Modifier.weight(1f))
                    Text(
                        (if (total > 0) "+ " else "") + Money.cop(total),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (total >= 0) AppPalette.Green else AppPalette.InkSoft
                    )
                }
                rows.forEach { MovementCard(it, categoryById[it.categoryId]) }
            }
        }
    }

    if (editing && account != null) {
        AccountEditorSheet(
            initial = account,
            onDismiss = { editing = false },
            onSave = { updated ->
                scope.launch { AccountActions.save(database, updated) }
                editing = false
            },
            onArchive = { a ->
                scope.launch { AccountActions.setArchived(database, a.id, !a.archived) }
                editing = false
                if (!a.archived) onBack()
            }
        )
    }
}

@Composable
private fun MovementCard(transaction: Transaction, category: com.finanzasapp.data.entity.Category?) {
    val bg = category?.color?.asColor() ?: AppPalette.SurfaceMuted
    val ink = if (category != null) bg.readableInk() else AppPalette.Ink
    val inkSoft = if (category != null) bg.readableInkSoft() else AppPalette.InkSoft
    val iconTint = category?.iconColor?.takeIf { it.isNotBlank() }?.asColor() ?: ink
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(bg)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(IconCatalog.forCategory(category?.name, category?.icon), null, tint = iconTint, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(Spacing.lg))
        Column(Modifier.weight(1f)) {
            Text(transaction.displayName(), style = MaterialTheme.typography.titleSmall, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(category?.name ?: "Sin categoría", style = MaterialTheme.typography.bodySmall, color = inkSoft, maxLines = 1)
        }
        Text(
            (if (transaction.type == TransactionType.INGRESO) "+ " else "− ") + Money.cop(transaction.amount),
            style = MaterialTheme.typography.titleSmall,
            color = ink
        )
    }
}

@Composable
private fun Totals(label: String, amount: Long, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    com.finanzasapp.ui.components.TintedCard(
        modifier = modifier,
        color = color.soft(0.10f),
        contentPadding = PaddingValues(Spacing.md)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = AppPalette.InkSoft)
        Text(Money.cop(amount), style = MaterialTheme.typography.titleMedium, color = color, maxLines = 1)
    }
}

@Composable
private fun Pills(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selected
            Surface(onClick = { onSelect(index) }, shape = MaterialTheme.shapes.small, color = if (isSelected) AppPalette.Accent else AppPalette.SurfaceMuted) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) AppPalette.OnAccent else AppPalette.InkSoft,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

private fun dayLabel(date: LocalDate): String {
    val today = Fechas.hoy()
    return when (date) {
        today -> "Hoy"
        today.minusDays(1) -> "Ayer"
        else -> Fechas.diaYMes(date)
    }
}
