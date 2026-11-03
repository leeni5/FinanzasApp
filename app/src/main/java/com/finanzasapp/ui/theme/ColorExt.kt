package com.finanzasapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Convierte los colores guardados como texto (#RRGGBB) en colores Compose.
 * Si el valor viene dañado, cae en un acento seguro en vez de romper la fila.
 */
fun String.asColor(fallback: Color = AppPalette.Indigo): Color =
    runCatching { Color(android.graphics.Color.parseColor(this)) }.getOrDefault(fallback)
