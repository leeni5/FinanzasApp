package com.finanzasapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/** Registro de una importación de archivo, para saber qué pasó y cuándo. */
@Entity(tableName = "import_jobs")
data class ImportJob(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val fileName: String,
    val kind: ImportKind,
    val status: ImportStatus,
    val detected: Int = 0,
    val imported: Int = 0,
    val error: String? = null,
    val createdAt: Instant
)
