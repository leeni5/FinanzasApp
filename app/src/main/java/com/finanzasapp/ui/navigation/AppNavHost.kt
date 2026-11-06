package com.finanzasapp.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.finanzasapp.ui.screens.AjustesScreen
import com.finanzasapp.ui.screens.inicio.InicioScreen
import com.finanzasapp.ui.screens.metas.MetasScreen
import com.finanzasapp.ui.screens.movimientos.MovimientosScreen
import com.finanzasapp.ui.screens.presupuestos.PresupuestosScreen

/** Duraciones de la transición entre pestañas, en milisegundos. */
private const val ENTER_MS = 220
private const val EXIT_MS = 90

/**
 * Cambio de pestaña rápido: la anterior se desvanece en 90 ms y la nueva
 * entra en 220 ms subiendo apenas unos píxeles. El fundido por defecto de
 * Navigation dura 700 ms y cruza ambas pantallas, que se siente lento.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Destination.INICIO.route,
        modifier = modifier,
        enterTransition = {
            fadeIn(tween(ENTER_MS, delayMillis = EXIT_MS / 2, easing = LinearEasing)) +
                slideInVertically(tween(ENTER_MS, easing = FastOutSlowInEasing)) { it / 40 }
        },
        exitTransition = { fadeOut(tween(EXIT_MS, easing = LinearEasing)) },
        popEnterTransition = {
            fadeIn(tween(ENTER_MS, delayMillis = EXIT_MS / 2, easing = LinearEasing))
        },
        popExitTransition = { fadeOut(tween(EXIT_MS, easing = LinearEasing)) }
    ) {
        composable(Destination.INICIO.route) { InicioScreen() }
        composable(Destination.MOVIMIENTOS.route) { MovimientosScreen() }
        composable(Destination.PRESUPUESTOS.route) { PresupuestosScreen() }
        composable(Destination.METAS.route) { MetasScreen() }
        composable(Destination.AJUSTES.route) { AjustesScreen() }
    }
}
