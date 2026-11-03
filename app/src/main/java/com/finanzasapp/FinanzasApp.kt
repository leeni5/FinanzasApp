package com.finanzasapp

import android.app.Application
import android.content.pm.ApplicationInfo
import com.finanzasapp.data.db.AppDatabase
import com.finanzasapp.data.seed.SeedData
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

    override fun onCreate() {
        super.onCreate()

        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        applicationScope.launch {
            SeedData.populateIfEmpty(database, includeDemoData = isDebuggable)
        }
    }
}
