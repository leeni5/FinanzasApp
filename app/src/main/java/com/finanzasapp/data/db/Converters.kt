package com.finanzasapp.data.db

import androidx.room.TypeConverter
import com.finanzasapp.data.entity.AccountType
import com.finanzasapp.data.entity.ImportKind
import com.finanzasapp.data.entity.ImportStatus
import com.finanzasapp.data.entity.PaymentFrequency
import com.finanzasapp.data.entity.TransactionSource
import com.finanzasapp.data.entity.TransactionType
import java.time.Instant
import java.time.LocalDate

/**
 * Todo se guarda en tipos primitivos que SQLite entiende:
 * fechas como epochDay / epochMilli, enums como su nombre.
 * Guardar strings legibles hace que la base siga siendo inspeccionable a mano.
 */
class Converters {

    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun fromEpochMilli(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun toEpochMilli(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun fromAccountType(value: String?): AccountType? = value?.let(AccountType::valueOf)

    @TypeConverter
    fun toAccountType(type: AccountType?): String? = type?.name

    @TypeConverter
    fun fromTransactionType(value: String?): TransactionType? = value?.let(TransactionType::valueOf)

    @TypeConverter
    fun toTransactionType(type: TransactionType?): String? = type?.name

    @TypeConverter
    fun fromTransactionSource(value: String?): TransactionSource? = value?.let(TransactionSource::valueOf)

    @TypeConverter
    fun toTransactionSource(source: TransactionSource?): String? = source?.name

    @TypeConverter
    fun fromPaymentFrequency(value: String?): PaymentFrequency? = value?.let(PaymentFrequency::valueOf)

    @TypeConverter
    fun toPaymentFrequency(frequency: PaymentFrequency?): String? = frequency?.name

    @TypeConverter
    fun fromImportKind(value: String?): ImportKind? = value?.let(ImportKind::valueOf)

    @TypeConverter
    fun toImportKind(kind: ImportKind?): String? = kind?.name

    @TypeConverter
    fun fromImportStatus(value: String?): ImportStatus? = value?.let(ImportStatus::valueOf)

    @TypeConverter
    fun toImportStatus(status: ImportStatus?): String? = status?.name
}
