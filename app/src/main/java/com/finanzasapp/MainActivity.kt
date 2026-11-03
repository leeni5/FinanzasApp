package com.finanzasapp

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.finanzasapp.ui.FinanzasRoot
import com.finanzasapp.ui.theme.FinanzasTheme

class MainActivity : ComponentActivity() {

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

        setContent {
            FinanzasTheme {
                FinanzasRoot()
            }
        }
    }
}
