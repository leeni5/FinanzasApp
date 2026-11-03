package com.finanzasapp.data.model

import com.finanzasapp.data.entity.Transaction

/**
 * Texto que se muestra al usuario.
 *
 * [Transaction.descriptionOriginal] nunca se sobrescribe: es la huella de lo
 * que llegó del banco o de lo que se escribió al crear el movimiento. Si el
 * usuario lo corrige, el texto nuevo vive en [Transaction.merchant].
 */
fun Transaction.displayName(): String =
    merchant?.takeIf { it.isNotBlank() } ?: descriptionOriginal
