package com.finanzasapp.ui.screens.movimientos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.withTransaction
import com.finanzasapp.data.db.AppDatabase
import com.finanzasapp.data.entity.Account
import com.finanzasapp.data.entity.AuditLog
import com.finanzasapp.data.entity.Category
import com.finanzasapp.data.entity.Transaction
import com.finanzasapp.data.entity.TransactionSource
import com.finanzasapp.data.entity.TransactionType
import com.finanzasapp.data.model.displayName
import com.finanzasapp.domain.dedupe.UniqueHash
import com.finanzasapp.ui.util.Fechas
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/** Los tres filtros visibles de la lista. */
enum class MovementFilter(val label: String) {
    TODOS("Todos"),
    GASTOS("Gastos"),
    INGRESOS("Ingresos")
}

/**
 * Datos mínimos para crear o editar un movimiento.
 * Es un borrador de UI: no toca la base hasta que el usuario guarda.
 */
data class MovementDraft(
    val id: Long? = null,
    val type: TransactionType = TransactionType.GASTO,
    val amount: Long = 0L,
    val description: String = "",
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val date: LocalDate = Fechas.hoy()
)

/** Un movimiento ya cruzado con su categoría y su cuenta. */
data class MovementRow(
    val transaction: Transaction,
    val category: Category?,
    val account: Account?
)

/** Movimientos de un mismo día, para agrupar la lista sin perder contexto. */
data class MovementGroup(
    val date: LocalDate,
    val rows: List<MovementRow>
)

data class MovimientosUiState(
    val month: YearMonth = YearMonth.now(Fechas.Bogota),
    val isCurrentMonth: Boolean = true,
    val canGoForward: Boolean = false,
    val query: String = "",
    val filter: MovementFilter = MovementFilter.TODOS,
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val groups: List<MovementGroup> = emptyList(),
    val income: Long = 0L,
    val expenses: Long = 0L,
    val monthCount: Int = 0,
    val visibleCount: Int = 0,
    val loading: Boolean = true
)

/**
 * Estado y escritura de la pantalla de Movimientos.
 *
 * La lista se calcula en memoria a partir del mes observado por Room:
 * con los volúmenes de una app personal eso alcanza y permite buscar por
 * descripción, categoría o cuenta sin mantener tres consultas distintas.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MovimientosViewModel(private val database: AppDatabase) : ViewModel() {

    private val today = Fechas.hoy()
    private val currentMonth = YearMonth.from(today)

    private val selectedMonth = MutableStateFlow(currentMonth)
    private val searchQuery = MutableStateFlow("")
    private val activeFilter = MutableStateFlow(MovementFilter.TODOS)

    private val monthTransactions = selectedMonth.flatMapLatest { month ->
        database.transactionDao()
            .observeBetween(month.atDay(1), month.atEndOfMonth())
            .map { transactions -> month to transactions }
    }

    val state: StateFlow<MovimientosUiState> = combine(
        monthTransactions,
        database.categoryDao().observeAll(),
        database.accountDao().observeAll(),
        searchQuery,
        activeFilter
    ) { (month, transactions), categories, accounts, query, filter ->
        val categoriesById = categories.associateBy { it.id }
        val accountsById = accounts.associateBy { it.id }
        val needle = UniqueHash.normalize(query)

        val rows = transactions.map { transaction ->
            MovementRow(
                transaction = transaction,
                category = transaction.categoryId?.let(categoriesById::get),
                account = accountsById[transaction.accountId]
            )
        }

        val filtered = rows.filter { row ->
            row.matchesFilter(filter) && row.matchesSearch(needle)
        }

        MovimientosUiState(
            month = month,
            isCurrentMonth = month == currentMonth,
            canGoForward = month.isBefore(currentMonth),
            query = query,
            filter = filter,
            categories = categories,
            accounts = accounts,
            groups = filtered
                .groupBy { it.transaction.date }
                .map { (date, dayRows) -> MovementGroup(date = date, rows = dayRows) }
                .sortedByDescending { it.date },
            income = rows.sumOf { row ->
                if (row.transaction.type == TransactionType.INGRESO) row.transaction.amount else 0L
            },
            expenses = rows.sumOf { row ->
                if (row.transaction.type == TransactionType.GASTO) row.transaction.amount else 0L
            },
            monthCount = rows.size,
            visibleCount = filtered.size,
            loading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MovimientosUiState()
    )

    fun previousMonth() {
        selectedMonth.update { it.minusMonths(1) }
    }

    fun nextMonth() {
        selectedMonth.update { month ->
            if (month.isBefore(currentMonth)) month.plusMonths(1) else month
        }
    }

    fun goToCurrentMonth() {
        selectedMonth.value = currentMonth
    }

    fun setQuery(value: String) {
        searchQuery.value = value
    }

    fun setFilter(value: MovementFilter) {
        activeFilter.value = value
    }

    fun save(draft: MovementDraft) {
        val description = draft.description.trim()
        val accountId = draft.accountId ?: return
        if (description.isEmpty() || draft.amount <= 0L) return

        viewModelScope.launch {
            val now = Instant.now()
            val hash = UniqueHash.of(accountId, draft.date, draft.amount, description)
            val transactionDao = database.transactionDao()

            database.withTransaction {
                if (draft.id == null) {
                    val transaction = Transaction(
                        accountId = accountId,
                        date = draft.date,
                        descriptionOriginal = description,
                        merchant = description,
                        amount = draft.amount,
                        type = draft.type,
                        categoryId = draft.categoryId,
                        uniqueHash = hash,
                        source = TransactionSource.MANUAL,
                        createdAt = now,
                        updatedAt = now
                    )
                    val id = transactionDao.insert(transaction)
                    database.systemDao().insertAudit(
                        AuditLog(
                            entity = "transaction",
                            entityId = id,
                            action = "CREADO",
                            afterJson = transaction.copy(id = id).toAuditJson(),
                            source = "usuario",
                            at = now
                        )
                    )
                } else {
                    val before = transactionDao.findById(draft.id) ?: return@withTransaction
                    val after = before.copy(
                        accountId = accountId,
                        date = draft.date,
                        merchant = description,
                        amount = draft.amount,
                        type = draft.type,
                        categoryId = draft.categoryId,
                        uniqueHash = hash,
                        updatedAt = now
                    )
                    transactionDao.update(after)
                    database.systemDao().insertAudit(
                        AuditLog(
                            entity = "transaction",
                            entityId = after.id,
                            action = "EDITADO",
                            beforeJson = before.toAuditJson(),
                            afterJson = after.toAuditJson(),
                            source = "usuario",
                            at = now
                        )
                    )
                }
            }

            // Si se guardó en otro mes, se muestra ese mes para que el
            // movimiento no parezca desaparecer. Nunca salta al futuro.
            val targetMonth = YearMonth.from(draft.date)
            if (!targetMonth.isAfter(currentMonth)) {
                selectedMonth.value = targetMonth
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            database.withTransaction {
                val before = database.transactionDao().findById(id) ?: return@withTransaction
                database.transactionDao().deleteById(id)
                database.systemDao().insertAudit(
                    AuditLog(
                        entity = "transaction",
                        entityId = id,
                        action = "BORRADO",
                        beforeJson = before.toAuditJson(),
                        source = "usuario",
                        at = Instant.now()
                    )
                )
            }
        }
    }

    private fun MovementRow.matchesFilter(filter: MovementFilter): Boolean = when (filter) {
        MovementFilter.TODOS -> true
        MovementFilter.GASTOS -> transaction.type == TransactionType.GASTO
        MovementFilter.INGRESOS -> transaction.type == TransactionType.INGRESO
    }

    private fun MovementRow.matchesSearch(needle: String): Boolean {
        if (needle.isEmpty()) return true
        val haystack = buildString {
            append(transaction.displayName())
            append(' ')
            append(category?.name.orEmpty())
            append(' ')
            append(account?.name.orEmpty())
            append(' ')
            append(transaction.notes.orEmpty())
        }
        return UniqueHash.normalize(haystack).contains(needle)
    }

    private fun Transaction.toAuditJson(): String = JSONObject()
        .put("cuenta", accountId)
        .put("fecha", date.toString())
        .put("descripcion", displayName())
        .put("monto", amount)
        .put("tipo", type.name)
        .put("categoria", categoryId ?: JSONObject.NULL)
        .toString()

    companion object {
        fun factory(database: AppDatabase) = viewModelFactory {
            initializer { MovimientosViewModel(database) }
        }
    }
}
