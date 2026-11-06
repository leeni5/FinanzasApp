package com.finanzasapp.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb

/**
 * Convierte los colores guardados como texto (#RRGGBB) en colores Compose.
 * Si el valor viene dañado, cae en un acento seguro en vez de romper la fila.
 */
fun String.asColor(fallback: Color = AppPalette.Indigo): Color =
    runCatching { Color(android.graphics.Color.parseColor(this)) }.getOrDefault(fallback)

/** Tonos para categorías, cuentas y metas: ninguno compite con las alertas. */
val IdentityPalette = listOf(
    Color(0xFF4C5BD4), Color(0xFF2E86C8), Color(0xFF7C5CE0), Color(0xFF148F84),
    Color(0xFF0E9AB5), Color(0xFFA24FB8), Color(0xFF6A9A1F), Color(0xFFD9548C),
    Color(0xFF2F9E63), Color(0xFF5B6B82)
)

/**
 * Color de identidad (categoría, meta) a partir del texto guardado.
 * Si el tono es rojo, naranja o ámbar —los de las alertas— se cambia por uno
 * de [IdentityPalette], elegido siempre igual para la misma clave. Así las
 * bases creadas antes de este cambio tampoco confunden una categoría con un aviso.
 */
fun identityColor(stored: String, key: String): Color {
    val color = stored.asColor()
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    val warm = hsv[1] > 0.25f && (hsv[0] < 55f || hsv[0] >= 345f)
    return if (warm) IdentityPalette[Math.floorMod(key.hashCode(), IdentityPalette.size)] else color
}

/**
 * Versión suave y opaca de un color: lo mezcla con blanco en vez de usar
 * transparencia. Así el tono es el mismo sobre cualquier fondo.
 */
fun Color.soft(amount: Float = 0.14f): Color = lerp(Color.White, this, amount)
