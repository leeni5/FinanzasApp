package com.finanzasapp.ui.screens.categorias

import androidx.room.withTransaction
import com.finanzasapp.data.db.AppDatabase
import com.finanzasapp.data.entity.AuditLog
import com.finanzasapp.data.entity.Category
import org.json.JSONObject
import java.time.Instant

/**
 * Altas, ediciones y archivo de categorías, cada una con su línea en el
 * historial. Las categorías no se borran: se archivan, para no dejar
 * movimientos viejos sin categoría.
 */
object CategoryActions {

    /** Crea o actualiza. Devuelve el id de la categoría guardada. */
    suspend fun save(database: AppDatabase, category: Category): Long {
        val dao = database.categoryDao()
        val now = Instant.now()
        val clean = category.copy(name = category.name.trim())
        return database.withTransaction {
            if (clean.id == 0L) {
                val order = (dao.all().maxOfOrNull { it.sortOrder } ?: -1) + 1
                val created = clean.copy(sortOrder = order)
                val id = dao.insert(created)
                database.systemDao().insertAudit(
                    AuditLog(
                        entity = "category",
                        entityId = id,
                        action = "CREADO",
                        afterJson = created.copy(id = id).toAuditJson(),
                        source = "usuario",
                        at = now
                    )
                )
                id
            } else {
                val before = dao.findById(clean.id)
                dao.update(clean)
                database.systemDao().insertAudit(
                    AuditLog(
                        entity = "category",
                        entityId = clean.id,
                        action = "EDITADO",
                        beforeJson = before?.toAuditJson(),
                        afterJson = clean.toAuditJson(),
                        source = "usuario",
                        at = now
                    )
                )
                clean.id
            }
        }
    }

    /** Archiva (o recupera) una categoría. */
    suspend fun setArchived(database: AppDatabase, id: Long, archived: Boolean) {
        val dao = database.categoryDao()
        database.withTransaction {
            val before = dao.findById(id) ?: return@withTransaction
            val after = before.copy(archived = archived)
            dao.update(after)
            database.systemDao().insertAudit(
                AuditLog(
                    entity = "category",
                    entityId = id,
                    action = if (archived) "ARCHIVADO" else "RECUPERADO",
                    beforeJson = before.toAuditJson(),
                    afterJson = after.toAuditJson(),
                    source = "usuario",
                    at = Instant.now()
                )
            )
        }
    }

    private fun Category.toAuditJson(): String = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("monthlyBudget", monthlyBudget)
        .put("color", color)
        .put("icon", icon)
        .put("iconColor", iconColor)
        .put("archived", archived)
        .toString()
}
