package com.finanzasapp.ui.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import com.finanzasapp.data.settings.Idioma

/**
 * Fechas en español y en hora de Bogotá.
 * Nunca se usa la zona del sistema: un teléfono puede estar en otra ciudad
 * y el mes financiero debe seguir siendo el de Colombia.
 */
object Fechas {

    val Bogota: ZoneId = ZoneId.of("America/Bogota")
    // Formatos según el idioma elegido en Ajustes. Se piden en cada uso: son
    // baratos y así un cambio de idioma se refleja sin reiniciar.
    private val locale: Locale get() = Idioma.locale
    private val en: Boolean get() = Idioma.isEnglish

    private val monthYear get() = DateTimeFormatter.ofPattern(if (en) "MMMM yyyy" else "MMMM 'de' yyyy", locale)
    private val longDate get() = DateTimeFormatter.ofPattern(if (en) "EEEE, MMMM d" else "EEEE, d 'de' MMMM", locale)
    private val shortDate get() = DateTimeFormatter.ofPattern(if (en) "MMM d" else "d MMM", locale)
    private val monthShort get() = DateTimeFormatter.ofPattern("MMM", locale)
    private val weekDay get() = DateTimeFormatter.ofPattern("EEEE", locale)

    fun hoy(): LocalDate = LocalDate.now(Bogota)

    fun mesActual(): String = capitalize(hoy().format(monthYear))

    fun mesDe(date: LocalDate): String = capitalize(date.format(monthYear))

    fun fechaLarga(date: LocalDate = hoy()): String = capitalize(date.format(longDate))

    fun corta(date: LocalDate): String =
        date.format(shortDate).replace(".", "").lowercase(locale)

    fun mesCorto(date: LocalDate): String =
        date.format(monthShort).replace(".", "").lowercase(locale)

    /** "viernes", para los encabezados de día de la lista. */
    fun diaSemana(date: LocalDate): String =
        capitalize(date.format(weekDay))

    /** "Buenos días", "Buenas tardes" o "Buenas noches" según la hora local. */
    fun saludo(): String = when (LocalTime.now(Bogota).hour) {
        in 5..11 -> if (en) "Good morning" else "Buenos días"
        in 12..18 -> if (en) "Good afternoon" else "Buenas tardes"
        else -> if (en) "Good evening" else "Buenas noches"
    }

    /**
     * Etiqueta corta para un pago programado.
     * "Vencido", "Hoy", "Mañana", "En 5 días" o la fecha corta.
     */
    fun vencimiento(date: LocalDate, today: LocalDate = hoy()): String {
        val days = ChronoUnit.DAYS.between(today, date)
        return when {
            days < 0 -> if (en) "Overdue" else "Vencido"
            days == 0L -> if (en) "Today" else "Hoy"
            days == 1L -> if (en) "Tomorrow" else "Mañana"
            days <= 7L -> if (en) "In $days days" else "En $days días"
            else -> corta(date)
        }
    }

    /** "2 de noviembre" a partir de hoy, para metas y resúmenes. */
    fun diaYMes(date: LocalDate): String =
        capitalize(date.format(DateTimeFormatter.ofPattern(if (en) "MMMM d" else "d 'de' MMMM", locale)))

    private fun capitalize(text: String): String =
        text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}
