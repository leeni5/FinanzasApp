package com.finanzasapp.domain.importing

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/**
 * Lector mínimo de XLSX.
 *
 * Un XLSX es un ZIP con XML. Solo se leen la primera hoja y la tabla de
 * textos compartidos, que es lo que necesitan las exportaciones financieras.
 * Así no se arrastra Apache POI ni se pide ningún permiso extra.
 */
object XlsxParser {

    fun parse(bytes: ByteArray, fileName: String): Pair<List<ParsedRow>, List<String>> {
        val rows = readRows(bytes)
        if (rows.isEmpty()) {
            throw ImportParseException("La hoja de cálculo está vacía o no tiene una hoja legible.")
        }
        return TableParser.parse(rows, fileName)
    }

    private fun readRows(bytes: ByteArray): List<List<String>> {
        val entries = mutableMapOf<String, ByteArray>()

        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (!entry.isDirectory && (
                            name == "xl/sharedStrings.xml" ||
                                name.startsWith("xl/worksheets/") && name.endsWith(".xml")
                            )
                    ) {
                        entries[name] = zip.readBytes()
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (error: Exception) {
            throw ImportParseException(
                "No pudimos abrir el archivo de Excel. " +
                    "Guárdalo de nuevo como .xlsx e inténtalo otra vez."
            )
        }

        val sheet = entries.keys
            .filter { it.startsWith("xl/worksheets/") && it.endsWith(".xml") }
            .sorted()
            .firstOrNull()
            ?: throw ImportParseException("El archivo de Excel no contiene ninguna hoja.")

        val shared = entries["xl/sharedStrings.xml"]?.let(::readSharedStrings).orEmpty()
        return readSheet(entries.getValue(sheet), shared)
    }

    private fun readSharedStrings(bytes: ByteArray): List<String> {
        val result = mutableListOf<String>()
        val parser = newParser(bytes)
        var current: StringBuilder? = null
        var inText = false
        var inPhonetic = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "si" -> current = StringBuilder()
                    "rPh" -> inPhonetic = true
                    "t" -> if (!inPhonetic) inText = true
                }

                XmlPullParser.TEXT -> if (inText && current != null) {
                    current?.append(parser.text)
                }

                XmlPullParser.END_TAG -> when (parser.name) {
                    "t" -> inText = false
                    "rPh" -> inPhonetic = false
                    "si" -> {
                        result += current?.toString().orEmpty()
                        current = null
                    }
                }
            }
            event = parser.next()
        }
        return result
    }

    private fun readSheet(bytes: ByteArray, shared: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val parser = newParser(bytes)
        var cells: MutableMap<Int, String>? = null
        var maxColumn = -1
        var cellReference: String? = null
        var cellType: String? = null
        var value: StringBuilder? = null
        var inValue = false
        var inInlineText = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> {
                        cells = mutableMapOf()
                        maxColumn = -1
                    }

                    "c" -> {
                        cellReference = parser.getAttributeValue(null, "r")
                        cellType = parser.getAttributeValue(null, "t")
                        value = StringBuilder()
                        inValue = false
                        inInlineText = false
                    }

                    "v" -> inValue = true
                    "t" -> if (cellType == "inlineStr") inInlineText = true
                }

                XmlPullParser.TEXT -> if (inValue || inInlineText) {
                    value?.append(parser.text)
                }

                XmlPullParser.END_TAG -> when (parser.name) {
                    "v" -> inValue = false
                    "t" -> inInlineText = false

                    "c" -> {
                        val column = columnIndex(cellReference)
                        val raw = value?.toString().orEmpty()
                        if (column >= 0) {
                            val resolved = if (cellType == "s") {
                                shared.getOrNull(raw.trim().toIntOrNull() ?: -1).orEmpty()
                            } else {
                                raw
                            }
                            cells?.put(column, resolved)
                            if (column > maxColumn) maxColumn = column
                        }
                        cellReference = null
                        cellType = null
                        value = null
                    }

                    "row" -> {
                        val current = cells
                        if (current != null && current.isNotEmpty()) {
                            val row = MutableList(maxColumn + 1) { "" }
                            current.forEach { (column, text) ->
                                if (column in row.indices) row[column] = text
                            }
                            if (row.any { it.isNotBlank() }) rows.add(row)
                        }
                        cells = null
                        maxColumn = -1
                    }
                }
            }
            event = parser.next()
        }
        return rows
    }

    private fun newParser(bytes: ByteArray): XmlPullParser {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(ByteArrayInputStream(bytes), null)
        return parser
    }

    private fun columnIndex(reference: String?): Int {
        if (reference.isNullOrBlank()) return -1
        var column = 0
        for (char in reference) {
            when (char) {
                in 'A'..'Z' -> column = column * 26 + (char - 'A' + 1)
                in 'a'..'z' -> column = column * 26 + (char - 'a' + 1)
                else -> break
            }
        }
        return column - 1
    }
}
