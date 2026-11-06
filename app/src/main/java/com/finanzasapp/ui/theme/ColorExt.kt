package com.finanzasapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb

/** Color de respaldo si un valor guardado viene dañado: violeta del acento. */
private val FallbackColor = Color(0xFF8B7CF6)

/**
 * Convierte los colores guardados como texto (#RRGGBB) en colores Compose.
 * Si el valor viene dañado, cae en un color seguro en vez de romper la fila.
 */
fun String.asColor(fallback: Color = FallbackColor): Color =
    runCatching { Color(android.graphics.Color.parseColor(this)) }.getOrDefault(fallback)

/** Convierte un color a texto #RRGGBB para guardarlo. */
fun Color.toHex(): String = String.format("#%06X", 0xFFFFFF and toArgb())

/**
 * Color de identidad (categoría, meta) a partir del texto guardado.
 * Se respeta tal cual: el usuario elige cualquier color, y las alertas se
 * distinguen por icono y texto, no solo por el tono.
 */
@Suppress("UNUSED_PARAMETER")
fun identityColor(stored: String, key: String): Color = stored.asColor()

/** Texto legible encima de un color: casi negro sobre claros, blanco sobre oscuros. */
fun Color.readableInk(): Color =
    if (luminance() > 0.45f) Color(0xFF15151B) else Color(0xFFFFFFFF)

/** Versión atenuada de un texto sobre un color (para la línea secundaria). */
fun Color.readableInkSoft(): Color =
    if (luminance() > 0.45f) Color(0xFF3B3B46) else Color(0xFFE6E6EE)

/**
 * Versión suave y opaca de un color: lo mezcla con la superficie del tema en
 * vez de usar transparencia. En modo oscuro queda un tono profundo; en claro,
 * un pastel.
 */
@Composable
@ReadOnlyComposable
fun Color.soft(amount: Float = 0.16f): Color =
    lerp(LocalAppColors.current.surface, this, if (LocalAppColors.current.isDark) amount * 1.4f else amount)

/** Paleta de partida para categorías nuevas y la importación. */
val IdentityPalette = listOf(
    Color(0xFF8B7CF6), Color(0xFF60A5FA), Color(0xFF2DD4BF), Color(0xFF3DD68C),
    Color(0xFFF5C451), Color(0xFFFB923C), Color(0xFFFF6B6B), Color(0xFFF472B6),
    Color(0xFFC084FC), Color(0xFF94A3B8), Color(0xFF22D3EE), Color(0xFFA3E635)
)
