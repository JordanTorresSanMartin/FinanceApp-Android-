package com.example.financeapp.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.financeapp.ui.screens.auth.LoginScreen
import com.example.financeapp.ui.screens.main.MainScaffold

import androidx.hilt.navigation.compose.hiltViewModel
import com.example.financeapp.ui.viewmodel.auth.AuthViewModel

@Composable
fun FinanceNavHost(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val rootNavController = rememberNavController()

    // 1. Redirección automática si existe sesión activa
    LaunchedEffect(Unit) {
        if (authViewModel.isUserLoggedIn()) {
            rootNavController.navigate(NavRoute.MainGraph) {
                popUpTo(NavRoute.AuthGraph) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = rootNavController,
        startDestination = NavRoute.AuthGraph,
        enterTransition = { fadeIn(animationSpec = tween(400)) + slideInHorizontally(animationSpec = tween(400)) },
        exitTransition = { fadeOut(animationSpec = tween(400)) + slideOutHorizontally(animationSpec = tween(400)) }
    ) {
        // Grafo de Autenticación
        navigation<NavRoute.AuthGraph>(startDestination = NavRoute.Login) {
            composable<NavRoute.Login> {
                LoginScreen(
                    onLoginSuccess = {
                        rootNavController.navigate(NavRoute.MainGraph) {
                            popUpTo(NavRoute.AuthGraph) { inclusive = true }
                        }
                    }
                )
            }
        }

        // Grafo Principal (Encapsulado en el Scaffold)
        composable<NavRoute.MainGraph> {
            MainScaffold(
                onNavigateToNewTransaction = {
                    rootNavController.navigate(NavRoute.NewTransaction)
                },
                onNavigateToArchitecture = {
                    rootNavController.navigate(NavRoute.ArchitectureOverview)
                },
                onLogout = {
                    rootNavController.navigate(NavRoute.AuthGraph) {
                        popUpTo(NavRoute.MainGraph) { inclusive = true }
                    }
                }
            )
        }

        // Pantallas fuera del Scaffold principal (Full Screen Modals)
        composable<NavRoute.NewTransaction> {
            com.example.financeapp.ui.screens.transactions.NewTransactionScreen(
                onBack = { rootNavController.popBackStack() }
            )
        }

        composable<NavRoute.ArchitectureOverview> {
            com.example.financeapp.ui.screens.profile.ArchitectureOverviewScreen(
                onBack = { rootNavController.popBackStack() }
            )
        }
    }
}
