package com.finanzasapp.data.security

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.os.SystemClock

/**
 * Bloqueo de la app con huella (o rostro), con el PIN, patrón o contraseña del
 * teléfono como alternativa. Usa el BiometricPrompt del sistema: la huella
 * nunca pasa por la app, Android solo responde «sí» o «no».
 *
 * Se pide al abrir la app y al volver a ella tras [GRACE_MS] en segundo plano,
 * para que elegir un archivo o responder un mensaje no obligue a desbloquear.
 */
class AppLock(context: Context) {

    private val prefs = context.getSharedPreferences("finanzas_seguridad", Context.MODE_PRIVATE)
    private var backgroundSince = 0L

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        }

    fun onBackground() {
        backgroundSince = SystemClock.elapsedRealtime()
    }

    /** ¿Hay que pedir la huella al volver? */
    fun shouldLockOnReturn(): Boolean =
        enabled && backgroundSince != 0L && SystemClock.elapsedRealtime() - backgroundSince > GRACE_MS

    fun markUnlocked() {
        backgroundSince = 0L
    }

    /**
     * Pide la huella. [onResult] recibe true si se desbloqueó; con false y un
     * mensaje si se canceló o falló.
     */
    fun authenticate(activity: Activity, title: String, onResult: (Boolean, String?) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            // Android 8: solo el bloqueo de pantalla del sistema.
            onResult(false, "Tu versión de Android no permite el bloqueo con huella en la app.")
            return
        }
        val builder = BiometricPrompt.Builder(activity)
            .setTitle(title)
            .setSubtitle("Usa tu huella o el bloqueo de tu teléfono")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(AUTHENTICATORS)
        } else {
            @Suppress("DEPRECATION")
            builder.setDeviceCredentialAllowed(true)
        }
        runCatching {
        builder.build().authenticate(
            CancellationSignal(),
            activity.mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    markUnlocked()
                    onResult(true, null)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onResult(false, errString.toString())
                }
            }
        )
        }.onFailure { onResult(false, "No se pudo abrir la huella: ${it.message ?: "error del sistema"}.") }
    }

    /** null si se puede usar; si no, el motivo para mostrarlo. */
    fun unavailableReason(context: Context): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return "Tu versión de Android no permite el bloqueo con huella en la app."
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val manager = context.getSystemService(BiometricManager::class.java)
            val status = runCatching { manager.canAuthenticate(AUTHENTICATORS) }
                .getOrElse { return "No se pudo consultar la huella: ${it.message ?: "error del sistema"}." }
            return when (status) {
                BiometricManager.BIOMETRIC_SUCCESS -> null
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                    "Primero registra una huella o un bloqueo de pantalla en los ajustes del teléfono."
                else -> "Este teléfono no tiene huella ni bloqueo de pantalla disponible."
            }
        }
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        return if (keyguard.isDeviceSecure) null
        else "Primero configura un bloqueo de pantalla en los ajustes del teléfono."
    }

    private companion object {
        const val KEY_ENABLED = "bloqueo_huella"
        const val GRACE_MS = 30_000L
        val AUTHENTICATORS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            0
        }
    }
}
