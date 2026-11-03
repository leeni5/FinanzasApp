package com.finanzasapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * El saldo NO se almacena: se calcula con [initialBalance] + ingresos - gastos.
 * Un saldo guardado se desincroniza; uno calculado nunca miente.
 *
 * Para tarjetas de crédito no existe "saldo": existe deuda y cupo disponible
 * ([creditLimit] - deuda total).
 */
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: AccountType,
    val initialBalance: Long = 0L,
    val creditLimit: Long? = null,
    /** Día de corte del extracto (1-31), solo tarjetas de crédito. */
    val cutoffDay: Int? = null,
    /** Día límite de pago (1-31), solo tarjetas de crédito. */
    val dueDay: Int? = null,
    val color: String = "#2383E2",
    val archived: Boolean = false
)
