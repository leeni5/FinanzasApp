package com.finanzasapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.finanzasapp.ui.FinanzasRoot
import com.finanzasapp.ui.theme.FinanzasTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            FinanzasTheme {
                FinanzasRoot()
            }
        }
    }
}
