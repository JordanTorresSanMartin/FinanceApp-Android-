package com.example.financeapp.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.financeapp.ui.screens.analytics.AnalyticsScreen
import com.example.financeapp.ui.screens.auth.LoginScreen
import com.example.financeapp.ui.screens.main.MainScaffold
import com.example.financeapp.ui.screens.transactions.EditTransactionScreen
import com.example.financeapp.ui.screens.transactions.NewTransactionScreen
import com.example.financeapp.ui.screens.upcoming.UpcomingScreen
import com.example.financeapp.ui.viewmodel.auth.AuthViewModel

/** Destinos que se pueden abrir desde fuera (widgets). */
sealed interface LaunchTarget {
    data class NewTransaction(val type: String) : LaunchTarget
    data object Upcoming : LaunchTarget
}

@Composable
fun FinanceNavHost(
    launchTarget: LaunchTarget?,
    onLaunchConsumed: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val loggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()
    // Mientras se lee la sesión guardada no mostramos nada (evita el destello del login).
    val resolved = loggedIn ?: run { Box(Modifier.fillMaxSize()); return }

    val nav = rememberNavController()
    // Se decide una sola vez; sobrevive a rotación. Los cambios de sesión navegan abajo.
    val startInMain = rememberSaveable { resolved }

    // La sesión manda: login, logout o sesión expirada mueven al grafo correcto.
    LaunchedEffect(resolved) {
        val inAuth = nav.currentBackStackEntry?.destination?.hierarchy
            ?.any { it.hasRoute(NavRoute.AuthGraph::class) } == true
        if (resolved && inAuth) {
            nav.navigate(NavRoute.MainGraph) { popUpTo(NavRoute.AuthGraph) { inclusive = true } }
        } else if (!resolved && !inAuth) {
            nav.navigate(NavRoute.AuthGraph) { popUpTo(nav.graph.id) { inclusive = true } }
        }
    }

    LaunchedEffect(launchTarget, resolved) {
        if (!resolved || launchTarget == null) return@LaunchedEffect
        when (launchTarget) {
            is LaunchTarget.NewTransaction -> nav.navigate(NavRoute.NewTransaction(launchTarget.type))
            LaunchTarget.Upcoming -> nav.navigate(NavRoute.Upcoming) { launchSingleTop = true }
        }
        onLaunchConsumed()
    }

    NavHost(
        navController = nav,
        startDestination = if (startInMain) NavRoute.MainGraph else NavRoute.AuthGraph,
        enterTransition = { fadeIn(tween(350)) + slideInHorizontally(tween(350)) { it / 4 } },
        exitTransition = { fadeOut(tween(350)) + slideOutHorizontally(tween(350)) { -it / 4 } },
        popEnterTransition = { fadeIn(tween(350)) },
        popExitTransition = { fadeOut(tween(350)) + slideOutHorizontally(tween(350)) { it / 4 } },
    ) {
        navigation<NavRoute.AuthGraph>(startDestination = NavRoute.Login) {
            composable<NavRoute.Login> { LoginScreen() }
        }

        composable<NavRoute.MainGraph> {
            MainScaffold(
                onNavigateToNewTransaction = { type -> nav.navigate(NavRoute.NewTransaction(type)) },
                onNavigateToEditTransaction = { id -> nav.navigate(NavRoute.EditTransaction(id)) },
                onNavigateToAnalytics = { nav.navigate(NavRoute.Analytics) },
                onNavigateToUpcoming = { nav.navigate(NavRoute.Upcoming) },
            )
        }

        composable<NavRoute.NewTransaction> { entry ->
            val route = entry.toRoute<NavRoute.NewTransaction>()
            NewTransactionScreen(initialType = route.type, onBack = { nav.popBackStack() })
        }
        composable<NavRoute.EditTransaction> { entry ->
            val route = entry.toRoute<NavRoute.EditTransaction>()
            EditTransactionScreen(transactionId = route.id, onBack = { nav.popBackStack() })
        }
        composable<NavRoute.Analytics> { AnalyticsScreen(onBack = { nav.popBackStack() }) }
        composable<NavRoute.Upcoming> { UpcomingScreen(onBack = { nav.popBackStack() }) }
    }
}
