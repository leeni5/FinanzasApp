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

@Composable
fun AjustesScreen() {
    val context = LocalContext.current
    val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    val database = remember(context) {
        (context.applicationContext as FinanzasApp).database
    }
    val imports by database.systemDao()
        .observeImports()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var showImport by remember { mutableStateOf(false) }

    PageScaffold(
        title = "Ajustes",
        subtitle = "Tus datos, tus reglas."
    ) {
        SectionHeader(title = "Privacidad y seguridad")

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
                title = "Sin permiso de internet",
                detail = "La app no puede enviar nada a ningún servidor. Cuando actives la IA, " +
                    "pediremos red solo para esa función."
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
                        text = "Resúmenes con Gemini",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "La IA será opcional y gratuita. Tu clave se guardará cifrada " +
                            "y solo se enviará el texto que elijas analizar. Llega en la fase 7.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
                    containerColor = AppPalette.Indigo,
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

        Text(
            text = "FinanzasApp 0.5.0 · Fase 4 · Hecha para tus finanzas, no para tus datos.",
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
