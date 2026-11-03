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

    @Query("SELECT * FROM transactions WHERE uniqueHash = :hash LIMIT 1")
    suspend fun findByHash(hash: String): Transaction?

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE uniqueHash = :hash)")
    suspend fun existsByHash(hash: String): Boolean

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

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM transactions")
    fun observeCount(): Flow<Int>
}
