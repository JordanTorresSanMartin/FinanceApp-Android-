package com.example.financeapp.ui.screens.transactions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.financeapp.data.model.Transaction
import com.example.financeapp.ui.theme.ExpenseRed
import com.example.financeapp.ui.theme.IncomeGreen
import com.example.financeapp.ui.viewmodel.transactions.TransactionsViewModel
import java.text.DateFormatSymbols
import java.util.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val groupedTransactions = uiState.filteredTransactions.groupBy { it.date }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Historial", fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Month Navigation
            MonthNavigation(
                year = uiState.currentYear,
                month = uiState.currentMonth,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth
            )

            // Search Bar
            TextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                placeholder = { Text("Buscar transacciones...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            // Filter Chips
            FilterChips(
                selectedFilter = uiState.filterType,
                onFilterSelected = viewModel::onFilterTypeChange
            )

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    groupedTransactions.forEach { (date, transactions) ->
                        stickyHeader {
                            DateHeader(date = date.toString())
                        }
                        items(transactions) { transaction ->
                            PremiumTransactionListItem(transaction)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthNavigation(
    year: Int,
    month: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val locale = Locale("es", "ES")
    val monthName = DateFormatSymbols(locale).months[month - 1]
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onPrevious()
        }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }
        Text(
            text = "${monthName.replaceFirstChar { it.uppercase() }} $year",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onNext()
        }) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChips(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf("All", "Income", "Expense").forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        when (filter) {
                            "All" -> "Todos"
                            "Income" -> "Ingresos"
                            else -> "Gastos"
                        }
                    )
                },
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun DateHeader(date: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = date,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun PremiumTransactionListItem(transaction: Transaction) {
    val haptic = LocalHapticFeedback.current
    ListItem(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
        headlineContent = { Text(transaction.description, fontWeight = FontWeight.Bold) },
        supportingContent = { 
            if (!transaction.notes.isNullOrBlank()) {
                Text(transaction.notes, style = MaterialTheme.typography.bodySmall) 
            }
        },
        trailingContent = {
            Text(
                text = "${if (transaction.type == "expense") "-" else "+"}$${transaction.amount}",
                color = if (transaction.type == "expense") ExpenseRed else IncomeGreen,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        },
        leadingContent = {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (transaction.type == "expense") Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = if (transaction.type == "expense") ExpenseRed else IncomeGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
