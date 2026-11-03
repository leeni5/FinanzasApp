package com.finanzasapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.finanzasapp.ui.screens.AjustesScreen
import com.finanzasapp.ui.screens.MetasScreen
import com.finanzasapp.ui.screens.PresupuestosScreen
import com.finanzasapp.ui.screens.inicio.InicioScreen
import com.finanzasapp.ui.screens.movimientos.MovimientosScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Destination.INICIO.route,
        modifier = modifier
    ) {
        composable(Destination.INICIO.route) { InicioScreen() }
        composable(Destination.MOVIMIENTOS.route) { MovimientosScreen() }
        composable(Destination.PRESUPUESTOS.route) { PresupuestosScreen() }
        composable(Destination.METAS.route) { MetasScreen() }
        composable(Destination.AJUSTES.route) { AjustesScreen() }
    }
}
