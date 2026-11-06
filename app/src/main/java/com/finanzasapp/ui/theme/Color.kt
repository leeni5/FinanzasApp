package com.finanzasapp.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Paleta limpia: papel casi blanco, tinta neutra y un solo acento.
 *
 * La base es tranquila (fondo gris cálido muy claro, tarjetas blancas sin borde).
 * El color se reserva para tres cosas: la marca, el dinero que entra/sale y los
 * estados de alerta. Todo lo demás se resuelve con espacio y jerarquía.
 *
 * Ningún color se usa directamente en pantallas: siempre a través de
 * MaterialTheme.colorScheme o de [AppPalette].
 */
object AppPalette {
    // Base
    val Background = Color(0xFFF8F8F6)
    val BackgroundTint = Color(0xFFF1F1EE)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFF4F4F1)
    val SurfaceStrong = Color(0xFFE9E9E5)

    // Texto
    val Ink = Color(0xFF1B1B1B)
    val InkSoft = Color(0xFF6F6F6C)
    val InkFaint = Color(0xFFA1A19D)
    val Border = Color(0xFFECECE9)

    // Acento principal
    val Indigo = Color(0xFF4C5BD4)
    val IndigoSoft = Color(0xFFEDEFFB)
    val IndigoDeep = Color(0xFF2A3390)

    // Acentos secundarios: solo para señales puntuales
    val Violet = Color(0xFF7C5CE0)
    val VioletSoft = Color(0xFFF1EDFC)

    val Teal = Color(0xFF148F84)
    val TealSoft = Color(0xFFE3F4F1)

    val Green = Color(0xFF1E9E62)
    val GreenSoft = Color(0xFFE7F5EC)

    val Amber = Color(0xFFC98A12)
    val AmberSoft = Color(0xFFFAF1DC)

    val Coral = Color(0xFFDE5A43)
    val CoralSoft = Color(0xFFFBEBE7)

    val Rose = Color(0xFFD9548C)
    val RoseSoft = Color(0xFFFBE7F0)

    val Sky = Color(0xFF2E86C8)
    val SkySoft = Color(0xFFE3F0FA)

    val Orange = Color(0xFFEE8A4A)

    /**
     * Tarjeta de saldo: índigo profundo, plana, sin degradado ni transparencias.
     * Sus textos usan tonos sólidos derivados del mismo índigo.
     */
    val Hero = Color(0xFF2A3390)
    val HeroInk = Color(0xFFFFFFFF)
    val HeroInkSoft = Color(0xFFC3C8F0)
    val HeroInkFaint = Color(0xFF8F96D8)
    val HeroLine = Color(0xFF3F48A3)
    val HeroChip = Color(0xFF3A43A0)
    val HeroDebt = Color(0xFFFFB4A3)
    val HeroOk = Color(0xFF9BE3B8)
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
