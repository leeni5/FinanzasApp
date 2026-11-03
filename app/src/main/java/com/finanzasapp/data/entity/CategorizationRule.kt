package com.finanzasapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Regla aprendida: "si la descripción contiene este patrón, es esta categoría".
 * Se crea automáticamente cuando el usuario corrige una categoría en la bandeja.
 */
@Entity(
    tableName = "categorization_rules",
    indices = [Index("categoryId")],
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CategorizationRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val pattern: String,
    val categoryId: Long,
    /** A mayor prioridad, se evalúa antes. */
    val priority: Int = 0,
    val timesApplied: Int = 0,
    val createdAt: Instant
)
