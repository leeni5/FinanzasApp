package com.finanzasapp.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Cómo se pinta la app. Oscuro es el valor por defecto. */
enum class ThemeMode(val label: String) {
    OSCURO("Oscuro"),
    CLARO("Claro"),
    SISTEMA("Según el teléfono")
}

private val Context.settingsDataStore by preferencesDataStore(name = "ajustes")

/**
 * Preferencias de la app (no financieras), guardadas con DataStore.
 * No contienen datos sensibles; las copias de Android siguen desactivadas.
 */
class SettingsStore(private val context: Context) {

    private val themeKey = stringPreferencesKey("tema")

    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        prefs[themeKey]?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } }
            ?: ThemeMode.OSCURO
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[themeKey] = mode.name }
    }
}
