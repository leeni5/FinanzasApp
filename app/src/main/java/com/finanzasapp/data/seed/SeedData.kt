package com.finanzasapp.data.seed

import androidx.room.withTransaction
import com.finanzasapp.data.db.AppDatabase
import com.finanzasapp.data.entity.Account
import com.finanzasapp.data.entity.AccountType
import com.finanzasapp.data.entity.AuditLog
import com.finanzasapp.data.entity.Category
import com.finanzasapp.data.entity.GoalContribution
import com.finanzasapp.data.entity.PaymentFrequency
import com.finanzasapp.data.entity.SavingsGoal
import com.finanzasapp.data.entity.ScheduledPayment
import com.finanzasapp.data.entity.Transaction
import com.finanzasapp.data.entity.TransactionSource
import com.finanzasapp.data.entity.TransactionType
import com.finanzasapp.domain.dedupe.UniqueHash
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Datos iniciales.
 *
 * La app se usa en Colombia y toda fecha se calcula en hora de Bogotá,
 * nunca en la zona del sistema (un celular puede estar en otra zona).
 *
 * [includeDemoData] solo es true en compilaciones de depuración: así el
 * APK final no aparece con movimientos y cuentas inventadas.
 */
object SeedData {

    val Bogota: ZoneId = ZoneId.of("America/Bogota")

    suspend fun populateIfEmpty(database: AppDatabase, includeDemoData: Boolean) {
        if (database.categoryDao().count() > 0) return

        database.withTransaction {
            // Doble verificación dentro de la transacción: dos arranques
            // simultáneos no deben sembrar dos veces.
            if (database.categoryDao().count() > 0) return@withTransaction

            val categoryIds = insertCategories(database)
            if (!includeDemoData) {
                insertCashAccount(database)
                return@withTransaction
            }

            insertDemo(database, categoryIds)
        }
    }

    // ---------------------------------------------------------------- categorías

    private data class SeedCategory(
        val name: String,
        val budget: Long,
        val color: String,
        val sortOrder: Int
    )

    private val categories = listOf(
        SeedCategory("Arriendo", 1_150_000L, "#E06A4E", 0),
        SeedCategory("Mercado", 850_000L, "#2F9E63", 1),
        SeedCategory("Servicios", 320_000L, "#D99A18", 2),
        SeedCategory("Transporte", 250_000L, "#2E86C8", 3),
        SeedCategory("Restaurantes", 280_000L, "#EE8A4A", 4),
        SeedCategory("Salud", 150_000L, "#0F9D8F", 5),
        SeedCategory("Ocio", 120_000L, "#8A5CF6", 6),
        SeedCategory("Otros", 100_000L, "#8C857B", 7),
        SeedCategory("Ingresos", 0L, "#2F9E63", 8)
    )

    private suspend fun insertCategories(database: AppDatabase): Map<String, Long> {
        val ids = mutableMapOf<String, Long>()
        for (seed in categories) {
            val id = database.categoryDao().insert(
                Category(
                    name = seed.name,
                    monthlyBudget = seed.budget,
                    color = seed.color,
                    sortOrder = seed.sortOrder
                )
            )
            ids[seed.name] = id
        }
        return ids
    }

    // ------------------------------------------------------------------ cuentas

    private suspend fun insertCashAccount(database: AppDatabase) {
        database.accountDao().insert(
            Account(
                name = "Efectivo",
                type = AccountType.EFECTIVO,
                initialBalance = 0L,
                color = "#2F9E63"
            )
        )
    }

    private data class SeedAccount(
        val name: String,
        val type: AccountType,
        val initialBalance: Long,
        val creditLimit: Long? = null,
        val cutoffDay: Int? = null,
        val dueDay: Int? = null,
        val color: String
    )

    private val accounts = listOf(
        SeedAccount("Bancolombia Ahorros", AccountType.AHORRO, 2_400_000L, color = "#D99A18"),
        SeedAccount("Nequi", AccountType.DEBITO, 350_000L, color = "#8A5CF6"),
        SeedAccount("Efectivo", AccountType.EFECTIVO, 150_000L, color = "#2F9E63"),
        SeedAccount(
            name = "Visa Bancolombia",
            type = AccountType.CREDITO,
            initialBalance = 0L,
            creditLimit = 6_000_000L,
            cutoffDay = 15,
            dueDay = 5,
            color = "#4C5FD5"
        )
    )

    // -------------------------------------------------------------- movimientos

    /** Un movimiento repetido todos los meses. */
    private data class MonthlyItem(
        val day: Int,
        val description: String,
        val amount: Long,
        val type: TransactionType,
        val category: String?,
        val accountIndex: Int
    )

    private val monthlyItems = listOf(
        MonthlyItem(1, "Pago de nómina", 4_800_000L, TransactionType.INGRESO, "Ingresos", 0),
        MonthlyItem(2, "Arriendo apartamento", 1_150_000L, TransactionType.GASTO, "Arriendo", 0),
        MonthlyItem(3, "Enel - energía", 132_000L, TransactionType.GASTO, "Servicios", 0),
        MonthlyItem(4, "Claro - internet", 94_900L, TransactionType.GASTO, "Servicios", 0),
        MonthlyItem(5, "Claro - plan celular", 44_900L, TransactionType.GASTO, "Servicios", 0),
        MonthlyItem(6, "Éxito - mercado", 312_400L, TransactionType.GASTO, "Mercado", 1),
        MonthlyItem(8, "Terpel - gasolina", 120_000L, TransactionType.GASTO, "Transporte", 3),
        MonthlyItem(10, "Rappi", 42_800L, TransactionType.GASTO, "Restaurantes", 3),
        MonthlyItem(12, "D1 - mercado", 168_300L, TransactionType.GASTO, "Mercado", 1),
        MonthlyItem(14, "Uber", 28_500L, TransactionType.GASTO, "Transporte", 1),
        MonthlyItem(15, "Netflix", 24_900L, TransactionType.GASTO, "Ocio", 3),
        MonthlyItem(16, "Farmacia Pasteur", 36_700L, TransactionType.GASTO, "Salud", 1),
        MonthlyItem(18, "Almuerzo oficina", 22_000L, TransactionType.GASTO, "Restaurantes", 2),
        MonthlyItem(20, "Carulla - mercado", 234_800L, TransactionType.GASTO, "Mercado", 3),
        MonthlyItem(21, "Agua Bogotá", 76_300L, TransactionType.GASTO, "Servicios", 0),
        MonthlyItem(22, "Cine Colombia", 38_000L, TransactionType.GASTO, "Ocio", 3),
        MonthlyItem(24, "Droguería Cruz Verde", 41_200L, TransactionType.GASTO, "Salud", 1),
        MonthlyItem(25, "Terpel - gasolina", 128_000L, TransactionType.GASTO, "Transporte", 3),
        MonthlyItem(26, "Cena con amigos", 68_500L, TransactionType.GASTO, "Restaurantes", 3),
        MonthlyItem(28, "Éxito - mercado", 96_700L, TransactionType.GASTO, "Mercado", 1)
    )

    /** Movimientos que solo existen en un mes concreto (offset respecto a hoy). */
    private val extraItems = mapOf(
        -3 to listOf(
            MonthlyItem(11, "Arreglo de bicicleta", 85_000L, TransactionType.GASTO, "Otros", 0)
        ),
        -2 to listOf(
            MonthlyItem(9, "Pago freelance", 750_000L, TransactionType.INGRESO, "Ingresos", 1),
            MonthlyItem(17, "Regalo de cumpleaños", 120_000L, TransactionType.GASTO, "Otros", 0)
        ),
        -1 to listOf(
            MonthlyItem(7, "Pago freelance", 600_000L, TransactionType.INGRESO, "Ingresos", 1),
            MonthlyItem(19, "Consulta médica", 90_000L, TransactionType.GASTO, "Salud", 1)
        ),
        // Un par de gastos de los primeros días del mes en curso: así la
        // pantalla de Inicio muestra varias categorías incluso el día 1.
        0 to listOf(
            MonthlyItem(1, "Éxito - mercado", 145_600L, TransactionType.GASTO, "Mercado", 1),
            MonthlyItem(1, "TransMilenio", 6_400L, TransactionType.GASTO, "Transporte", 2),
            MonthlyItem(2, "Café Juan Valdez", 14_800L, TransactionType.GASTO, "Restaurantes", 2),
            MonthlyItem(2, "Farmacia Pasteur", 28_900L, TransactionType.GASTO, "Salud", 1),
            MonthlyItem(2, "Uber", 18_200L, TransactionType.GASTO, "Transporte", 1)
        )
    )

    private suspend fun insertDemo(database: AppDatabase, categoryIds: Map<String, Long>) {
        val today = LocalDate.now(Bogota)

        val accountIds = accounts.map { seed ->
            database.accountDao().insert(
                Account(
                    name = seed.name,
                    type = seed.type,
                    initialBalance = seed.initialBalance,
                    creditLimit = seed.creditLimit,
                    cutoffDay = seed.cutoffDay,
                    dueDay = seed.dueDay,
                    color = seed.color
                )
            )
        }

        val rows = mutableListOf<Transaction>()

        for (monthOffset in -3..0) {
            val monthStart = today.plusMonths(monthOffset.toLong()).withDayOfMonth(1)
            val items = monthlyItems + extraItems[monthOffset].orEmpty()

            for (item in items) {
                val date = monthStart.withDayOfMonth(item.day)
                if (date.isAfter(today)) continue

                val accountId = accountIds[item.accountIndex]
                rows += Transaction(
                    accountId = accountId,
                    date = date,
                    descriptionOriginal = item.description,
                    amount = item.amount,
                    type = item.type,
                    categoryId = item.category?.let(categoryIds::get),
                    uniqueHash = UniqueHash.of(accountId, date, item.amount, item.description),
                    source = TransactionSource.MANUAL,
                    createdAt = instantAt(date, 9),
                    updatedAt = instantAt(date, 9)
                )
            }
        }

        database.transactionDao().insertAll(rows)

        insertScheduledPayments(database, accountIds, today)
        insertGoal(database, today)

        database.systemDao().insertAudit(
            AuditLog(
                entity = "app",
                entityId = 0L,
                action = "SEMBRADO",
                source = "seed",
                afterJson = "{\"cuentas\":${accountIds.size},\"movimientos\":${rows.size}}",
                at = Instant.now()
            )
        )
    }

    // ------------------------------------------------------------- programados

    private suspend fun insertScheduledPayments(
        database: AppDatabase,
        accountIds: List<Long>,
        today: LocalDate
    ) {
        val scheduled = listOf(
            ScheduledPayment(
                name = "Arriendo",
                amount = 1_150_000L,
                frequency = PaymentFrequency.MENSUAL,
                nextDueDate = nextDayOfMonth(today, 2),
                accountId = accountIds[0]
            ),
            ScheduledPayment(
                name = "Tarjeta Visa",
                amount = 450_000L,
                frequency = PaymentFrequency.MENSUAL,
                nextDueDate = nextDayOfMonth(today, 5),
                accountId = accountIds[3]
            ),
            ScheduledPayment(
                name = "Netflix",
                amount = 24_900L,
                frequency = PaymentFrequency.MENSUAL,
                nextDueDate = nextDayOfMonth(today, 15),
                accountId = accountIds[3]
            ),
            ScheduledPayment(
                name = "Seguro todo riesgo",
                amount = 1_890_000L,
                frequency = PaymentFrequency.ANUAL,
                nextDueDate = today.plusMonths(2).withDayOfMonth(20),
                accountId = accountIds[0]
            )
        )
        scheduled.forEach { database.paymentDao().insert(it) }
    }

    /** Próxima fecha con ese día del mes, hoy incluido, respetando meses cortos. */
    fun nextDayOfMonth(today: LocalDate, day: Int): LocalDate {
        val thisMonth = today.withDayOfMonth(minOf(day, today.lengthOfMonth()))
        if (!thisMonth.isBefore(today)) return thisMonth
        val nextMonth = today.plusMonths(1)
        return nextMonth.withDayOfMonth(minOf(day, nextMonth.lengthOfMonth()))
    }

    // -------------------------------------------------------------------- metas

    private suspend fun insertGoal(database: AppDatabase, today: LocalDate) {
        val goalId = database.goalDao().insertGoal(
            SavingsGoal(
                name = "Viaje a Cartagena",
                targetAmount = 3_500_000L,
                targetDate = today.plusMonths(7).withDayOfMonth(15),
                color = "#4C5FD5"
            )
        )

        for (monthOffset in -2..0) {
            val date = today.plusMonths(monthOffset.toLong()).withDayOfMonth(10)
            if (date.isAfter(today)) continue
            database.goalDao().insertContribution(
                GoalContribution(goalId = goalId, date = date, amount = 300_000L)
            )
        }
    }

    private fun instantAt(date: LocalDate, hour: Int): Instant =
        date.atTime(hour, 0).atZone(Bogota).toInstant()
}
