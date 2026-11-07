package com.finanzasapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.finanzasapp.data.entity.Transaction
import com.finanzasapp.data.model.CategorySpending
import com.finanzasapp.data.model.MonthTotals
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Insert
    suspend fun insertAll(transactions: List<Transaction>): List<Long>

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :from AND :to ORDER BY date DESC, id DESC")
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE requiresReview = 1 ORDER BY date DESC, id DESC")
    fun observePendingReview(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun findById(id: Long): Transaction?

    /** Las dos patas de una transferencia. */
    @Query("SELECT * FROM transactions WHERE transferGroupId = :groupId ORDER BY transferOut DESC")
    suspend fun findTransferLegs(groupId: String): List<Transaction>

    @Query("SELECT COUNT(*) FROM transactions WHERE requiresReview = 1")
    fun observePendingReviewCount(): Flow<Int>

    /** Gastos e ingresos sin categoría: candidatos para una regla nueva. */
    @Query("SELECT * FROM transactions WHERE categoryId IS NULL AND type != 'TRANSFERENCIA'")
    suspend fun uncategorized(): List<Transaction>

    /** Todo, de lo más antiguo a lo más reciente, para exportar. */
    @Query("SELECT * FROM transactions ORDER BY date ASC, id ASC")
    suspend fun all(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE uniqueHash = :hash LIMIT 1")
    suspend fun findByHash(hash: String): Transaction?

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE uniqueHash = :hash)")
    suspend fun existsByHash(hash: String): Boolean

    /** Todas las huellas, para detectar duplicados sin traer los movimientos completos. */
    @Query("SELECT uniqueHash FROM transactions")
    suspend fun allHashes(): List<String>

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE type = 'GASTO' AND date BETWEEN :from AND :to"
    )
    suspend fun sumExpensesBetween(from: LocalDate, to: LocalDate): Long

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE type = 'INGRESO' AND date BETWEEN :from AND :to"
    )
    suspend fun sumIncomeBetween(from: LocalDate, to: LocalDate): Long

    /** Ingresos y gastos del rango en una sola pasada. */
    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type = 'INGRESO' THEN amount ELSE 0 END), 0) AS income,
               COALESCE(SUM(CASE WHEN type = 'GASTO' THEN amount ELSE 0 END), 0) AS expenses
        FROM transactions
        WHERE date BETWEEN :from AND :to
        """
    )
    fun observeMonthTotals(from: LocalDate, to: LocalDate): Flow<MonthTotals>

    /** Gasto del rango por categoría, de mayor a menor. */
    @Query(
        """
        SELECT c.id AS categoryId,
               c.name AS name,
               c.color AS color,
               c.icon AS icon,
               c.iconColor AS iconColor,
               c.monthlyBudget AS budget,
               COALESCE(SUM(t.amount), 0) AS spent
        FROM categories c
        LEFT JOIN transactions t ON t.categoryId = c.id
            AND t.type = 'GASTO'
            AND t.date BETWEEN :from AND :to
        WHERE c.archived = 0
        GROUP BY c.id
        HAVING COALESCE(SUM(t.amount), 0) > 0
        ORDER BY COALESCE(SUM(t.amount), 0) DESC
        """
    )
    fun observeCategorySpending(from: LocalDate, to: LocalDate): Flow<List<CategorySpending>>

    /** Todas las categorías activas, incluso sin gasto, para vigilar su límite. */
    @Query(
        """
        SELECT c.id AS categoryId,
               c.name AS name,
               c.color AS color,
               c.icon AS icon,
               c.iconColor AS iconColor,
               COALESCE((SELECT b.amount FROM category_budgets b
                         WHERE b.categoryId = c.id AND b.fromMonth <= :month
                         ORDER BY b.fromMonth DESC LIMIT 1), c.monthlyBudget) AS budget,
               COALESCE(SUM(t.amount), 0) AS spent
        FROM categories c
        LEFT JOIN transactions t ON t.categoryId = c.id
            AND t.type = 'GASTO'
            AND t.date BETWEEN :from AND :to
        WHERE c.archived = 0
        GROUP BY c.id
        ORDER BY c.sortOrder ASC, c.name ASC
        """
    )
    fun observeBudgetProgress(from: LocalDate, to: LocalDate, month: Int): Flow<List<CategorySpending>>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM transactions")
    fun observeCount(): Flow<Int>

    /** Todos los movimientos de una cuenta, del más reciente al más antiguo. */
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC, id DESC")
    fun observeByAccount(accountId: Long): Flow<List<Transaction>>

    /**
     * Gastos de una cuenta por categoría en un rango, para la torta.
     * Incluye categorías archivadas: el gasto pasado sigue contando.
     */
    @Query(
        """
        SELECT c.id AS categoryId,
               c.name AS name,
               c.color AS color,
               c.icon AS icon,
               c.iconColor AS iconColor,
               c.monthlyBudget AS budget,
               SUM(t.amount) AS spent
        FROM transactions t
        JOIN categories c ON c.id = t.categoryId
        WHERE t.accountId = :accountId
          AND t.type = 'GASTO'
          AND t.date BETWEEN :from AND :to
        GROUP BY c.id
        ORDER BY SUM(t.amount) DESC
        """
    )
    fun observeAccountSpending(accountId: Long, from: LocalDate, to: LocalDate): Flow<List<CategorySpending>>

    /** Gastos sin categoría de una cuenta en un rango (porción «Sin categoría»). */
    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE accountId = :accountId AND type = 'GASTO' AND categoryId IS NULL " +
            "AND date BETWEEN :from AND :to"
    )
    fun observeAccountUncategorized(accountId: Long, from: LocalDate, to: LocalDate): Flow<Long>
}
