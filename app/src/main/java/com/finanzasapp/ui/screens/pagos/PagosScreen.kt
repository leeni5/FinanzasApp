package com.finanzasapp.ui.screens.pagos

import com.finanzasapp.R
import androidx.compose.ui.res.stringResource
import com.finanzasapp.data.settings.Idioma
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finanzasapp.FinanzasApp
import com.finanzasapp.data.entity.Account
import com.finanzasapp.data.entity.Category
import com.finanzasapp.data.entity.PaymentFrequency
import com.finanzasapp.data.entity.ScheduledPayment
import com.finanzasapp.ui.components.AppCard
import com.finanzasapp.ui.components.EmptyState
import com.finanzasapp.ui.components.ScrollAwareFab
import com.finanzasapp.ui.components.SectionHeader
import com.finanzasapp.ui.components.isScrollingUp
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.Spacing
import com.finanzasapp.ui.theme.asColor
import com.finanzasapp.ui.theme.soft
import com.finanzasapp.ui.util.Fechas
import com.finanzasapp.ui.util.Money
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Pantalla de pagos programados: vencidos, próximos y pausados. */
@Composable
fun PagosScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val database = remember(context) { (context.applicationContext as FinanzasApp).database }
    val payments by database.paymentDao().observeAll().collectAsStateWithLifecycle(initialValue = null)
    val accounts by database.accountDao().observeActive().collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by database.categoryDao().observeActive().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    var editing by remember { mutableStateOf<ScheduledPayment?>(null) }
    var creating by remember { mutableStateOf(false) }
    var paying by remember { mutableStateOf<ScheduledPayment?>(null) }
    var busy by remember { mutableStateOf(false) }

    val today = Fechas.hoy()
    val all = payments.orEmpty()
    val overdue = all.filter { it.active && it.nextDueDate.isBefore(today) }
    val upcoming = all.filter { it.active && !it.nextDueDate.isBefore(today) }
    val paused = all.filter { !it.active }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.page, end = Spacing.page, top = Spacing.lg, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(onClick = onBack, shape = CircleShape, color = AppPalette.SurfaceMuted) {
                        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.fz_back), tint = AppPalette.Ink)
                        }
                    }
                    Spacer(Modifier.width(Spacing.md))
                    Text(stringResource(R.string.fz_pagos_title), style = MaterialTheme.typography.headlineMedium, color = AppPalette.Ink)
                }
                Spacer(Modifier.height(Spacing.md))
            }

            when {
                payments == null -> Unit
                all.isEmpty() -> item {
                    EmptyState(
                        title = stringResource(R.string.fz_pagos_empty_title),
                        message = stringResource(R.string.fz_pagos_empty_message),
                        actionLabel = stringResource(R.string.fz_pagos_add_first),
                        onAction = { creating = true }
                    )
                }
                else -> {
                    if (overdue.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.fz_pagos_overdue), trailing = "${overdue.size}") }
                        items(overdue, key = { it.id }) { p ->
                            PaymentCard(p, accounts, categories, onPay = { paying = p }, onClick = { editing = p })
                        }
                    }
                    if (upcoming.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.fz_pagos_upcoming), trailing = "${upcoming.size}", modifier = Modifier.padding(top = Spacing.sm)) }
                        items(upcoming, key = { it.id }) { p ->
                            PaymentCard(p, accounts, categories, onPay = { paying = p }, onClick = { editing = p })
                        }
                    }
                    if (paused.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.fz_pagos_paused), trailing = "${paused.size}", modifier = Modifier.padding(top = Spacing.sm)) }
                        items(paused, key = { it.id }) { p ->
                            PaymentCard(p, accounts, categories, onPay = null, onClick = { editing = p })
                        }
                    }
                }
            }
        }

        ScrollAwareFab(
            visible = listState.isScrollingUp(),
            description = stringResource(R.string.fz_pagos_new_description),
            onClick = { creating = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = Spacing.page, bottom = Spacing.lg)
        )
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp))
    }

    if (creating || editing != null) {
        PaymentEditorSheet(
            initial = editing,
            accounts = accounts,
            categories = categories,
            onDismiss = { creating = false; editing = null },
            onSave = { draft ->
                scope.launch { PaymentActions.save(context, database, draft) }
                creating = false; editing = null
            },
            onToggleActive = { p ->
                scope.launch { PaymentActions.setActive(context, database, p.id, !p.active) }
                editing = null
            },
            onDelete = { p ->
                scope.launch { PaymentActions.delete(context, database, p.id) }
                editing = null
            }
        )
    }

    paying?.let { payment ->
        PayDialog(
            payment = payment,
            accounts = accounts,
            busy = busy,
            onDismiss = { if (!busy) paying = null },
            onConfirm = { accountId ->
                busy = true
                scope.launch {
                    val receipt = PaymentActions.markPaid(
                        context, database, payment.id, payment.nextDueDate, Fechas.hoy(), accountId
                    )
                    busy = false
                    paying = null
                    if (receipt == null) {
                        snackbar.showSnackbar(Idioma.text(R.string.fz_pagos_cycle_already_paid))
                    } else {
                        val result = snackbar.showSnackbar(
                            message = Idioma.text(R.string.fz_pagos_paid_snackbar, payment.name),
                            actionLabel = Idioma.text(R.string.fz_undo),
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            PaymentActions.undoPaid(context, database, receipt)
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun PaymentCard(
    payment: ScheduledPayment,
    accounts: List<Account>,
    categories: List<Category>,
    onPay: (() -> Unit)?,
    onClick: () -> Unit
) {
    val today = Fechas.hoy()
    val overdue = payment.active && payment.nextDueDate.isBefore(today)
    val category = categories.firstOrNull { it.id == payment.categoryId }
    val dateColor = when {
        !payment.active -> AppPalette.InkFaint
        overdue -> AppPalette.Coral
        else -> category?.color?.asColor() ?: AppPalette.Accent
    }
    val account = accounts.firstOrNull { it.id == payment.accountId }

    AppCard(onClick = onClick, elevated = false, contentPadding = PaddingValues(Spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .size(52.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(dateColor.soft()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(payment.nextDueDate.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, color = dateColor)
                Text(Fechas.mesCorto(payment.nextDueDate), style = MaterialTheme.typography.labelSmall, color = dateColor)
            }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(payment.name, style = MaterialTheme.typography.titleSmall, color = AppPalette.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = buildString {
                        append(
                            when {
                                !payment.active && payment.frequency == PaymentFrequency.UNICA -> stringResource(R.string.fz_pagos_paid)
                                !payment.active -> stringResource(R.string.fz_pagos_paused_one)
                                else -> Fechas.vencimiento(payment.nextDueDate)
                            }
                        )
                        append(" · ")
                        append(payment.frequency.label())
                        account?.let { append(" · ${it.name}") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overdue) AppPalette.Coral else AppPalette.InkSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            Column(horizontalAlignment = Alignment.End) {
                Text(Money.cop(payment.amount), style = MaterialTheme.typography.titleSmall, color = AppPalette.Ink)
                if (onPay != null) {
                    Spacer(Modifier.height(Spacing.xs))
                    Surface(onClick = onPay, shape = MaterialTheme.shapes.small, color = AppPalette.Accent) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = AppPalette.OnAccent, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.fz_pagos_pay), style = MaterialTheme.typography.labelMedium, color = AppPalette.OnAccent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PayDialog(
    payment: ScheduledPayment,
    accounts: List<Account>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit
) {
    var accountId by remember { mutableStateOf(payment.accountId ?: accounts.firstOrNull()?.id) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.fz_pagos_mark_paid_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    stringResource(R.string.fz_pagos_mark_paid_message, Money.cop(payment.amount), payment.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppPalette.InkSoft
                )
                if (payment.accountId == null) {
                    Text(stringResource(R.string.fz_pagos_from_account), style = MaterialTheme.typography.labelLarge, color = AppPalette.Ink)
                    ChipRow(accounts.map { it.id to it.name }, accountId) { accountId = it }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(accountId) },
                enabled = !busy && accountId != null,
                colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Accent, contentColor = AppPalette.OnAccent)
            ) { Text(if (busy) stringResource(R.string.fz_saving) else stringResource(R.string.fz_pagos_mark_paid)) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(R.string.fz_cancel), color = AppPalette.InkSoft) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentEditorSheet(
    initial: ScheduledPayment?,
    accounts: List<Account>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (ScheduledPayment) -> Unit,
    onToggleActive: (ScheduledPayment) -> Unit,
    onDelete: (ScheduledPayment) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var amountText by remember { mutableStateOf(initial?.amount?.toString().orEmpty()) }
    var frequency by remember { mutableStateOf(initial?.frequency ?: PaymentFrequency.MENSUAL) }
    var date by remember { mutableStateOf(initial?.nextDueDate ?: Fechas.hoy()) }
    var accountId by remember { mutableStateOf(initial?.accountId ?: accounts.firstOrNull()?.id) }
    var categoryId by remember { mutableStateOf(initial?.categoryId) }
    var showDate by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var tried by remember { mutableStateOf(false) }

    val amount = amountText.toLongOrNull() ?: 0L
    val nameError = tried && name.isBlank()
    val amountError = tried && amount <= 0L
    val accountError = tried && accountId == null

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
                .padding(bottom = Spacing.xxl)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(if (initial == null) stringResource(R.string.fz_pagos_new) else stringResource(R.string.fz_pagos_edit), style = MaterialTheme.typography.headlineSmall, color = AppPalette.Ink)

            Field(name, { name = it.take(40) }, stringResource(R.string.fz_name), if (nameError) stringResource(R.string.fz_write_name) else null)
            Field(
                amountText, { v -> amountText = v.filter(Char::isDigit).take(12) }, stringResource(R.string.fz_pagos_amount),
                if (amountError) stringResource(R.string.fz_pagos_amount_error) else null,
                KeyboardOptions(keyboardType = KeyboardType.Number),
                com.finanzasapp.ui.components.ThousandsTransformation
            )

            Label(stringResource(R.string.fz_pagos_frequency))
            ChipRow(
                PaymentFrequency.entries.map { it.ordinal.toLong() to it.label() },
                frequency.ordinal.toLong()
            ) { frequency = PaymentFrequency.entries[it.toInt()] }

            Label(stringResource(R.string.fz_pagos_next_due))
            Surface(onClick = { showDate = true }, shape = MaterialTheme.shapes.medium, color = AppPalette.SurfaceMuted, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CalendarMonth, null, tint = AppPalette.InkSoft)
                    Spacer(Modifier.width(Spacing.md))
                    Text(Fechas.fechaLarga(date), style = MaterialTheme.typography.bodyMedium, color = AppPalette.Ink, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.fz_change), style = MaterialTheme.typography.labelMedium, color = AppPalette.Accent)
                }
            }

            Label(stringResource(R.string.fz_account))
            ChipRow(accounts.map { it.id to it.name }, accountId) { accountId = it }
            if (accountError) ErrorText(stringResource(R.string.fz_pagos_account_error))

            Label(stringResource(R.string.fz_pagos_category))
            ChipRow(listOf(-1L to stringResource(R.string.fz_pagos_none)) + categories.map { it.id to it.name }, categoryId ?: -1L) {
                categoryId = it.takeIf { id -> id >= 0 }
            }

            Button(
                onClick = {
                    tried = true
                    if (name.isBlank() || amount <= 0L || accountId == null) return@Button
                    onSave(
                        (initial ?: ScheduledPayment(name = "", amount = 0L, frequency = frequency, nextDueDate = date)).copy(
                            name = name.trim(),
                            amount = amount,
                            frequency = frequency,
                            nextDueDate = date,
                            accountId = accountId,
                            categoryId = categoryId
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Accent, contentColor = AppPalette.OnAccent)
            ) { Text(if (initial == null) stringResource(R.string.fz_pagos_schedule) else stringResource(R.string.fz_save_changes), style = MaterialTheme.typography.labelLarge) }

            if (initial != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { onToggleActive(initial) }) {
                        Icon(if (initial.active) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = AppPalette.InkSoft)
                        Spacer(Modifier.width(Spacing.xs))
                        Text(if (initial.active) stringResource(R.string.fz_pagos_pause) else stringResource(R.string.fz_pagos_resume), color = AppPalette.InkSoft)
                    }
                    TextButton(onClick = { showDelete = true }) { Text(stringResource(R.string.fz_delete), color = AppPalette.Coral) }
                }
            }
        }
    }

    if (showDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDate = false
                }) { Text(stringResource(R.string.fz_done)) }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text(stringResource(R.string.fz_cancel)) } }
        ) { DatePicker(state = state) }
    }

    if (showDelete && initial != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.fz_pagos_delete_title, initial.name)) },
            text = { Text(stringResource(R.string.fz_pagos_delete_message)) },
            confirmButton = { TextButton(onClick = { showDelete = false; onDelete(initial) }) { Text(stringResource(R.string.fz_delete), color = AppPalette.Coral) } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text(stringResource(R.string.fz_cancel)) } }
        )
    }
}

private fun PaymentFrequency.label(): String = when (this) {
    PaymentFrequency.MENSUAL -> Idioma.text(R.string.fz_freq_monthly)
    PaymentFrequency.ANUAL -> Idioma.text(R.string.fz_freq_yearly)
    PaymentFrequency.UNICA -> Idioma.text(R.string.fz_freq_once)
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    error: String?,
    keyboard: KeyboardOptions = KeyboardOptions.Default,
    visual: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None
) {
    Column {
        TextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            singleLine = true,
            isError = error != null,
            keyboardOptions = keyboard,
            visualTransformation = visual,
            shape = MaterialTheme.shapes.medium,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = AppPalette.SurfaceMuted,
                unfocusedContainerColor = AppPalette.SurfaceMuted,
                errorContainerColor = AppPalette.SurfaceMuted,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = AppPalette.Accent,
                focusedLabelColor = AppPalette.Accent
            )
        )
        error?.let { ErrorText(it) }
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = AppPalette.Coral, modifier = Modifier.padding(top = Spacing.xs, start = Spacing.xs))
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = AppPalette.InkSoft)
}

@Composable
private fun ChipRow(options: List<Pair<Long, String>>, selected: Long?, onSelect: (Long) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        items(options, key = { it.first }) { (id, label) ->
            val isSelected = id == selected
            Surface(
                onClick = { onSelect(id) },
                shape = MaterialTheme.shapes.small,
                color = if (isSelected) AppPalette.Accent else AppPalette.SurfaceMuted
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) AppPalette.OnAccent else AppPalette.InkSoft,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}
