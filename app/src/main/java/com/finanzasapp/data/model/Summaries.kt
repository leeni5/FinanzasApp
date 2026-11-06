package com.finanzasapp.data.model

import com.finanzasapp.data.entity.AccountType
import java.time.LocalDate

/**
 * Resultados agregados que Room calcula en SQL.
 * Se mantienen fuera de las entidades: no son tablas, son vistas del mes.
 */

/** Saldo calculado de una cuenta: saldo inicial + ingresos - gastos. */
data class AccountBalance(
    val accountId: Long,
    val name: String,
    val type: AccountType,
    val color: String,
    val logo: String,
    val initialBalance: Long,
    val creditLimit: Long?,
    val balance: Long
)

/** Ingresos y gastos de un rango de fechas. */
data class MonthTotals(
    val income: Long,
    val expenses: Long
)

/** Gasto acumulado por categoría, con su presupuesto mensual. */
data class CategorySpending(
    val categoryId: Long,
    val name: String,
    val color: String,
    val icon: String,
    val iconColor: String,
    val budget: Long,
    val spent: Long
)

/** Avance de una meta: el ahorrado es la suma de sus aportes. */
data class GoalProgress(
    val goalId: Long,
    val name: String,
    val targetAmount: Long,
    val targetDate: LocalDate,
    val color: String,
    val completed: Boolean,
    val saved: Long
)
