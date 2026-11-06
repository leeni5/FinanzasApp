package com.finanzasapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.finanzasapp.ui.components.BallNavItem
import com.finanzasapp.ui.components.BallNavigationBar
import com.finanzasapp.ui.navigation.AppNavHost
import com.finanzasapp.ui.navigation.Destination
import com.finanzasapp.ui.navigation.Routes
import androidx.compose.runtime.LaunchedEffect

/**
 * Armazón único: barra inferior flotante y contenido que cambia.
 * Navegar entre pestañas no apila historial: cada pestaña conserva su estado.
 * En pantallas que no son pestaña (pagos, categorías) la barra se esconde.
 */
@Composable
fun FinanzasRoot(
    openRequest: String? = null,
    onOpenHandled: () -> Unit = {}
) {
    val navController = rememberNavController()

    // Una notificación de pago pide abrir la pantalla de pagos.
    LaunchedEffect(openRequest) {
        if (openRequest == Routes.PAGOS) {
            navController.navigate(Routes.PAGOS) { launchSingleTop = true }
            onOpenHandled()
        }
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Destination.INICIO.route
    val tabIndex = Destination.entries.indexOfFirst { it.route == currentRoute }
    val onTab = tabIndex >= 0

    // La bola recuerda la última pestaña aunque se abra una pantalla interna.
    var lastTab by remember { mutableIntStateOf(0) }
    if (onTab) lastTab = tabIndex

    val items = Destination.entries.map {
        BallNavItem(
            label = stringResource(it.labelRes),
            icon = it.icon,
            selectedIcon = it.selectedIcon
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = onTab,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                BallNavigationBar(
                    items = items,
                    selectedIndex = lastTab,
                    onSelect = { index ->
                        val destination = Destination.entries[index]
                        if (destination.route == currentRoute) return@BallNavigationBar
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}
