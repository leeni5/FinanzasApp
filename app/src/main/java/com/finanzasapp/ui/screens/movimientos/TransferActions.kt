package com.finanzasapp.ui.screens.movimientos

import androidx.room.withTransaction
import com.finanzasapp.data.db.AppDatabase
import com.finanzasapp.data.entity.AuditLog
import com.finanzasapp.data.entity.Transaction
import com.finanzasapp.data.entity.TransactionSource
import com.finanzasapp.data.entity.TransactionType
import com.finanzasapp.domain.dedupe.UniqueHash
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Transferencias entre cuentas propias: dos movimientos unidos por
 * [Transaction.transferGroupId], uno que sale y otro que entra.
 *
 * No son gasto ni ingreso: mueven saldo de una cuenta a otra sin tocar los
 * totales del mes ni los presupuestos. Pagar la tarjeta desde el banco es una
 * transferencia: baja el saldo del banco y la deuda de la tarjeta.
 * Se crean y se borran juntas, nunca una pata sola.
 */
object TransferActions {

    suspend fun create(
        database: AppDatabase,
        fromAccountId: Long,
        toAccountId: Long,
        amount: Long,
        date: LocalDate,
        description: String
    ) {
        if (fromAccountId == toAccountId || amount <= 0L) return
        val text = description.trim().ifBlank { "Transferencia" }
        val now = Instant.now()
        val group = UUID.randomUUID().toString()
        val dao = database.transactionDao()

        database.withTransaction {
            val legs = listOf(
                fromAccountId to true,
                toAccountId to false
            ).map { (accountId, out) ->
                Transaction(
                    accountId = accountId,
                    date = date,
                    descriptionOriginal = text,
                    merchant = text,
                    amount = amount,
                    type = TransactionType.TRANSFERENCIA,
                    // La huella incluye el grupo: dos transferencias iguales el
                    // mismo día son dos movimientos distintos.
                    uniqueHash = UniqueHash.of(accountId, date, amount, "$text $group ${if (out) "sale" else "entra"}"),
                    source = TransactionSource.MANUAL,
                    transferGroupId = group,
                    transferOut = out,
                    createdAt = now,
                    updatedAt = now
                )
            }
            val ids = dao.insertAll(legs)
            database.systemDao().insertAudit(
                AuditLog(
                    entity = "transfer",
                    entityId = ids.first(),
                    action = "CREADO",
                    afterJson = JSONObject()
                        .put("desde", fromAccountId)
                        .put("hacia", toAccountId)
                        .put("monto", amount)
                        .put("fecha", date.toString())
                        .put("descripcion", text)
                        .toString(),
                    source = "usuario",
                    at = now
                )
            )
        }
    }

    /** Borra las dos patas de la transferencia a la que pertenece [groupId]. */
    suspend fun delete(database: AppDatabase, groupId: String) {
        val dao = database.transactionDao()
        database.withTransaction {
            val legs = dao.findTransferLegs(groupId)
            if (legs.isEmpty()) return@withTransaction
            legs.forEach { dao.deleteById(it.id) }
            val out = legs.firstOrNull { it.transferOut } ?: legs.first()
            val into = legs.firstOrNull { !it.transferOut } ?: legs.last()
            database.systemDao().insertAudit(
                AuditLog(
                    entity = "transfer",
                    entityId = out.id,
                    action = "BORRADO",
                    beforeJson = JSONObject()
                        .put("desde", out.accountId)
                        .put("hacia", into.accountId)
                        .put("monto", out.amount)
                        .put("fecha", out.date.toString())
                        .put("descripcion", out.descriptionOriginal)
                        .toString(),
                    source = "usuario",
                    at = Instant.now()
                )
            )
        }
    }
}
