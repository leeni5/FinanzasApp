package com.finanzasapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/** Aporte puntual a una meta de ahorro. */
@Entity(
    tableName = "goal_contributions",
    indices = [Index("goalId")],
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoal::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GoalContribution(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val goalId: Long,
    val date: LocalDate,
    val amount: Long
)
