package com.finanzasapp.domain.importing

import com.finanzasapp.R
import com.finanzasapp.data.settings.Idioma

/**
 * Lector CSV sin dependencias.
 *
 * Detecta el separador (coma, punto y coma, tabulación o barra) y respeta
 * comillas, comas y saltos de línea dentro de un campo.
 */
object CsvParser {

    fun parse(bytes: ByteArray, fileName: String): Pair<List<ParsedRow>, List<String>> {
        val text = ParseUtils.decodeText(bytes)
            .replace("\r\n", "\n")
            .replace('\r', '\n')

        if (text.isBlank()) throw ImportParseException(Idioma.text(R.string.mv_parse_empty))

        val rows = readRows(text)
        if (rows.isEmpty()) throw ImportParseException(Idioma.text(R.string.mv_parse_no_rows_found))

        return TableParser.parse(rows, fileName)
    }

    private fun readRows(text: String): List<List<String>> {
        val delimiter = detectDelimiter(text)
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var index = 0

        while (index < text.length) {
            val char = text[index]
            if (inQuotes) {
                if (char == '"') {
                    if (index + 1 < text.length && text[index + 1] == '"') {
                        cell.append('"')
                        index++
                    } else {
                        inQuotes = false
                    }
                } else {
                    cell.append(char)
                }
            } else {
                when (char) {
                    '"' -> inQuotes = true
                    delimiter -> {
                        row.add(cell.toString().trim())
                        cell.clear()
                    }
                    '\n' -> {
                        row.add(cell.toString().trim())
                        cell.clear()
                        if (row.any { it.isNotEmpty() }) rows.add(row)
                        row = mutableListOf()
                    }
                    else -> cell.append(char)
                }
            }
            index++
        }

        row.add(cell.toString().trim())
        if (row.any { it.isNotEmpty() }) rows.add(row)
        return rows
    }

    private fun detectDelimiter(text: String): Char {
        val lines = text.lineSequence().filter { it.isNotBlank() }.take(5).toList()
        val candidates = listOf(';', ',', '\t', '|')
        return candidates.maxByOrNull { delimiter ->
            lines.sumOf { line -> countOutsideQuotes(line, delimiter) }
        } ?: ','
    }

    private fun countOutsideQuotes(line: String, delimiter: Char): Int {
        var inQuotes = false
        var count = 0
        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == delimiter && !inQuotes -> count++
            }
        }
        return count
    }
}
