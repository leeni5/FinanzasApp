package com.finanzasapp.ui.screens.cuentas

import com.finanzasapp.R
import androidx.compose.ui.res.stringResource
import com.finanzasapp.data.settings.Idioma
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finanzasapp.FinanzasApp
import com.finanzasapp.data.entity.Account
import com.finanzasapp.data.entity.AccountType
import com.finanzasapp.data.model.AccountBalance
import com.finanzasapp.ui.components.AccountLogo
import com.finanzasapp.ui.components.AppCard
import com.finanzasapp.ui.components.AnimatedMoney
import com.finanzasapp.ui.components.BankCatalog
import com.finanzasapp.ui.components.ColorPicker
import com.finanzasapp.ui.components.HeroSurface
import com.finanzasapp.ui.components.ScrollAwareFab
import com.finanzasapp.ui.components.SectionHeader
import com.finanzasapp.ui.components.ThousandsTransformation
import com.finanzasapp.ui.components.isScrollingUp
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.IdentityPalette
import com.finanzasapp.ui.theme.Spacing
import com.finanzasapp.ui.theme.asColor
import com.finanzasapp.ui.theme.toHex
import com.finanzasapp.ui.util.Money
import kotlinx.coroutines.launch

/** Etiqueta en español de cada tipo de cuenta. */
fun AccountType.label(): String = when (this) {
    AccountType.AHORRO -> Idioma.text(R.string.fz_acc_type_savings)
    AccountType.DEBITO -> Idioma.text(R.string.fz_acc_type_debit)
    AccountType.CREDITO -> Idioma.text(R.string.fz_acc_type_credit)
    AccountType.EFECTIVO -> Idioma.text(R.string.fz_acc_type_cash)
}

/** Lista de cuentas con su saldo: lo que hay, lo que se debe y cada cuenta. */
@Composable
fun CuentasScreen(onBack: () -> Unit, onOpenAccount: (Long) -> Unit) {
    val context = LocalContext.current
    val database = remember(context) { (context.applicationContext as FinanzasApp).database }
    val balances by database.accountDao().observeBalances().collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var creating by remember { mutableStateOf(false) }

    val list = balances.orEmpty()
    val available = list.filter { it.type != AccountType.CREDITO }.sumOf { it.balance }
    val debt = list.filter { it.type == AccountType.CREDITO }.sumOf { (-it.balance).coerceAtLeast(0L) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.page, end = Spacing.page, top = Spacing.lg, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            item {
                BackTitle(stringResource(R.string.fz_acc_title), onBack)
                Spacer(Modifier.height(Spacing.md))
                HeroSurface {
                    Text(stringResource(R.string.fz_acc_available), style = MaterialTheme.typography.labelMedium, color = AppPalette.HeroInkSoft)
                    Spacer(Modifier.height(Spacing.xs))
                    AnimatedMoney(available, MaterialTheme.typography.displaySmall, AppPalette.HeroInk)
                    if (debt > 0L) {
                        Spacer(Modifier.height(Spacing.sm))
                        Text(stringResource(R.string.fz_acc_card_debt, Money.cop(debt)), style = MaterialTheme.typography.bodySmall, color = AppPalette.HeroDebt)
                    }
                }
                Spacer(Modifier.height(Spacing.md))
                SectionHeader(stringResource(R.string.fz_acc_your_accounts), trailing = "${list.size}")
            }

            items(list, key = { it.accountId }) { account ->
                AccountRow(account) { onOpenAccount(account.accountId) }
            }
        }

        ScrollAwareFab(
            visible = listState.isScrollingUp(),
            description = stringResource(R.string.fz_acc_new),
            onClick = { creating = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = Spacing.page, bottom = Spacing.lg)
        )
    }

    if (creating) {
        AccountEditorSheet(
            initial = null,
            onDismiss = { creating = false },
            onSave = { account ->
                scope.launch { AccountActions.save(database, account) }
                creating = false
            }
        )
    }
}

@Composable
private fun AccountRow(account: AccountBalance, onClick: () -> Unit) {
    val isCredit = account.type == AccountType.CREDITO
    AppCard(onClick = onClick, elevated = false, contentPadding = PaddingValues(Spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AccountLogo(account.name, account.logo, account.type, account.color, size = 46.dp)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleSmall, color = AppPalette.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(account.type.label(), style = MaterialTheme.typography.bodySmall, color = AppPalette.InkSoft)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isCredit) Money.cop((-account.balance).coerceAtLeast(0L)) else Money.cop(account.balance),
                    style = MaterialTheme.typography.titleSmall,
                    color = when {
                        isCredit && account.balance < 0 -> AppPalette.Coral
                        account.balance < 0 -> AppPalette.Coral
                        else -> AppPalette.Ink
                    }
                )
                Text(if (isCredit) stringResource(R.string.fz_acc_debt) else stringResource(R.string.fz_acc_balance), style = MaterialTheme.typography.labelSmall, color = AppPalette.InkFaint)
            }
        }
    }
}

@Composable
fun BackTitle(title: String, onBack: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(onClick = onBack, shape = CircleShape, color = AppPalette.SurfaceMuted) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.fz_back), tint = AppPalette.Ink)
            }
        }
        Spacer(Modifier.width(Spacing.md))
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = AppPalette.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

/** Crear o editar una cuenta: nombre, entidad (logo), tipo, saldo inicial y color. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AccountEditorSheet(
    initial: Account?,
    onDismiss: () -> Unit,
    onSave: (Account) -> Unit,
    onArchive: ((Account) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var logo by remember { mutableStateOf(initial?.logo.orEmpty()) }
    var type by remember { mutableStateOf(initial?.type ?: AccountType.AHORRO) }
    var balanceText by remember { mutableStateOf(initial?.initialBalance?.takeIf { it != 0L }?.let { kotlin.math.abs(it).toString() }.orEmpty()) }
    var limitText by remember { mutableStateOf(initial?.creditLimit?.toString().orEmpty()) }
    var color by remember { mutableStateOf((initial?.color ?: IdentityPalette.random().toHex()).asColor()) }
    var tried by remember { mutableStateOf(false) }

    // Si la entidad no se eligió a mano, se sugiere por el nombre.
    val guessed = remember(name) { BankCatalog.guess(name)?.key.orEmpty() }
    val effectiveLogo = logo.ifBlank { guessed }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                AccountLogo(name.ifBlank { stringResource(R.string.fz_account) }, effectiveLogo, type, color.toHex(), size = 48.dp)
                Spacer(Modifier.width(Spacing.md))
                Text(
                    if (initial == null) stringResource(R.string.fz_acc_new) else stringResource(R.string.fz_acc_edit),
                    style = MaterialTheme.typography.headlineSmall,
                    color = AppPalette.Ink
                )
            }

            FieldBox(name, { name = it.take(32) }, stringResource(R.string.fz_acc_name_hint), if (tried && name.isBlank()) stringResource(R.string.fz_write_name) else null)

            Label(stringResource(R.string.fz_acc_bank))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                BankChoice(null, selected = effectiveLogo.isBlank(), type = type, color = color.toHex()) { logo = "" }
                BankCatalog.banks.forEach { bank ->
                    BankChoice(bank.key, selected = effectiveLogo == bank.key, type = type, color = color.toHex()) { logo = bank.key }
                }
            }

            Label(stringResource(R.string.fz_acc_type))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                AccountType.entries.forEach { option ->
                    val selected = option == type
                    Surface(onClick = { type = option }, shape = MaterialTheme.shapes.small, color = if (selected) AppPalette.Accent else AppPalette.SurfaceMuted) {
                        Text(
                            option.label(),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) AppPalette.OnAccent else AppPalette.InkSoft,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            if (type == AccountType.CREDITO) {
                FieldBox(limitText, { limitText = it.filter(Char::isDigit).take(12) }, stringResource(R.string.fz_acc_credit_limit), null, number = true)
                FieldBox(balanceText, { balanceText = it.filter(Char::isDigit).take(12) }, stringResource(R.string.fz_acc_current_debt_optional), null, number = true)
            } else {
                FieldBox(balanceText, { balanceText = it.filter(Char::isDigit).take(12) }, stringResource(R.string.fz_acc_initial_balance), null, number = true)
            }

            Label(stringResource(R.string.fz_acc_color_hint))
            ColorPicker(color = color, onColorChange = { color = it })

            Button(
                onClick = {
                    tried = true
                    if (name.isBlank()) return@Button
                    val amount = balanceText.toLongOrNull() ?: 0L
                    onSave(
                        (initial ?: Account(name = "", type = type)).copy(
                            name = name.trim(),
                            type = type,
                            logo = logo.ifBlank { guessed },
                            // En tarjetas, la deuda inicial resta: saldo negativo = deuda.
                            initialBalance = if (type == AccountType.CREDITO) -amount else amount,
                            creditLimit = if (type == AccountType.CREDITO) limitText.toLongOrNull() else null,
                            color = color.toHex()
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Accent, contentColor = AppPalette.OnAccent)
            ) { Text(if (initial == null) stringResource(R.string.fz_acc_create) else stringResource(R.string.fz_save_changes), style = MaterialTheme.typography.labelLarge) }

            if (initial != null && onArchive != null) {
                TextButton(onClick = { onArchive(initial) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (initial.archived) stringResource(R.string.fz_acc_restore) else stringResource(R.string.fz_acc_archive), color = AppPalette.Coral)
                }
                Text(
                    stringResource(R.string.fz_acc_archive_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.InkFaint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun BankChoice(key: String?, selected: Boolean, type: AccountType, color: String, onClick: () -> Unit) {
    val bank = BankCatalog.byKey(key)
    Column(
        modifier = Modifier
            .widthIn(min = 64.dp)
            .clip(MaterialTheme.shapes.medium)
            .then(if (selected) Modifier.border(2.dp, AppPalette.Accent, MaterialTheme.shapes.medium) else Modifier)
            .clickable(onClick = onClick)
            .padding(Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AccountLogo(bank?.name ?: stringResource(R.string.fz_acc_other), key ?: "", if (bank == null) type else AccountType.AHORRO, color, size = 40.dp)
        Text(
            bank?.name ?: stringResource(R.string.fz_acc_other),
            style = MaterialTheme.typography.labelSmall,
            color = AppPalette.InkSoft,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(64.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = AppPalette.InkSoft)
}

@Composable
private fun FieldBox(value: String, onChange: (String) -> Unit, label: String, error: String?, number: Boolean = false) {
    Column {
        TextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            singleLine = true,
            isError = error != null,
            prefix = if (number) ({ Text("$ ") }) else null,
            visualTransformation = if (number) ThousandsTransformation else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
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
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = AppPalette.Coral, modifier = Modifier.padding(top = Spacing.xs)) }
    }
}
