package com.example.financeapp.ui.screens.main

import androidx.compose.material.icons.filled.CalendarMonth
import com.example.financeapp.ui.screens.analysis.AnalysisHub
import com.example.financeapp.ui.screens.upcoming.UpcomingScreen

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.financeapp.ui.navigation.NavRoute
import com.example.financeapp.ui.screens.budgets.BudgetsScreen
import com.example.financeapp.ui.screens.dashboard.DashboardScreen
import com.example.financeapp.ui.screens.profile.ProfileScreen
import com.example.financeapp.ui.screens.summary.AnnualSummaryScreen
import com.example.financeapp.ui.screens.transactions.TransactionsScreen
import com.example.financeapp.util.LocalAppHaptics

private data class BottomNavItem(val label: String, val icon: ImageVector, val route: NavRoute)

@Composable
fun MainScaffold(
    onNavigateToNewTransaction: (String) -> Unit,
    onNavigateToEditTransaction: (String) -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToUpcoming: () -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val haptics = LocalAppHaptics.current

    val items = listOf(
        BottomNavItem("Inicio", Icons.Filled.Dashboard, NavRoute.Dashboard),
        BottomNavItem("Movimientos", Icons.AutoMirrored.Filled.ListAlt, NavRoute.Transactions),
        BottomNavItem("Pagos", Icons.Filled.CalendarMonth, NavRoute.Payments),
        BottomNavItem("Análisis", Icons.Filled.PieChart, NavRoute.AnalysisHub),
        BottomNavItem("Perfil", Icons.Filled.AccountCircle, NavRoute.Profile),
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                items.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.hasRoute(item.route::class) } == true
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = {
                            Text(
                                item.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        selected = selected,
                        onClick = { haptics?.selection(); navController.navigateToTab(item.route) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoute.Dashboard,
            modifier = Modifier.padding(innerPadding),
            // Material "fade through" para pestañas: sale rápido, entra con fundido y leve escala.
            enterTransition = { fadeIn(tween(220, delayMillis = 90)) + scaleIn(tween(220, delayMillis = 90), initialScale = 0.96f) },
            exitTransition = { fadeOut(tween(90)) },
            popEnterTransition = { fadeIn(tween(220, delayMillis = 90)) + scaleIn(tween(220, delayMillis = 90), initialScale = 0.96f) },
            popExitTransition = { fadeOut(tween(90)) },
        ) {
            composable<NavRoute.Dashboard> {
                DashboardScreen(
                    onNavigateToNewTransaction = onNavigateToNewTransaction,
                    // Ahora son pestañas: se cambia de pestaña en vez de abrir una pantalla encima.
                    onNavigateToAnalytics = { navController.navigateToTab(NavRoute.AnalysisHub) },
                    onNavigateToUpcoming = { navController.navigateToTab(NavRoute.Payments) },
                )
            }
            composable<NavRoute.Transactions> {
                TransactionsScreen(
                    onNavigateToNewTransaction = { onNavigateToNewTransaction("gasto") },
                    onNavigateToEditTransaction = onNavigateToEditTransaction,
                )
            }
            composable<NavRoute.Payments> { UpcomingScreen(onBack = {}, showBack = false) }
            composable<NavRoute.AnalysisHub> { AnalysisHub() }
            composable<NavRoute.Profile> { ProfileScreen() }
        }
    }
}

/** Cambiar de pestaña conservando el estado de cada una (scroll, mes elegido…). */
private fun androidx.navigation.NavController.navigateToTab(route: NavRoute) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
