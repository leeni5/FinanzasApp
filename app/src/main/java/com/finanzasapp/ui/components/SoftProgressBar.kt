package com.finanzasapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.finanzasapp.ui.theme.soft

/** Desaceleración marcada: arranca rápido y se posa suave al final. */
internal val EaseOutQuint = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/**
 * Barra de progreso redondeada que crece desde cero al aparecer.
 *
 * El avance animado se lee solo dentro de `drawBehind`: cada cuadro de la
 * animación redibuja la barra, pero no la recompone ni la vuelve a medir.
 * Eso es lo que la mantiene fluida en una pantalla de 120 Hz.
 */
@Composable
fun SoftProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    trackColor: Color = color.soft()
) {
    val target = progress.coerceIn(0f, 1f)
    // Recuerda el último valor dibujado aunque la barra salga de la pantalla:
    // al volver no crece de nuevo desde cero.
    var settled by rememberSaveable { mutableFloatStateOf(0f) }
    val animated = remember { Animatable(settled) }

    LaunchedEffect(target) {
        if (animated.value != target) {
            animated.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 700, easing = EaseOutQuint)
            )
        }
        settled = target
    }

    androidx.compose.foundation.layout.Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .drawBehind {
                val radius = CornerRadius(size.height / 2f)
                drawRoundRect(color = trackColor, cornerRadius = radius)
                val width = size.width * animated.value
                if (width > 0f) {
                    drawRoundRect(
                        color = color,
                        size = Size(width.coerceAtLeast(size.height), size.height),
                        cornerRadius = radius
                    )
                }
            }
    )
}
