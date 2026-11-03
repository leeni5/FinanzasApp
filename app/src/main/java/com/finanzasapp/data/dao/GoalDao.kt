package com.finanzasapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.finanzasapp.data.entity.GoalContribution
import com.finanzasapp.data.entity.SavingsGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Insert
    suspend fun insertGoal(goal: SavingsGoal): Long

    @Update
    suspend fun updateGoal(goal: SavingsGoal)

    @Delete
    suspend fun deleteGoal(goal: SavingsGoal)

    @Query("SELECT * FROM savings_goals ORDER BY completed ASC, targetDate ASC")
    fun observeGoals(): Flow<List<SavingsGoal>>

    @Insert
    suspend fun insertContribution(contribution: GoalContribution): Long

    @Delete
    suspend fun deleteContribution(contribution: GoalContribution)

    @Query("SELECT * FROM goal_contributions WHERE goalId = :goalId ORDER BY date DESC")
    fun observeContributions(goalId: Long): Flow<List<GoalContribution>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM goal_contributions WHERE goalId = :goalId")
    fun observeTotal(goalId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM goal_contributions WHERE goalId = :goalId")
    suspend fun total(goalId: Long): Long

    @Query("SELECT COUNT(*) FROM savings_goals")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM savings_goals")
    fun observeCount(): Flow<Int>
}
