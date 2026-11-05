package com.finanzasapp.ui.screens.presupuestos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.withTransaction
import com.finanzasapp.data.db.AppDatabase
import com.finanzasapp.data.entity.AuditLog
import com.finanzasapp.data.entity.Category
import com.finanzasapp.ui.util.Fechas
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant
import java.time.YearMonth

/** Señal visual de una categoría frente a su límite mensual. */
enum class BudgetStatus {
    NORMAL,
    COMPLETO,
    AL_LIMITE,
    RIESGO,
    EXCEDIDO,
    SIN_LIMITE
}

/** Categoría con su límite, su gasto del mes y la lectura del ritmo. */
data class BudgetRow(
    val categoryId: Long,
    val name: String,
    val budget: Long,
    val spent: Long,
    val remaining: Long,
    val progress: Float,
    val status: BudgetStatus
)

data class PresupuestosUiState(
    val month: YearMonth = YearMonth.now(Fechas.Bogota),
    val isCurrentMonth: Boolean = true,
    val canGoForward: Boolean = false,
    val budgeted: List<BudgetRow> = emptyList(),
    val unbudgeted: List<BudgetRow> = emptyList(),
    val totalBudget: Long = 0L,
    val totalSpent: Long = 0L,
    val remaining: Long = 0L,
    val progress: Float = 0f,
    val daysLeft: Int = 0,
    val dailyAllowance: Long = 0L,
    val atRisk: List<BudgetRow> = emptyList(),
    val loading: Boolean = true
)

/**
 * Estado de la pantalla de presupuestos.
 *
 * El límite vive en la categoría y se aplica a todos los meses. La pantalla
 * solo cambia el rango de gasto que se compara contra ese límite.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PresupuestosViewModel(private val database: AppDatabase) : ViewModel() {

    private val today = Fechas.hoy()
    private val currentMonth = YearMonth.from(today)
    private val selectedMonth = MutableStateFlow(currentMonth)

    private val spending = selectedMonth.flatMapLatest { month ->
        database.transactionDao()
            .observeBudgetProgress(month.atDay(1), month.atEndOfMonth())
            .map { rows -> month to rows }
    }

    val state: StateFlow<PresupuestosUiState> = spending
        .map { (month, categories) -> buildState(month, categories) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PresupuestosUiState()
        )

    fun previousMonth() {
        selectedMonth.update { it.minusMonths(1) }
    }

    fun nextMonth() {
        selectedMonth.update { month ->
            if (month.isBefore(currentMonth)) month.plusMonths(1) else month
        }
    }

    fun setBudget(row: BudgetRow, amount: Long) {
        val normalized = amount.coerceAtLeast(0L)
        viewModelScope.launch {
            database.withTransaction {
                val before = database.categoryDao().findById(row.categoryId) ?: return@withTransaction
                if (before.monthlyBudget == normalized) return@withTransaction

                val after = before.copy(monthlyBudget = normalized)
                database.categoryDao().update(after)
                database.systemDao().insertAudit(
                    AuditLog(
                        entity = "category",
                        entityId = after.id,
                        action = "EDITADO",
                        beforeJson = before.toAuditJson(),
                        afterJson = after.toAuditJson(),
                        source = "usuario",
                        at = Instant.now()
                    )
                )
            }
        }
    }

    private fun buildState(
        month: YearMonth,
        categories: List<com.finanzasapp.data.model.CategorySpending>
    ): PresupuestosUiState {
        val isCurrent = month == currentMonth
        val elapsed = when {
            month.isBefore(currentMonth) -> 1f
            isCurrent -> today.dayOfMonth.toFloat() / month.lengthOfMonth()
            else -> 0f
        }

        val rows = categories
            .filterNot { it.name.equals("Ingresos", ignoreCase = true) }
            .map { item ->
                val remaining = item.budget - item.spent
                BudgetRow(
                    categoryId = item.categoryId,
                    name = item.name,
                    budget = item.budget,
                    spent = item.spent,
                    remaining = remaining,
                    progress = if (item.budget > 0L) {
                        item.spent.toFloat() / item.budget
                    } else {
                        0f
                    },
                    status = statusFor(item.spent, item.budget, isCurrent, elapsed)
                )
            }

        val budgeted = rows
            .filter { it.budget > 0L }
            .sortedWith(
                compareByDescending<BudgetRow> { statusWeight(it.status) }
                    .thenByDescending { it.progress }
                    .thenBy { it.name }
            )
        val unbudgeted = rows
            .filter { it.budget <= 0L }
            .sortedWith(compareByDescending<BudgetRow> { it.spent }.thenBy { it.name })

        val totalBudget = budgeted.sumOf { it.budget }
        val totalSpent = budgeted.sumOf { it.spent }
        val remaining = totalBudget - totalSpent
        val daysLeft = if (isCurrent) {
            month.lengthOfMonth() - today.dayOfMonth + 1
        } else {
            0
        }

        return PresupuestosUiState(
            month = month,
            isCurrentMonth = isCurrent,
            canGoForward = month.isBefore(currentMonth),
            budgeted = budgeted,
            unbudgeted = unbudgeted,
            totalBudget = totalBudget,
            totalSpent = totalSpent,
            remaining = remaining,
            progress = if (totalBudget > 0L) totalSpent.toFloat() / totalBudget else 0f,
            daysLeft = daysLeft,
            dailyAllowance = if (daysLeft > 0) {
                remaining.coerceAtLeast(0L) / daysLeft
            } else {
                0L
            },
            atRisk = budgeted.filter {
                it.status == BudgetStatus.EXCEDIDO || it.status == BudgetStatus.RIESGO
            },
            loading = false
        )
    }

    private fun statusFor(
        spent: Long,
        budget: Long,
        isCurrent: Boolean,
        elapsed: Float
    ): BudgetStatus {
        if (budget <= 0L) return BudgetStatus.SIN_LIMITE
        if (spent > budget) return BudgetStatus.EXCEDIDO

        val remaining = budget - spent
        if (remaining == 0L) return BudgetStatus.COMPLETO

        // Solo se juzga el ritmo con al menos 15% del mes transcurrido:
        // antes, un solo gasto grande daría una falsa alarma.
        if (isCurrent && elapsed >= 0.15f && spent > 0L) {
            val projected = spent / elapsed
            if (projected > budget * 1.05f && remaining > budget / 20) {
                return BudgetStatus.RIESGO
            }
        }

        if (remaining <= budget / 10) return BudgetStatus.AL_LIMITE
        return BudgetStatus.NORMAL
    }

    private fun statusWeight(status: BudgetStatus): Int = when (status) {
        BudgetStatus.EXCEDIDO -> 4
        BudgetStatus.RIESGO -> 3
        BudgetStatus.AL_LIMITE -> 2
        BudgetStatus.COMPLETO -> 1
        BudgetStatus.NORMAL, BudgetStatus.SIN_LIMITE -> 0
    }

    private fun Category.toAuditJson(): String = JSONObject()
        .put("nombre", name)
        .put("presupuesto", monthlyBudget)
        .toString()

    companion object {
        fun factory(database: AppDatabase) = viewModelFactory {
            initializer { PresupuestosViewModel(database) }
        }
    }
}
