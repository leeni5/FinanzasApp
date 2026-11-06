package com.finanzasapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.Spacing
import com.finanzasapp.ui.util.Money

/** Una porción de la torta. */
data class DonutSlice(val label: String, val value: Long, val color: Color)

/**
 * Torta en forma de anillo con el total al centro y la leyenda al lado.
 *
 * Las porciones se separan con un hueco pequeño para que dos colores
 * parecidos no se fundan. El anillo se "dibuja" al aparecer (700 ms); la
 * animación se lee solo en la fase de dibujo.
 * Las porciones menores al 3 % se agrupan en «Otras» para que la leyenda
 * siga siendo legible.
 */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    centerLabel: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 150.dp,
    otherColor: Color = AppPalette.InkFaint
) {
    val total = slices.sumOf { it.value }.coerceAtLeast(1L)
    val grouped = remember(slices, otherColor) {
        val (big, small) = slices.filter { it.value > 0 }.partition { it.value * 100 / total >= 3 }
        if (small.isEmpty()) big else big + DonutSlice("Otras", small.sumOf { it.value }, otherColor)
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(grouped) { progress.animateTo(1f, tween(700, easing = EaseOutQuint)) }
    val track = AppPalette.SurfaceMuted

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(diameter)) {
                val strokeWidth = size.minDimension * 0.16f
                val inset = strokeWidth / 2f
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(inset, inset)
                drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(strokeWidth))

                val gap = if (grouped.size > 1) 2.5f else 0f
                var start = -90f
                val sweepTotal = 360f * progress.value
                grouped.forEach { slice ->
                    val sweep = 360f * slice.value / total
                    val visible = (sweepTotal - (start + 90f)).coerceIn(0f, sweep)
                    if (visible - gap > 0f) {
                        drawArc(
                            color = slice.color,
                            startAngle = start + gap / 2f,
                            sweepAngle = visible - gap,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(strokeWidth, cap = StrokeCap.Butt)
                        )
                    }
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(centerLabel, style = MaterialTheme.typography.labelSmall, color = AppPalette.InkSoft)
                Text(
                    Money.cop(slices.sumOf { it.value }),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppPalette.Ink,
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.width(Spacing.lg))

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            grouped.take(6).forEach { slice ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ColorDot(slice.color, size = 10.dp)
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        slice.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppPalette.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${(slice.value * 100 / total)} %",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppPalette.InkSoft
                    )
                }
            }
        }
    }
}
