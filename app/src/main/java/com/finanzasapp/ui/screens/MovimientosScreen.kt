package com.finanzasapp.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.Composable
import com.finanzasapp.ui.components.EmptyState
import com.finanzasapp.ui.components.PageScaffold
import com.finanzasapp.ui.theme.AppPalette

@Composable
fun MovimientosScreen() {
    PageScaffold(
        title = "Movimientos",
        subtitle = "Ingresos y gastos, en orden.",
        eyebrow = "Fase 1",
        accent = AppPalette.Sky
    ) {
        EmptyState(
            title = "La lista llega en la fase 1",
            message = "Aquí vas a registrar un gasto en tres toques, corregir la categoría, " +
                "buscar por texto y filtrar por mes. La tabla, los índices y la regla de " +
                "duplicados ya están creados; falta la pantalla.",
            icon = Icons.AutoMirrored.Filled.List,
            accent = AppPalette.Sky
        )
    }
}
