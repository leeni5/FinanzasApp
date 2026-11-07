package com.finanzasapp.domain.ai

import com.finanzasapp.data.settings.Idioma
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
 * esos movimientos. No usa la IA ni la red: son reglas simples en español y en
 * inglés.
 *
 * Entiende «hoy», «ayer», «esta semana», «la semana pasada», «este mes»,
 * «el mes pasado», «este año», «el año pasado», «2025», «los últimos 3 meses»
 * (o días/semanas/años), nombres de mes con o sin año («octubre»,
 * «marzo de 2025») y rangos entre meses («de agosto a octubre»). En inglés,
 * lo equivalente: «today», «last week», «March 2025», «last 3 months»…
 * Si no encuentra nada devuelve null y quien llama decide qué usar.
 * El nombre del lapso (label) sale en el idioma de la app.
 */
object PeriodDetector {

    private val locale = Locale.forLanguageTag("es-CO")

    private val months = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    )

    private val monthsEn = listOf(
        "january", "february", "march", "april", "may", "june",
        "july", "august", "september", "october", "november", "december"
    )

    private val numberWords = mapOf(
        "un" to 1, "una" to 1, "uno" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4,
        "cinco" to 5, "seis" to 6, "siete" to 7, "ocho" to 8, "nueve" to 9,
        "diez" to 10, "once" to 11, "doce" to 12
    )

    private val numberWordsEn = mapOf(
        "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6,
        "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10, "eleven" to 11, "twelve" to 12
    )

    /** Texto del lapso en el idioma de la app. */
    private fun tr(es: String, en: String) = if (Idioma.isEnglish) en else es

    fun detect(question: String, today: LocalDate): DateRange? {
        // Sin tildes ni mayúsculas: «últimos» y «ultimos» valen igual.
        val text = " " + UniqueHash.normalize(question.replace("setiembre", "septiembre")) + " "
        val month = YearMonth.from(today)

        lastN(text, today)?.let { return it }
        lastNEnglish(text, today)?.let { return it }

        when {
            " hoy " in text || " today " in text -> return DateRange(today, today, tr("hoy", "today"))
            " antier " in text || " anteayer " in text || " day before yesterday " in text ->
                return today.minusDays(2).let { DateRange(it, it, tr("antier", "the day before yesterday")) }
            " ayer " in text || " yesterday " in text ->
                return today.minusDays(1).let { DateRange(it, it, tr("ayer", "yesterday")) }
            " semana pasada " in text || " semana anterior " in text ||
                " last week " in text || " previous week " in text -> {
                val start = today.with(DayOfWeek.MONDAY).minusWeeks(1)
                return DateRange(start, start.plusDays(6), tr("la semana pasada", "last week"))
            }
            " esta semana " in text || " this week " in text ->
                return DateRange(today.with(DayOfWeek.MONDAY), today, tr("esta semana", "this week"))
            " mes pasado " in text || " mes anterior " in text ||
                " last month " in text || " previous month " in text -> {
                val previous = month.minusMonths(1)
                return DateRange(previous.atDay(1), previous.atEndOfMonth(), monthLabel(previous))
            }
            " ano pasado " in text || " ano anterior " in text ||
                " last year " in text || " previous year " in text -> {
                val year = today.year - 1
                return DateRange(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), "$year")
            }
        }

        // Meses por nombre, con año opcional detrás («octubre de 2025», «oct 2025» no).
        val mentioned = mutableListOf<YearMonth>()
        fun addMonth(number: Int, explicitYear: Int?) {
            val year = explicitYear
                // Sin año: la ocurrencia más reciente que no sea futura.
                ?: if (number <= today.monthValue) today.year else today.year - 1
            mentioned += YearMonth.of(year, number)
        }
        val monthPattern = Regex("\\b(${months.joinToString("|")})\\b(?:\\s+(?:de\\s+|del\\s+)?(\\d{4}))?")
        for (match in monthPattern.findAll(text)) {
            addMonth(months.indexOf(match.groupValues[1]) + 1, match.groupValues[2].toIntOrNull())
        }
        // En inglés: «March», «March 2025», «March of 2025», «from August to October».
        // «may» también es verbo: solo cuenta como mes con año detrás o tras in/from/to…
        val monthPatternEn = Regex(
            "\\b(?:(in|from|to|since|until|till|through|during|of|and|between)\\s+)?" +
                "(${monthsEn.joinToString("|")}|sept)\\b(?:\\s+(?:of\\s+)?(\\d{4}))?"
        )
        for (match in monthPatternEn.findAll(text)) {
            val name = match.groupValues[2]
            val year = match.groupValues[3].toIntOrNull()
            if (name == "may" && match.groupValues[1].isEmpty() && year == null) continue
            val number = if (name == "sept") 9 else monthsEn.indexOf(name) + 1
            addMonth(number, year)
        }
        if (mentioned.isNotEmpty()) {
            val first = mentioned.min()
            val last = mentioned.max()
            val label = if (first == last) monthLabel(first) else "${monthLabel(first)} ${tr("a", "to")} ${monthLabel(last)}"
            return DateRange(first.atDay(1), minOf(last.atEndOfMonth(), today), label)
        }

        if (" este mes " in text || " mes actual " in text || " en el mes " in text ||
            " this month " in text || " current month " in text
        ) {
            return DateRange(month.atDay(1), today, tr("este mes", "this month"))
        }

        // Años sueltos: «en 2025», «entre 2024 y 2025».
        val years = Regex("\\b(20\\d{2})\\b").findAll(text).map { it.value.toInt() }
            .filter { it in 2015..today.year }.toList()
        if (years.isNotEmpty()) {
            val from = LocalDate.of(years.min(), 1, 1)
            val to = minOf(LocalDate.of(years.max(), 12, 31), today)
            return DateRange(
                from,
                to,
                if (years.min() == years.max()) "${years.min()}" else "${years.min()} ${tr("a", "to")} ${years.max()}"
            )
        }
        if (" este ano " in text || " ano actual " in text || " en el ano " in text ||
            " this year " in text || " current year " in text
        ) {
            return DateRange(today.withDayOfYear(1), today, tr("este año", "this year"))
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
            unit.startsWith("dia") -> DateRange(today.minusDays(n - 1L), today, daysLabel(n))
            unit.startsWith("semana") -> DateRange(today.minusWeeks(n.toLong()).plusDays(1), today, weeksLabel(n))
            unit.startsWith("mes") -> DateRange(YearMonth.from(today).minusMonths(n - 1L).atDay(1), today, monthsLabel(n))
            else -> DateRange(today.minusYears(n.toLong()).plusDays(1), today, yearsLabel(n))
        }
    }

    /**
     * «last 3 months», «past 30 days», «the last two weeks». Exige el número:
     * «last month», «last week» y «last year» son el período anterior completo.
     */
    private fun lastNEnglish(text: String, today: LocalDate): DateRange? {
        val match = Regex("\\b(?:last|past|previous)\\s+(\\d{1,3}|${numberWordsEn.keys.joinToString("|")})\\s+(days?|weeks?|months?|years?)\\b")
            .find(text) ?: return null
        val raw = match.groupValues[1]
        val n = raw.toIntOrNull() ?: numberWordsEn[raw] ?: 1
        val unit = match.groupValues[2]
        return when {
            unit.startsWith("day") -> DateRange(today.minusDays(n - 1L), today, daysLabel(n))
            unit.startsWith("week") -> DateRange(today.minusWeeks(n.toLong()).plusDays(1), today, weeksLabel(n))
            unit.startsWith("month") -> DateRange(YearMonth.from(today).minusMonths(n - 1L).atDay(1), today, monthsLabel(n))
            else -> DateRange(today.minusYears(n.toLong()).plusDays(1), today, yearsLabel(n))
        }
    }

    private fun daysLabel(n: Int) = tr("últimos $n días", "last $n days")
    private fun weeksLabel(n: Int) = tr("últimas $n semanas", "last $n weeks")
    private fun monthsLabel(n: Int) = if (n == 1) tr("este mes", "this month") else tr("últimos $n meses", "last $n months")
    private fun yearsLabel(n: Int) = if (n == 1) tr("último año", "last year") else tr("últimos $n años", "last $n years")

    private fun monthLabel(month: YearMonth): String =
        if (Idioma.isEnglish) month.month.getDisplayName(TextStyle.FULL, Locale.US) + " " + month.year
        else month.month.getDisplayName(TextStyle.FULL, locale) + " de " + month.year

    /** Lo que se usa cuando la pregunta no menciona fechas. */
    fun default(today: LocalDate): DateRange =
        DateRange(YearMonth.from(today).minusMonths(11).atDay(1), today, tr("últimos 12 meses", "last 12 months"))
}
