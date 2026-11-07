package com.finanzasapp.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.finanzasapp.FinanzasApp
import com.finanzasapp.MainActivity
import com.finanzasapp.R
import com.finanzasapp.data.settings.Idioma
import com.finanzasapp.ui.util.Money

/**
 * Muestra un aviso de pago. Antes de avisar vuelve a leer el pago: si se
 * pausó, se pagó, se borró o cambió de fecha, no muestra nada.
 */
class PaymentReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @SuppressLint("MissingPermission") // se comprueba con canNotify()
    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_ID, -1L)
        val days = inputData.getInt(KEY_DAYS, -1)
        val dueEpochDay = inputData.getLong(KEY_DUE, Long.MIN_VALUE)
        if (id < 0 || days < 0) return Result.success()

        val database = (applicationContext as FinanzasApp).database
        val payment = database.paymentDao().findById(id) ?: return Result.success()

        val stillValid = payment.active &&
            payment.nextDueDate.toEpochDay() == dueEpochDay &&
            payment.lastPaidDueDate != payment.nextDueDate &&
            (payment.lastNotifiedDaysBefore == null || days < payment.lastNotifiedDaysBefore)
        if (!stillValid) return Result.success()

        // Sin permiso, la app sigue funcionando: el aviso simplemente no sale.
        if (!PaymentReminderScheduler.canNotify(applicationContext)) return Result.success()

        val title = if (days == 0) Idioma.text(R.string.aj_notif_due_today, payment.name) else Idioma.text(R.string.aj_notif_due_in_days, payment.name, days)
        val text = if (days == 0) Money.cop(payment.amount) else "${payment.name} · ${Money.cop(payment.amount)}"

        val open = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_PAYMENTS)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            applicationContext,
            id.toInt(),
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, PaymentReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_pago)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(id.toInt(), notification)
        database.paymentDao().markNotified(id, days)
        return Result.success()
    }

    companion object {
        const val KEY_ID = "id"
        const val KEY_DAYS = "days"
        const val KEY_DUE = "due"
    }
}
