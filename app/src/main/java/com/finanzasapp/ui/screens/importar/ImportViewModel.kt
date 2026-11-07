package com.finanzasapp.ui.screens.importar

import android.content.Context
import com.finanzasapp.data.settings.Idioma
import com.finanzasapp.R
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.finanzasapp.data.db.AppDatabase
import com.finanzasapp.data.entity.Account
import com.finanzasapp.data.importing.ImportService
import com.finanzasapp.domain.importing.ImportFormat
import com.finanzasapp.domain.importing.ImportParseException
import com.finanzasapp.domain.importing.ImportPreview
import com.finanzasapp.domain.importing.ImportResult
import com.finanzasapp.domain.importing.ParseUtils
import com.finanzasapp.domain.importing.ParsedImport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ImportPhase {
    IDLE,
    READING,
    PREVIEW,
    IMPORTING,
    DONE,
    ERROR
}

data class ImportUiState(
    val phase: ImportPhase = ImportPhase.IDLE,
    val fileName: String = "",
    val format: ImportFormat? = null,
    val preview: ImportPreview? = null,
    val result: ImportResult? = null,
    val accounts: List<Account> = emptyList(),
    val defaultAccountId: Long? = null,
    val error: String? = null
)

/**
 * Estado del flujo de importación.
 *
 * El archivo se lee una sola vez y se guarda en memoria mientras el usuario
 * revisa la vista previa. Si cambia la cuenta por defecto, se recalcula la
 * vista previa sin volver a abrir el archivo.
 */
class ImportViewModel(
    private val context: Context,
    private val database: AppDatabase
) : ViewModel() {

    private val service = ImportService(context, database)
    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    private var parsedImport: ParsedImport? = null
    private var defaultAccountId: Long? = null

    init {
        viewModelScope.launch {
            val accounts = database.accountDao().all().filter { !it.archived }
            val fallback = accounts.firstOrNull {
                ParseUtils.normalize(it.name) == "efectivo"
            } ?: accounts.firstOrNull()
            defaultAccountId = fallback?.id
            _state.update {
                it.copy(accounts = accounts, defaultAccountId = fallback?.id)
            }
        }
    }

    fun onFilePicked(uri: Uri) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    phase = ImportPhase.READING,
                    error = null,
                    preview = null,
                    result = null
                )
            }

            try {
                val fileName = queryFileName(uri)
                val mime = context.contentResolver.getType(uri)
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: throw ImportParseException(Idioma.text(R.string.mv_import_open_error))

                if (bytes.size > MAX_BYTES) {
                    throw ImportParseException(
                        Idioma.text(R.string.mv_import_too_big)
                    )
                }

                val format = ParseUtils.detectFormat(fileName, mime, bytes)
                val parsed = service.parse(fileName, bytes, format)
                parsedImport = parsed
                val preview = service.buildPreview(parsed, defaultAccountId)

                _state.update {
                    it.copy(
                        phase = ImportPhase.PREVIEW,
                        fileName = fileName,
                        format = format,
                        preview = preview
                    )
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        phase = ImportPhase.ERROR,
                        error = error.message ?: Idioma.text(R.string.mv_import_read_error)
                    )
                }
            }
        }
    }

    fun setDefaultAccount(accountId: Long) {
        val parsed = parsedImport ?: return
        defaultAccountId = accountId
        viewModelScope.launch {
            try {
                val preview = service.buildPreview(parsed, accountId)
                _state.update {
                    it.copy(defaultAccountId = accountId, preview = preview)
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        phase = ImportPhase.ERROR,
                        error = error.message ?: Idioma.text(R.string.mv_import_preview_error)
                    )
                }
            }
        }
    }

    fun confirm() {
        val preview = _state.value.preview ?: return
        viewModelScope.launch {
            _state.update { it.copy(phase = ImportPhase.IMPORTING, error = null) }
            try {
                val result = service.import(preview)
                _state.update { it.copy(phase = ImportPhase.DONE, result = result) }
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        phase = ImportPhase.ERROR,
                        error = error.message ?: Idioma.text(R.string.mv_import_save_error)
                    )
                }
            }
        }
    }

    fun reset() {
        parsedImport = null
        _state.update {
            it.copy(
                phase = ImportPhase.IDLE,
                fileName = "",
                format = null,
                preview = null,
                result = null,
                error = null
            )
        }
    }

    private fun queryFileName(uri: Uri): String {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    return cursor.getString(index) ?: "archivo"
                }
            }
        }
        return uri.lastPathSegment ?: "archivo"
    }

    companion object {
        private const val MAX_BYTES = 25 * 1024 * 1024

        fun factory(context: Context, database: AppDatabase) = viewModelFactory {
            initializer { ImportViewModel(context.applicationContext, database) }
        }
    }
}
