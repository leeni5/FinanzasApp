package com.finanzasapp.ui.screens.pagos

import android.content.Context
import androidx.room.withTransaction
import com.finanzasapp.data.db.AppDatabase
import com.finanzasapp.data.entity.AuditLog
import com.finanzasapp.data.entity.ScheduledPayment
import com.finanzasapp.data.entity.Transaction
import com.finanzasapp.data.entity.TransactionSource
import com.finanzasapp.data.entity.TransactionType
import com.finanzasapp.domain.dedupe.UniqueHash
import com.finanzasapp.domain.payments.PaymentCycle
import com.finanzasapp.notifications.PaymentReminderScheduler
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate

/** Lo necesario para deshacer un pago recién marcado. */
data class PaidReceipt(val before: ScheduledPayment, val transactionId: Long)

/**
 * Alta, edición, pausa, borrado y pago de pagos programados. Cada cambio
 * queda en el historial y reprograma (o cancela) sus avisos.
 */
object PaymentActions {

    suspend fun save(context: Context, database: AppDatabase, draft: ScheduledPayment) {
        val dao = database.paymentDao()
        val now = Instant.now()
        val saved = database.withTransaction {
            if (draft.id == 0L) {
                val created = draft.copy(anchorDay = draft.nextDueDate.dayOfMonth, lastNotifiedDaysBefore = null)
                val id = dao.insert(created)
                audit(database, id, "CREADO", null, created.copy(id = id), now)
                created.copy(id = id)
            } else {
                val before = dao.findById(draft.id) ?: return@withTransaction null
                val dateChanged = before.nextDueDate != draft.nextDueDate
                val after = draft.copy(
                    anchorDay = if (dateChanged) draft.nextDueDate.dayOfMonth else before.anchorDay,
                    lastNotifiedDaysBefore = if (dateChanged || before.frequency != draft.frequency) null else before.lastNotifiedDaysBefore,
                    lastPaidDueDate = before.lastPaidDueDate
                )
                dao.update(after)
                audit(database, after.id, "EDITADO", before, after, now)
                after
            }
        }
        saved?.let { PaymentReminderScheduler.schedule(context, it) }
    }

    suspend fun setActive(context: Context, database: AppDatabase, id: Long, active: Boolean) {
        val after = database.withTransaction {
            val before = database.paymentDao().findById(id) ?: return@withTransaction null
            val after = before.copy(active = active, lastNotifiedDaysBefore = null)
            database.paymentDao().update(after)
            audit(database, id, if (active) "REANUDADO" else "PAUSADO", before, after, Instant.now())
            after
        } ?: return
        PaymentReminderScheduler.schedule(context, after)
    }

    suspend fun delete(context: Context, database: AppDatabase, id: Long) {
        database.withTransaction {
            val before = database.paymentDao().findById(id) ?: return@withTransaction
            database.paymentDao().delete(before)
            audit(database, id, "BORRADO", before, null, Instant.now())
        }
        PaymentReminderScheduler.cancel(context, id)
    }

    /**
     * Registra el pago del ciclo [expectedDue]: crea un gasto, avanza el
     * vencimiento y deja todo auditado, en una sola transacción.
     *
     * Devuelve null si el ciclo ya se había pagado, si el pago cambió o si no
     * tiene cuenta: así un doble toque nunca crea dos gastos.
     */
    suspend fun markPaid(
        context: Context,
        database: AppDatabase,
        id: Long,
        expectedDue: LocalDate,
        payDate: LocalDate,
        accountId: Long?
    ): PaidReceipt? {
        val now = Instant.now()
        val result = database.withTransaction {
            val before = database.paymentDao().findById(id) ?: return@withTransaction null
            if (!before.active || before.nextDueDate != expectedDue) return@withTransaction null
            if (before.lastPaidDueDate == expectedDue) return@withTransaction null
            val account = accountId ?: before.accountId ?: return@withTransaction null

            // La descripción del hash incluye el ciclo: no choca con un gasto
            // manual idéntico ni con el pago de otro mes.
            val cycleLabel = "${before.name} · $expectedDue"
            val transaction = Transaction(
                accountId = account,
                date = payDate,
                descriptionOriginal = before.name,
                merchant = before.name,
                amount = before.amount,
                type = TransactionType.GASTO,
                categoryId = before.categoryId?.takeIf { database.categoryDao().findById(it) != null },
                isRecurring = true,
                uniqueHash = UniqueHash.of(account, payDate, before.amount, cycleLabel),
                source = TransactionSource.MANUAL,
                notes = "Pago programado · ciclo del $expectedDue",
                createdAt = now,
                updatedAt = now
            )
            val transactionId = database.transactionDao().insert(transaction)
            database.systemDao().insertAudit(
                AuditLog(
                    entity = "transaction",
                    entityId = transactionId,
                    action = "CREADO",
                    afterJson = JSONObject()
                        .put("monto", transaction.amount)
                        .put("fecha", payDate.toString())
                        .put("pagoProgramado", id)
                        .toString(),
                    source = "pago_programado",
                    at = now
                )
            )

            val next = PaymentCycle.next(before)
            val after = before.copy(
                nextDueDate = next ?: before.nextDueDate,
                active = next != null,
                lastPaidDueDate = expectedDue,
                lastNotifiedDaysBefore = null,
                anchorDay = PaymentCycle.anchorOf(before)
            )
            database.paymentDao().update(after)
            audit(database, id, "PAGADO", before, after, now, extra = mapOf("ciclo" to expectedDue.toString(), "movimiento" to transactionId))
            PaidReceipt(before, transactionId) to after
        } ?: return null

        PaymentReminderScheduler.schedule(context, result.second)
        return result.first
    }

    /** Deshace un pago recién marcado: borra el gasto y devuelve la fecha. */
    suspend fun undoPaid(context: Context, database: AppDatabase, receipt: PaidReceipt) {
        val now = Instant.now()
        val restored = database.withTransaction {
            val current = database.paymentDao().findById(receipt.before.id) ?: return@withTransaction null
            database.transactionDao().findById(receipt.transactionId)?.let {
                database.transactionDao().deleteById(it.id)
                database.systemDao().insertAudit(
                    AuditLog(
                        entity = "transaction",
                        entityId = it.id,
                        action = "BORRADO",
                        beforeJson = JSONObject().put("monto", it.amount).put("fecha", it.date.toString()).toString(),
                        source = "pago_programado",
                        at = now
                    )
                )
            }
            val back = receipt.before
            database.paymentDao().update(back)
            audit(database, back.id, "PAGO_DESHECHO", current, back, now)
            back
        } ?: return
        PaymentReminderScheduler.schedule(context, restored)
    }

    private suspend fun audit(
        database: AppDatabase,
        id: Long,
        action: String,
        before: ScheduledPayment?,
        after: ScheduledPayment?,
        at: Instant,
        extra: Map<String, Any> = emptyMap()
    ) {
        database.systemDao().insertAudit(
            AuditLog(
                entity = "payment",
                entityId = id,
                action = action,
                beforeJson = before?.toJson(),
                afterJson = after?.toJson(extra),
                source = "usuario",
                at = at
            )
        )
    }

    private fun ScheduledPayment.toJson(extra: Map<String, Any> = emptyMap()): String =
        JSONObject()
            .put("nombre", name)
            .put("monto", amount)
            .put("frecuencia", frequency.name)
            .put("vence", nextDueDate.toString())
            .put("cuenta", accountId)
            .put("categoria", categoryId)
            .put("activo", active)
            .put("pagadoHasta", lastPaidDueDate?.toString())
            .apply { extra.forEach { (k, v) -> put(k, v) } }
            .toString()
}
