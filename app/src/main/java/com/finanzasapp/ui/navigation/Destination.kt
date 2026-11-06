package com.finanzasapp.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import com.finanzasapp.R

/**
 * Las cinco pestañas de la app, en el orden en que se muestran.
 * Cada una tiene icono de contorno (inactiva) y relleno (activa).
 * La ruta es estable: cambiarla rompe el estado guardado de navegación.
 */
enum class Destination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    INICIO("inicio", R.string.tab_inicio, Icons.Outlined.Home, Icons.Rounded.Home),
    MOVIMIENTOS(
        "movimientos",
        R.string.tab_movimientos,
        Icons.AutoMirrored.Outlined.ReceiptLong,
        Icons.AutoMirrored.Rounded.ReceiptLong
    ),
    PRESUPUESTOS("presupuestos", R.string.tab_presupuestos, Icons.Outlined.DonutLarge, Icons.Rounded.DonutLarge),
    METAS("metas", R.string.tab_metas, Icons.Outlined.Savings, Icons.Rounded.Savings),
    AJUSTES("ajustes", R.string.tab_ajustes, Icons.Outlined.Tune, Icons.Rounded.Tune)
}
