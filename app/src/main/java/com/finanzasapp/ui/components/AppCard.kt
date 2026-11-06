package com.finanzasapp.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.Spacing

/**
 * Sombra de las tarjetas: baja y difusa. En claro va teñida del acento; en
 * oscuro es negra, porque una sombra de color sobre fondo negro no se ve.
 */
@Composable
fun Modifier.cardShadow(shape: Shape = RoundedCornerShape(16.dp)): Modifier {
    val tint = if (AppPalette.IsDark) Color.Black else AppPalette.AccentDeep
    return shadow(
        elevation = 6.dp,
        shape = shape,
        clip = false,
        ambientColor = tint,
        spotColor = tint
    )
}

/**
 * Escala al presionar: la tarjeta cede un poco y vuelve con un resorte.
 * Se aplica en la capa gráfica, así no recompone ni mide de nuevo.
 */
@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Contenedor base: superficie blanca, esquinas de 16 dp y, por defecto,
 * sombra suave. Si es tocable, responde con una leve escala al presionar.
 *
 * [elevated] en false cambia la sombra por un borde fino: es lo que usan las
 * tarjetas que se repiten dentro de una lista con scroll. Medido en el
 * teléfono de prueba, una sombra por fila duplicaba los cuadros con tirones.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentPadding: PaddingValues = PaddingValues(Spacing.lg),
    elevated: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.large
    val depth = if (elevated) Modifier.cardShadow(shape) else Modifier
    val border = if (elevated) null else BorderStroke(1.dp, AppPalette.Border)

    if (onClick != null) {
        val interaction = remember { MutableInteractionSource() }
        Surface(
            onClick = onClick,
            interactionSource = interaction,
            modifier = modifier
                .fillMaxWidth()
                .pressScale(interaction)
                .then(depth),
            shape = shape,
            color = containerColor,
            border = border
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    } else {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .then(depth),
            shape = shape,
            color = containerColor,
            border = border
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    }
}

/**
 * Tarjeta protagonista de saldo: índigo plano, sombra y dos círculos sólidos
 * en la esquina. Da profundidad sin degradados ni transparencias.
 */
@Composable
fun HeroSurface(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Spacing.xl),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.extraLarge
    val decorFar = AppPalette.HeroDecorFar
    val decorNear = AppPalette.HeroDecorNear
    Box(
        modifier = modifier
            .fillMaxWidth()
            .cardShadow(shape)
            .clip(shape)
            .background(AppPalette.Hero)
            .drawBehind {
                drawCircle(
                    color = decorFar,
                    radius = size.width * 0.42f,
                    center = Offset(size.width * 0.98f, -size.height * 0.10f)
                )
                drawCircle(
                    color = decorNear,
                    radius = size.width * 0.20f,
                    center = Offset(size.width * 0.92f, size.height * 1.02f)
                )
            }
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** Tarjeta de color suave, sin borde ni sombra, para cifras y avisos. */
@Composable
fun TintedCard(
    modifier: Modifier = Modifier,
    color: Color,
    contentPadding: PaddingValues = PaddingValues(Spacing.md),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = color
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}
