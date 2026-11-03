package com.finanzasapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
import com.finanzasapp.data.entity.GoalContribution
import com.finanzasapp.data.entity.ImportJob
import com.finanzasapp.data.entity.SavingsGoal
import com.finanzasapp.data.entity.ScheduledPayment
import com.finanzasapp.data.entity.Transaction

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
        AuditLog::class
    ],
    version = 1,
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

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                ).build().also { instance = it }
            }
    }
}
