package com.finanzasapp.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.finanzasapp.R

/**
 * Las cinco pestañas de la app, en el orden en que se muestran.
 * La ruta es estable: cambiarla rompe el estado guardado de navegación.
 */
enum class Destination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector
) {
    INICIO("inicio", R.string.tab_inicio, Icons.Filled.Home),
    MOVIMIENTOS("movimientos", R.string.tab_movimientos, Icons.AutoMirrored.Filled.List),
    PRESUPUESTOS("presupuestos", R.string.tab_presupuestos, Icons.Filled.DateRange),
    METAS("metas", R.string.tab_metas, Icons.Filled.Star),
    AJUSTES("ajustes", R.string.tab_ajustes, Icons.Filled.Settings)
}
