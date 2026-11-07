package com.finanzasapp.domain.importing

import com.finanzasapp.domain.dedupe.UniqueHash
import com.finanzasapp.data.settings.Idioma
import com.finanzasapp.R

/**
 * Lector del PDF de Budge (budge-app.info).
 *
 * El PDF no trae una tabla real: el texto sale como una lista de líneas.
 * Cada movimiento empieza con una fecha DD-MM-YYYY; después vienen el pago,
 * el monto, COP, "true" y, opcionalmente, categoría y cuenta.
 *
 * Las categorías y cuentas se reconocen por nombre, no por número de línea,
 * porque la exportación parte los textos largos en varias líneas.
 */
object BudgeTextParser {

    private val dateStart = Regex("^(\\d{2}-\\d{2}-\\d{4})(?:\\s+(.*))?$")
    private val amountLine = Regex("^-?\\d{1,3}(?:[.,]\\d{3})*(?:[.,]\\d+)?$|^-?\\d+$")
    private val currencyLine = Regex("^[A-Z]{3}$")

    private val ignoredHeaders = setOf(
        "Date", "Payment", "Amount", "Currency", "Is paid",
        "Category", "Subcategory", "Account", "Goal", "Description"
    )

    private val knownCategories = listOf(
        "Paquetes de Datos", "Transporte", "Restaurantes", "Subcategoría",
        "Salario", "Servicios", "Regalos", "Monedas", "Negocio", "Temporal",
        "Devuelto", "Comida", "Cositas", "Cuarto", "Casita", "Salud", "Ropa",
        "Arriendo", "Mercado", "Ocio", "Ingresos", "Otros", "Otro"
    )

    private val knownAccounts = listOf(
        "Bancolombia Ahorros", "Visa Bancolombia", "Bancolombia", "Nequi", "Efectivo"
    )

    fun parse(text: String): List<ParsedRow> {
        val lines = text
            .split(Regex("\\r?\\n"))
            .map { it.trim() }
            .filter { line ->
                line.isNotEmpty() &&
                    !line.startsWith("---PAGE") &&
                    !line.startsWith("Transactions between") &&
                    line != "budge-app.info" &&
                    line !in ignoredHeaders
            }

        val blocks = mutableListOf<Pair<String, List<String>>>()
        var currentDate: String? = null
        var currentLines = mutableListOf<String>()

        for (line in lines) {
            // Budge deja la fecha al inicio de la fila ("20-04-2026 Salario 210000").
            // Algunos extractores la ponen sola en su propia línea; ambos casos
            // producen el mismo bloque: la fecha y el resto de sus líneas.
            val match = dateStart.matchEntire(line)
            if (match != null) {
                if (currentDate != null) blocks += currentDate to currentLines
                currentDate = match.groupValues[1]
                currentLines = mutableListOf()
                val rest = match.groupValues[2].trim()
                if (rest.isNotEmpty()) currentLines += rest
            } else if (currentDate != null) {
                currentLines += line
            }
        }
        if (currentDate != null) blocks += currentDate to currentLines

        val rows = mutableListOf<ParsedRow>()

        for ((dateText, block) in blocks) {
            val date = ParseUtils.parseDate(dateText) ?: continue
            if (block.isEmpty()) continue

            // Las descripciones largas se parten en varias líneas y la fecha
            // puede venir pegada al inicio. Se unen y se trabaja por palabras.
            val tokens = block
                .joinToString(" ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .split(" ")
                .filter { it.isNotEmpty() }

            val amountIndex = findAmountIndex(tokens)
            if (amountIndex < 0) continue

            val amount = ParseUtils.parseAmount(tokens[amountIndex]) ?: continue
            if (amount == 0L) continue

            val description = tokens
                .subList(0, amountIndex)
                .joinToString(" ")
                .trim()
                .ifBlank { "Movimiento importado" }

            val tail = tokens.subList(amountIndex + 1, tokens.size)
            val paid = tail.none { it.equals("false", ignoreCase = true) }
            val currency = tail.firstOrNull { currencyLine.matches(it) } ?: "COP"

            val content = tail
                .filterNot { currencyLine.matches(it) }
                .filterNot { it.equals("true", ignoreCase = true) }
                .filterNot { it.equals("false", ignoreCase = true) }
                .joinToString(" ")
                .replace(Regex("\\s+"), " ")
                .trim()

            val normalized = UniqueHash.normalize(content)
            var rest = normalized
            var category: String? = null
            var account: String? = null

            for (candidate in knownCategories.sortedByDescending { it.length }) {
                val normalizedCandidate = UniqueHash.normalize(candidate)
                if (normalizedCandidate.isNotEmpty() &&
                    (rest == normalizedCandidate || rest.startsWith("$normalizedCandidate "))
                ) {
                    category = candidate
                    rest = rest.removePrefix(normalizedCandidate).trim()
                    break
                }
            }

            for (candidate in knownAccounts.sortedByDescending { it.length }) {
                val normalizedCandidate = UniqueHash.normalize(candidate)
                if (normalizedCandidate.isNotEmpty() &&
                    (rest == normalizedCandidate || rest.startsWith("$normalizedCandidate "))
                ) {
                    account = candidate
                    rest = rest.removePrefix(normalizedCandidate).trim()
                    break
                }
            }

            if (category == null && rest.isNotBlank()) {
                category = content
            }

            rows += ParsedRow(
                date = date,
                description = description,
                amount = amount,
                currency = currency,
                paid = paid,
                categoryName = category,
                accountName = account,
                line = 0
            )
        }

        if (rows.isEmpty()) {
            throw ImportParseException(
                Idioma.text(R.string.mv_parse_pdf_none)
            )
        }
        return rows
    }

    private fun findAmountIndex(lines: List<String>): Int {
        for (index in lines.indices) {
            if (!amountLine.matches(lines[index])) continue
            val next = lines.getOrNull(index + 1)
            if (next != null &&
                (currencyLine.matches(next) ||
                    next.equals("true", ignoreCase = true) ||
                    next.equals("false", ignoreCase = true))
            ) {
                return index
            }
        }

        // Si ninguna línea cumple la señal fuerte, el monto es la última
        // línea numérica antes de COP / true / false.
        val marker = lines.indexOfFirst {
            currencyLine.matches(it) ||
                it.equals("true", ignoreCase = true) ||
                it.equals("false", ignoreCase = true)
        }
        if (marker > 0) {
            for (index in marker - 1 downTo 0) {
                if (amountLine.matches(lines[index])) return index
            }
        }

        return lines.indexOfFirst { amountLine.matches(it) }
    }
}
