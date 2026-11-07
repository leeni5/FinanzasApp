package com.finanzasapp.ui.screens.revision

import androidx.room.withTransaction
import com.finanzasapp.data.db.AppDatabase
import com.finanzasapp.data.entity.AuditLog
import com.finanzasapp.data.entity.Transaction
import com.finanzasapp.data.model.displayName
import org.json.JSONObject
import java.time.Instant

/** Sacar movimientos de la bandeja «Por revisar», con su línea en el historial. */
object ReviewActions {

    /** Confirma un movimiento, opcionalmente con otra categoría. */
    suspend fun confirm(database: AppDatabase, transactionId: Long, categoryId: Long?) {
        val dao = database.transactionDao()
        database.withTransaction {
            val before = dao.findById(transactionId) ?: return@withTransaction
            val after = before.copy(
                categoryId = categoryId,
                requiresReview = false,
                updatedAt = Instant.now()
            )
            dao.update(after)
            database.systemDao().insertAudit(
                AuditLog(
                    entity = "transaction",
                    entityId = transactionId,
                    action = "REVISADO",
                    beforeJson = before.reviewJson(),
                    afterJson = after.reviewJson(),
                    source = "usuario",
                    at = after.updatedAt
                )
            )
        }
    }

    /** Confirma todos los pendientes tal como están. */
    suspend fun confirmAll(database: AppDatabase, transactions: List<Transaction>) {
        transactions.forEach { confirm(database, it.id, it.categoryId) }
    }

    private fun Transaction.reviewJson(): String = JSONObject()
        .put("descripcion", displayName())
        .put("monto", amount)
        .put("tipo", type.name)
        .put("categoria", categoryId ?: JSONObject.NULL)
        .put("porRevisar", requiresReview)
        .toString()
}
