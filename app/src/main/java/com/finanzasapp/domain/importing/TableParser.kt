package com.finanzasapp.domain.importing

import kotlin.math.abs

/**
 * Convierte una tabla (CSV o Excel) en movimientos.
 *
 * Los encabezados se buscan por nombre, no por posición, para tolerar
 * exportaciones con columnas en otro orden. Si no hay encabezado se asume
 * el orden de Budge: fecha, pago, monto, moneda, pagado...
 */
object TableParser {

    private val dateNames = setOf(
        "fecha", "date", "dia", "fecha movimiento", "fecha de la transaccion",
        "transaction date"
    )
    private val paymentNames = setOf(
        "payment", "concepto", "detalle", "merchant", "comercio", "nombre",
        "movimiento"
    )
    private val descriptionNames = setOf("description", "descripcion")
    private val notesNames = setOf(
        "notes", "nota", "notas", "observacion", "observaciones"
    )
    private val amountNames = setOf(
        "amount", "monto", "valor", "importe", "total", "cantidad",
        "debito", "credito"
    )
    private val currencyNames = setOf("currency", "moneda", "divisa")
    private val paidNames = setOf("is paid", "pagado", "paid", "realizado", "estado")
    private val categoryNames = setOf("category", "categoria", "rubro")
    private val subcategoryNames = setOf("subcategory", "subcategoria")
    private val accountNames = setOf("account", "cuenta", "cuenta origen", "cuenta destino")
    private val goalNames = setOf("goal", "meta")
    private val typeNames = setOf("type", "tipo", "naturaleza")

    private val knownHeaders = dateNames + paymentNames + descriptionNames + notesNames +
        amountNames + currencyNames + paidNames + categoryNames + subcategoryNames +
        accountNames + goalNames + typeNames

    fun parse(rows: List<List<String>>, fileName: String): Pair<List<ParsedRow>, List<String>> {
        if (rows.isEmpty()) throw ImportParseException("El archivo no tiene filas.")

        val headerIndex = rows.indexOfFirst { row ->
            val keys = row.map { ParseUtils.normalize(it) }.toSet()
            keys.any { it in dateNames } &&
                keys.any { it in amountNames } &&
                keys.count { it in knownHeaders } >= 3
        }

        val headers: Map<String, Int> = if (headerIndex >= 0) {
            buildMap {
                rows[headerIndex].forEachIndexed { index, raw ->
                    val key = ParseUtils.normalize(raw)
                    if (key.isNotEmpty() && key !in this) put(key, index)
                }
            }
        } else {
            emptyMap()
        }

        fun firstIndex(names: Set<String>): Int? =
            names.firstNotNullOfOrNull { headers[it] }

        val dateIndex = firstIndex(dateNames) ?: if (headerIndex < 0) 0 else null
        val amountIndex = firstIndex(amountNames) ?: if (headerIndex < 0) 2 else null

        if (dateIndex == null || amountIndex == null) {
            throw ImportParseException(
                "No encontramos las columnas de fecha y monto. " +
                    "Revisa que el archivo tenga encabezados como Fecha, Descripción y Monto."
            )
        }

        var descriptionIndex = firstIndex(paymentNames)
        if (descriptionIndex == null) descriptionIndex = firstIndex(descriptionNames)

        var notesIndex = firstIndex(notesNames + descriptionNames)
        if (notesIndex == descriptionIndex) notesIndex = null

        val currencyIndex = firstIndex(currencyNames) ?: if (headerIndex < 0) 3 else null
        val paidIndex = firstIndex(paidNames) ?: if (headerIndex < 0) 4 else null
        val categoryIndex = firstIndex(categoryNames) ?: if (headerIndex < 0) 5 else null
        val subcategoryIndex = firstIndex(subcategoryNames) ?: if (headerIndex < 0) 6 else null
        val accountIndex = firstIndex(accountNames) ?: if (headerIndex < 0) 7 else null
        val goalIndex = firstIndex(goalNames) ?: if (headerIndex < 0) 8 else null
        val typeIndex = firstIndex(typeNames)

        val parsed = mutableListOf<ParsedRow>()
        val warnings = mutableListOf<String>()
        var omitted = 0
        val start = if (headerIndex >= 0) headerIndex + 1 else 0

        for (rowIndex in start until rows.size) {
            val row = rows[rowIndex]

            fun cell(index: Int?): String =
                index?.let { row.getOrNull(it)?.trim().orEmpty() } ?: ""

            val dateRaw = cell(dateIndex)
            if (dateRaw.isBlank()) continue

            val date = ParseUtils.parseDate(dateRaw)
            if (date == null) {
                omitted++
                if (warnings.size < 8) {
                    warnings += "Fila ${rowIndex + 1}: no entendimos la fecha “$dateRaw”."
                }
                continue
            }

            val amountRaw = cell(amountIndex)
            var amount = ParseUtils.parseAmount(amountRaw)
            if (amount == null || amount == 0L) {
                omitted++
                if (warnings.size < 8) {
                    warnings += "Fila ${rowIndex + 1}: el monto “$amountRaw” no es válido."
                }
                continue
            }

            val typeText = ParseUtils.normalize(cell(typeIndex))
            if (typeText.contains("ingreso") ||
                typeText.contains("income") ||
                typeText.contains("entrada")
            ) {
                amount = abs(amount)
            } else if (typeText.contains("gasto") ||
                typeText.contains("expense") ||
                typeText.contains("salida")
            ) {
                amount = -abs(amount)
            }

            val category = cell(categoryIndex).takeIf { it.isNotBlank() }
            var description = cell(descriptionIndex)
                .ifBlank { category.orEmpty() }
                .ifBlank { "Movimiento importado" }
                .replace(Regex("\\s+"), " ")
                .trim()

            val notes = buildList {
                cell(notesIndex).takeIf { it.isNotBlank() }?.let(::add)
                cell(subcategoryIndex).takeIf { it.isNotBlank() }?.let {
                    add("Subcategoría: $it")
                }
                cell(goalIndex).takeIf { it.isNotBlank() }?.let { add("Meta: $it") }
            }.joinToString(" · ").takeIf { it.isNotBlank() }

            parsed += ParsedRow(
                date = date,
                description = description,
                amount = amount,
                currency = cell(currencyIndex).ifBlank { "COP" }.uppercase(),
                paid = ParseUtils.parsePaid(cell(paidIndex).takeIf { it.isNotBlank() }),
                categoryName = category,
                accountName = cell(accountIndex).takeIf { it.isNotBlank() },
                notes = notes,
                line = rowIndex + 1
            )
        }

        if (omitted > warnings.size) {
            warnings += "Y ${omitted - warnings.size} filas más con datos incompletos."
        }

        if (parsed.isEmpty()) {
            throw ImportParseException(
                "No encontramos movimientos válidos en “$fileName”. " +
                    "Revisa que las fechas y los montos estén en columnas separadas."
            )
        }

        return parsed to warnings
    }
}
