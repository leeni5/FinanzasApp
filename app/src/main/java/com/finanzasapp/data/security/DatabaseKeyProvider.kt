package com.finanzasapp.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Crea y guarda la frase que cifra la base de datos.
 *
 * El proceso es deliberadamente indirecto:
 *  1. Se generan 32 bytes aleatorios (la frase de SQLCipher).
 *  2. Esos bytes se cifran con AES-256/GCM usando una llave que vive en el
 *     Android Keystore y que nunca se puede exportar.
 *  3. Solo el resultado cifrado se guarda en las preferencias privadas.
 *
 * Así, aunque alguien copie la carpeta de la app, no obtiene ni la base
 * legible ni la frase: la llave está respaldada por el hardware del teléfono.
 */
object DatabaseKeyProvider {

    private const val PREFS = "finanzas_seguridad"
    private const val PREF_PASSPHRASE = "db_passphrase_v1"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "finanzas_db_key_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private const val PASSPHRASE_BYTES = 32

    /**
     * Devuelve la misma frase en cada arranque. Si es la primera vez,
     * genera una nueva y la deja envuelta con la llave del Keystore.
     */
    fun getOrCreatePassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val wrapped = prefs.getString(PREF_PASSPHRASE, null)
        if (wrapped != null) return unwrap(wrapped)

        val passphrase = ByteArray(PASSPHRASE_BYTES).also {
            SecureRandom().nextBytes(it)
        }
        val saved = prefs.edit()
            .putString(PREF_PASSPHRASE, wrap(passphrase))
            .commit()

        check(saved) { "No se pudo guardar la clave cifrada de la base de datos." }
        return passphrase
    }

    private fun wrap(passphrase: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val payload = cipher.iv + cipher.doFinal(passphrase)
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun unwrap(wrapped: String): ByteArray {
        val payload = Base64.decode(wrapped, Base64.NO_WRAP)
        require(payload.size > IV_BYTES) { "La clave guardada está incompleta." }

        val iv = payload.copyOfRange(0, IV_BYTES)
        val encrypted = payload.copyOfRange(IV_BYTES, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(TAG_BITS, iv)
        )
        return cipher.doFinal(encrypted)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
