package com.finanzasapp.ui.screens.inicio

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finanzasapp.FinanzasApp
import com.finanzasapp.data.entity.AccountType
import com.finanzasapp.ui.components.AccountLogo
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.Spacing
import com.finanzasapp.ui.util.Money

/**
 * Cuentas en una tira horizontal: logo, nombre y saldo (o deuda en tarjetas).
 * Tocar una abre su detalle; la última tarjeta lleva a agregar otra.
 */
@Composable
fun AccountsStrip(onOpenAccount: (Long) -> Unit, onOpenAccounts: () -> Unit) {
    val context = LocalContext.current
    val database = remember(context) { (context.applicationContext as FinanzasApp).database }
    val balances by database.accountDao().observeBalances().collectAsStateWithLifecycle(initialValue = emptyList())

    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        items(balances, key = { it.accountId }) { account ->
            val isCredit = account.type == AccountType.CREDITO
            Surface(
                onClick = { onOpenAccount(account.accountId) },
                shape = MaterialTheme.shapes.large,
                color = AppPalette.Surface,
                modifier = Modifier.width(148.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    AccountLogo(account.name, account.logo, account.type, account.color, size = 36.dp)
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        account.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = AppPalette.InkSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        Money.cop(if (isCredit) (-account.balance).coerceAtLeast(0L) else account.balance),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isCredit || account.balance < 0) AppPalette.Coral else AppPalette.Ink,
                        maxLines = 1
                    )
                    if (isCredit) {
                        Text("Deuda", style = MaterialTheme.typography.labelSmall, color = AppPalette.InkFaint)
                    }
                }
            }
        }
        item(key = "agregar") {
            Surface(
                onClick = onOpenAccounts,
                shape = MaterialTheme.shapes.large,
                color = AppPalette.Background,
                modifier = Modifier
                    .width(112.dp)
                    .border(1.dp, AppPalette.Border, MaterialTheme.shapes.large)
            ) {
                Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Add, null, tint = AppPalette.Accent)
                    }
                    Spacer(Modifier.height(Spacing.md))
                    Text("Cuentas", style = MaterialTheme.typography.labelLarge, color = AppPalette.Accent)
                    Text("ver y agregar", style = MaterialTheme.typography.labelSmall, color = AppPalette.InkFaint)
                }
            }
        }
    }
}
