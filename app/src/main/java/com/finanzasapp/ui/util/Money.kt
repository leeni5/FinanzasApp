package com.finanzasapp.ui.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * Formato de dinero colombiano: pesos sin centavos y separador de miles.
 * Se calcula siempre a partir de un Long, nunca de un Double: un peso
 * perdido por redondeo es un peso que no cuadra.
 */
object Money {

    private val locale = Locale.forLanguageTag("es-CO")
    private val symbols = DecimalFormatSymbols(locale)
    private val thousands = DecimalFormat("#,##0", symbols)

    /** "$ 1.234.567" o "-$ 45.000". */
    fun cop(amount: Long): String = synchronized(thousands) {
        val prefix = if (amount < 0) "-\$ " else "\$ "
        prefix + thousands.format(abs(amount))
    }

    /** "1.234.567" sin símbolo, para textos donde "$" ya está en la etiqueta. */
    fun number(amount: Long): String = synchronized(thousands) {
        thousands.format(abs(amount))
    }

    /** "42%" a partir de una fracción. */
    fun percent(fraction: Float): String = "${(fraction * 100).toInt()}%"
}
