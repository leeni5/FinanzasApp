package com.finanzasapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.finanzasapp.data.settings.ThemeMode

/**
 * Tema de la app: oscuro por defecto, claro o según el teléfono.
 *
 * Deliberadamente NO usamos color dinámico de Android: la identidad depende
 * de que la paleta sea siempre la misma, en cualquier teléfono.
 */
@Composable
fun FinanzasTheme(
    mode: ThemeMode = ThemeMode.OSCURO,
    content: @Composable () -> Unit
) {
    val dark = when (mode) {
        ThemeMode.OSCURO -> true
        ThemeMode.CLARO -> false
        ThemeMode.SISTEMA -> isSystemInDarkTheme()
    }
    val colors = if (dark) DarkAppColors else LightAppColors
    val scheme = remember(colors) { colors.toColorScheme() }

    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
