package com.finanzasapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.Spacing

/**
 * true cuando la lista está quieta arriba o el usuario sube.
 * Sirve para esconder el botón flotante mientras se baja: así nunca tapa
 * el monto de una fila que se está leyendo.
 */
@Composable
fun LazyListState.isScrollingUp(): Boolean {
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            if (previousIndex != firstVisibleItemIndex) {
                previousIndex > firstVisibleItemIndex
            } else {
                previousOffset >= firstVisibleItemScrollOffset
            }.also {
                previousIndex = firstVisibleItemIndex
                previousOffset = firstVisibleItemScrollOffset
            }
        }
    }.value
}

/** Botón flotante de alta que entra y sale con el scroll. */
@Composable
fun ScrollAwareFab(
    visible: Boolean,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        FloatingActionButton(
            onClick = onClick,
            shape = MaterialTheme.shapes.large,
            containerColor = AppPalette.Indigo,
            contentColor = AppPalette.HeroInk
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = description,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Encabezado común: título y, si hace falta, una línea de contexto.
 * Sin cejas, rayas ni etiquetas de color. El espacio hace el resto.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (subtitle != null) {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Estructura de las pantallas que caben en un scroll normal.
 * Un solo margen lateral para toda la app y aire de sobra arriba.
 */
@Composable
fun PageScaffold(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.page)
            .padding(top = Spacing.xl, bottom = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl)
    ) {
        ScreenHeader(title = title, subtitle = subtitle)
        content()
    }
}

/**
 * Estado vacío: icono neutro, un título y una explicación honesta.
 * La acción es opcional para no mostrar botones que todavía no hacen nada.
 */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Info,
    accent: Color = AppPalette.Indigo,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        IconBubble(
            icon = icon,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            background = AppPalette.SurfaceMuted,
            size = 44.dp
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(top = Spacing.xs)
        )
        if (actionLabel != null && onAction != null) {
            TextButton(
                onClick = onAction,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.padding(top = Spacing.sm)
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = accent
                )
            }
        }
    }
}
