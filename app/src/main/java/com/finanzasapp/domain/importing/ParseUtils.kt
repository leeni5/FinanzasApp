package com.finanzasapp.domain.importing

import com.finanzasapp.domain.dedupe.UniqueHash
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Conversiones compartidas por CSV, XLSX y PDF.
 *
 * Todo se interpreta como lo haría una persona en Colombia: día primero,
 * separador de miles con punto y moneda COP por defecto.
 */
object ParseUtils {

    private val spanish = Locale.forLanguageTag("es-CO")

    private val dateFormats = listOf(
        "d-M-yyyy",
        "d/M/yyyy",
        "d.M.yyyy",
        "yyyy-M-d",
        "yyyy/M/d",
        "d-M-yy",
        "d/M/yy",
        "d 'de' MMMM 'de' yyyy",
        "d MMM yyyy",
        "MMM d, yyyy"
    ).map { DateTimeFormatter.ofPattern(it, spanish) }

    fun normalize(value: String): String = UniqueHash.normalize(value)

    /**
     * Acepta "210000", "-2.500", "1.234,56", "$ 1.234", "(2.500)" y similares.
     * Devuelve pesos enteros; los centavos se redondean al peso más cercano.
     */
    fun parseAmount(raw: String): Long? {
        val text = raw.trim()
        if (text.isEmpty()) return null

        val negative = text.startsWith("-") ||
            text.endsWith("-") ||
            text.startsWith("−") ||
            (text.startsWith("(") && text.endsWith(")"))

        var digits = text
            .replace('−', '-')
            .replace(Regex("[^0-9.,]"), "")

        if (digits.isEmpty()) return null
        digits = digits.replace("-", "")
        if (digits.isEmpty()) return null

        val commaCount = digits.count { it == ',' }
        val dotCount = digits.count { it == '.' }
        val decimal = when {
            commaCount > 0 && dotCount > 0 ->
                if (digits.lastIndexOf(',') > digits.lastIndexOf('.')) ',' else '.'

            commaCount == 1 && dotCount == 0 ->
                if (digits.length - digits.lastIndexOf(',') - 1 in 1..2) ',' else null

            dotCount == 1 && commaCount == 0 ->
                if (digits.length - digits.lastIndexOf('.') - 1 in 1..2) '.' else null

            else -> null
        }

        val normalized = if (decimal != null) {
            val index = digits.lastIndexOf(decimal)
            val integer = digits.substring(0, index).replace(Regex("[.,]"), "")
            val fraction = digits.substring(index + 1).replace(Regex("[.,]"), "")
            "$integer.$fraction"
        } else {
            digits.replace(Regex("[.,]"), "")
        }

        val value = normalized.toBigDecimalOrNull() ?: return null
        val signed = if (negative) value.negate() else value
        return signed.setScale(0, RoundingMode.HALF_UP).toLong()
    }

    /**
     * Fechas de Budge (20-04-2026), ISO (2026-04-20) y seriales de Excel.
     * Si no se reconoce, devuelve null para que la fila se reporte como omitida.
     */
    fun parseDate(raw: String): LocalDate? {
        val text = raw.trim()
        if (text.isEmpty()) return null

        text.toDoubleOrNull()?.let { serial ->
            if (serial in 1.0..80_000.0) return excelDate(serial)
        }

        for (formatter in dateFormats) {
            try {
                return LocalDate.parse(text, formatter)
            } catch (_: DateTimeParseException) {
                // Se prueba el siguiente formato.
            }
        }
        return null
    }

    fun parsePaid(raw: String?): Boolean {
        val value = raw?.trim()?.lowercase(Locale.ROOT) ?: return true
        if (value.isEmpty()) return true
        return value !in setOf("false", "no", "0", "n", "pendiente", "unpaid", "sin pagar")
    }

    fun decodeText(bytes: ByteArray): String {
        if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
        ) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        }

        val utf8 = String(bytes, Charsets.UTF_8)
        return if ('\uFFFD' in utf8) {
            String(bytes, Charset.forName("windows-1252"))
        } else {
            utf8
        }
    }

    fun detectFormat(fileName: String, mime: String?, bytes: ByteArray): ImportFormat {
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val type = mime?.lowercase(Locale.ROOT).orEmpty()

        if (extension == "pdf" || type == "application/pdf") return ImportFormat.PDF
        if (extension == "xlsx" || extension == "xlsm" || type.contains("spreadsheetml")) {
            return ImportFormat.XLSX
        }

        if (bytes.size >= 5 && String(bytes, 0, 5, Charsets.US_ASCII) == "%PDF-") {
            return ImportFormat.PDF
        }
        if (bytes.size >= 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()) {
            return ImportFormat.XLSX
        }

        return ImportFormat.CSV
    }

    private fun excelDate(serial: Double): LocalDate {
        // Excel cuenta desde 1899-12-30 y arrastra el error del año bisiesto de 1900.
        return LocalDate.of(1899, 12, 30).plusDays(serial.toLong())
    }
}
