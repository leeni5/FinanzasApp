package com.finanzasapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Tema único de la app.
 *
 * Deliberadamente NO usamos color dinámico de Android: el estilo Notion
 * depende de que la paleta sea siempre la misma.
 */
@Composable
fun FinanzasTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NotionColorScheme,
        typography = NotionTypography,
        shapes = NotionShapes,
        content = content
    )
}
