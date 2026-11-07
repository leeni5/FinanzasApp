package com.finanzasapp.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * Movimiento financiero.
 *
 * Trazabilidad: guardamos el texto original tal como llegó ([descriptionOriginal]),
 * el origen ([source]) y la confianza de la IA ([confidence]).
 * Nunca se sobrescribe ese texto: si el usuario edita, se cambia [merchant] o [notes],
 * y la edición queda registrada en AuditLog.
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index("accountId"),
        Index("categoryId"),
        Index("date"),
        Index("uniqueHash")
    ],
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val accountId: Long,
    val date: LocalDate,
    val descriptionOriginal: String,
    val merchant: String? = null,
    /** Siempre positivo. El signo lo decide [type]. */
    val amount: Long,
    val type: TransactionType,
    val categoryId: Long? = null,
    val requiresReview: Boolean = false,
    val isRecurring: Boolean = false,
    /** Huella para descartar duplicados exactos. */
    val uniqueHash: String,
    val source: TransactionSource,
    val confidence: Float? = null,
    /** Une las dos patas de una misma transferencia. */
    val transferGroupId: String? = null,
    /**
     * Solo transferencias (desde la v4): true en la pata que sale de la
     * cuenta, false en la que entra. Así el saldo sabe hacia dónde restar.
     */
    @ColumnInfo(defaultValue = "0") val transferOut: Boolean = false,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)
