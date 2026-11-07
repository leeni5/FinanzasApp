package com.finanzasapp.ui.screens.respaldo

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.TableView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.finanzasapp.FinanzasApp
import com.finanzasapp.R
import com.finanzasapp.data.settings.Idioma
import androidx.compose.ui.res.stringResource
import com.finanzasapp.data.backup.BackupContents
import com.finanzasapp.data.backup.BackupException
import com.finanzasapp.data.backup.BackupService
import com.finanzasapp.notifications.PaymentReminderScheduler
import com.finanzasapp.ui.components.AppCard
import com.finanzasapp.ui.components.IconBubble
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.Spacing
import com.finanzasapp.ui.util.Fechas
import kotlinx.coroutines.launch

/** Qué diálogo de contraseña está abierto. */
private enum class PasswordMode { CREAR, ABRIR }

/**
 * Exportar a CSV, crear una copia cifrada y restaurarla.
 * Los archivos se guardan donde elija el usuario (Descargas, Drive…), con el
 * selector del sistema: la app no necesita permisos de almacenamiento.
 */
@Composable
fun BackupCard() {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as FinanzasApp }
    val database = app.database
    val scope = rememberCoroutineScope()

    var busy by remember { mutableStateOf(false) }
    var passwordMode by remember { mutableStateOf<PasswordMode?>(null) }
    var pendingPassword by remember { mutableStateOf<CharArray?>(null) }
    var openUri by remember { mutableStateOf<Uri?>(null) }
    var toRestore by remember { mutableStateOf<BackupContents?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()

    fun run(block: suspend () -> Unit) {
        busy = true
        scope.launch {
            try {
                block()
            } catch (e: BackupException) {
                error = e.message
            } catch (e: Exception) {
                error = Idioma.text(R.string.aj_backup_failed_generic, e.message ?: e.javaClass.simpleName)
            } finally {
                busy = false
            }
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) run {
            val count = BackupService.exportCsv(context, database, uri)
            toast(Idioma.text(R.string.aj_csv_exported, count))
        }
    }

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val password = pendingPassword
        pendingPassword = null
        if (uri != null && password != null) run {
            try {
                val count = BackupService.createBackup(context, database, uri, password)
                toast(Idioma.text(R.string.aj_backup_created, count))
            } finally {
                password.fill(' ')
            }
        }
    }

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            openUri = uri
            passwordMode = PasswordMode.ABRIR
        }
    }

    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(icon = Icons.Rounded.Backup, tint = AppPalette.Green, size = 44.dp)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.aj_backup_title), style = MaterialTheme.typography.titleMedium, color = AppPalette.Ink)
                Text(
                    text = stringResource(R.string.aj_backup_detail),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.InkSoft
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Button(
                onClick = { passwordMode = PasswordMode.CREAR },
                enabled = !busy,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Accent, contentColor = AppPalette.OnAccent),
                contentPadding = PaddingValues(vertical = Spacing.md)
            ) { Text(stringResource(R.string.aj_backup_create)) }
            OutlinedButton(
                onClick = { openLauncher.launch(arrayOf("*/*")) },
                enabled = !busy,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(vertical = Spacing.md)
            ) { Text(stringResource(R.string.aj_backup_restore), color = AppPalette.Ink) }
        }
        if (busy) {
            Spacer(Modifier.height(Spacing.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp, color = AppPalette.Accent)
                Spacer(Modifier.width(Spacing.sm))
                Text(stringResource(R.string.aj_backup_working), style = MaterialTheme.typography.bodySmall, color = AppPalette.InkSoft)
            }
        }
    }

    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(icon = Icons.Rounded.TableView, tint = AppPalette.Sky, size = 44.dp)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.aj_csv_title), style = MaterialTheme.typography.titleMedium, color = AppPalette.Ink)
                Text(
                    text = stringResource(R.string.aj_csv_detail),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.InkSoft
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
        OutlinedButton(
            onClick = { csvLauncher.launch("finanzas-${Fechas.hoy()}.csv") },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            contentPadding = PaddingValues(vertical = Spacing.md)
        ) { Text(stringResource(R.string.aj_csv_export), color = AppPalette.Ink) }
    }

    passwordMode?.let { mode ->
        PasswordDialog(
            creating = mode == PasswordMode.CREAR,
            onDismiss = { passwordMode = null; openUri = null },
            onConfirm = { password ->
                passwordMode = null
                if (mode == PasswordMode.CREAR) {
                    pendingPassword = password
                    createLauncher.launch("finanzas-${Fechas.hoy()}.finz")
                } else {
                    val uri = openUri ?: return@PasswordDialog
                    openUri = null
                    run {
                        try {
                            toRestore = BackupService.readBackup(context, uri, password)
                        } finally {
                            password.fill(' ')
                        }
                    }
                }
            }
        )
    }

    toRestore?.let { contents ->
        val date = contents.createdAt.atZone(Fechas.Bogota).toLocalDate()
        AlertDialog(
            onDismissRequest = { toRestore = null },
            title = { Text(stringResource(R.string.aj_restore_title)) },
            text = {
                Text(
                    stringResource(R.string.aj_restore_detail, Fechas.diaYMes(date), date.year, contents.movements, contents.accounts, contents.categories, contents.goals, contents.payments)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    toRestore = null
                    run {
                        BackupService.restore(database, contents)
                        PaymentReminderScheduler.rescheduleAll(context, database)
                        toast(Idioma.text(R.string.aj_restore_done, contents.movements))
                    }
                }) { Text(stringResource(R.string.aj_restore_confirm), color = AppPalette.Coral) }
            },
            dismissButton = { TextButton(onClick = { toRestore = null }) { Text(stringResource(R.string.aj_cancel)) } }
        )
    }

    error?.let { message ->
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text(stringResource(R.string.aj_error_title)) },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { error = null }) { Text(stringResource(R.string.aj_ok)) } }
        )
    }
}

@Composable
private fun PasswordDialog(
    creating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (CharArray) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    val longEnough = password.length >= BackupService.MIN_PASSWORD
    val valid = if (creating) longEnough && password == repeat else password.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (creating) stringResource(R.string.aj_password_create_title) else stringResource(R.string.aj_password_open_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = if (creating) {
                        stringResource(R.string.aj_password_create_hint, BackupService.MIN_PASSWORD)
                    } else {
                        stringResource(R.string.aj_password_open_hint)
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.aj_password_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                if (creating) {
                    OutlinedTextField(
                        value = repeat,
                        onValueChange = { repeat = it },
                        label = { Text(stringResource(R.string.aj_password_repeat)) },
                        singleLine = true,
                        isError = repeat.isNotEmpty() && repeat != password,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password.toCharArray()) }, enabled = valid) {
                Text(if (creating) stringResource(R.string.aj_password_pick_location) else stringResource(R.string.aj_password_open_backup))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.aj_cancel)) } }
    )
}
