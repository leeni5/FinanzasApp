package com.finanzasapp.data.rules

import androidx.room.withTransaction
import com.finanzasapp.data.db.AppDatabase
import com.finanzasapp.data.entity.AuditLog
import com.finanzasapp.data.entity.CategorizationRule
import com.finanzasapp.data.entity.Transaction
import com.finanzasapp.data.model.displayName
import com.finanzasapp.domain.dedupe.UniqueHash
import org.json.JSONObject
import java.time.Instant

/**
 * Reglas de categorización: «si la descripción contiene X, va a la categoría Y».
 *
 * La comparación ignora mayúsculas, tildes y signos (la misma normalización de
 * la huella), así «ÉXITO» y «exito calle 80» caen en la misma regla. Una regla
 * solo rellena movimientos sin categoría: nunca cambia una que ya se eligió.
 */
object RuleActions {

    /** Reglas listas para comparar: patrón ya normalizado, la más prioritaria primero. */
    class Matcher(rules: List<CategorizationRule>) {
        private val ordered = rules
            .map { UniqueHash.normalize(it.pattern) to it }
            .filter { it.first.isNotEmpty() }

        fun match(text: String): CategorizationRule? {
            val haystack = UniqueHash.normalize(text)
            if (haystack.isEmpty()) return null
            return ordered.firstOrNull { (pattern, _) -> haystack.contains(pattern) }?.second
        }
    }

    suspend fun matcher(database: AppDatabase): Matcher =
        Matcher(database.systemDao().allRules())

    /**
     * Crea una regla y la aplica a los movimientos que hoy no tienen categoría.
     * Devuelve cuántos movimientos quedaron categorizados.
     */
    suspend fun create(database: AppDatabase, pattern: String, categoryId: Long): Int {
        val clean = pattern.trim()
        if (UniqueHash.normalize(clean).isEmpty()) return 0
        val now = Instant.now()
        return database.withTransaction {
            val rule = CategorizationRule(pattern = clean, categoryId = categoryId, createdAt = now)
            val id = database.systemDao().insertRule(rule)
            database.systemDao().insertAudit(
                AuditLog(
                    entity = "rule",
                    entityId = id,
                    action = "CREADO",
                    afterJson = JSONObject()
                        .put("patron", clean)
                        .put("categoria", categoryId)
                        .toString(),
                    source = "usuario",
                    at = now
                )
            )
            applyToUncategorized(database, rule.copy(id = id), now)
        }
    }

    suspend fun delete(database: AppDatabase, id: Long) {
        database.withTransaction {
            val before = database.systemDao().findRule(id) ?: return@withTransaction
            database.systemDao().deleteRule(id)
            database.systemDao().insertAudit(
                AuditLog(
                    entity = "rule",
                    entityId = id,
                    action = "BORRADO",
                    beforeJson = JSONObject()
                        .put("patron", before.pattern)
                        .put("categoria", before.categoryId)
                        .toString(),
                    source = "usuario",
                    at = Instant.now()
                )
            )
        }
    }

    /** Debe llamarse dentro de una transacción. */
    private suspend fun applyToUncategorized(
        database: AppDatabase,
        rule: CategorizationRule,
        now: Instant
    ): Int {
        val matcher = Matcher(listOf(rule))
        val dao = database.transactionDao()
        var applied = 0
        for (transaction in dao.uncategorized()) {
            if (matcher.match(transaction.searchText()) == null) continue
            val after = transaction.copy(categoryId = rule.categoryId, updatedAt = now)
            dao.update(after)
            database.systemDao().insertAudit(
                AuditLog(
                    entity = "transaction",
                    entityId = transaction.id,
                    action = "EDITADO",
                    beforeJson = transaction.ruleAuditJson(),
                    afterJson = after.ruleAuditJson(),
                    source = "regla",
                    at = now
                )
            )
            applied++
        }
        if (applied > 0) database.systemDao().addRuleApplications(rule.id, applied)
        return applied
    }

    /** Texto contra el que se comparan las reglas. */
    fun Transaction.searchText(): String = "${displayName()} $descriptionOriginal"

    private fun Transaction.ruleAuditJson(): String = JSONObject()
        .put("descripcion", displayName())
        .put("monto", amount)
        .put("tipo", type.name)
        .put("categoria", categoryId ?: JSONObject.NULL)
        .toString()
}
