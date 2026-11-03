package com.finanzasapp.ui.screens.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.finanzasapp.data.db.AppDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Conteos de la base. En la fase 1 esta pantalla se llena con el saldo real,
 * el ritmo de gasto y los próximos pagos.
 */
data class InicioUiState(
    val accounts: Int = 0,
    val categories: Int = 0,
    val transactions: Int = 0,
    val goals: Int = 0,
    val loading: Boolean = true
)

class InicioViewModel(database: AppDatabase) : ViewModel() {

    val state: StateFlow<InicioUiState> = combine(
        database.accountDao().observeCount(),
        database.categoryDao().observeCount(),
        database.transactionDao().observeCount(),
        database.goalDao().observeCount()
    ) { accounts, categories, transactions, goals ->
        InicioUiState(
            accounts = accounts,
            categories = categories,
            transactions = transactions,
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
