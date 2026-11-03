package com.finanzasapp.domain.dedupe

import java.security.MessageDigest
import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale

/**
 * Huella determinista de un movimiento.
 *
 * Dos veces el mismo extracto produce el mismo hash, así que importar
 * el archivo dos veces no duplica nada.
 *
 * La normalización quita acentos, mayúsculas y signos para que
 * "Éxito  Calle 80" y "exito calle 80" generen la misma huella.
 */
object UniqueHash {

    private val diacritics = Regex("\\p{M}+")
    private val nonAlphanumeric = Regex("[^a-z0-9 ]")
    private val repeatedSpaces = Regex("\\s+")

    fun of(accountId: Long, date: LocalDate, amount: Long, description: String): String {
        val payload = buildString {
            append(accountId)
            append('|')
            append(date.toEpochDay())
            append('|')
            append(amount)
            append('|')
            append(normalize(description))
        }
        return sha256(payload)
    }

    fun normalize(value: String): String {
        val decomposed = Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        return decomposed
            .replace(diacritics, "")
            .replace(nonAlphanumeric, " ")
            .replace(repeatedSpaces, " ")
            .trim()
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
