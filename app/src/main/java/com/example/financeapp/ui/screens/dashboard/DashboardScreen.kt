package com.example.financeapp.ui.screens.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.financeapp.data.model.MonthlySummary
import com.example.financeapp.ui.components.HeroCard
import com.example.financeapp.ui.components.TonalCard
import com.example.financeapp.ui.theme.ExpenseRed
import com.example.financeapp.ui.theme.IncomeGreen
import com.example.financeapp.ui.viewmodel.dashboard.DashboardUiState
import com.example.financeapp.ui.viewmodel.dashboard.DashboardViewModel
import java.text.DateFormatSymbols
import java.util.*
import androidx.compose.ui.tooling.preview.Preview
import com.example.financeapp.ui.theme.FinanceAppTheme

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    FinanceAppTheme {
        DashboardContent(
            state = DashboardUiState.Success(
                summary = MonthlySummary(balance = 1500.0, totalIncome = 3000.0, totalExpenses = 1500.0, savingsPct = 50.0),
                budgetStatus = listOf(),
                recentTransactions = listOf(),
                currentYear = 2024,
                currentMonth = 6
            ),
            onPreviousMonth = {},
            onNextMonth = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    var showFintocSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Resumen Mensual",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                actions = {
                    IconButton(onClick = { showFintocSheet = true }) {
                        Icon(Icons.Default.AccountBalance, contentDescription = "Vincular Banco")
                    }
                    IconButton(onClick = { /* TODO: Open Notifications */ }) {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        if (showFintocSheet) {
            com.example.financeapp.ui.components.FintocBottomSheet(
                onDismiss = { showFintocSheet = false },
                onLinkSuccess = { 
                    showFintocSheet = false
                    // TODO: Refresh data
                }
            )
        }
        Box(modifier = Modifier.padding(innerPadding)) {
            when (val state = uiState) {
                is DashboardUiState.Loading -> {
                    // TODO: Shimmer Loader
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is DashboardUiState.Success -> {
                    DashboardContent(
                        state = state,
                        onPreviousMonth = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.previousMonth() 
                        },
                        onNextMonth = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.nextMonth() 
                        }
                    )
                }
                is DashboardUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardContent(
    state: DashboardUiState.Success,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Month Selector
        item {
            MonthSelector(
                year = state.currentYear,
                month = state.currentMonth,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth
            )
        }

        // Hero Card (Net Balance)
        item {
            HeroBalanceCard(state.summary)
        }

        // Tonal Summary Cards (Income & Expense)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryTonalCard(
                    modifier = Modifier.weight(1f),
                    label = "Ingresos",
                    amount = "$${state.summary?.totalIncome ?: 0.0}",
                    icon = Icons.Default.TrendingUp,
                    color = IncomeGreen
                )
                SummaryTonalCard(
                    modifier = Modifier.weight(1f),
                    label = "Gastos",
                    amount = "$${state.summary?.totalExpenses ?: 0.0}",
                    icon = Icons.Default.TrendingDown,
                    color = ExpenseRed
                )
            }
        }

        // Budget Status List
        item {
            Text(
                text = "Estado del Presupuesto",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        items(state.budgetStatus) { status ->
            PremiumBudgetCard(status)
        }

        // Recent Transactions
        item {
            Text(
                text = "Transacciones Recientes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        items(state.recentTransactions) { transaction ->
            PremiumTransactionItem(transaction)
        }
    }
}

@Composable
fun MonthSelector(
    year: Int,
    month: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val locale = Locale.forLanguageTag("es-ES")
    val monthName = DateFormatSymbols(locale).months[month - 1]
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }
        Text(
            text = "${monthName.replaceFirstChar { it.uppercase() }} $year",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
fun HeroBalanceCard(summary: MonthlySummary?) {
    HeroCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "Balance Neto",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = "$${summary?.balance ?: 0.0}",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
            shape = CircleShape
        ) {
            Text(
                text = "Ahorro: ${summary?.savingsPct ?: 0.0}% del ingreso",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SummaryTonalCard(
    modifier: Modifier,
    label: String,
    amount: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    TonalCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun PremiumBudgetCard(status: com.example.financeapp.data.model.BudgetStatus) {
    val animatedProgress by animateFloatAsState(
        targetValue = ((status.pctUsed ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f),
        label = "progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = status.categoryName ?: "Sin categoría",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${status.spent ?: 0.0} / ${status.budgetAmount ?: 0.0}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = when {
                    (status.pctUsed ?: 0.0) >= 100.0 -> ExpenseRed
                    (status.pctUsed ?: 0.0) >= 85.0 -> Color(0xFFFFC107)
                    else -> IncomeGreen
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun PremiumTransactionItem(transaction: com.example.financeapp.data.model.Transaction) {
    ListItem(
        modifier = Modifier.padding(horizontal = 8.dp),
        headlineContent = { 
            Text(transaction.description, fontWeight = FontWeight.Bold) 
        },
        supportingContent = { 
            Text(transaction.date.toString(), style = MaterialTheme.typography.bodySmall) 
        },
        trailingContent = {
            Text(
                text = "${if (transaction.type == "expense") "-" else "+"}$${transaction.amount}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (transaction.type == "expense") ExpenseRed else IncomeGreen
            )
        },
        leadingContent = {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (transaction.type == "expense") Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = if (transaction.type == "expense") ExpenseRed else IncomeGreen
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
