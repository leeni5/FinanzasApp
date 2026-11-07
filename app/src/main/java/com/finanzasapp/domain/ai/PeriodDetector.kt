package com.finanzasapp.domain.ai

import com.finanzasapp.domain.dedupe.UniqueHash
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Rango de fechas que se le envía a la IA, con un nombre para mostrarlo. */
data class DateRange(val from: LocalDate, val to: LocalDate, val label: String)

/**
 * Lee la pregunta en el teléfono y deduce de qué lapso habla, para enviar solo
 * esos movimientos. No usa la IA ni la red: son reglas simples en español.
 *
 * Entiende «hoy», «ayer», «esta semana», «la semana pasada», «este mes»,
 * «el mes pasado», «este año», «el año pasado», «2025», «los últimos 3 meses»
 * (o días/semanas/años), nombres de mes con o sin año («octubre»,
 * «marzo de 2025») y rangos entre meses («de agosto a octubre»).
 * Si no encuentra nada devuelve null y quien llama decide qué usar.
 */
object PeriodDetector {

    private val locale = Locale.forLanguageTag("es-CO")

    private val months = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    )

    private val numberWords = mapOf(
        "un" to 1, "una" to 1, "uno" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4,
        "cinco" to 5, "seis" to 6, "siete" to 7, "ocho" to 8, "nueve" to 9,
        "diez" to 10, "once" to 11, "doce" to 12
    )

    fun detect(question: String, today: LocalDate): DateRange? {
        // Sin tildes ni mayúsculas: «últimos» y «ultimos» valen igual.
        val text = " " + UniqueHash.normalize(question.replace("setiembre", "septiembre")) + " "
        val month = YearMonth.from(today)

        lastN(text, today)?.let { return it }

        when {
            " hoy " in text -> return DateRange(today, today, "hoy")
            " antier " in text || " anteayer " in text ->
                return today.minusDays(2).let { DateRange(it, it, "antier") }
            " ayer " in text -> return today.minusDays(1).let { DateRange(it, it, "ayer") }
            " semana pasada " in text || " semana anterior " in text -> {
                val start = today.with(DayOfWeek.MONDAY).minusWeeks(1)
                return DateRange(start, start.plusDays(6), "la semana pasada")
            }
            " esta semana " in text ->
                return DateRange(today.with(DayOfWeek.MONDAY), today, "esta semana")
            " mes pasado " in text || " mes anterior " in text -> {
                val previous = month.minusMonths(1)
                return DateRange(previous.atDay(1), previous.atEndOfMonth(), monthLabel(previous))
            }
            " ano pasado " in text || " ano anterior " in text -> {
                val year = today.year - 1
                return DateRange(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), "$year")
            }
        }

        // Meses por nombre, con año opcional detrás («octubre de 2025», «oct 2025» no).
        val mentioned = mutableListOf<YearMonth>()
        val monthPattern = Regex("\\b(${months.joinToString("|")})\\b(?:\\s+(?:de\\s+|del\\s+)?(\\d{4}))?")
        for (match in monthPattern.findAll(text)) {
            val number = months.indexOf(match.groupValues[1]) + 1
            val year = match.groupValues[2].toIntOrNull()
                // Sin año: la ocurrencia más reciente que no sea futura.
                ?: if (number <= today.monthValue) today.year else today.year - 1
            mentioned += YearMonth.of(year, number)
        }
        if (mentioned.isNotEmpty()) {
            val first = mentioned.min()
            val last = mentioned.max()
            val label = if (first == last) monthLabel(first) else "${monthLabel(first)} a ${monthLabel(last)}"
            return DateRange(first.atDay(1), minOf(last.atEndOfMonth(), today), label)
        }

        if (" este mes " in text || " mes actual " in text || " en el mes " in text) {
            return DateRange(month.atDay(1), today, "este mes")
        }

        // Años sueltos: «en 2025», «entre 2024 y 2025».
        val years = Regex("\\b(20\\d{2})\\b").findAll(text).map { it.value.toInt() }
            .filter { it in 2015..today.year }.toList()
        if (years.isNotEmpty()) {
            val from = LocalDate.of(years.min(), 1, 1)
            val to = minOf(LocalDate.of(years.max(), 12, 31), today)
            return DateRange(from, to, if (years.min() == years.max()) "${years.min()}" else "${years.min()} a ${years.max()}")
        }
        if (" este ano " in text || " ano actual " in text || " en el ano " in text) {
            return DateRange(today.withDayOfYear(1), today, "este año")
        }
        return null
    }

    /** «últimos 3 meses», «los ultimos dos años», «último mes», «últimas 2 semanas». */
    private fun lastN(text: String, today: LocalDate): DateRange? {
        val match = Regex("\\bultim[oa]s?\\s+(?:(\\d{1,3}|${numberWords.keys.joinToString("|")})\\s+)?(dias?|semanas?|meses|mes|anos?)\\b")
            .find(text) ?: return null
        val raw = match.groupValues[1]
        val n = raw.toIntOrNull() ?: numberWords[raw] ?: 1
        val unit = match.groupValues[2]
        return when {
            unit.startsWith("dia") -> DateRange(today.minusDays(n - 1L), today, "últimos $n días")
            unit.startsWith("semana") -> DateRange(today.minusWeeks(n.toLong()).plusDays(1), today, "últimas $n semanas")
            unit.startsWith("mes") -> DateRange(
                YearMonth.from(today).minusMonths(n - 1L).atDay(1),
                today,
                if (n == 1) "este mes" else "últimos $n meses"
            )
            else -> DateRange(today.minusYears(n.toLong()).plusDays(1), today, if (n == 1) "último año" else "últimos $n años")
        }
    }

    private fun monthLabel(month: YearMonth): String =
        month.month.getDisplayName(TextStyle.FULL, locale) + " de " + month.year

    /** Lo que se usa cuando la pregunta no menciona fechas. */
    fun default(today: LocalDate): DateRange =
        DateRange(YearMonth.from(today).minusMonths(11).atDay(1), today, "últimos 12 meses")
}
