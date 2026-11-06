package com.finanzasapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.finanzasapp.data.entity.ScheduledPayment
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface PaymentDao {

    @Insert
    suspend fun insert(payment: ScheduledPayment): Long

    @Update
    suspend fun update(payment: ScheduledPayment)

    @Delete
    suspend fun delete(payment: ScheduledPayment)

    @Query("SELECT * FROM scheduled_payments WHERE active = 1 ORDER BY nextDueDate ASC")
    fun observeUpcoming(): Flow<List<ScheduledPayment>>

    @Query("SELECT * FROM scheduled_payments ORDER BY nextDueDate ASC")
    fun observeAll(): Flow<List<ScheduledPayment>>

    @Query(
        "SELECT * FROM scheduled_payments WHERE active = 1 " +
            "AND nextDueDate BETWEEN :from AND :to ORDER BY nextDueDate ASC"
    )
    suspend fun dueBetween(from: LocalDate, to: LocalDate): List<ScheduledPayment>

    @Query("UPDATE scheduled_payments SET lastNotifiedDaysBefore = :days WHERE id = :id")
    suspend fun markNotified(id: Long, days: Int?)

    @Query("SELECT COUNT(*) FROM scheduled_payments")
    suspend fun count(): Int

    @Query("SELECT * FROM scheduled_payments WHERE id = :id")
    suspend fun findById(id: Long): ScheduledPayment?

    @Query("SELECT * FROM scheduled_payments WHERE active = 1")
    suspend fun allActive(): List<ScheduledPayment>
}
