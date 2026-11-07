package com.finanzasapp.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.finanzasapp.R
import androidx.compose.ui.res.stringResource
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.Spacing
import com.finanzasapp.ui.theme.soft

/** Una opción del botón desplegable. */
data class SpeedDialAction(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

/**
 * Botón «+» que, al tocarlo, despliega sus opciones encima (por ejemplo Gasto
 * e Ingreso) y oscurece el fondo. El «+» gira 45° hasta formar una «×».
 * Tocar fuera, la «×» o el botón atrás lo cierra.
 *
 * Va dentro de un Box a pantalla completa: ocupa todo para poder oscurecer
 * el fondo y se alinea abajo a la derecha.
 */
@Composable
fun SpeedDialFab(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    visible: Boolean,
    actions: List<SpeedDialAction>,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = expanded) { onExpandedChange(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // Fondo oscurecido mientras está abierto.
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(140))
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(AppPalette.Background.copy(alpha = 0.82f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onExpandedChange(false) }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Spacing.page, bottom = Spacing.lg),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Opciones: entran de abajo hacia arriba, una tras otra.
            actions.forEachIndexed { index, action ->
                val order = actions.size - 1 - index
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(tween(160, delayMillis = order * 40)) +
                        slideInVertically(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)) { it / 2 } +
                        scaleIn(initialScale = 0.85f, animationSpec = tween(180, delayMillis = order * 40)),
                    exit = fadeOut(tween(100)) + scaleOut(targetScale = 0.9f, animationSpec = tween(100))
                ) {
                    SpeedDialOption(action) {
                        onExpandedChange(false)
                        action.onClick()
                    }
                }
            }

            AnimatedVisibility(
                visible = visible || expanded,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                val rotation by animateFloatAsState(
                    targetValue = if (expanded) 45f else 0f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
                    label = "fabRotation"
                )
                FloatingActionButton(
                    onClick = { onExpandedChange(!expanded) },
                    shape = MaterialTheme.shapes.large,
                    containerColor = AppPalette.Accent,
                    contentColor = AppPalette.OnAccent
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = if (expanded) stringResource(R.string.aj_fab_close) else stringResource(R.string.aj_fab_add),
                        modifier = Modifier
                            .size(26.dp)
                            .graphicsLayer { rotationZ = rotation }
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedDialOption(action: SpeedDialAction, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = AppPalette.SurfaceStrong
    ) {
        Row(
            modifier = Modifier.padding(start = Spacing.md, end = Spacing.xl, top = Spacing.md, bottom = Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(action.color.soft()),
                contentAlignment = Alignment.Center
            ) {
                Icon(action.icon, null, tint = action.color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(Spacing.md))
            Text(action.label, style = MaterialTheme.typography.titleMedium, color = AppPalette.Ink)
        }
    }
}
