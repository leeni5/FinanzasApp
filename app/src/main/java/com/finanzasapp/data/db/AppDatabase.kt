package com.finanzasapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.finanzasapp.data.dao.AccountDao
import com.finanzasapp.data.dao.CategoryDao
import com.finanzasapp.data.dao.GoalDao
import com.finanzasapp.data.dao.PaymentDao
import com.finanzasapp.data.dao.SystemDao
import com.finanzasapp.data.dao.TransactionDao
import com.finanzasapp.data.entity.Account
import com.finanzasapp.data.entity.AuditLog
import com.finanzasapp.data.entity.CategorizationRule
import com.finanzasapp.data.entity.Category
import com.finanzasapp.data.entity.CategoryBudget
import com.finanzasapp.data.entity.GoalContribution
import com.finanzasapp.data.entity.ImportJob
import com.finanzasapp.data.entity.SavingsGoal
import com.finanzasapp.data.entity.ScheduledPayment
import com.finanzasapp.data.entity.Transaction
import com.finanzasapp.data.security.DatabaseKeyProvider
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        Account::class,
        Category::class,
        Transaction::class,
        CategorizationRule::class,
        SavingsGoal::class,
        GoalContribution::class,
        ScheduledPayment::class,
        ImportJob::class,
        AuditLog::class,
        CategoryBudget::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun goalDao(): GoalDao
    abstract fun paymentDao(): PaymentDao
    abstract fun systemDao(): SystemDao

    companion object {
        private const val DB_NAME = "finanzas.db"

        @Volatile
        private var instance: AppDatabase? = null

        /**
         * Base cifrada con SQLCipher. La frase se obtiene del Keystore;
         * nunca se escribe en claro ni se comparte entre dispositivos.
         */
        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): AppDatabase {
            System.loadLibrary("sqlcipher")
            val passphrase = DatabaseKeyProvider.getOrCreatePassphrase(context)
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DB_NAME
            )
                .openHelperFactory(SupportOpenHelperFactory(passphrase))
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
        }

        /**
         * v1 → v2:
         * - categories.iconColor: color del icono elegido por el usuario.
         * - scheduled_payments.lastPaidDueDate: ciclo ya pagado (idempotencia).
         * - scheduled_payments.anchorDay: día de vencimiento original, tomado
         *   del nextDueDate actual (guardado como epochDay).
         * - scheduled_payments.categoryId: categoría del gasto al pagar.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN iconColor TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE scheduled_payments ADD COLUMN lastPaidDueDate INTEGER")
                db.execSQL("ALTER TABLE scheduled_payments ADD COLUMN anchorDay INTEGER")
                db.execSQL("ALTER TABLE scheduled_payments ADD COLUMN categoryId INTEGER")
                db.execSQL(
                    "UPDATE scheduled_payments SET anchorDay = " +
                        "CAST(strftime('%d', nextDueDate * 86400, 'unixepoch') AS INTEGER)"
                )
            }
        }

        /** v2 → v3: accounts.logo, la entidad elegida para mostrar su logo. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN logo TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v3 → v4: transactions.transferOut, la dirección de cada pata de una transferencia. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN transferOut INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * v4 → v5: category_budgets, presupuesto por mes. Cada categoría arranca
         * con su límite actual «desde siempre» (fromMonth = 0).
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `category_budgets` (`categoryId` INTEGER NOT NULL, " +
                        "`fromMonth` INTEGER NOT NULL, `amount` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`categoryId`, `fromMonth`), FOREIGN KEY(`categoryId`) " +
                        "REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_category_budgets_categoryId` ON `category_budgets` (`categoryId`)")
                db.execSQL(SEED_BUDGETS_SQL)
            }
        }

        /** Siembra el límite actual de cada categoría como «desde siempre». */
        const val SEED_BUDGETS_SQL =
            "INSERT OR IGNORE INTO category_budgets (categoryId, fromMonth, amount) " +
                "SELECT id, 0, monthlyBudget FROM categories"

        /** Versión del esquema. Las copias de versiones anteriores se adaptan al restaurar. */
        const val SCHEMA_VERSION = 5
    }
}
