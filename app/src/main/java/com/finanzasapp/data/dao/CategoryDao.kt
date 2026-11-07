package com.finanzasapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.finanzasapp.data.entity.Category
import com.finanzasapp.data.entity.CategoryBudget
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)

    @Query("SELECT * FROM categories WHERE archived = 0 ORDER BY sortOrder ASC, name ASC")
    fun observeActive(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY archived ASC, sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun findById(id: Long): Category?

    @Query("SELECT * FROM categories ORDER BY archived ASC, sortOrder ASC, name ASC")
    suspend fun all(): List<Category>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: CategoryBudget)

    /** Al editar un mes, los cambios posteriores quedan reemplazados por este. */
    @Query("DELETE FROM category_budgets WHERE categoryId = :categoryId AND fromMonth > :month")
    suspend fun deleteBudgetsAfter(categoryId: Long, month: Int)

    @Query("SELECT COUNT(*) FROM category_budgets WHERE categoryId = :categoryId AND fromMonth < :month")
    suspend fun countBudgetsBefore(categoryId: Long, month: Int): Int

    @Query("SELECT COUNT(*) FROM categories")
    fun observeCount(): Flow<Int>
}
