package com.finanzasapp.ui.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Fechas en español y en hora de Bogotá.
 * Nunca se usa la zona del sistema: un teléfono puede estar en otra ciudad
 * y el mes financiero debe seguir siendo el de Colombia.
 */
object Fechas {

    val Bogota: ZoneId = ZoneId.of("America/Bogota")
    private val locale = Locale.forLanguageTag("es-CO")

    private val monthYear = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", locale)
    private val longDate = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", locale)
    private val shortDate = DateTimeFormatter.ofPattern("d MMM", locale)
    private val monthShort = DateTimeFormatter.ofPattern("MMM", locale)
    private val weekDay = DateTimeFormatter.ofPattern("EEEE", locale)

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
        in 5..11 -> "Buenos días"
        in 12..18 -> "Buenas tardes"
        else -> "Buenas noches"
    }

    /**
     * Etiqueta corta para un pago programado.
     * "Vencido", "Hoy", "Mañana", "En 5 días" o la fecha corta.
     */
    fun vencimiento(date: LocalDate, today: LocalDate = hoy()): String {
        val days = ChronoUnit.DAYS.between(today, date)
        return when {
            days < 0 -> "Vencido"
            days == 0L -> "Hoy"
            days == 1L -> "Mañana"
            days <= 7L -> "En $days días"
            else -> corta(date)
        }
    }

    /** "2 de noviembre" a partir de hoy, para metas y resúmenes. */
    fun diaYMes(date: LocalDate): String =
        capitalize(date.format(DateTimeFormatter.ofPattern("d 'de' MMMM", locale)))

    private fun capitalize(text: String): String =
        text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}
