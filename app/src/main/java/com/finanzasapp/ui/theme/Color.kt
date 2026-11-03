package com.finanzasapp.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Paleta base del sistema visual, inspirada en Notion.
 * Ningún color de aquí debería usarse directamente en pantallas:
 * siempre a través de MaterialTheme.colorScheme o de [NotionPalette].
 */
object NotionPalette {
    val Background = Color(0xFFFFFFFF)
    val Surface = Color(0xFFF7F7F5)
    val SurfaceHover = Color(0xFFEFEFED)
    val TextPrimary = Color(0xFF37352F)
    val TextSecondary = Color(0xFF787774)
    val TextTertiary = Color(0xFF9B9A97)
    val Border = Color(0xFFE9E9E7)
    val Accent = Color(0xFF2383E2)
    val AccentSoft = Color(0xFFE7F1FB)
    val Success = Color(0xFF448361)
    val Warning = Color(0xFFCB912F)
    val Danger = Color(0xFFD44C47)
}

val NotionColorScheme = lightColorScheme(
    primary = NotionPalette.Accent,
    onPrimary = Color.White,
    primaryContainer = NotionPalette.AccentSoft,
    onPrimaryContainer = NotionPalette.TextPrimary,

    secondary = NotionPalette.TextPrimary,
    onSecondary = Color.White,
    secondaryContainer = NotionPalette.Surface,
    onSecondaryContainer = NotionPalette.TextPrimary,

    background = NotionPalette.Background,
    onBackground = NotionPalette.TextPrimary,

    surface = NotionPalette.Background,
    onSurface = NotionPalette.TextPrimary,
    surfaceVariant = NotionPalette.Surface,
    onSurfaceVariant = NotionPalette.TextSecondary,

    outline = NotionPalette.Border,
    outlineVariant = NotionPalette.Border,

    error = NotionPalette.Danger,
    onError = Color.White,
    errorContainer = Color(0xFFFBECEC),
    onErrorContainer = NotionPalette.Danger,

    scrim = Color(0x66000000)
)
