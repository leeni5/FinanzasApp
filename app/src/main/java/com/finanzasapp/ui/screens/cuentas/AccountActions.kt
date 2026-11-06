package com.finanzasapp.ui.screens.cuentas

import androidx.room.withTransaction
import com.finanzasapp.data.db.AppDatabase
import com.finanzasapp.data.entity.Account
import com.finanzasapp.data.entity.AuditLog
import org.json.JSONObject
import java.time.Instant

/**
 * Altas, ediciones y archivo de cuentas, con su línea en el historial.
 * Una cuenta no se borra desde aquí: borrarla eliminaría en cascada sus
 * movimientos. Se archiva y deja de aparecer en listas y formularios.
 */
object AccountActions {

    suspend fun save(database: AppDatabase, account: Account): Long {
        val dao = database.accountDao()
        val now = Instant.now()
        val clean = account.copy(name = account.name.trim())
        return database.withTransaction {
            if (clean.id == 0L) {
                val id = dao.insert(clean)
                database.systemDao().insertAudit(
                    AuditLog(entity = "account", entityId = id, action = "CREADO", afterJson = clean.copy(id = id).toJson(), source = "usuario", at = now)
                )
                id
            } else {
                val before = dao.findById(clean.id)
                dao.update(clean)
                database.systemDao().insertAudit(
                    AuditLog(entity = "account", entityId = clean.id, action = "EDITADO", beforeJson = before?.toJson(), afterJson = clean.toJson(), source = "usuario", at = now)
                )
                clean.id
            }
        }
    }

    suspend fun setArchived(database: AppDatabase, id: Long, archived: Boolean) {
        database.withTransaction {
            val before = database.accountDao().findById(id) ?: return@withTransaction
            val after = before.copy(archived = archived)
            database.accountDao().update(after)
            database.systemDao().insertAudit(
                AuditLog(
                    entity = "account",
                    entityId = id,
                    action = if (archived) "ARCHIVADO" else "RECUPERADO",
                    beforeJson = before.toJson(),
                    afterJson = after.toJson(),
                    source = "usuario",
                    at = Instant.now()
                )
            )
        }
    }

    private fun Account.toJson(): String = JSONObject()
        .put("id", id)
        .put("nombre", name)
        .put("tipo", type.name)
        .put("saldoInicial", initialBalance)
        .put("cupo", creditLimit)
        .put("color", color)
        .put("logo", logo)
        .put("archivada", archived)
        .toString()
}
