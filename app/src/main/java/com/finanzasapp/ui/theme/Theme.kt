package com.finanzasapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Tema único de la app.
 *
 * Deliberadamente NO usamos color dinámico de Android: la identidad depende
 * de que la paleta cálida sea siempre la misma, en cualquier teléfono.
 */
@Composable
fun FinanzasTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
