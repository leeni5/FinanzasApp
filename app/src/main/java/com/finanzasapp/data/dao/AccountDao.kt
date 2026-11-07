package com.finanzasapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.finanzasapp.data.entity.Account
import com.finanzasapp.data.model.AccountBalance
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Insert
    suspend fun insert(account: Account): Long

    @Update
    suspend fun update(account: Account)

    @Delete
    suspend fun delete(account: Account)

    @Query("SELECT * FROM accounts ORDER BY archived ASC, name ASC")
    fun observeAll(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE archived = 0 ORDER BY name ASC")
    fun observeActive(): Flow<List<Account>>

    /**
     * Saldo por cuenta calculado en SQL.
     * No se guarda nunca: así no puede desincronizarse de los movimientos.
     */
    @Query(
        """
        SELECT a.id AS accountId,
               a.name AS name,
               a.type AS type,
               a.color AS color,
               a.logo AS logo,
               a.initialBalance AS initialBalance,
               a.creditLimit AS creditLimit,
               a.initialBalance + COALESCE(SUM(
                   CASE WHEN t.type = 'INGRESO' THEN t.amount
                        WHEN t.type = 'GASTO' THEN -t.amount
                        WHEN t.type = 'TRANSFERENCIA' AND t.transferOut = 1 THEN -t.amount
                        WHEN t.type = 'TRANSFERENCIA' THEN t.amount
                        ELSE 0 END
               ), 0) AS balance
        FROM accounts a
        LEFT JOIN transactions t ON t.accountId = a.id
        WHERE a.archived = 0
        GROUP BY a.id
        ORDER BY a.name ASC
        """
    )
    fun observeBalances(): Flow<List<AccountBalance>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun findById(id: Long): Account?

    @Query("SELECT * FROM accounts ORDER BY archived ASC, name ASC")
    suspend fun all(): List<Account>

    @Query("UPDATE accounts SET archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM accounts")
    fun observeCount(): Flow<Int>
}
