package com.finanzasapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.Spacing

/** Una pestaña de la barra: icono inactivo, icono activo y texto. */
data class BallNavItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

private val BarHeight = 68.dp
private val BallSize = 44.dp

/**
 * Barra inferior flotante con una "bola" de acento que se desliza hasta la
 * pestaña elegida.
 *
 * El movimiento usa un resorte con poco rebote (0.72) para que llegue con
 * vida pero sin bambolearse. La bola se mueve con `offset` en la fase de
 * colocación: no recompone los iconos mientras viaja.
 */
@Composable
fun BallNavigationBar(
    items: List<BallNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(26.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .cardShadow(shape)
                .clip(shape)
                .background(AppPalette.Surface)
        ) {
            val itemWidth = maxWidth / items.size
            val ballX by animateDpAsState(
                targetValue = itemWidth * selectedIndex + (itemWidth - BallSize) / 2,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
                label = "ballX"
            )

            // La bola, detrás de los iconos.
            Box(
                modifier = Modifier
                    .offset(x = ballX, y = 7.dp)
                    .size(BallSize)
                    .clip(CircleShape)
                    .background(AppPalette.Accent)
            )

            Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                items.forEachIndexed { index, item ->
                    BallNavItemView(
                        item = item,
                        selected = index == selectedIndex,
                        onClick = { onSelect(index) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BallNavItemView(
    item: BallNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor by animateColorAsState(
        targetValue = if (selected) AppPalette.OnAccent else AppPalette.InkFaint,
        animationSpec = tween(180),
        label = "iconColor"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) AppPalette.Ink else AppPalette.InkFaint,
        animationSpec = tween(180),
        label = "labelColor"
    )
    // Pequeño "pop" del icono al quedar elegido.
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "iconScale"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(BallSize),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.icon,
                contentDescription = item.label,
                tint = iconColor,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )
        }
        Spacer(Modifier.height(1.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp, fontSize = 10.sp),
            color = labelColor,
            maxLines = 1,
            softWrap = false
        )
    }
}
