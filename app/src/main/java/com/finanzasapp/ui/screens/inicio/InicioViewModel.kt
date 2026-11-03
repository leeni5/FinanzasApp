package com.finanzasapp.ui.screens.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.finanzasapp.data.db.AppDatabase
import com.finanzasapp.data.entity.AccountType
import com.finanzasapp.data.entity.ScheduledPayment
import com.finanzasapp.data.model.CategorySpending
import com.finanzasapp.data.model.GoalProgress
import com.finanzasapp.ui.util.Fechas
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Todo lo que Inicio necesita, en un solo estado.
 * La UI no consulta la base: recibe cifras ya calculadas por Room.
 */
data class InicioUiState(
    val availableBalance: Long = 0L,
    val cardDebt: Long = 0L,
    val monthIncome: Long = 0L,
    val monthExpenses: Long = 0L,
    val categories: List<CategorySpending> = emptyList(),
    val payments: List<ScheduledPayment> = emptyList(),
    val goals: List<GoalProgress> = emptyList(),
    val loading: Boolean = true
)

class InicioViewModel(database: AppDatabase) : ViewModel() {

    private val today = Fechas.hoy()
    private val monthStart = today.withDayOfMonth(1)

    val state: StateFlow<InicioUiState> = combine(
        database.accountDao().observeBalances(),
        database.transactionDao().observeMonthTotals(monthStart, today),
        database.transactionDao().observeCategorySpending(monthStart, today),
        database.paymentDao().observeUpcoming(),
        database.goalDao().observeProgress()
    ) { balances, totals, categories, payments, goals ->
        val available = balances
            .filter { it.type != AccountType.CREDITO }
            .sumOf { it.balance }

        // En una tarjeta el saldo negativo es la deuda. Nunca se muestra
        // "saldo" de una tarjeta: se muestra lo que se debe.
        val debt = balances
            .filter { it.type == AccountType.CREDITO }
            .sumOf { -it.balance }
            .coerceAtLeast(0L)

        InicioUiState(
            availableBalance = available,
            cardDebt = debt,
            monthIncome = totals.income,
            monthExpenses = totals.expenses,
            categories = categories,
            payments = payments,
            goals = goals,
            loading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InicioUiState()
    )

    companion object {
        fun factory(database: AppDatabase) = viewModelFactory {
            initializer { InicioViewModel(database) }
        }
    }
}
