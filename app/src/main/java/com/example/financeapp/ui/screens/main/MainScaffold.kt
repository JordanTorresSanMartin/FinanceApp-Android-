package com.example.financeapp.ui.screens.main

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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
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

private data class BottomNavItem(val label: String, val icon: ImageVector, val route: NavRoute)

@Composable
fun MainScaffold(
    onNavigateToNewTransaction: (String) -> Unit,
    onNavigateToEditTransaction: (String) -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToUpcoming: () -> Unit,
    onLogout: () -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val haptic = LocalHapticFeedback.current

    val items = listOf(
        BottomNavItem("Dashboard", Icons.Filled.Dashboard, NavRoute.Dashboard),
        BottomNavItem("Transacciones", Icons.AutoMirrored.Filled.ListAlt, NavRoute.Transactions),
        BottomNavItem("Presupuesto", Icons.Filled.PieChart, NavRoute.Budgets),
        BottomNavItem("Resumen", Icons.Filled.BarChart, NavRoute.AnnualSummary),
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
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        selected = selected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
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
        ) {
            composable<NavRoute.Dashboard> {
                DashboardScreen(
                    onNavigateToNewTransaction = { onNavigateToNewTransaction("gasto") },
                    onNavigateToAnalytics = onNavigateToAnalytics,
                    onNavigateToUpcoming = onNavigateToUpcoming,
                )
            }
            composable<NavRoute.Transactions> {
                TransactionsScreen(
                    onNavigateToNewTransaction = { onNavigateToNewTransaction("gasto") },
                    onNavigateToEditTransaction = onNavigateToEditTransaction,
                )
            }
            composable<NavRoute.Budgets> { BudgetsScreen() }
            composable<NavRoute.AnnualSummary> { AnnualSummaryScreen() }
            composable<NavRoute.Profile> { ProfileScreen(onLogout = onLogout) }
        }
    }
}
