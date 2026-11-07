package com.finanzasapp.data.settings

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import java.util.Locale

/** Idiomas de la interfaz. Español es el de siempre; los textos base están en values/. */
enum class AppLanguage(val tag: String, val label: String) {
    ES("es", "Español"),
    EN("en", "English")
}

/**
 * Idioma de la app, independiente del idioma del teléfono.
 *
 * Se guarda en preferencias privadas y se aplica envolviendo el contexto de la
 * actividad y de la aplicación (attachBaseContext): así stringResource() y
 * getString() devuelven el texto del idioma elegido. Cambiarlo recrea la
 * actividad; los datos del usuario (nombres de cuentas, categorías) no se
 * traducen.
 */
object Idioma {

    private const val PREFS = "finanzas_ajustes"
    private const val KEY = "idioma"

    /** Contexto de la app en el idioma elegido, para textos fuera de Compose. */
    @Volatile
    private var localized: Context? = null
    private lateinit var appContext: Context

    @Volatile
    var current: AppLanguage = AppLanguage.ES
        private set

    val locale: Locale
        get() = if (current == AppLanguage.EN) Locale.US else Locale.forLanguageTag("es-CO")

    val isEnglish: Boolean get() = current == AppLanguage.EN

    /** Lee el idioma guardado. Se llama una vez, al crear la aplicación. */
    fun init(context: Context) {
        appContext = context.applicationContext ?: context
        current = read(context)
        Locale.setDefault(locale)
        localized = null
    }

    private fun read(context: Context): AppLanguage =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?.let { saved -> AppLanguage.entries.firstOrNull { it.name == saved } }
            ?: AppLanguage.ES

    /** Envuelve un contexto con el idioma guardado (para attachBaseContext). */
    fun wrap(base: Context): Context {
        current = read(base)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        Locale.setDefault(locale)
        return base.createConfigurationContext(config)
    }

    /** Cambia el idioma y recrea la pantalla para mostrarlo. */
    fun set(activity: Activity, language: AppLanguage) {
        if (language == current) return
        activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, language.name).commit()
        current = language
        Locale.setDefault(locale)
        localized = null
        activity.recreate()
    }

    /** Texto traducido fuera de Compose (errores, avisos, ViewModels). */
    fun text(@StringRes id: Int, vararg args: Any): String {
        val ctx = localized ?: wrap(appContext).also { localized = it }
        return if (args.isEmpty()) ctx.getString(id) else ctx.getString(id, *args)
    }
}
