package com.finanzasapp.data.ai

import com.finanzasapp.data.db.AppDatabase
import com.finanzasapp.data.entity.TransactionType
import com.finanzasapp.data.settings.Idioma
import com.finanzasapp.data.model.displayName
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import com.finanzasapp.domain.ai.DateRange

/** El texto exacto que se enviará, con un resumen para mostrar antes de enviarlo. */
data class FinanceSnapshot(val text: String, val movements: Int, val truncated: Boolean)

/**
 * Arma el contexto financiero que acompaña a la pregunta.
 * Es texto plano y se le muestra al usuario tal cual antes de enviarlo:
 * lo que no está aquí, la IA no lo ve. No incluye notas, ids ni datos del
 * teléfono, y [includeDetail] = false deja solo totales.
 */
object FinanceContext {

    private const val MAX_ROWS = 800

    suspend fun build(
        database: AppDatabase,
        range: DateRange,
        includeDetail: Boolean,
        today: LocalDate
    ): FinanceSnapshot {
        val from = range.from
        val to = range.to
        val categories = database.categoryDao().all().associateBy { it.id }
        val accounts = database.accountDao().all().associateBy { it.id }
        val balances = database.accountDao().observeBalances().first()
        val movements = database.transactionDao().observeBetween(from, to).first()
        val counted = movements.filter { it.type != TransactionType.TRANSFERENCIA }

        val text = buildString {
            appendLine("Hoy es $today. Moneda: pesos colombianos (COP), montos enteros.")
            appendLine("Período de los datos: ${range.label}, del $from al $to.")
            appendLine()
            appendLine("SALDOS ACTUALES POR CUENTA (en tarjetas de crédito, negativo = deuda):")
            balances.forEach { appendLine("- ${it.name} (${it.type.name.lowercase()}): ${it.balance}") }
            appendLine()
            val income = counted.filter { it.type == TransactionType.INGRESO }.sumOf { it.amount }
            val expenses = counted.filter { it.type == TransactionType.GASTO }.sumOf { it.amount }
            appendLine("TOTALES DEL PERÍODO: ingresos $income; gastos $expenses; balance ${income - expenses}.")
            appendLine()
            appendLine("GASTO POR CATEGORÍA (presupuesto mensual entre paréntesis; 0 = sin presupuesto):")
            counted.filter { it.type == TransactionType.GASTO }
                .groupBy { it.categoryId }
                .map { (id, list) -> (id?.let(categories::get)) to list.sumOf { it.amount } }
                .sortedByDescending { it.second }
                .forEach { (category, total) ->
                    appendLine("- ${category?.name ?: "Sin categoría"}: $total (${category?.monthlyBudget ?: 0})")
                }

            if (includeDetail) {
                appendLine()
                val shown = movements.take(MAX_ROWS)
                appendLine("MOVIMIENTOS (fecha; tipo; descripción; monto; categoría; cuenta), del más reciente:")
                shown.forEach { t ->
                    val type = when (t.type) {
                        TransactionType.INGRESO -> "ingreso"
                        TransactionType.GASTO -> "gasto"
                        TransactionType.TRANSFERENCIA -> if (t.transferOut) "transferencia sale" else "transferencia entra"
                    }
                    appendLine(
                        listOf(
                            t.date.toString(),
                            type,
                            t.displayName().replace(';', ','),
                            t.amount.toString(),
                            t.categoryId?.let(categories::get)?.name ?: "sin categoría",
                            accounts[t.accountId]?.name ?: "?"
                        ).joinToString("; ")
                    )
                }
                if (movements.size > MAX_ROWS) {
                    appendLine("(Se muestran los $MAX_ROWS más recientes de ${movements.size}; los totales sí incluyen todos.)")
                }
            }
        }
        return FinanceSnapshot(text.trim(), movements.size, includeDetail && movements.size > MAX_ROWS)
    }

    /** Instrucciones fijas del asistente, en el idioma de la app. */
    val systemPrompt: String
        get() = if (Idioma.isEnglish) systemPromptEnglish else systemPromptSpanish

    private val systemPromptEnglish = """
        You are the assistant of FinanzasApp, a personal finance app in Colombia.
        Always answer in English, briefly and clearly, using only the data given below.
        The data block is written in Spanish; translate any labels you mention.
        Write amounts as $ 1.234.567 (dot as thousands separator, no decimals),
        in Colombian pesos.
        The data covers only the stated period. If the question needs dates that
        are not included, say so and suggest naming the time span in the question
        (for example "in March" or "the last 6 months"); never make up figures.
        Transfers between accounts are neither expenses nor income.
        You do not give investment advice: if asked, make clear you are not a
        financial advisor and limit the answer to the user's numbers.
        You cannot create, edit or delete transactions.
    """.trimIndent()

    private val systemPromptSpanish = """
        Eres el asistente de FinanzasApp, una app personal de finanzas en Colombia.
        Responde en español, breve y claro, usando solo los datos que te doy abajo.
        Escribe los montos como $ 1.234.567 (punto de miles, sin decimales).
        Los datos cubren solo el período indicado. Si la pregunta necesita fechas
        que no están, dilo y sugiere nombrar el lapso en la pregunta (por ejemplo
        «en marzo» o «los últimos 6 meses»); no inventes cifras.
        Las transferencias entre cuentas no son gastos ni ingresos.
        No das asesoría de inversión: si te la piden, aclara que no eres asesor
        financiero y limita la respuesta a los números del usuario.
        No puedes crear, editar ni borrar movimientos.
    """.trimIndent()
}
