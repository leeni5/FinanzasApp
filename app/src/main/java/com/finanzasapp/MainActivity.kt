package com.finanzasapp

import android.Manifest
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finanzasapp.data.settings.ThemeMode
import com.finanzasapp.notifications.PaymentReminderScheduler
import com.finanzasapp.ui.FinanzasRoot
import com.finanzasapp.ui.theme.FinanzasTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /** Pantalla que pidió abrir una notificación ("pagos"), si la hay. */
    private var openRequest by mutableStateOf<String?>(null)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // Si lo concede, los avisos pendientes se programan de nuevo; si no,
            // la app sigue igual y Ajustes muestra cómo activarlos.
            if (granted) {
                val app = application as FinanzasApp
                app.applicationScope.launch {
                    PaymentReminderScheduler.rescheduleAll(app, app.database)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // En la versión final, la pantalla de finanzas no aparece en la vista
        // reciente ni se puede capturar. En depuración lo dejamos libre para
        // poder revisar el diseño.
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!isDebuggable) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        openRequest = intent?.getStringExtra(EXTRA_OPEN)
        askNotificationPermissionOnce(savedInstanceState)

        val settings = (application as FinanzasApp).settings
        setContent {
            val mode by settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.OSCURO)
            val dark = mode == ThemeMode.OSCURO ||
                (mode == ThemeMode.SISTEMA && isSystemInDarkTheme())

            // Barras del sistema con iconos claros u oscuros según el tema.
            LaunchedEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    )
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            FinanzasTheme(mode = mode) {
                FinanzasRoot(
                    openRequest = openRequest,
                    onOpenHandled = { openRequest = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_OPEN)?.let { openRequest = it }
    }

    /**
     * En Android 13+ los avisos de pago necesitan permiso. Se pide una vez al
     * abrir la app (no al rotar); si se niega, no se vuelve a insistir aquí.
     */
    private fun askNotificationPermissionOnce(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (PaymentReminderScheduler.canNotify(this)) return
        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) return
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    companion object {
        const val EXTRA_OPEN = "abrir"
        const val OPEN_PAYMENTS = "pagos"
    }
}
