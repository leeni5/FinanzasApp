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

    // El permiso puede cambiar fuera de la app: se vuelve a leer al regresar.
    var notificationsOn by remember { mutableStateOf(PaymentReminderScheduler.canNotify(context)) }
    LifecycleResumeEffect(Unit) {
        notificationsOn = PaymentReminderScheduler.canNotify(context)
        onPauseOrDispose { }
    }

    PageScaffold(
        title = "Ajustes",
        subtitle = "Tus datos, tus reglas."
    ) {
        SectionHeader(title = "Apariencia")
        AppCard {
            Text("Tema", style = MaterialTheme.typography.titleSmall, color = AppPalette.Ink)
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
                            text = mode.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) AppPalette.OnAccent else AppPalette.InkSoft,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        SectionHeader(title = "Organización")
        AppCard(contentPadding = PaddingValues(vertical = Spacing.xs)) {
            NavRow(Icons.Rounded.AccountBalanceWallet, AppPalette.Sky, "Cuentas", "Nequi, bancos, efectivo y tarjetas.", onOpenAccounts)
            NavRow(Icons.Rounded.Category, AppPalette.Violet, "Categorías", "Crea las tuyas con icono y colores.", onOpenCategories)
            NavRow(Icons.Rounded.EventRepeat, AppPalette.Teal, "Pagos programados", "Arriendo, servicios y suscripciones con aviso.", onOpenPayments)
            NavRow(
                Icons.Rounded.FactCheck,
                if (pendingReview > 0) AppPalette.Amber else AppPalette.Green,
                if (pendingReview > 0) "Revisión · $pendingReview por revisar" else "Revisión",
                "Pendientes, reglas de categorías e historial de cambios.",
                onOpenReview
            )
        }

        SectionHeader(title = "Avisos")
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
                        text = if (notificationsOn) "Avisos de pago activos" else "Avisos de pago apagados",
                        style = MaterialTheme.typography.titleSmall,
                        color = AppPalette.Ink
                    )
                    Text(
                        text = if (notificationsOn) {
                            "Te avisamos 7 días, 2 días y el día en que vence cada pago."
                        } else {
                            "La app funciona igual, pero no podrá recordarte los pagos."
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
                ) { Text("Activar avisos") }
            }
        }

        SectionHeader(title = "Privacidad y seguridad")

        LockCard()

        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(
                    icon = Icons.Filled.Lock,
                    tint = AppPalette.Teal,
                    size = 50.dp
                )
                Spacer(Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Todo local y cifrado",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tu base de datos vive en este teléfono, protegida con AES-256.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(Spacing.md))

            SecurityRow(
                title = "Base de datos cifrada",
                detail = "SQLCipher cifra el archivo completo. Sin la clave solo hay ruido ilegible."
            )
            SecurityRow(
                title = "Clave en el Android Keystore",
                detail = "La llave que descifra tus datos nunca sale del almacén seguro del teléfono."
            )
            SecurityRow(
                title = "Sin copias en la nube",
                detail = "La copia de seguridad de Android está desactivada: tu base no se sube a Google Drive."
            )
            SecurityRow(
                title = "Internet solo para la IA",
                detail = "La app no envía nada a ningún servidor salvo cuando usas el chat con IA, " +
                    "y antes te muestra exactamente qué datos salen."
            )

            if (isDebuggable) {
                SecurityRow(
                    title = "Capturas permitidas en pruebas",
                    detail = "Esta compilación deja tomar capturas para revisar el diseño. " +
                        "La versión final bloqueará capturas y la vista reciente.",
                    icon = Icons.Filled.Info
                )
            } else {
                SecurityRow(
                    title = "Capturas bloqueadas",
                    detail = "La vista reciente y las capturas de pantalla quedan ocultas."
                )
            }
        }

        SectionHeader(title = "Inteligencia artificial")
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
                        text = "Chat con IA",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Pregunta sobre tus gastos con Groq, Mistral, OpenRouter o Gemini y tu " +
                            "propia clave, guardada cifrada. Antes de enviar ves qué datos salen. " +
                            "La IA solo responde: no cambia tus movimientos.",
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
            ) { Text("Abrir el chat") }
        }

        SectionHeader(title = "Tus datos")
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
                        text = "Importar movimientos",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "CSV, Excel o el PDF de Budge. Se muestra una vista previa " +
                            "y nada se guarda hasta que confirmes.",
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
                    text = "Elegir archivo para importar",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        BackupCard()

        ShareApkCard()

        if (imports.isNotEmpty()) {
            SectionHeader(title = "Últimas importaciones")
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

        SectionHeader(title = "Créditos")
        AppCard {
            Text(
                text = "Logos de las entidades: Wikimedia Commons, recortados a su símbolo. Las marcas pertenecen a sus dueños " +
                    "y se usan solo para identificar cada cuenta.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "Logo de Nequi: recorte de «Nequi Colombia logo», autor Nequi (www.nequi.com.co), " +
                    "licencia CC BY-SA 4.0 (creativecommons.org/licenses/by-sa/4.0).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "FinanzasApp ${context.packageManager.getPackageInfo(context.packageName, 0).versionName} · Hecha para tus finanzas, no para tus datos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }

    if (showImport) {
        ImportSheet(onDismiss = { showImport = false })
    }
}

@Composable
private fun ImportHistoryRow(job: ImportJob) {
    val succeeded = job.status == ImportStatus.COMPLETADO
    val label = when (job.status) {
        ImportStatus.COMPLETADO -> "Completada"
        ImportStatus.ERROR -> "Con error"
        ImportStatus.PROCESANDO -> "Procesando"
        ImportStatus.REVISAR -> "Por revisar"
        ImportStatus.PENDIENTE -> "Pendiente"
    }
    val color = if (succeeded) AppPalette.Green else AppPalette.Coral

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
                        append(job.imported)
                        append(if (job.imported == 1) " movimiento" else " movimientos")
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
