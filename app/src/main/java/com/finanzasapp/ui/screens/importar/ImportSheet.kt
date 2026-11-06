package com.finanzasapp.ui.screens.importar

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finanzasapp.FinanzasApp
import com.finanzasapp.data.entity.Account
import com.finanzasapp.domain.importing.ImportPreview
import com.finanzasapp.domain.importing.ImportResult
import com.finanzasapp.domain.importing.ImportRowPreview
import com.finanzasapp.ui.components.AppCard
import com.finanzasapp.ui.components.SoftPill
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.Spacing
import com.finanzasapp.ui.util.Fechas
import com.finanzasapp.ui.util.Money
import java.time.LocalDate

/**
 * Flujo completo de importación en una hoja inferior:
 * elegir archivo → vista previa → confirmar → resultado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSheet(
    onDismiss: () -> Unit,
    onImported: ((LocalDate?) -> Unit)? = null
) {
    val context = LocalContext.current
    val database = remember(context) {
        (context.applicationContext as FinanzasApp).database
    }
    val viewModel: ImportViewModel = viewModel(
        factory = ImportViewModel.factory(context.applicationContext, database)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.onFilePicked(uri)
        }
    }

    val openPicker = {
        picker.launch(
            arrayOf(
                "application/pdf",
                "text/csv",
                "text/comma-separated-values",
                "text/plain",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "*/*"
            )
        )
    }

    fun close() {
        viewModel.reset()
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = ::close,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AppPalette.Border) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.page)
                .padding(top = Spacing.xs, bottom = Spacing.xxl)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Importar movimientos",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "CSV, Excel o PDF. Nada sale de este teléfono.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    onClick = ::close,
                    shape = CircleShape,
                    color = AppPalette.SurfaceMuted
                ) {
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cerrar",
                            tint = AppPalette.InkSoft,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            when (state.phase) {
                ImportPhase.IDLE -> IdleContent(onPick = openPicker)

                ImportPhase.READING -> BusyContent(
                    title = "Leyendo el archivo…",
                    detail = "Estamos separando fechas, montos y categorías."
                )

                ImportPhase.PREVIEW -> state.preview?.let { preview ->
                    PreviewContent(
                        preview = preview,
                        accounts = state.accounts,
                        defaultAccountId = state.defaultAccountId,
                        onDefaultAccount = viewModel::setDefaultAccount,
                        onConfirm = viewModel::confirm,
                        onPickAnother = {
                            viewModel.reset()
                            openPicker()
                        },
                        onCancel = ::close
                    )
                }

                ImportPhase.IMPORTING -> BusyContent(
                    title = "Guardando movimientos…",
                    detail = "Se están creando las categorías y cuentas que faltan."
                )

                ImportPhase.DONE -> state.result?.let { result ->
                    DoneContent(
                        result = result,
                        onView = {
                            onImported?.invoke(result.latestDate)
                            close()
                        },
                        onClose = ::close
                    )
                }

                ImportPhase.ERROR -> ErrorContent(
                    message = state.error ?: "No pudimos leer el archivo.",
                    onRetry = {
                        viewModel.reset()
                        openPicker()
                    },
                    onClose = ::close
                )
            }
        }
    }
}

@Composable
private fun IdleContent(onPick: () -> Unit) {
    AppCard(containerColor = AppPalette.SurfaceMuted) {
        Text(
            text = "Qué puedes importar",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "• CSV y Excel con columnas de fecha, descripción y monto.\n" +
                "• PDF exportado por Budge.\n" +
                "• Se muestra una vista previa antes de guardar.\n" +
                "• Si vuelves a importar el mismo archivo, no se duplica nada.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Spacer(Modifier.height(Spacing.lg))

    Button(
        onClick = onPick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = AppPalette.Accent,
            contentColor = AppPalette.OnAccent
        )
    ) {
        Text(
            text = "Elegir archivo",
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun PreviewContent(
    preview: ImportPreview,
    accounts: List<Account>,
    defaultAccountId: Long?,
    onDefaultAccount: (Long) -> Unit,
    onConfirm: () -> Unit,
    onPickAnother: () -> Unit,
    onCancel: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = preview.fileName,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(Spacing.sm))
        SoftPill(text = preview.format.label, color = AppPalette.Accent)
    }

    Spacer(Modifier.height(Spacing.md))

    Surface(
        shape = MaterialTheme.shapes.large,
        color = AppPalette.SurfaceMuted
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            PreviewStat(
                label = "Detectados",
                value = preview.detected.toString(),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            PreviewStat(
                label = "Ingresos",
                value = Money.cop(preview.income),
                color = AppPalette.Green,
                modifier = Modifier.weight(1f)
            )
            PreviewStat(
                label = "Gastos",
                value = Money.cop(preview.expenses),
                color = AppPalette.Coral,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (preview.duplicates > 0) {
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = "${preview.duplicates} ya estaban registrados y se omitirán. " +
                "Se importarán ${preview.importable}.",
            style = MaterialTheme.typography.bodySmall,
            color = AppPalette.Amber
        )
    }

    if (preview.newCategories.isNotEmpty() || preview.newAccounts.isNotEmpty()) {
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = buildString {
                append("Se crearán ")
                val parts = buildList {
                    if (preview.newCategories.isNotEmpty()) {
                        add("${preview.newCategories.size} categorías")
                    }
                    if (preview.newAccounts.isNotEmpty()) {
                        add("${preview.newAccounts.size} cuentas")
                    }
                }
                append(parts.joinToString(" y "))
                val names = (preview.newCategories + preview.newAccounts).take(3)
                if (names.isNotEmpty()) {
                    append(": ")
                    append(names.joinToString(", "))
                    if (preview.newCategories.size + preview.newAccounts.size > 3) append("…")
                }
                append(".")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (preview.rowsWithoutAccount > 0) {
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = "Cuenta para ${preview.rowsWithoutAccount} filas sin cuenta",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.sm))
        if (accounts.isEmpty()) {
            Text(
                text = "Se creará la cuenta “Efectivo”.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(accounts, key = { it.id }) { account ->
                    AccountChip(
                        name = account.name,
                        selected = account.id == defaultAccountId,
                        onClick = { onDefaultAccount(account.id) }
                    )
                }
            }
        }
    }

    if (preview.warnings.isNotEmpty()) {
        Spacer(Modifier.height(Spacing.md))
        preview.warnings.take(3).forEach { warning ->
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = AppPalette.Amber,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    text = warning,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.Amber
                )
            }
        }
    }

    Spacer(Modifier.height(Spacing.lg))
    Text(
        text = "Vista previa",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(Spacing.xs))

    preview.rows.take(6).forEachIndexed { index, row ->
        if (index > 0) {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline
            )
        }
        ImportRowLine(row)
    }

    if (preview.rows.size > 6) {
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "y ${preview.rows.size - 6} movimientos más…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Spacer(Modifier.height(Spacing.lg))

    Button(
        onClick = onConfirm,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = preview.importable > 0,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = AppPalette.Accent,
            contentColor = AppPalette.OnAccent,
            disabledContainerColor = AppPalette.SurfaceStrong,
            disabledContentColor = AppPalette.InkFaint
        )
    ) {
        Text(
            text = if (preview.importable > 0) {
                "Importar ${preview.importable} movimientos"
            } else {
                "Nada nuevo por importar"
            },
            style = MaterialTheme.typography.labelLarge
        )
    }

    TextButton(
        onClick = onPickAnother,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Elegir otro archivo",
            style = MaterialTheme.typography.labelLarge,
            color = AppPalette.Accent
        )
    }

    TextButton(
        onClick = onCancel,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Cancelar",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PreviewStat(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AccountChip(
    name: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) AppPalette.Ink else AppPalette.SurfaceMuted
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) AppPalette.OnAccent else AppPalette.InkSoft,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ImportRowLine(row: ImportRowPreview) {
    val muted = row.duplicate
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.description,
                style = MaterialTheme.typography.titleSmall,
                color = if (muted) AppPalette.InkFaint else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append(Fechas.corta(row.date))
                    append(" · ")
                    append(row.categoryName ?: "Sin categoría")
                    row.accountName?.takeIf { it.isNotBlank() }?.let {
                        append(" · ")
                        append(it)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(Spacing.sm))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = amountLabel(row.amount),
                style = MaterialTheme.typography.labelLarge,
                color = when {
                    muted -> AppPalette.InkFaint
                    row.amount >= 0L -> AppPalette.Green
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            if (muted) {
                Text(
                    text = "Ya existe",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppPalette.InkFaint
                )
            }
        }
    }
}

private fun amountLabel(amount: Long): String =
    if (amount > 0L) "+ ${Money.cop(amount)}" else "− ${Money.cop(-amount)}"

@Composable
private fun BusyContent(title: String, detail: String) {    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            strokeWidth = 3.dp,
            color = AppPalette.Accent
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DoneContent(
    result: ImportResult,
    onView: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = AppPalette.Green,
            modifier = Modifier.size(52.dp)
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = "Importación completa",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = buildString {
                append("Se guardaron ${result.imported} movimientos.")
                if (result.duplicates > 0) {
                    append(" Se omitieron ${result.duplicates} que ya existían.")
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (result.newCategories.isNotEmpty() || result.newAccounts.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = "También se crearon las categorías y cuentas que faltaban.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(Spacing.xl))

        if (result.latestDate != null) {
            Button(
                onClick = onView,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppPalette.Accent,
                    contentColor = AppPalette.OnAccent
                )
            ) {
                Text(
                    text = "Ver los movimientos importados",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(Modifier.height(Spacing.sm))
        }

        TextButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Cerrar",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = AppPalette.Coral,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(Spacing.sm))
            Column {
                Text(
                    text = "No pudimos importar",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))

        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppPalette.Accent,
                contentColor = AppPalette.OnAccent
            )
        ) {
            Text(
                text = "Elegir otro archivo",
                style = MaterialTheme.typography.labelLarge
            )
        }

        TextButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Cerrar",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
