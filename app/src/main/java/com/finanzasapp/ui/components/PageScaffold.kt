package com.finanzasapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.Spacing
import java.util.Locale

/**
 * Estructura común de todas las pantallas: ceja de color, título suave,
 * subtítulo y contenido desplazable con márgenes consistentes.
 */
@Composable
fun PageScaffold(
    title: String,
    subtitle: String? = null,
    eyebrow: String? = null,
    accent: Color = AppPalette.Indigo,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg)
            .padding(top = Spacing.xl, bottom = Spacing.xxxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            if (eyebrow != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Text(
                        text = eyebrow.uppercase(Locale.forLanguageTag("es-CO")),
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        letterSpacing = 1.2.sp
                    )
                }
                Spacer(Modifier.height(Spacing.xxs))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        content()
    }
}

/**
 * Estado vacío amable: icono tintado, título y una explicación honesta.
 * Sin ilustraciones genéricas ni botones que todavía no hacen nada.
 */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Info,
    accent: Color = AppPalette.Indigo
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        IconBubble(icon = icon, tint = accent)
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(top = Spacing.xs)
        )
    }
}
