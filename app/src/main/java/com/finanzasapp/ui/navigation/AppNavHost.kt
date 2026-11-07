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
import com.finanzasapp.ui.screens.categorias.CategoriasScreen
import com.finanzasapp.ui.screens.cuentas.CuentaDetalleScreen
import com.finanzasapp.ui.screens.cuentas.CuentasScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.finanzasapp.ui.screens.pagos.PagosScreen
import com.finanzasapp.ui.screens.inicio.InicioScreen
import com.finanzasapp.ui.screens.metas.MetasScreen
import com.finanzasapp.ui.screens.movimientos.MovimientosScreen
import com.finanzasapp.ui.screens.presupuestos.PresupuestosScreen
import com.finanzasapp.ui.screens.revision.RevisionScreen
import com.finanzasapp.ui.screens.chat.ChatScreen

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
        composable(Destination.INICIO.route) {
            InicioScreen(
                onOpenPayments = { navController.navigate(Routes.PAGOS) },
                onOpenAccounts = { navController.navigate(Routes.CUENTAS) },
                onOpenAccount = { id -> navController.navigate(Routes.cuenta(id)) }
            )
        }
        composable(Destination.MOVIMIENTOS.route) {
            MovimientosScreen(onOpenChat = { navController.navigate(Routes.CHAT) })
        }
        composable(Destination.PRESUPUESTOS.route) { PresupuestosScreen() }
        composable(Destination.METAS.route) { MetasScreen() }
        composable(Destination.AJUSTES.route) {
            AjustesScreen(
                onOpenCategories = { navController.navigate(Routes.CATEGORIAS) },
                onOpenPayments = { navController.navigate(Routes.PAGOS) },
                onOpenAccounts = { navController.navigate(Routes.CUENTAS) },
                onOpenReview = { navController.navigate(Routes.REVISION) },
                onOpenChat = { navController.navigate(Routes.CHAT) }
            )
        }
        composable(Routes.CHAT) {
            ChatScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.REVISION) {
            RevisionScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CUENTAS) {
            CuentasScreen(
                onBack = { navController.popBackStack() },
                onOpenAccount = { id -> navController.navigate(Routes.cuenta(id)) }
            )
        }
        composable(
            route = Routes.CUENTA,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            CuentaDetalleScreen(
                accountId = entry.arguments?.getLong("id") ?: 0L,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.CATEGORIAS) {
            CategoriasScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PAGOS) {
            PagosScreen(onBack = { navController.popBackStack() })
        }
    }
}

/** Pantallas que no son pestaña: se abren encima y esconden la barra. */
object Routes {
    const val CATEGORIAS = "categorias"
    const val PAGOS = "pagos"
    const val CUENTAS = "cuentas"
    const val REVISION = "revision"
    const val CHAT = "chat"
    const val CUENTA = "cuenta/{id}"
    fun cuenta(id: Long) = "cuenta/$id"
}
