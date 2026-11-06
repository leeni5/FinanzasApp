package com.finanzasapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Pago programado: arriendo, servicios, suscripciones, cuotas.
 * [lastNotifiedDaysBefore] evita repetir el mismo aviso cada día.
 */
@Entity(
    tableName = "scheduled_payments",
    indices = [Index("accountId"), Index("nextDueDate")],
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class ScheduledPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val amount: Long,
    val frequency: PaymentFrequency,
    val nextDueDate: LocalDate,
    val accountId: Long? = null,
    val active: Boolean = true,
    /** Último aviso enviado: 7, 2 o 0. Null si nunca se avisó de este ciclo. */
    val lastNotifiedDaysBefore: Int? = null,
    /**
     * Vencimiento del último ciclo pagado (desde la v2). Si coincide con
     * [nextDueDate] ese ciclo ya se pagó: un doble toque no crea dos gastos.
     */
    val lastPaidDueDate: LocalDate? = null,
    /**
     * Día del mes en que vence (1–31), desde la v2. Permite que un pago del 31
     * pase al 28/29 en febrero y vuelva al 31 en marzo.
     */
    val anchorDay: Int? = null,
    /**
     * Categoría del gasto que se crea al pagar (desde la v2). Sin llave
     * foránea: las categorías se archivan, no se borran; si falta, el gasto
     * queda sin categoría.
     */
    val categoryId: Long? = null
)
