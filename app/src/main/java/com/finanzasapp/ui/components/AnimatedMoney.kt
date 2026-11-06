package com.finanzasapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.finanzasapp.ui.util.Money

/**
 * Monto en pesos que cuenta hasta su valor al aparecer o al cambiar.
 *
 * La interpolación se hace en `Long` (no en `Float`, que pierde precisión por
 * encima de ~16 millones). Las figuras tabulares del tema evitan que el texto
 * "baile" mientras cuenta.
 */
@Composable
fun AnimatedMoney(
    amount: Long,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    durationMillis: Int = 850
) {
    // rememberSaveable: si la cifra sale de la pantalla en una lista y vuelve,
    // recuerda lo que ya mostró y no vuelve a contar desde cero. Contar de
    // nuevo en cada scroll recomponía el texto en cada cuadro (medido: el
    // doble de cuadros con tirones).
    val shown = rememberSaveable { mutableLongStateOf(0L) }

    LaunchedEffect(amount) {
        val from = shown.longValue
        if (from == amount) return@LaunchedEffect
        val progress = Animatable(0f)
        progress.animateTo(1f, tween(durationMillis, easing = EaseOutQuint)) {
            shown.longValue = from + ((amount - from) * value.toDouble()).toLong()
        }
        shown.longValue = amount
    }

    Text(
        text = Money.cop(shown.longValue),
        style = style,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}
