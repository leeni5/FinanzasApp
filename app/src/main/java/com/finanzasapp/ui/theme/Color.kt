package com.finanzasapp.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colores con nombre de la app, en dos versiones: oscura (la de arranque) y clara.
 *
 * El color se reserva para cuatro cosas: el acento violeta, el dinero que
 * entra/sale, las alertas y las categorías (que elige el usuario). Las alertas
 * nunca dependen solo del color: siempre llevan icono y texto.
 *
 * Todo es opaco: los tonos "suaves" son mezclas sólidas, no transparencias.
 */
@Immutable
data class AppColors(
    val isDark: Boolean,
    // Base
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val surfaceStrong: Color,
    val border: Color,
    // Texto
    val ink: Color,
    val inkSoft: Color,
    val inkFaint: Color,
    // Acento (violeta)
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val accentDeep: Color,
    // Dinero
    val green: Color,
    val greenSoft: Color,
    val coral: Color,
    val coralSoft: Color,
    // Alertas: ritmo alto (ámbar), límite alcanzado (rosa), excedido (coral)
    val amber: Color,
    val amberSoft: Color,
    val limit: Color,
    // Señales puntuales
    val teal: Color,
    val sky: Color,
    val violet: Color,
    // Tarjeta de saldo
    val hero: Color,
    val heroInk: Color,
    val heroInkSoft: Color,
    val heroInkFaint: Color,
    val heroLine: Color,
    val heroChip: Color,
    val heroDecorFar: Color,
    val heroDecorNear: Color,
    val heroDebt: Color,
    val heroOk: Color
)

val DarkAppColors = AppColors(
    isDark = true,
    background = Color(0xFF0E0F14),
    surface = Color(0xFF191A22),
    surfaceMuted = Color(0xFF22232D),
    surfaceStrong = Color(0xFF2C2D39),
    border = Color(0xFF272833),
    ink = Color(0xFFF1F1F5),
    inkSoft = Color(0xFFA4A5B2),
    inkFaint = Color(0xFF6B6C7A),
    accent = Color(0xFF8B7CF6),
    onAccent = Color(0xFF15102E),
    accentSoft = Color(0xFF2A2548),
    accentDeep = Color(0xFFCBC3FF),
    green = Color(0xFF3DD68C),
    greenSoft = Color(0xFF15291F),
    coral = Color(0xFFFF6B6B),
    coralSoft = Color(0xFF331B1E),
    amber = Color(0xFFF5C451),
    amberSoft = Color(0xFF332B16),
    limit = Color(0xFFF472B6),
    teal = Color(0xFF2DD4BF),
    sky = Color(0xFF60A5FA),
    violet = Color(0xFFA78BFA),
    hero = Color(0xFF231E3F),
    heroInk = Color(0xFFFFFFFF),
    heroInkSoft = Color(0xFFC0B8EC),
    heroInkFaint = Color(0xFF8C84BE),
    heroLine = Color(0xFF383162),
    heroChip = Color(0xFF362F62),
    heroDecorFar = Color(0xFF2A2449),
    heroDecorNear = Color(0xFF312A55),
    heroDebt = Color(0xFFFF9AA2),
    heroOk = Color(0xFF7FE3B2)
)

val LightAppColors = AppColors(
    isDark = false,
    background = Color(0xFFF6F6F8),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFF0F0F4),
    surfaceStrong = Color(0xFFE4E4EA),
    border = Color(0xFFE8E8EE),
    ink = Color(0xFF17171C),
    inkSoft = Color(0xFF686874),
    inkFaint = Color(0xFF9C9CA8),
    accent = Color(0xFF6D5AE6),
    onAccent = Color(0xFFFFFFFF),
    accentSoft = Color(0xFFEEEBFD),
    accentDeep = Color(0xFF3B2C9A),
    green = Color(0xFF16A363),
    greenSoft = Color(0xFFE5F6EC),
    coral = Color(0xFFE5484D),
    coralSoft = Color(0xFFFDECEC),
    amber = Color(0xFFB7860B),
    amberSoft = Color(0xFFFBF3DC),
    limit = Color(0xFFD6408B),
    teal = Color(0xFF0F9488),
    sky = Color(0xFF2B7FD6),
    violet = Color(0xFF7C5CE0),
    hero = Color(0xFF2E2766),
    heroInk = Color(0xFFFFFFFF),
    heroInkSoft = Color(0xFFC9C2F2),
    heroInkFaint = Color(0xFF948CCB),
    heroLine = Color(0xFF433B82),
    heroChip = Color(0xFF423A85),
    heroDecorFar = Color(0xFF362E74),
    heroDecorNear = Color(0xFF3D357E),
    heroDebt = Color(0xFFFFA9B0),
    heroOk = Color(0xFF8CE6BA)
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

/**
 * Acceso corto a los colores del tema activo desde las pantallas:
 * `AppPalette.Accent`, `AppPalette.InkSoft`, etc.
 */
object AppPalette {
    private val c: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current

    val IsDark: Boolean @Composable @ReadOnlyComposable get() = c.isDark

    val Background: Color @Composable @ReadOnlyComposable get() = c.background
    val Surface: Color @Composable @ReadOnlyComposable get() = c.surface
    val SurfaceMuted: Color @Composable @ReadOnlyComposable get() = c.surfaceMuted
    val SurfaceStrong: Color @Composable @ReadOnlyComposable get() = c.surfaceStrong
    val Border: Color @Composable @ReadOnlyComposable get() = c.border

    val Ink: Color @Composable @ReadOnlyComposable get() = c.ink
    val InkSoft: Color @Composable @ReadOnlyComposable get() = c.inkSoft
    val InkFaint: Color @Composable @ReadOnlyComposable get() = c.inkFaint

    val Accent: Color @Composable @ReadOnlyComposable get() = c.accent
    val OnAccent: Color @Composable @ReadOnlyComposable get() = c.onAccent
    val AccentSoft: Color @Composable @ReadOnlyComposable get() = c.accentSoft
    val AccentDeep: Color @Composable @ReadOnlyComposable get() = c.accentDeep

    val Green: Color @Composable @ReadOnlyComposable get() = c.green
    val GreenSoft: Color @Composable @ReadOnlyComposable get() = c.greenSoft
    val Coral: Color @Composable @ReadOnlyComposable get() = c.coral
    val CoralSoft: Color @Composable @ReadOnlyComposable get() = c.coralSoft
    val Amber: Color @Composable @ReadOnlyComposable get() = c.amber
    val AmberSoft: Color @Composable @ReadOnlyComposable get() = c.amberSoft
    val Limit: Color @Composable @ReadOnlyComposable get() = c.limit

    val Teal: Color @Composable @ReadOnlyComposable get() = c.teal
    val Sky: Color @Composable @ReadOnlyComposable get() = c.sky
    val Violet: Color @Composable @ReadOnlyComposable get() = c.violet

    val Hero: Color @Composable @ReadOnlyComposable get() = c.hero
    val HeroInk: Color @Composable @ReadOnlyComposable get() = c.heroInk
    val HeroInkSoft: Color @Composable @ReadOnlyComposable get() = c.heroInkSoft
    val HeroInkFaint: Color @Composable @ReadOnlyComposable get() = c.heroInkFaint
    val HeroLine: Color @Composable @ReadOnlyComposable get() = c.heroLine
    val HeroChip: Color @Composable @ReadOnlyComposable get() = c.heroChip
    val HeroDecorFar: Color @Composable @ReadOnlyComposable get() = c.heroDecorFar
    val HeroDecorNear: Color @Composable @ReadOnlyComposable get() = c.heroDecorNear
    val HeroDebt: Color @Composable @ReadOnlyComposable get() = c.heroDebt
    val HeroOk: Color @Composable @ReadOnlyComposable get() = c.heroOk
}

/** Esquema de Material 3 derivado de los colores de la app. */
fun AppColors.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentSoft,
        onPrimaryContainer = accentDeep,
        secondary = teal,
        onSecondary = onAccent,
        tertiary = violet,
        onTertiary = onAccent,
        background = background,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = surfaceMuted,
        onSurfaceVariant = inkSoft,
        surfaceContainerLowest = surface,
        surfaceContainerLow = background,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceMuted,
        surfaceContainerHighest = surfaceStrong,
        surfaceTint = accent,
        inverseSurface = ink,
        inverseOnSurface = background,
        outline = border,
        outlineVariant = border,
        error = coral,
        onError = onAccent,
        errorContainer = coralSoft,
        onErrorContainer = coral,
        scrim = Color(0xFF000000)
    )
}
