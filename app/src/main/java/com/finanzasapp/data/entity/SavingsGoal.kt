package com.finanzasapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Meta de ahorro.
 * El monto actual NO se almacena: es la suma de los [GoalContribution].
 * Así el avance y el ritmo de los últimos 3 meses siempre son calculables.
 */
@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val targetAmount: Long,
    val targetDate: LocalDate,
    val color: String = "#2383E2",
    val completed: Boolean = false
)
