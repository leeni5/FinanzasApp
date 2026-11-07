package com.finanzasapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Presupuesto de una categoría a partir de un mes (desde la v5).
 *
 * El límite que rige en un mes es la fila con el [fromMonth] más alto que no
 * pase de ese mes. Editar el presupuesto en marzo crea una fila «desde marzo»
 * y borra las posteriores: marzo y los meses siguientes cambian, febrero y
 * los anteriores conservan su límite. [fromMonth] es año*100+mes (202603);
 * 0 significa «desde siempre».
 */
@Entity(
    tableName = "category_budgets",
    primaryKeys = ["categoryId", "fromMonth"],
    indices = [Index("categoryId")],
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CategoryBudget(
    val categoryId: Long,
    val fromMonth: Int,
    val amount: Long
)
