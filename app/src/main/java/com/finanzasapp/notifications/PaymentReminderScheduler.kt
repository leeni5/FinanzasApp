package com.finanzasapp.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.finanzasapp.R
import com.finanzasapp.data.db.AppDatabase
import com.finanzasapp.data.settings.Idioma
import com.finanzasapp.data.entity.ScheduledPayment
import com.finanzasapp.ui.util.Fechas
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Programa los avisos de cada pago a 7, 2 y 0 días de su vencimiento, a las
 * 9:00 de Bogotá. WorkManager no garantiza el minuto exacto; para avisos de
 * pago es suficiente y evita pedir alarmas exactas.
 *
 * Cada aviso es un trabajo único `payment-{id}-{días}-{vencimiento}` con la
 * etiqueta `payment-{id}`: así reprogramar es idempotente y cancelar un pago
 * borra todos sus avisos de una vez.
 */
object PaymentReminderScheduler {

    const val CHANNEL_ID = "pagos_programados"
    val DAYS_BEFORE = listOf(7, 2, 0)
    private val NOTIFY_AT: LocalTime = LocalTime.of(9, 0)

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            Idioma.text(R.string.aj_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = Idioma.text(R.string.aj_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** true si la app puede mostrar notificaciones (en Android 13+ hace falta permiso). */
    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        return context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    }

    fun tagFor(paymentId: Long) = "payment-$paymentId"

    fun cancel(context: Context, paymentId: Long) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tagFor(paymentId))
    }

    /**
     * Cancela los avisos del pago y programa los de su ciclo actual.
     * Si la app estuvo cerrada y ya pasaron varios avisos, solo se envía el
     * más cercano al vencimiento que todavía no se haya enviado.
     */
    fun schedule(context: Context, payment: ScheduledPayment, now: Instant = Instant.now()) {
        cancel(context, payment.id)
        if (!payment.active || payment.lastPaidDueDate == payment.nextDueDate) return

        val today = Fechas.hoy()
        val due = payment.nextDueDate
        if (due.isBefore(today)) return

        val pending = DAYS_BEFORE.filter { days ->
            payment.lastNotifiedDaysBefore == null || days < payment.lastNotifiedDaysBefore
        }
        val triggers = pending.map { days ->
            days to due.minusDays(days.toLong()).atTime(NOTIFY_AT).atZone(Fechas.Bogota).toInstant()
        }
        val (past, future) = triggers.partition { (_, at) -> !at.isAfter(now) }

        val work = WorkManager.getInstance(context)
        // El más cercano de los avisos atrasados, ahora mismo.
        past.minByOrNull { it.first }?.let { (days, _) ->
            enqueue(work, payment, days, Duration.ZERO)
        }
        future.forEach { (days, at) ->
            enqueue(work, payment, days, Duration.between(now, at))
        }
    }

    /** Reprogramación idempotente de todos los pagos activos (al arrancar). */
    suspend fun rescheduleAll(context: Context, database: AppDatabase) {
        database.paymentDao().allActive().forEach { schedule(context, it) }
    }

    private fun enqueue(work: WorkManager, payment: ScheduledPayment, days: Int, delay: Duration) {
        val request = OneTimeWorkRequestBuilder<PaymentReminderWorker>()
            .setInitialDelay(delay.toMillis().coerceAtLeast(0L), TimeUnit.MILLISECONDS)
            .addTag(tagFor(payment.id))
            .setInputData(
                workDataOf(
                    PaymentReminderWorker.KEY_ID to payment.id,
                    PaymentReminderWorker.KEY_DAYS to days,
                    PaymentReminderWorker.KEY_DUE to payment.nextDueDate.toEpochDay()
                )
            )
            .build()
        work.enqueueUniqueWork(
            "payment-${payment.id}-$days-${payment.nextDueDate.toEpochDay()}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
