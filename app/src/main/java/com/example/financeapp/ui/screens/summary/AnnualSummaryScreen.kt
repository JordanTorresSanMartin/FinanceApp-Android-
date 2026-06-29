package com.example.financeapp.ui.screens.summary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financeapp.data.model.MonthlySummary
import com.example.financeapp.ui.theme.ExpenseRed
import com.example.financeapp.ui.theme.IncomeGreen
import com.example.financeapp.ui.viewmodel.summary.AnnualSummaryViewModel
import java.text.DateFormatSymbols
import java.util.*

@Composable
fun AnnualSummaryScreen(
    viewModel: AnnualSummaryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        YearNavigation(
            year = uiState.currentYear,
            onPrevious = viewModel::previousYear,
            onNext = viewModel::nextYear
        )

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(text = "Comparativa Ingresos vs Gastos", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    AnnualBarChart(uiState.summaries)
                }

                item {
                    Text(text = "Desglose Mensual", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(uiState.summaries) { summary ->
                    MonthlySummaryItem(summary)
                }
            }
        }
    }
}

@Composable
fun YearNavigation(
    year: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPrevious()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Año anterior")
            }
            Text(
                text = year.toString(), 
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onNext()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Año siguiente")
            }
        }
    }
}

@Composable
fun AnnualBarChart(summaries: List<MonthlySummary>) {
    val maxAmount = summaries.flatMap { listOf(it.totalIncome ?: 0.0, it.totalExpenses ?: 0.0) }.maxOrNull() ?: 1.0
    val incomeColor = IncomeGreen
    val expenseColor = ExpenseRed
    val axisColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = Modifier.padding(8.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            val barGroupWidth = size.width / 12f
            val barWidth = barGroupWidth / 3f
            
            // Draw axis
            drawLine(
                color = axisColor,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 1.dp.toPx()
            )

            summaries.forEachIndexed { index, summary ->
                val incomeHeight = (summary.totalIncome ?: 0.0).toFloat() / maxAmount.toFloat() * size.height
                val expenseHeight = (summary.totalExpenses ?: 0.0).toFloat() / maxAmount.toFloat() * size.height
                
                val xGroupStart = index * barGroupWidth
                
                // Income bar
                drawRect(
                    color = incomeColor,
                    topLeft = Offset(xGroupStart + barWidth / 2f, size.height - incomeHeight),
                    size = Size(barWidth, incomeHeight)
                )
                
                // Expense bar
                drawRect(
                    color = expenseColor,
                    topLeft = Offset(xGroupStart + barWidth * 1.5f, size.height - expenseHeight),
                    size = Size(barWidth, expenseHeight)
                )
            }
        }
        
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem("Ingresos", incomeColor)
            Spacer(modifier = Modifier.width(24.dp))
            LegendItem("Gastos", expenseColor)
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(12.dp), color = color, shape = MaterialTheme.shapes.extraSmall) {}
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun MonthlySummaryItem(summary: MonthlySummary) {
    val locale = Locale.forLanguageTag("es")
    val monthName = DateFormatSymbols(locale).months[(summary.month ?: 1) - 1]
    
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = monthName.replaceFirstChar { it.uppercase() }, 
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                SummaryColumn("Ingresos", "$${summary.totalIncome ?: 0.0}", IncomeGreen)
                SummaryColumn("Gastos", "$${summary.totalExpenses ?: 0.0}", ExpenseRed)
                SummaryColumn("Balance", "$${summary.balance ?: 0.0}", MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun SummaryColumn(label: String, value: String, valueColor: Color) {
    Column {
        Text(
            text = label, 
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value, 
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}
