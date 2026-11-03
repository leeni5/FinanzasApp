package com.finanzasapp.ui.screens

import androidx.compose.runtime.Composable
import com.finanzasapp.ui.components.EmptyState
import com.finanzasapp.ui.components.PageScaffold

@Composable
fun AjustesScreen() {
    PageScaffold(
        title = "Ajustes",
        subtitle = "Tus datos, tus reglas."
    ) {
        EmptyState(
            title = "La configuración llega al final",
            message = "Aquí vas a pegar tu clave de Gemini, revisar las reglas que la app " +
                "aprendió, exportar tus datos y ajustar los avisos de pagos. La clave se " +
                "guardará solo en este teléfono."
        )
    }
}
