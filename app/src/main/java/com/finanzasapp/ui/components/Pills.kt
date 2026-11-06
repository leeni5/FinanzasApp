package com.finanzasapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.finanzasapp.data.entity.Category
import com.finanzasapp.ui.theme.asColor
import com.finanzasapp.ui.theme.readableInk
import com.finanzasapp.ui.theme.soft

/** Etiqueta pequeña de color: "Hoy", "42%", "COP". */
@Composable
fun SoftPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    background: Color = color.soft()
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(background)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

/** Icono dentro de un círculo tintado (bloques de Ajustes). */
@Composable
fun IconBubble(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    background: Color = tint.soft(),
    size: Dp = 44.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

/**
 * Ficha de categoría: cuadro redondeado lleno con el color de la categoría y
 * su icono encima, en el color de icono elegido (o blanco/negro automático).
 */
@Composable
fun CategoryBadge(
    categoryName: String?,
    color: Color,
    modifier: Modifier = Modifier,
    iconKey: String? = null,
    iconColor: Color? = null,
    size: Dp = 40.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = IconCatalog.forCategory(categoryName, iconKey),
            contentDescription = null,
            tint = iconColor ?: color.readableInk(),
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

/** Ficha de una categoría guardada, con su color e icono. */
@Composable
fun CategoryBadge(category: Category, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    CategoryBadge(
        categoryName = category.name,
        color = category.color.asColor(),
        iconKey = category.icon,
        iconColor = category.iconColor.takeIf { it.isNotBlank() }?.asColor(),
        modifier = modifier,
        size = size
    )
}

/** Punto de color para listas de categorías. */
@Composable
fun ColorDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color, CircleShape)
    )
}
