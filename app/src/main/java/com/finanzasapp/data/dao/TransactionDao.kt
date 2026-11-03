package com.finanzasapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.finanzasapp.data.entity.Transaction
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

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM transactions")
    fun observeCount(): Flow<Int>
}
