package com.finanzasapp.data.backup

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.room.withTransaction
import com.finanzasapp.R
import com.finanzasapp.data.db.AppDatabase
import com.finanzasapp.data.settings.Idioma
import com.finanzasapp.data.entity.AuditLog
import com.finanzasapp.data.entity.TransactionType
import com.finanzasapp.data.model.displayName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.time.Instant
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Error que se le puede mostrar al usuario tal cual. */
class BackupException(message: String) : Exception(message)

/** Lo que trae una copia ya descifrada, antes de restaurarla. */
class BackupContents internal constructor(
    val createdAt: Instant,
    val counts: Map<String, Int>,
    internal val tables: JSONObject
) {
    val movements: Int get() = counts["transactions"] ?: 0
    val accounts: Int get() = counts["accounts"] ?: 0
    val categories: Int get() = counts["categories"] ?: 0
    val goals: Int get() = counts["savings_goals"] ?: 0
    val payments: Int get() = counts["scheduled_payments"] ?: 0
}

/**
 * Copia de seguridad y exportación.
 *
 * La base vive cifrada con una llave del Keystore que no sale del teléfono, así
 * que una copia del archivo no serviría en otro equipo. La copia es otra cosa:
 * todas las tablas en JSON, comprimidas y cifradas con AES-256-GCM a partir de
 * una contraseña que elige el usuario (PBKDF2, 210.000 vueltas). Sin esa
 * contraseña el archivo es ilegible; si se olvida, la copia no se recupera.
 *
 * Formato: "FINZ" + versión (1 byte) + sal (16) + IV (12) + texto cifrado.
 */
object BackupService {

    private val MAGIC = "FINZ".toByteArray(Charsets.US_ASCII)
    private const val FORMAT_VERSION: Byte = 1
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val ITERATIONS = 210_000
    private const val KEY_BITS = 256

    const val MIN_PASSWORD = 8

    /**
     * Orden en que se insertan las tablas al restaurar (padres antes que hijos).
     * Al borrar se recorre al revés.
     */
    private val TABLES = listOf(
        "accounts",
        "categories",
        "category_budgets",
        "categorization_rules",
        "transactions",
        "savings_goals",
        "goal_contributions",
        "scheduled_payments",
        "import_jobs",
        "audit_log"
    )

    // ------------------------------------------------------------ copia

    suspend fun createBackup(
        context: Context,
        database: AppDatabase,
        uri: Uri,
        password: CharArray
    ): Int = withContext(Dispatchers.IO) {
        if (password.size < MIN_PASSWORD) {
            throw BackupException(Idioma.text(R.string.aj_err_password_short, MIN_PASSWORD))
        }
        val tables = JSONObject()
        var movements = 0
        database.withTransaction {
            val db = database.openHelper.writableDatabase
            for (table in TABLES) {
                val rows = JSONArray()
                db.query("SELECT * FROM $table").use { cursor ->
                    while (cursor.moveToNext()) rows.put(cursor.rowToJson())
                }
                if (table == "transactions") movements = rows.length()
                tables.put(table, rows)
            }
        }
        val payload = JSONObject()
            .put("app", "FinanzasApp")
            .put("schema", AppDatabase.SCHEMA_VERSION)
            .put("createdAt", Instant.now().toString())
            .put("tables", tables)
            .toString()
            .toByteArray(Charsets.UTF_8)

        val encrypted = encrypt(gzip(payload), password)
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw BackupException(Idioma.text(R.string.aj_err_write))
        stream.use { it.write(encrypted) }
        database.systemDao().insertAudit(
            AuditLog(
                entity = "backup",
                entityId = 0L,
                action = "COPIA",
                afterJson = JSONObject().put("movimientos", movements).toString(),
                source = "usuario",
                at = Instant.now()
            )
        )
        movements
    }

    /** Descifra y valida la copia sin tocar la base. */
    suspend fun readBackup(context: Context, uri: Uri, password: CharArray): BackupContents =
        withContext(Dispatchers.IO) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw BackupException(Idioma.text(R.string.aj_err_read))
            val json = try {
                JSONObject(String(gunzip(decrypt(bytes, password)), Charsets.UTF_8))
            } catch (error: BackupException) {
                throw error
            } catch (error: Exception) {
                throw BackupException(Idioma.text(R.string.aj_err_damaged))
            }
            if (json.optString("app") != "FinanzasApp") {
                throw BackupException(Idioma.text(R.string.aj_err_not_backup))
            }
            val schema = json.optInt("schema", -1)
            // Copias de versiones anteriores (desde la v4) se adaptan al restaurar;
            // una copia de una app más nueva no se puede leer aquí.
            if (schema < 4 || schema > AppDatabase.SCHEMA_VERSION) {
                throw BackupException(
                    Idioma.text(R.string.aj_err_schema, schema, AppDatabase.SCHEMA_VERSION)
                )
            }
            val tables = json.getJSONObject("tables")
            BackupContents(
                createdAt = runCatching { Instant.parse(json.getString("createdAt")) }
                    .getOrDefault(Instant.EPOCH),
                counts = TABLES.associateWith { tables.optJSONArray(it)?.length() ?: 0 },
                tables = tables
            )
        }

    /**
     * Reemplaza TODO lo que hay en la app por el contenido de la copia, en una
     * sola transacción: si algo falla, la base queda como estaba.
     */
    suspend fun restore(database: AppDatabase, contents: BackupContents) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val db = database.openHelper.writableDatabase
            for (table in TABLES.asReversed()) db.execSQL("DELETE FROM $table")
            for (table in TABLES) {
                val rows = contents.tables.optJSONArray(table) ?: continue
                for (index in 0 until rows.length()) {
                    db.insert(table, SQLiteDatabase.CONFLICT_ABORT, rows.getJSONObject(index).toContentValues())
                }
            }
            // Copias de la v4: no traen presupuestos por mes; cada categoría
            // arranca con su límite actual, igual que en la migración.
            if (!contents.tables.has("category_budgets")) db.execSQL(AppDatabase.SEED_BUDGETS_SQL)
            database.systemDao().insertAudit(
                AuditLog(
                    entity = "backup",
                    entityId = 0L,
                    action = "RESTAURADO",
                    afterJson = JSONObject()
                        .put("copiaDel", contents.createdAt.toString())
                        .put("movimientos", contents.movements)
                        .toString(),
                    source = "usuario",
                    at = Instant.now()
                )
            )
        }
    }

    // ------------------------------------------------------------ CSV

    /**
     * Movimientos en CSV con las columnas que entiende la importación:
     * Fecha, Tipo, Descripción, Monto (negativo en gastos), Categoría, Cuenta, Notas.
     * Va sin cifrar: es para abrirlo en una hoja de cálculo.
     */
    suspend fun exportCsv(context: Context, database: AppDatabase, uri: Uri): Int =
        withContext(Dispatchers.IO) {
            val transactions = database.transactionDao().all()
            val categories = database.categoryDao().all().associateBy { it.id }
            val accounts = database.accountDao().all().associateBy { it.id }
            val csv = buildString {
                append('﻿') // BOM: Excel abre las tildes bien.
                appendLine(Idioma.text(R.string.aj_csv_header))
                for (t in transactions) {
                    val (type, signed) = when (t.type) {
                        TransactionType.INGRESO -> Idioma.text(R.string.aj_csv_income) to t.amount
                        TransactionType.GASTO -> Idioma.text(R.string.aj_csv_expense) to -t.amount
                        TransactionType.TRANSFERENCIA ->
                            if (t.transferOut) Idioma.text(R.string.aj_csv_transfer_out) to -t.amount
                            else Idioma.text(R.string.aj_csv_transfer_in) to t.amount
                    }
                    appendLine(
                        listOf(
                            t.date.toString(),
                            type,
                            t.displayName(),
                            signed.toString(),
                            t.categoryId?.let(categories::get)?.name.orEmpty(),
                            accounts[t.accountId]?.name.orEmpty(),
                            t.notes.orEmpty()
                        ).joinToString(",") { it.csvCell() }
                    )
                }
            }
            val stream = context.contentResolver.openOutputStream(uri, "wt")
                ?: throw BackupException(Idioma.text(R.string.aj_err_write))
            stream.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
            database.systemDao().insertAudit(
                AuditLog(
                    entity = "backup",
                    entityId = 0L,
                    action = "EXPORTADO",
                    afterJson = JSONObject().put("movimientos", transactions.size).toString(),
                    source = "usuario",
                    at = Instant.now()
                )
            )
            transactions.size
        }

    private fun String.csvCell(): String =
        if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + replace("\"", "\"\"") + "\""
        } else {
            this
        }

    // ------------------------------------------------------------ filas

    private fun Cursor.rowToJson(): JSONObject {
        val row = JSONObject()
        for (i in 0 until columnCount) {
            val name = getColumnName(i)
            when (getType(i)) {
                Cursor.FIELD_TYPE_NULL -> row.put(name, JSONObject.NULL)
                Cursor.FIELD_TYPE_INTEGER -> row.put(name, getLong(i))
                Cursor.FIELD_TYPE_FLOAT -> row.put(name, getDouble(i))
                else -> row.put(name, getString(i))
            }
        }
        return row
    }

    private fun JSONObject.toContentValues(): ContentValues {
        val values = ContentValues()
        for (key in keys()) {
            when (val value = get(key)) {
                JSONObject.NULL -> values.putNull(key)
                is Int -> values.put(key, value.toLong())
                is Long -> values.put(key, value)
                is Double -> values.put(key, value)
                is Boolean -> values.put(key, if (value) 1L else 0L)
                else -> values.put(key, value.toString())
            }
        }
        return values
    }

    // ------------------------------------------------------------ cifrado

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_BITS)
        try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun encrypt(plain: ByteArray, password: CharArray): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(128, iv))
        cipher.updateAAD(MAGIC)
        val body = cipher.doFinal(plain)
        return MAGIC + byteArrayOf(FORMAT_VERSION) + salt + iv + body
    }

    private fun decrypt(file: ByteArray, password: CharArray): ByteArray {
        val header = MAGIC.size + 1
        if (file.size < header + SALT_BYTES + IV_BYTES + 16 ||
            !file.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)
        ) {
            throw BackupException(Idioma.text(R.string.aj_err_not_backup))
        }
        if (file[MAGIC.size] != FORMAT_VERSION) {
            throw BackupException(Idioma.text(R.string.aj_err_format))
        }
        val salt = file.copyOfRange(header, header + SALT_BYTES)
        val iv = file.copyOfRange(header + SALT_BYTES, header + SALT_BYTES + IV_BYTES)
        val body = file.copyOfRange(header + SALT_BYTES + IV_BYTES, file.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(128, iv))
        cipher.updateAAD(MAGIC)
        return try {
            cipher.doFinal(body)
        } catch (error: AEADBadTagException) {
            throw BackupException(Idioma.text(R.string.aj_err_password_wrong))
        }
    }

    private fun gzip(bytes: ByteArray): ByteArray =
        ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(bytes) } }.toByteArray()

    private fun gunzip(bytes: ByteArray): ByteArray =
        GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
}
