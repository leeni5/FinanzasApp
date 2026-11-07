package com.finanzasapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.finanzasapp.R
import androidx.compose.ui.res.stringResource
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.IdentityPalette
import com.finanzasapp.ui.theme.Spacing
import com.finanzasapp.ui.theme.readableInk
import com.finanzasapp.ui.theme.toHex

/**
 * Selector de color libre: muestras rápidas y, debajo, tono, saturación y
 * brillo para llegar a cualquier color. Las barras de tono muestran el
 * espectro porque es la única forma de elegir un tono a ojo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPicker(
    color: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
    swatches: List<Color> = IdentityPalette + listOf(Color(0xFFFFFFFF), Color(0xFF1F2029))
) {
    // El estado HSV vive aquí (no se recalcula desde el color en cada cambio)
    // para no perder el tono al bajar la intensidad a cero.
    val initial = remember {
        FloatArray(3).also { android.graphics.Color.colorToHSV(color.toArgb(), it) }
    }
    var hue by remember { mutableStateOf(initial[0]) }
    var sat by remember { mutableStateOf(initial[1]) }
    var value by remember { mutableStateOf(initial[2]) }
    val emitTo by rememberUpdatedState(onColorChange)

    fun emit() = emitTo(Color.hsv(hue, sat, value))

    fun pick(swatch: Color) {
        val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(swatch.toArgb(), it) }
        hue = hsv[0]; sat = hsv[1]; value = hsv[2]
        emitTo(swatch)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            swatches.forEach { swatch ->
                val selected = swatch.toHex() == color.toHex()
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(swatch)
                        .border(1.dp, AppPalette.Border, CircleShape)
                        .clickable { pick(swatch) },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = stringResource(R.string.aj_color_selected),
                            tint = swatch.readableInk(),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        SliderRow(stringResource(R.string.aj_color_hue)) {
            GradientSlider(
                fraction = hue / 360f,
                brush = Brush.horizontalGradient(
                    listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { Color.hsv(it, 1f, 1f) }
                ),
                onChange = { hue = it * 360f; emit() }
            )
        }
        SliderRow(stringResource(R.string.aj_color_saturation)) {
            GradientSlider(
                fraction = sat,
                brush = Brush.horizontalGradient(
                    listOf(Color.hsv(hue, 0f, value), Color.hsv(hue, 1f, value))
                ),
                onChange = { sat = it; emit() }
            )
        }
        SliderRow(stringResource(R.string.aj_color_brightness)) {
            GradientSlider(
                fraction = value,
                brush = Brush.horizontalGradient(
                    listOf(Color.Black, Color.hsv(hue, sat, 1f))
                ),
                onChange = { value = it; emit() }
            )
        }
        Text(
            text = color.toHex(),
            style = MaterialTheme.typography.labelMedium,
            color = AppPalette.InkSoft
        )
    }
}

@Composable
private fun SliderRow(label: String, content: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = AppPalette.InkSoft,
            modifier = Modifier.width(76.dp)
        )
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}

/** Barra con un círculo que se arrastra o se toca para elegir un valor 0–1. */
@Composable
private fun GradientSlider(
    fraction: Float,
    brush: Brush,
    onChange: (Float) -> Unit
) {
    val ring = AppPalette.Ink
    val change by rememberUpdatedState(onChange)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures { change((it.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectDragGestures { pointer, _ ->
                    change((pointer.position.x / size.width).coerceIn(0f, 1f))
                }
            }
    ) {
        val trackHeight = size.height * 0.5f
        val top = (size.height - trackHeight) / 2f
        drawRoundRect(
            brush = brush,
            topLeft = Offset(0f, top),
            size = androidx.compose.ui.geometry.Size(size.width, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2f)
        )
        val x = fraction.coerceIn(0f, 1f) * size.width
        drawCircle(color = Color.White, radius = size.height / 2.4f, center = Offset(x, size.height / 2f))
        drawCircle(
            color = ring,
            radius = size.height / 2.4f,
            center = Offset(x, size.height / 2f),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

/** Espacio pequeño reutilizable entre bloques del editor. */
@Composable
fun VSpace(height: androidx.compose.ui.unit.Dp) = Spacer(Modifier.height(height))
