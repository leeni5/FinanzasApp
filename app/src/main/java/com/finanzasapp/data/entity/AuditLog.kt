package com.finanzasapp.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Historial inmutable. Nunca se borra.
 * Es lo que permite responder "¿por qué este movimiento dice esto?".
 */
@Entity(
    tableName = "audit_log",
    indices = [Index(value = ["entity", "entityId"])]
)
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** "transaction", "goal", "account"... */
    val entity: String,
    val entityId: Long,
    /** "CREADO", "EDITADO", "BORRADO". */
    val action: String,
    val beforeJson: String? = null,
    val afterJson: String? = null,
    /** Quién lo originó: "usuario", "seed", "importacion", "ia". */
    val source: String,
    val at: Instant
)
