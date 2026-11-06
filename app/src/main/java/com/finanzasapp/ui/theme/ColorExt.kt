package com.finanzasapp.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Convierte los colores guardados como texto (#RRGGBB) en colores Compose.
 * Si el valor viene dañado, cae en un acento seguro en vez de romper la fila.
 */
fun String.asColor(fallback: Color = AppPalette.Indigo): Color =
    runCatching { Color(android.graphics.Color.parseColor(this)) }.getOrDefault(fallback)

/**
 * Versión suave y opaca de un color: lo mezcla con blanco en vez de usar
 * transparencia. Así el tono es el mismo sobre cualquier fondo.
 */
fun Color.soft(amount: Float = 0.14f): Color = lerp(Color.White, this, amount)
