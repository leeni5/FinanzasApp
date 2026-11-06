package com.finanzasapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingCart
import com.finanzasapp.ui.theme.soft
import java.text.Normalizer
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

/** Icono dentro de un círculo tintado. Da color sin recargar la pantalla. */
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
 * Icono de la categoría dentro de un círculo de su color.
 * Reemplaza a la inicial: una letra no dice si es la categoría o el comercio.
 */
@Composable
fun CategoryBadge(
    categoryName: String?,
    color: Color,
    modifier: Modifier = Modifier,
    income: Boolean = false,
    size: Dp = 40.dp
) {
    IconBubble(
        icon = if (income) Icons.Filled.KeyboardArrowDown else categoryIcon(categoryName),
        tint = color,
        modifier = modifier,
        size = size
    )
}

/**
 * Elige un icono por palabras clave del nombre, sin acentos ni mayúsculas.
 * La tabla `categories` tiene columna `icon`, pero hoy siempre llega vacía
 * (semilla e importación no la llenan), así que se deduce del nombre.
 */
fun categoryIcon(name: String?): ImageVector {
    val key = Normalizer.normalize(name.orEmpty(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()
    fun has(vararg words: String) = words.any { key.contains(it) }
    return when {
        key.isBlank() -> Icons.Filled.List
        has("arriendo", "alquiler", "vivienda", "hogar", "casa") -> Icons.Filled.Home
        has("mercado", "super", "compra") -> Icons.Filled.ShoppingCart
        has("celular", "telefon", "plan movil") -> Icons.Filled.Phone
        has("servicio", "luz", "agua", "gas", "internet", "energia") -> Icons.Filled.Build
        has("transporte", "uber", "taxi", "gasolina", "bus", "carro", "moto") -> Icons.Filled.LocationOn
        has("restaurante", "comida", "cafe", "domicilio") -> Icons.Filled.Favorite
        has("salud", "farmacia", "medic", "eps", "droguer") -> Icons.Filled.AddCircle
        has("ocio", "entreten", "cine", "streaming", "suscrip") -> Icons.Filled.PlayArrow
        has("educa", "curso", "estudio", "colegio", "universidad") -> Icons.Filled.Edit
        has("tarjeta", "credito", "deuda", "banco", "prestamo") -> Icons.Filled.AccountBox
        has("ingreso", "salario", "sueldo", "nomina", "honorario") -> Icons.Filled.KeyboardArrowDown
        else -> Icons.Filled.List
    }
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
