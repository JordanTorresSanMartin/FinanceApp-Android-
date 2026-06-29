package com.example.financeapp.ui.screens.budgets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financeapp.data.model.BudgetStatus
import com.example.financeapp.ui.screens.transactions.MonthNavigation
import com.example.financeapp.ui.theme.ExpenseRed
import com.example.financeapp.ui.theme.WarningOrange
import com.example.financeapp.ui.viewmodel.budgets.BudgetsViewModel

@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        MonthNavigation(
            year = uiState.currentYear,
            month = uiState.currentMonth,
            onPrevious = viewModel::previousMonth,
            onNext = viewModel::nextMonth
        )

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.budgets) { budgetStatus ->
                    BudgetEditItem(
                        status = budgetStatus,
                        onSave = { amount ->
                            budgetStatus.categoryId?.let { id ->
                                viewModel.updateBudgetAmount(id, amount)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetEditItem(
    status: BudgetStatus,
    onSave: (Double) -> Unit
) {
    var amountText by remember(status.budgetAmount) {
        mutableStateOf(status.budgetAmount?.toString() ?: "0")
    }
    var isEditing by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = status.categoryName ?: "Sin nombre", 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (isEditing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            modifier = Modifier.width(100.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            shape = MaterialTheme.shapes.small
                        )
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val amount = amountText.toDoubleOrNull() ?: 0.0
                                onSave(amount)
                                isEditing = false
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("OK")
                        }
                    }
                } else {
                    IconButton(onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isEditing = true 
                    }) {
                        Icon(
                            imageVector = Icons.Default.Edit, 
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.SpaceBetween, 
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Gastado", 
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${status.spent ?: 0.0}", 
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Presupuesto", 
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${status.budgetAmount ?: 0.0}", 
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { (status.pctUsed?.toFloat() ?: 0f) / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when {
                    (status.pctUsed ?: 0.0) >= 100.0 -> ExpenseRed
                    (status.pctUsed ?: 0.0) >= 85.0 -> WarningOrange
                    else -> MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            Text(
                text = "Disponible: $${status.available ?: 0.0}", 
                style = MaterialTheme.typography.labelSmall,
                color = if ((status.available ?: 0.0) < 0) ExpenseRed else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
