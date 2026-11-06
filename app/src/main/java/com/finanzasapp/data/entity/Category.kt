package com.finanzasapp.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Categoría de gasto o ingreso, creada por el usuario o por la importación.
 * [monthlyBudget] en 0 significa "sin presupuesto": se registra pero no se vigila.
 * [icon] es la clave de un icono del catálogo de la app ("" = deducir del nombre).
 * [iconColor] es #RRGGBB; "" = blanco o negro según el fondo (columna desde la v2).
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val monthlyBudget: Long = 0L,
    val color: String = "#787774",
    val icon: String = "",
    val sortOrder: Int = 0,
    val archived: Boolean = false,
    @ColumnInfo(defaultValue = "") val iconColor: String = ""
)
