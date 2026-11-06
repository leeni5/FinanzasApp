package com.finanzasapp

import android.app.Application
import android.content.pm.ApplicationInfo
import com.finanzasapp.data.db.AppDatabase
import com.finanzasapp.data.seed.SeedData
import com.finanzasapp.data.settings.SettingsStore
import com.finanzasapp.notifications.PaymentReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Punto de ensamblaje de la app.
 *
 * No hay inyección de dependencias: con un solo módulo y una sola base,
 * un objeto Application basta y evita una librería más.
 */
class FinanzasApp : Application() {

    /** Alcance para trabajo que debe sobrevivir a cualquier pantalla. */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.get(this) }

    /** Preferencias de la app (tema). */
    val settings: SettingsStore by lazy { SettingsStore(this) }

    override fun onCreate() {
        super.onCreate()

        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        PaymentReminderScheduler.createChannel(this)
        applicationScope.launch {
            SeedData.populateIfEmpty(database, includeDemoData = isDebuggable)
            // Reprogramación idempotente de avisos: cubre cambios de fecha del
            // teléfono, actualizaciones de la app y zona horaria.
            PaymentReminderScheduler.rescheduleAll(this@FinanzasApp, database)
        }
    }
}
