package com.example.financeapp.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.financeapp.ui.navigation.NavRoute
import com.example.financeapp.ui.screens.budgets.BudgetsScreen
import com.example.financeapp.ui.screens.dashboard.DashboardScreen
import com.example.financeapp.ui.screens.summary.AnnualSummaryScreen
import com.example.financeapp.ui.screens.transactions.TransactionsScreen
import com.example.financeapp.ui.theme.FinanceAppTheme
import com.example.financeapp.ui.viewmodel.auth.AuthViewModel

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: NavRoute
)

@Composable
fun MainScaffold(
    onNavigateToNewTransaction: () -> Unit,
    onNavigateToArchitecture: () -> Unit,
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val haptic = LocalHapticFeedback.current

    val items = listOf(
        BottomNavItem("Dashboard", Icons.Default.Dashboard, NavRoute.Dashboard),
        BottomNavItem("Historial", Icons.AutoMirrored.Filled.ListAlt, NavRoute.Transactions),
        BottomNavItem("Presupuesto", Icons.Default.PieChart, NavRoute.Budgets),
        BottomNavItem("Análisis", Icons.Default.Analytics, NavRoute.AnnualSummary),
        BottomNavItem("Perfil", Icons.Default.AccountCircle, NavRoute.Profile)
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                tonalElevation = 8.dp
            ) {
                items.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.hasRoute(item.route::class) } == true
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                item.icon, 
                                contentDescription = item.label,
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            ) 
                        },
                        label = { 
                            Text(
                                item.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            ) 
                        },
                        selected = selected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            navController.navigate(item.route) {
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
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigateToNewTransaction()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar", fontWeight = FontWeight.Bold)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoute.Dashboard,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<NavRoute.Dashboard> { DashboardScreen() }
            composable<NavRoute.Transactions> { TransactionsScreen() }
            composable<NavRoute.Budgets> { BudgetsScreen() }
            composable<NavRoute.AnnualSummary> { AnnualSummaryScreen() }
            composable<NavRoute.Profile> { 
                ProfileScreen(
                    onNavigateToArchitecture = onNavigateToArchitecture,
                    onLogout = {
                        authViewModel.logout()
                        onLogout()
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    FinanceAppTheme {
        ProfileScreen(
            onNavigateToArchitecture = {},
            onLogout = {}
        )
    }
}

@Composable
fun ProfileScreen(
    onNavigateToArchitecture: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Configuración de Perfil",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedButton(
            onClick = onNavigateToArchitecture,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(Icons.Default.Analytics, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ver Arquitectura Técnica")
        }
        
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = MaterialTheme.shapes.large
        ) {
            Text("Cerrar Sesión")
        }
    }
}
