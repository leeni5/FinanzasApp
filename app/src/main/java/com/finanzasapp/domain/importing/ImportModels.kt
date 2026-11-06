package com.finanzasapp.domain.importing

import com.finanzasapp.data.entity.TransactionType
import java.time.LocalDate

/** Formatos que la app puede leer sin conexión. */
enum class ImportFormat(val label: String) {
    CSV("CSV"),
    XLSX("Excel"),
    PDF("PDF")
}

/**
 * Una fila ya convertida a tipos seguros.
 * [amount] conserva el signo del archivo: negativo es gasto, positivo es ingreso.
 */
data class ParsedRow(
    val date: LocalDate,
    val description: String,
    val amount: Long,
    val currency: String = "COP",
    val paid: Boolean = true,
    val categoryName: String? = null,
    val accountName: String? = null,
    val notes: String? = null,
    val line: Int = 0
)

data class ParsedImport(
    val fileName: String,
    val format: ImportFormat,
    val rows: List<ParsedRow>,
    val warnings: List<String>
)

data class ImportRowPreview(
    val date: LocalDate,
    val description: String,
    val amount: Long,
    val type: TransactionType,
    val categoryName: String?,
    val accountName: String?,
    val currency: String,
    val paid: Boolean,
    val notes: String?,
    val duplicate: Boolean
)

/**
 * Lo que el usuario ve antes de confirmar.
 * Nada se escribe en la base hasta que se llama a ImportService.import().
 */
data class ImportPreview(
    val fileName: String,
    val format: ImportFormat,
    val detected: Int,
    val rows: List<ImportRowPreview>,
    val importable: Int,
    val duplicates: Int,
    val income: Long,
    val expenses: Long,
    val newCategories: List<String>,
    val newAccounts: List<String>,
    val warnings: List<String>,
    val defaultAccountName: String?,
    val rowsWithoutAccount: Int,
    val latestDate: LocalDate?
)

data class ImportResult(
    val imported: Int,
    val duplicates: Int,
    val newCategories: List<String>,
    val newAccounts: List<String>,
    val latestDate: LocalDate?
)

/** Error legible para el usuario; nunca muestra una traza técnica en pantalla. */
class ImportParseException(message: String) : Exception(message)
