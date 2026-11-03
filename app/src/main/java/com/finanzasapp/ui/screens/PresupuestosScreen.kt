package com.finanzasapp.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import com.finanzasapp.ui.components.EmptyState
import com.finanzasapp.ui.components.PageScaffold
import com.finanzasapp.ui.theme.AppPalette

@Composable
fun PresupuestosScreen() {
    PageScaffold(
        title = "Presupuestos",
        subtitle = "Cuánto puedes gastar sin romper el mes.",
        eyebrow = "Fase 2",
        accent = AppPalette.Amber
    ) {
        EmptyState(
            title = "Los límites se activan en la fase 2",
            message = "Cada categoría ya tiene su presupuesto mensual guardado. En la fase 2 " +
                "esta pantalla muestra lo gastado, lo que queda y el aviso cuando el ritmo " +
                "de gasto pone el mes en riesgo.",
            icon = Icons.Filled.DateRange,
            accent = AppPalette.Amber
        )
    }
}
