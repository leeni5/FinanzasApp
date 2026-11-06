package com.finanzasapp.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Muestra un monto con puntos de miles mientras se escribe (1.234.567),
 * sin cambiar el texto real: el campo sigue guardando solo dígitos.
 *
 * El mapa de posiciones mantiene el cursor en el lugar correcto aunque se
 * inserten los puntos.
 */
object ThousandsTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        if (digits.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val out = StringBuilder()
        // originalToTransformed[i] = posición en el texto con puntos del dígito i.
        val originalToTransformed = IntArray(digits.length + 1)
        digits.forEachIndexed { i, ch ->
            val remaining = digits.length - i
            if (i > 0 && remaining % 3 == 0) out.append('.')
            originalToTransformed[i] = out.length
            out.append(ch)
        }
        originalToTransformed[digits.length] = out.length

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                originalToTransformed[offset.coerceIn(0, digits.length)]

            override fun transformedToOriginal(offset: Int): Int {
                val target = offset.coerceIn(0, out.length)
                // El dígito original más cercano por la izquierda.
                var index = 0
                while (index < digits.length && originalToTransformed[index] < target) index++
                return index
            }
        }
        return TransformedText(AnnotatedString(out.toString()), mapping)
    }
}
