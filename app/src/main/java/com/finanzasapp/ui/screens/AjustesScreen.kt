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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.finanzasapp.ui.components.AppCard
import com.finanzasapp.ui.components.IconBubble
import com.finanzasapp.ui.components.PageScaffold
import com.finanzasapp.ui.components.SectionHeader
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.Spacing

@Composable
fun AjustesScreen() {
    val context = LocalContext.current
    val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    PageScaffold(
        title = "Ajustes",
        subtitle = "Tus datos, tus reglas."
    ) {
        SectionHeader(title = "Privacidad y seguridad")

        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(
                    icon = Icons.Filled.Lock,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    background = AppPalette.SurfaceMuted,
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
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    background = AppPalette.SurfaceMuted,
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
                    icon = Icons.Filled.Check,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    background = AppPalette.SurfaceMuted,
                    size = 44.dp
                )
                Spacer(Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Exportar e importar",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "CSV y XLSX, sin pasar por la nube. Llega en la fase 4.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            text = "FinanzasApp 0.2.0 · Hecha para tus finanzas, no para tus datos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            background = AppPalette.SurfaceMuted,
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
