package com.finanzasapp.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Paleta de la app: papel cálido, tinta suave y acentos vivos.
 *
 * La base sigue siendo tranquila (fondo crema, tarjetas blancas, bordes finos),
 * pero cada sección tiene un acento propio. Así la app se siente viva sin
 * convertirse en un arcoíris: el color se usa para señalar, no para decorar.
 *
 * Ningún color se usa directamente en pantallas: siempre a través de
 * MaterialTheme.colorScheme o de [AppPalette].
 */
object AppPalette {
    // Base
    val Background = Color(0xFFFAF7F2)
    val BackgroundTint = Color(0xFFF3ECE3)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFF5F0E9)
    val SurfaceStrong = Color(0xFFEDE5DA)

    // Texto
    val Ink = Color(0xFF2E2A26)
    val InkSoft = Color(0xFF6D675F)
    val InkFaint = Color(0xFF9A938A)
    val Border = Color(0xFFEAE2D8)

    // Acentos
    val Indigo = Color(0xFF4C5FD5)
    val IndigoSoft = Color(0xFFE8EBFB)
    val IndigoDeep = Color(0xFF2A3694)

    val Violet = Color(0xFF8A5CF6)
    val VioletSoft = Color(0xFFF1EAFF)

    val Teal = Color(0xFF0F9D8F)
    val TealSoft = Color(0xFFDDF5F1)

    val Green = Color(0xFF2F9E63)
    val GreenSoft = Color(0xFFE1F4E9)

    val Amber = Color(0xFFD99A18)
    val AmberSoft = Color(0xFFFCF1D9)

    val Coral = Color(0xFFE06A4E)
    val CoralSoft = Color(0xFFFCEAE4)

    val Rose = Color(0xFFD9548C)
    val RoseSoft = Color(0xFFFBE7F0)

    val Sky = Color(0xFF2E86C8)
    val SkySoft = Color(0xFFE3F0FA)

    val Orange = Color(0xFFEE8A4A)

    /**
     * Degradado del saldo. Es el único degradado de la app: el punto focal
     * de la pantalla de inicio. Del resto, superficies planas.
     */
    val HeroGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF3947BE),
            Color(0xFF5B50D6),
            Color(0xFF8A5CF6)
        )
    )
}

val AppColorScheme = lightColorScheme(
    primary = AppPalette.Indigo,
    onPrimary = Color.White,
    primaryContainer = AppPalette.IndigoSoft,
    onPrimaryContainer = AppPalette.IndigoDeep,

    secondary = AppPalette.Teal,
    onSecondary = Color.White,
    secondaryContainer = AppPalette.TealSoft,
    onSecondaryContainer = Color(0xFF0A5F57),

    tertiary = AppPalette.Violet,
    onTertiary = Color.White,
    tertiaryContainer = AppPalette.VioletSoft,
    onTertiaryContainer = Color(0xFF4A2A96),

    background = AppPalette.Background,
    onBackground = AppPalette.Ink,

    surface = AppPalette.Surface,
    onSurface = AppPalette.Ink,
    surfaceVariant = AppPalette.SurfaceMuted,
    onSurfaceVariant = AppPalette.InkSoft,

    surfaceContainerLowest = AppPalette.Surface,
    surfaceContainerLow = AppPalette.Background,
    surfaceContainer = AppPalette.SurfaceMuted,
    surfaceContainerHigh = AppPalette.SurfaceStrong,
    surfaceContainerHighest = AppPalette.SurfaceStrong,

    outline = AppPalette.Border,
    outlineVariant = AppPalette.Border,

    error = AppPalette.Coral,
    onError = Color.White,
    errorContainer = AppPalette.CoralSoft,
    onErrorContainer = Color(0xFF9B3620),

    scrim = Color(0x66000000)
)
