package com.finanzasapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.finanzasapp.data.entity.AuditLog
import com.finanzasapp.data.entity.CategorizationRule
import com.finanzasapp.data.entity.ImportJob
import kotlinx.coroutines.flow.Flow

/** Reglas, importaciones e historial: todo lo que sostiene la trazabilidad. */
@Dao
interface SystemDao {

    @Insert
    suspend fun insertRule(rule: CategorizationRule): Long

    @Update
    suspend fun updateRule(rule: CategorizationRule)

    @Query("SELECT * FROM categorization_rules ORDER BY priority DESC, timesApplied DESC")
    fun observeRules(): Flow<List<CategorizationRule>>

    @Query("SELECT * FROM categorization_rules ORDER BY priority DESC, timesApplied DESC")
    suspend fun allRules(): List<CategorizationRule>

    @Query(
        "SELECT * FROM categorization_rules " +
            "WHERE LOWER(:text) LIKE '%' || LOWER(pattern) || '%' " +
            "ORDER BY priority DESC LIMIT 1"
    )
    suspend fun matchRule(text: String): CategorizationRule?

    @Insert
    suspend fun insertImport(job: ImportJob): Long

    @Update
    suspend fun updateImport(job: ImportJob)

    @Query("SELECT * FROM import_jobs ORDER BY createdAt DESC")
    fun observeImports(): Flow<List<ImportJob>>

    @Insert
    suspend fun insertAudit(entry: AuditLog): Long

    @Query("SELECT * FROM audit_log WHERE entity = :entity AND entityId = :entityId ORDER BY at DESC")
    suspend fun auditFor(entity: String, entityId: Long): List<AuditLog>

    @Query("SELECT * FROM audit_log ORDER BY at DESC LIMIT :limit")
    suspend fun recentAudit(limit: Int = 100): List<AuditLog>
}
