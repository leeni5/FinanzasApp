package com.finanzasapp.data.importing

import android.content.Context
import com.finanzasapp.data.settings.Idioma
import com.finanzasapp.R
import androidx.room.withTransaction
import com.finanzasapp.data.dao.AccountDao
import com.finanzasapp.data.dao.CategoryDao
import com.finanzasapp.data.db.AppDatabase
import com.finanzasapp.data.entity.Account
import com.finanzasapp.data.entity.AccountType
import com.finanzasapp.data.entity.AuditLog
import com.finanzasapp.data.entity.Category
import com.finanzasapp.data.entity.ImportJob
import com.finanzasapp.data.entity.ImportKind
import com.finanzasapp.data.entity.ImportStatus
import com.finanzasapp.data.entity.Transaction
import com.finanzasapp.data.entity.TransactionSource
import com.finanzasapp.data.entity.TransactionType
import com.finanzasapp.data.model.displayName
import com.finanzasapp.data.rules.RuleActions
import com.finanzasapp.domain.dedupe.UniqueHash
import com.finanzasapp.domain.importing.BudgeTextParser
import com.finanzasapp.domain.importing.CsvParser
import com.finanzasapp.domain.importing.ImportFormat
import com.finanzasapp.domain.importing.ImportParseException
import com.finanzasapp.domain.importing.ImportPreview
import com.finanzasapp.domain.importing.ImportResult
import com.finanzasapp.domain.importing.ImportRowPreview
import com.finanzasapp.domain.importing.ParseUtils
import com.finanzasapp.domain.importing.ParsedImport
import com.finanzasapp.domain.importing.ParsedRow
import com.finanzasapp.domain.importing.PdfParser
import com.finanzasapp.domain.importing.XlsxParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import kotlin.math.abs

/**
 * Puente entre los archivos y la base cifrada.
 *
 * Primero arma una vista previa sin escribir nada; solo cuando el usuario
 * confirma se crean las categorías/cuentas que falten y se insertan los
 * movimientos. Todo ocurre en una transacción y queda auditado.
 */
class ImportService(
    private val context: Context,
    private val database: AppDatabase
) {

    private val categoryDao: CategoryDao get() = database.categoryDao()
    private val accountDao: AccountDao get() = database.accountDao()

    suspend fun parse(
        fileName: String,
        bytes: ByteArray,
        format: ImportFormat
    ): ParsedImport = withContext(Dispatchers.Default) {
        val (rows, warnings) = when (format) {
            ImportFormat.CSV -> CsvParser.parse(bytes, fileName)
            ImportFormat.XLSX -> XlsxParser.parse(bytes, fileName)
            ImportFormat.PDF -> PdfParser.parse(context, bytes) to emptyList()
        }

        val usable = rows.filter { row ->
            row.currency.isBlank() || row.currency.equals("COP", ignoreCase = true)
        }
        val skippedCurrency = rows.size - usable.size
        val finalWarnings = warnings.toMutableList()
        if (skippedCurrency > 0) {
            finalWarnings += Idioma.text(R.string.mv_import_non_cop, skippedCurrency)
        }

        if (usable.isEmpty()) {
            throw ImportParseException(
                Idioma.text(R.string.mv_import_no_cop)
            )
        }

        ParsedImport(
            fileName = fileName,
            format = format,
            rows = usable,
            warnings = finalWarnings
        )
    }

    suspend fun buildPreview(
        parsed: ParsedImport,
        defaultAccountId: Long? = null
    ): ImportPreview = withContext(Dispatchers.IO) {
        val categories = categoryDao.all()
        val accounts = accountDao.all()
        val activeAccounts = accounts.filter { !it.archived }
        val activeCategories = categories.filter { !it.archived }

        val categoryByKey = activeCategories.associateBy { ParseUtils.normalize(it.name) }
        val accountByKey = activeAccounts.associateBy { ParseUtils.normalize(it.name) }

        val fallbackAccount = defaultAccountId
            ?.let { id -> activeAccounts.firstOrNull { it.id == id } }
            ?: activeAccounts.firstOrNull { ParseUtils.normalize(it.name) == "efectivo" }
            ?: activeAccounts.firstOrNull()

        val fallbackIsNew = fallbackAccount == null
        val fallbackName = fallbackAccount?.name ?: "Efectivo"

        val newCategories = linkedSetOf<String>()
        val newAccounts = linkedSetOf<String>()
        if (fallbackIsNew) newAccounts += fallbackName

        val existingBaseCounts = mutableMapOf<String, Int>()
        for (hash in database.transactionDao().allHashes()) {
            val base = hash.substringBefore('#')
            existingBaseCounts[base] = (existingBaseCounts[base] ?: 0) + 1
        }

        val localCounts = mutableMapOf<String, Int>()
        val previews = mutableListOf<ImportRowPreview>()
        var rowsWithoutAccount = 0

        for (row in parsed.rows) {
            val resolvedCategory = resolveCategory(row.categoryName, categoryByKey)
            val resolvedAccount = resolveAccount(row.accountName, activeAccounts, accountByKey)
            val account = resolvedAccount ?: fallbackAccount
            if (row.accountName.isNullOrBlank()) rowsWithoutAccount++

            val baseHash = if (account != null) {
                UniqueHash.of(
                    accountId = account.id,
                    date = row.date,
                    amount = abs(row.amount),
                    description = row.description
                )
            } else {
                null
            }

            val duplicate = if (baseHash != null) {
                val occurrence = (localCounts[baseHash] ?: 0) + 1
                localCounts[baseHash] = occurrence
                occurrence <= (existingBaseCounts[baseHash] ?: 0)
            } else {
                false
            }

            if (!duplicate) {
                if (resolvedCategory == null && !row.categoryName.isNullOrBlank()) {
                    newCategories += row.categoryName.trim()
                }
                if (resolvedAccount == null && !row.accountName.isNullOrBlank()) {
                    newAccounts += row.accountName.trim()
                }
            }

            previews += ImportRowPreview(
                date = row.date,
                description = row.description,
                amount = row.amount,
                type = if (row.amount >= 0L) TransactionType.INGRESO else TransactionType.GASTO,
                categoryName = row.categoryName,
                accountName = row.accountName,
                currency = row.currency,
                paid = row.paid,
                notes = row.notes,
                duplicate = duplicate
            )
        }

        val importable = previews.filterNot { it.duplicate }

        ImportPreview(
            fileName = parsed.fileName,
            format = parsed.format,
            detected = previews.size,
            rows = previews,
            importable = importable.size,
            duplicates = previews.size - importable.size,
            income = importable.sumOf { if (it.amount > 0L) it.amount else 0L },
            expenses = importable.sumOf { if (it.amount < 0L) -it.amount else 0L },
            newCategories = newCategories.toList(),
            newAccounts = newAccounts.toList(),
            warnings = parsed.warnings,
            defaultAccountName = fallbackName,
            rowsWithoutAccount = rowsWithoutAccount,
            latestDate = importable.maxOfOrNull { it.date }
        )
    }

    suspend fun import(preview: ImportPreview): ImportResult = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                val now = Instant.now()
                val categories = categoryDao.all().toMutableList()
                val accounts = accountDao.all().toMutableList()

                val categoryByKey = categories
                    .filter { !it.archived }
                    .associateBy { ParseUtils.normalize(it.name) }
                    .toMutableMap()
                val accountByKey = accounts
                    .filter { !it.archived }
                    .associateBy { ParseUtils.normalize(it.name) }
                    .toMutableMap()

                var nextCategoryOrder = (categories.maxOfOrNull { it.sortOrder } ?: -1) + 1

                for (name in preview.newCategories) {
                    val key = ParseUtils.normalize(name)
                    if (key.isBlank() || categoryByKey.containsKey(key)) continue
                    val category = Category(
                        name = name.trim(),
                        monthlyBudget = 0L,
                        color = colorFor(nextCategoryOrder),
                        sortOrder = nextCategoryOrder
                    )
                    val id = categoryDao.insert(category)
                    categoryByKey[key] = category.copy(id = id)
                    nextCategoryOrder++
                }

                for (name in preview.newAccounts) {
                    val key = ParseUtils.normalize(name)
                    if (key.isBlank() || accountByKey.containsKey(key)) continue
                    val type = if (key.contains("efectivo")) AccountType.EFECTIVO else AccountType.DEBITO
                    val account = Account(
                        name = name.trim(),
                        type = type,
                        initialBalance = 0L,
                        color = colorFor(accountByKey.size)
                    )
                    val id = accountDao.insert(account)
                    accountByKey[key] = account.copy(id = id)
                }

                val fallbackAccount = preview.defaultAccountName
                    ?.let { name -> accountByKey[ParseUtils.normalize(name)] }
                    ?: accountByKey.values.firstOrNull()

                if (fallbackAccount == null) {
                    throw ImportParseException(
                        Idioma.text(R.string.mv_import_no_account)
                    )
                }

                val existingBaseCounts = mutableMapOf<String, Int>()
                for (hash in database.transactionDao().allHashes()) {
                    val base = hash.substringBefore('#')
                    existingBaseCounts[base] = (existingBaseCounts[base] ?: 0) + 1
                }
                val localCounts = mutableMapOf<String, Int>()
                val toInsert = mutableListOf<Transaction>()
                // Las reglas solo rellenan filas que el archivo trae sin categoría.
                val rules = RuleActions.matcher(database)
                val ruleHits = mutableMapOf<Long, Int>()

                for (row in preview.rows) {
                    val rule = if (row.categoryName.isNullOrBlank() ||
                        resolveCategory(row.categoryName, categoryByKey) == null
                    ) {
                        rules.match(row.description)
                    } else {
                        null
                    }
                    val categoryId = resolveCategory(row.categoryName, categoryByKey)?.id
                        ?: rule?.categoryId
                    val account = resolveAccount(row.accountName, accountByKey.values.toList(), accountByKey)
                        ?: fallbackAccount

                    val amount = abs(row.amount)
                    if (amount == 0L) continue

                    val baseHash = UniqueHash.of(
                        accountId = account.id,
                        date = row.date,
                        amount = amount,
                        description = row.description
                    )
                    val occurrence = (localCounts[baseHash] ?: 0) + 1
                    localCounts[baseHash] = occurrence

                    if (occurrence <= (existingBaseCounts[baseHash] ?: 0)) continue

                    val hash = if (occurrence == 1) baseHash else "$baseHash#$occurrence"

                    if (rule != null && categoryId == rule.categoryId) {
                        ruleHits[rule.id] = (ruleHits[rule.id] ?: 0) + 1
                    }

                    toInsert += Transaction(
                        accountId = account.id,
                        date = row.date,
                        descriptionOriginal = row.description,
                        merchant = row.description,
                        amount = amount,
                        type = row.type,
                        categoryId = categoryId,
                        // Pendientes de pago o sin categoría: a la bandeja «Por revisar».
                        requiresReview = !row.paid || categoryId == null,
                        uniqueHash = hash,
                        source = preview.format.toSource(),
                        createdAt = now,
                        updatedAt = now,
                        notes = row.notes
                    )
                }

                val ids = database.transactionDao().insertAll(toInsert)
                ruleHits.forEach { (ruleId, times) ->
                    database.systemDao().addRuleApplications(ruleId, times)
                }
                val audits = toInsert.mapIndexed { index, transaction ->
                    AuditLog(
                        entity = "transaction",
                        entityId = ids.getOrElse(index) { 0L },
                        action = "CREADO",
                        afterJson = transaction.toAuditJson(),
                        source = "importacion",
                        at = now
                    )
                }
                if (audits.isNotEmpty()) {
                    database.systemDao().insertAudits(audits)
                }

                val job = ImportJob(
                    fileName = preview.fileName,
                    kind = preview.format.toKind(),
                    status = ImportStatus.COMPLETADO,
                    detected = preview.detected,
                    imported = toInsert.size,
                    error = null,
                    createdAt = now
                )
                val jobId = database.systemDao().insertImport(job)
                database.systemDao().insertAudit(
                    AuditLog(
                        entity = "import",
                        entityId = jobId,
                        action = "COMPLETADO",
                        afterJson = JSONObject()
                            .put("archivo", preview.fileName)
                            .put("detectados", preview.detected)
                            .put("importados", toInsert.size)
                            .put("duplicados", preview.detected - toInsert.size)
                            .toString(),
                        source = "importacion",
                        at = now
                    )
                )

                ImportResult(
                    imported = toInsert.size,
                    duplicates = preview.detected - toInsert.size,
                    newCategories = preview.newCategories,
                    newAccounts = preview.newAccounts,
                    latestDate = toInsert.maxOfOrNull { it.date }
                )
            }
        } catch (error: Exception) {
            runCatching {
                database.systemDao().insertImport(
                    ImportJob(
                        fileName = preview.fileName,
                        kind = preview.format.toKind(),
                        status = ImportStatus.ERROR,
                        detected = preview.detected,
                        imported = 0,
                        error = error.message?.take(300),
                        createdAt = Instant.now()
                    )
                )
            }
            throw error
        }
    }

    // ------------------------------------------------------------- resolución

    private fun resolveCategory(
        name: String?,
        categories: Map<String, Category>
    ): Category? {
        if (name.isNullOrBlank()) return null
        val key = ParseUtils.normalize(name)
        categories[key]?.let { return it }

        val alias = when (key) {
            "otro" -> "otros"
            "restaurante" -> "restaurantes"
            "servicio" -> "servicios"
            "ingreso" -> "ingresos"
            else -> null
        }
        return alias?.let(categories::get)
    }

    private fun resolveAccount(
        name: String?,
        accounts: List<Account>,
        byKey: Map<String, Account>
    ): Account? {
        if (name.isNullOrBlank()) return null
        val key = ParseUtils.normalize(name)
        if (key.isBlank()) return null
        byKey[key]?.let { return it }

        return accounts.firstOrNull { account ->
            val accountKey = ParseUtils.normalize(account.name)
            accountKey == key || accountKey.contains(key) || key.contains(accountKey)
        }
    }

    private fun colorFor(index: Int): String = COLORS[index.mod(COLORS.size)]

    private fun ImportFormat.toKind(): ImportKind = when (this) {
        ImportFormat.CSV -> ImportKind.CSV
        ImportFormat.XLSX -> ImportKind.XLSX
        ImportFormat.PDF -> ImportKind.PDF
    }

    private fun ImportFormat.toSource(): TransactionSource = when (this) {
        ImportFormat.CSV -> TransactionSource.CSV
        ImportFormat.XLSX -> TransactionSource.XLSX
        ImportFormat.PDF -> TransactionSource.PDF
    }

    private fun Transaction.toAuditJson(): String = JSONObject()
        .put("cuenta", accountId)
        .put("fecha", date.toString())
        .put("descripcion", displayName())
        .put("monto", amount)
        .put("tipo", type.name)
        .put("categoria", categoryId ?: JSONObject.NULL)
        .put("origen", source.name)
        .toString()

    companion object {
        // Sin rojos, naranjas ni ámbar: esos tonos son de las alertas.
        private val COLORS = listOf(
            "#4C5BD4", "#2E86C8", "#7C5CE0", "#148F84", "#0E9AB5",
            "#A24FB8", "#6A9A1F", "#D9548C", "#2F9E63", "#5B6B82"
        )
    }
}
