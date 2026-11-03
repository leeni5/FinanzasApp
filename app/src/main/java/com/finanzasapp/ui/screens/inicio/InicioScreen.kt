package com.finanzasapp.ui.screens.inicio

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finanzasapp.FinanzasApp
import com.finanzasapp.ui.components.NotionCard
import com.finanzasapp.ui.components.PageScaffold
import com.finanzasapp.ui.theme.Spacing
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun InicioScreen() {
    val context = LocalContext.current
    val database = remember(context) {
        (context.applicationContext as FinanzasApp).database
    }
    val viewModel: InicioViewModel = viewModel(factory = InicioViewModel.factory(database))
    val state by viewModel.state.collectAsStateWithLifecycle()

    val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    val month = remember {
        LocalDate.now(ZoneId.of("America/Bogota"))
            .format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale.forLanguageTag("es-CO")))
            .replaceFirstChar { it.uppercase(Locale.forLanguageTag("es-CO")) }
    }

    PageScaffold(
        title = "Inicio",
        subtitle = month
    ) {
        NotionCard {
            Text(
                text = "Tu base de datos está lista",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Todo se guarda en este teléfono. Nada se envía a ningún servidor.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Spacing.md),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline
            )

            SummaryRow("Cuentas", state.accounts)
            SummaryRow("Categorías", state.categories)
            SummaryRow("Movimientos", state.transactions)
            SummaryRow("Metas", state.goals, showDivider = false)
        }

        NotionCard {
            Text(
                text = "Qué sigue",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "En la siguiente fase esta pantalla muestra el saldo, el gasto del mes " +
                    "y los pagos que vienen. La estructura ya está construida y probada " +
                    "contra la base de datos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.sm)
            )
        }

        if (isDebuggable) {
            Text(
                text = "Compilación de prueba: las cuentas y movimientos que ves son de ejemplo. " +
                    "En la versión final la base arranca vacía, solo con las categorías.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.xs)
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: Int,
    showDivider: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(vertical = Spacing.sm),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
