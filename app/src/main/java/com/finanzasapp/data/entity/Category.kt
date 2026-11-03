package com.finanzasapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Categoría de gasto o ingreso.
 * [monthlyBudget] en 0 significa "sin presupuesto": se registra pero no se vigila.
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val monthlyBudget: Long = 0L,
    val color: String = "#787774",
    val icon: String = "",
    val sortOrder: Int = 0,
    val archived: Boolean = false
)
