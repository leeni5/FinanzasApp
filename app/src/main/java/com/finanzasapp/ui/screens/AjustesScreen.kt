package com.finanzasapp.ui.screens

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finanzasapp.FinanzasApp
import com.finanzasapp.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.finanzasapp.data.entity.ImportJob
import com.finanzasapp.data.entity.ImportStatus
import com.finanzasapp.ui.components.AppCard
import com.finanzasapp.ui.components.IconBubble
import com.finanzasapp.ui.components.PageScaffold
import com.finanzasapp.ui.components.SectionHeader
import com.finanzasapp.ui.screens.importar.ImportSheet
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.Spacing
import com.finanzasapp.ui.util.Fechas
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.FactCheck
import com.finanzasapp.ui.screens.respaldo.BackupCard
import com.finanzasapp.ui.screens.respaldo.LockCard
import com.finanzasapp.ui.screens.respaldo.ShareApkCard
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.finanzasapp.data.settings.ThemeMode
import com.finanzasapp.data.settings.AppLanguage
import com.finanzasapp.data.settings.Idioma
import com.finanzasapp.notifications.PaymentReminderScheduler
import kotlinx.coroutines.launch

/** Fila que abre otra pantalla: icono de color, título, detalle y flecha. */
@Composable
private fun NavRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    detail: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBubble(icon = icon, tint = tint, size = 40.dp)
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = AppPalette.Ink)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = AppPalette.InkSoft)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = AppPalette.InkFaint)
    }
}

@Composable
fun AjustesScreen(
    onOpenCategories: () -> Unit = {},
    onOpenPayments: () -> Unit = {},
    onOpenAccounts: () -> Unit = {},
    onOpenReview: () -> Unit = {},
    onOpenChat: () -> Unit = {}
) {
    val context = LocalContext.current
    val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    val app = remember(context) { context.applicationContext as FinanzasApp }
    val database = app.database
    val imports by database.systemDao()
        .observeImports()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val pendingReview by database.transactionDao()
        .observePendingReviewCount()
        .collectAsStateWithLifecycle(initialValue = 0)
    val themeMode by app.settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.OSCURO)
    val scope = rememberCoroutineScope()
    var showImport by remember { mutableStateOf(false) }
    var showCredits by remember { mutableStateOf(false) }

    // El permiso puede cambiar fuera de la app: se vuelve a leer al regresar.
    var notificationsOn by remember { mutableStateOf(PaymentReminderScheduler.canNotify(context)) }
    LifecycleResumeEffect(Unit) {
        notificationsOn = PaymentReminderScheduler.canNotify(context)
        onPauseOrDispose { }
    }

    PageScaffold(
        title = stringResource(R.string.aj_title),
        subtitle = stringResource(R.string.aj_subtitle)
    ) {
        SectionHeader(title = stringResource(R.string.aj_section_appearance))
        AppCard {
            Text(stringResource(R.string.aj_theme_title), style = MaterialTheme.typography.titleSmall, color = AppPalette.Ink)
            Spacer(Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ThemeMode.entries.forEach { mode ->
                    val selected = mode == themeMode
                    Surface(
                        onClick = { scope.launch { app.settings.setThemeMode(mode) } },
                        shape = MaterialTheme.shapes.small,
                        color = if (selected) AppPalette.Accent else AppPalette.SurfaceMuted
                    ) {
                        Text(
                            text = stringResource(mode.labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) AppPalette.OnAccent else AppPalette.InkSoft,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(Spacing.md))
            Text(stringResource(R.string.aj_language_title), style = MaterialTheme.typography.titleSmall, color = AppPalette.Ink)
            Spacer(Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                AppLanguage.entries.forEach { language ->
                    val selected = language == Idioma.current
                    Surface(
                        onClick = { (context as? android.app.Activity)?.let { Idioma.set(it, language) } },
                        shape = MaterialTheme.shapes.small,
                        color = if (selected) AppPalette.Accent else AppPalette.SurfaceMuted
                    ) {
                        Text(
                            text = language.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) AppPalette.OnAccent else AppPalette.InkSoft,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        SectionHeader(title = stringResource(R.string.aj_section_organization))
        AppCard(contentPadding = PaddingValues(vertical = Spacing.xs)) {
            NavRow(Icons.Rounded.AccountBalanceWallet, AppPalette.Sky, stringResource(R.string.aj_nav_accounts), stringResource(R.string.aj_nav_accounts_detail), onOpenAccounts)
            NavRow(Icons.Rounded.Category, AppPalette.Violet, stringResource(R.string.aj_nav_categories), stringResource(R.string.aj_nav_categories_detail), onOpenCategories)
            NavRow(Icons.Rounded.EventRepeat, AppPalette.Teal, stringResource(R.string.aj_nav_payments), stringResource(R.string.aj_nav_payments_detail), onOpenPayments)
            NavRow(
                Icons.Rounded.FactCheck,
                if (pendingReview > 0) AppPalette.Amber else AppPalette.Green,
                if (pendingReview > 0) stringResource(R.string.aj_nav_review_pending, pendingReview) else stringResource(R.string.aj_nav_review),
                stringResource(R.string.aj_nav_review_detail),
                onOpenReview
            )
        }

        SectionHeader(title = stringResource(R.string.aj_section_alerts))
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(
                    icon = if (notificationsOn) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                    tint = if (notificationsOn) AppPalette.Green else AppPalette.Amber,
                    size = 44.dp
                )
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (notificationsOn) stringResource(R.string.aj_alerts_on) else stringResource(R.string.aj_alerts_off),
                        style = MaterialTheme.typography.titleSmall,
                        color = AppPalette.Ink
                    )
                    Text(
                        text = if (notificationsOn) {
                            stringResource(R.string.aj_alerts_on_detail)
                        } else {
                            stringResource(R.string.aj_alerts_off_detail)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.InkSoft
                    )
                }
            }
            if (!notificationsOn) {
                Spacer(Modifier.height(Spacing.md))
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Accent, contentColor = AppPalette.OnAccent)
                ) { Text(stringResource(R.string.aj_alerts_enable)) }
            }
        }

        SectionHeader(title = stringResource(R.string.aj_section_privacy))

        LockCard()

        SectionHeader(title = stringResource(R.string.aj_section_ai))
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(
                    icon = Icons.Filled.Info,
                    tint = AppPalette.Violet,
                    size = 44.dp
                )
                Spacer(Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.aj_ai_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.aj_ai_detail),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))
            Button(
                onClick = onOpenChat,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Accent, contentColor = AppPalette.OnAccent)
            ) { Text(stringResource(R.string.aj_ai_open)) }
        }

        SectionHeader(title = stringResource(R.string.aj_section_data))
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(
                    icon = Icons.Filled.DateRange,
                    tint = AppPalette.Sky,
                    size = 44.dp
                )
                Spacer(Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.aj_import_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.aj_import_detail),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))

            // Un solo botón, lleno y con un verbo: elegir el archivo es el
            // único paso aquí; la confirmación llega después, en la vista previa.
            Button(
                onClick = { showImport = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppPalette.Accent,
                    contentColor = AppPalette.HeroInk
                ),
                contentPadding = PaddingValues(vertical = Spacing.md)
            ) {
                Text(
                    text = stringResource(R.string.aj_import_pick),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        BackupCard()

        ShareApkCard()

        if (imports.isNotEmpty()) {
            SectionHeader(title = stringResource(R.string.aj_section_recent_imports))
            AppCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = Spacing.lg,
                vertical = Spacing.sm
            )) {
                imports.take(4).forEachIndexed { index, job ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                    ImportHistoryRow(job = job)
                }
            }
        }

        Text(
            text = stringResource(R.string.aj_footer, context.packageManager.getPackageInfo(context.packageName, 0).versionName.toString()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        // Créditos discretos: un enlace pequeño que despliega las licencias.
        // La licencia CC BY-SA del logo de Nequi exige mostrar el crédito.
        Text(
            text = if (showCredits) stringResource(R.string.aj_credits_hide) else stringResource(R.string.aj_credits_show),
            style = MaterialTheme.typography.labelSmall,
            color = AppPalette.InkFaint,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showCredits = !showCredits }
                .padding(vertical = Spacing.xs)
        )
        if (showCredits) {
            Text(
                text = stringResource(R.string.aj_credits_text),
                style = MaterialTheme.typography.labelSmall,
                color = AppPalette.InkFaint,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showImport) {
        ImportSheet(onDismiss = { showImport = false })
    }
}

@Composable
private fun ImportHistoryRow(job: ImportJob) {
    val succeeded = job.status == ImportStatus.COMPLETADO
    val label = when (job.status) {
        ImportStatus.COMPLETADO -> stringResource(R.string.aj_import_status_done)
        ImportStatus.ERROR -> stringResource(R.string.aj_import_status_error)
        ImportStatus.PROCESANDO -> stringResource(R.string.aj_import_status_processing)
        ImportStatus.REVISAR -> stringResource(R.string.aj_import_status_review)
        ImportStatus.PENDIENTE -> stringResource(R.string.aj_import_status_pending)
    }
    val color = if (succeeded) AppPalette.Green else AppPalette.Coral
    val importedText = pluralStringResource(R.plurals.aj_import_count, job.imported, job.imported)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = job.fileName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append(Fechas.corta(job.createdAt.atZone(Fechas.Bogota).toLocalDate()))
                    append(" · ")
                    append(label)
                    if (job.imported > 0) {
                        append(" · ")
                        append(importedText)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = job.kind.name,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun SecurityRow(
    title: String,
    detail: String,
    icon: ImageVector = Icons.Filled.Check
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.Top
    ) {
        IconBubble(
            icon = icon,
            tint = if (icon == Icons.Filled.Check) AppPalette.Green else AppPalette.Amber,
            size = 28.dp
        )
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
