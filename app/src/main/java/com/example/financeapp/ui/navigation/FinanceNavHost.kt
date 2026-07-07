package com.example.financeapp.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
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

@Composable
fun FinanceNavHost(
    initialNewTxType: String? = null,
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val nav = rememberNavController()

    LaunchedEffect(Unit) {
        if (authViewModel.isUserLoggedIn()) {
            nav.navigate(NavRoute.MainGraph) {
                popUpTo(NavRoute.AuthGraph) { inclusive = true }
            }
            if (initialNewTxType != null) {
                nav.navigate(NavRoute.NewTransaction(initialNewTxType))
            }
        }
    }

    NavHost(
        navController = nav,
        startDestination = NavRoute.AuthGraph,
        enterTransition = { fadeIn(tween(350)) + slideInHorizontally(tween(350)) { it / 4 } },
        exitTransition = { fadeOut(tween(350)) + slideOutHorizontally(tween(350)) { -it / 4 } },
        popEnterTransition = { fadeIn(tween(350)) },
        popExitTransition = { fadeOut(tween(350)) + slideOutHorizontally(tween(350)) { it / 4 } },
    ) {
        navigation<NavRoute.AuthGraph>(startDestination = NavRoute.Login) {
            composable<NavRoute.Login> {
                LoginScreen(
                    onLoginSuccess = {
                        nav.navigate(NavRoute.MainGraph) {
                            popUpTo(NavRoute.AuthGraph) { inclusive = true }
                        }
                    },
                )
            }
        }

        composable<NavRoute.MainGraph> {
            MainScaffold(
                onNavigateToNewTransaction = { type -> nav.navigate(NavRoute.NewTransaction(type)) },
                onNavigateToEditTransaction = { id -> nav.navigate(NavRoute.EditTransaction(id)) },
                onNavigateToAnalytics = { nav.navigate(NavRoute.Analytics) },
                onNavigateToUpcoming = { nav.navigate(NavRoute.Upcoming) },
                onLogout = {
                    nav.navigate(NavRoute.AuthGraph) {
                        popUpTo(NavRoute.MainGraph) { inclusive = true }
                    }
                },
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
