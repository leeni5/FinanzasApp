package com.finanzasapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Esquinas contenidas: lo justo para que se sienta app móvil, sin burbujas.
 * Las tarjetas usan [Shapes.large]; campos y botones, [Shapes.medium];
 * chips y barras, [Shapes.small].
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)
