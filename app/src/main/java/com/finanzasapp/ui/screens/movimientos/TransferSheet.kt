package com.finanzasapp.ui.screens.movimientos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.res.stringResource
import com.finanzasapp.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finanzasapp.data.entity.Account
import com.finanzasapp.ui.components.ThousandsTransformation
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.Spacing
import com.finanzasapp.ui.util.Fechas
import com.finanzasapp.ui.util.Money
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Datos de una transferencia nueva. */
data class TransferDraft(
    val fromAccountId: Long,
    val toAccountId: Long,
    val amount: Long,
    val date: LocalDate,
    val description: String
)

/** Mover dinero entre cuentas propias: de dónde sale, a dónde llega y cuánto. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferSheet(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (TransferDraft) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var fromId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var toId by remember { mutableStateOf(accounts.getOrNull(1)?.id) }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Fechas.hoy()) }
    var showDate by remember { mutableStateOf(false) }

    val amount = amountText.toLongOrNull() ?: 0L
    val canSave = amount > 0L && fromId != null && toId != null && fromId != toId

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AppPalette.SurfaceStrong) }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = Spacing.page)
                .padding(bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(stringResource(R.string.mv_transfer), style = MaterialTheme.typography.headlineSmall, color = AppPalette.Ink)
            Text(
                stringResource(R.string.mv_transfer_intro),
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.InkSoft
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { value -> amountText = value.filter { it.isDigit() }.take(12) },
                label = { Text(stringResource(R.string.mv_amount)) },
                prefix = { Text("$ ") },
                singleLine = true,
                visualTransformation = ThousandsTransformation,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            AccountChoice(stringResource(R.string.mv_from), accounts, fromId) { fromId = it }
            AccountChoice(stringResource(R.string.mv_to), accounts, toId) { toId = it }
            if (fromId != null && fromId == toId) {
                Text(stringResource(R.string.mv_two_accounts), style = MaterialTheme.typography.bodySmall, color = AppPalette.Coral)
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.mv_description_optional)) },
                placeholder = { Text(stringResource(R.string.mv_transfer)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Surface(
                onClick = { showDate = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = AppPalette.SurfaceMuted
            ) {
                Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DateRange, null, tint = AppPalette.InkSoft)
                    Spacer(Modifier.width(Spacing.sm))
                    Text(Fechas.fechaLarga(date), style = MaterialTheme.typography.bodyMedium, color = AppPalette.Ink)
                }
            }

            Button(
                onClick = {
                    val from = fromId ?: return@Button
                    val to = toId ?: return@Button
                    onSave(TransferDraft(from, to, amount, date, description))
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Accent, contentColor = AppPalette.OnAccent)
            ) {
                Text(if (amount > 0L) stringResource(R.string.mv_transfer_amount, Money.cop(amount)) else stringResource(R.string.mv_transfer_action))
            }
        }
    }

    if (showDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    showDate = false
                }) { Text(stringResource(R.string.mv_done)) }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text(stringResource(R.string.mv_cancel)) } }
        ) { DatePicker(state = state, showModeToggle = false) }
    }
}

@Composable
private fun AccountChoice(label: String, accounts: List<Account>, selected: Long?, onSelect: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = AppPalette.Ink)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            accounts.forEach { account ->
                val isSelected = account.id == selected
                Surface(
                    onClick = { onSelect(account.id) },
                    shape = MaterialTheme.shapes.small,
                    color = if (isSelected) AppPalette.Accent else AppPalette.SurfaceMuted
                ) {
                    Text(
                        account.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) AppPalette.OnAccent else AppPalette.InkSoft,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/** Detalle de una transferencia ya hecha: solo se puede borrar (las dos patas). */
@Composable
fun TransferInfoDialog(
    description: String,
    amount: Long,
    date: LocalDate,
    fromName: String,
    toName: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var confirming by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(description) },
        text = {
            Text(
                if (confirming) {
                    stringResource(R.string.mv_transfer_delete_message)
                } else {
                    stringResource(R.string.mv_transfer_info, Money.cop(amount), fromName, toName, Fechas.diaYMes(date))
                }
            )
        },
        confirmButton = {
            if (confirming) {
                TextButton(onClick = onDelete) { Text(stringResource(R.string.mv_delete_transfer), color = AppPalette.Coral) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.mv_close)) }
            }
        },
        dismissButton = {
            if (confirming) {
                TextButton(onClick = { confirming = false }) { Text(stringResource(R.string.mv_cancel)) }
            } else {
                TextButton(onClick = { confirming = true }) { Text(stringResource(R.string.mv_delete_short), color = AppPalette.Coral) }
            }
        }
    )
}
