package com.finanzasapp.ui.screens

import androidx.compose.runtime.Composable
import com.finanzasapp.ui.components.EmptyState
import com.finanzasapp.ui.components.PageScaffold

@Composable
fun MetasScreen() {
    PageScaffold(
        title = "Metas",
        subtitle = "Ahorrar también es parte del plan."
    ) {
        EmptyState(
            title = "El seguimiento llega en la fase 3",
            message = "Una meta no guarda un \"monto actual\" escrito a mano: se calcula sumando " +
                "sus aportes. Así el avance y el ritmo de los últimos tres meses nunca mienten. " +
                "La estructura ya está lista."
        )
    }
}
