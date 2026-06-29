package com.example.financeapp.ui.screens.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.financeapp.data.model.MonthlySummary
import com.example.financeapp.ui.components.RoundIconButton
import com.example.financeapp.ui.theme.FinanceTheme
import com.example.financeapp.ui.viewmodel.summary.AnnualSummaryViewModel
import com.example.financeapp.util.formatMoneyCLP
import com.example.financeapp.util.monthShortLabel
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun AnnualSummaryScreen(
    viewModel: AnnualSummaryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }
    val finance = FinanceTheme.colors

    val fullYear = (1..12).map { m ->
        state.summaries.firstOrNull { it.month == m }
            ?: MonthlySummary(year = state.currentYear, month = m, totalIncome = 0.0, totalExpenses = 0.0, balance = 0.0, savingsPct = 0.0)
    }
    val yearIncome = fullYear.sumOf { it.totalIncome ?: 0.0 }
    val yearExpense = fullYear.sumOf { it.totalExpenses ?: 0.0 }
    val yearBalance = yearIncome - yearExpense
    val maxVal = max(fullYear.maxOf { it.totalIncome ?: 0.0 }, fullYear.maxOf { it.totalExpenses ?: 0.0 }).coerceAtLeast(1.0)

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(Icons.Filled.ChevronLeft, viewModel::previousYear, size = 40.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("RESUMEN ANUAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${state.currentYear}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            RoundIconButton(Icons.Filled.ChevronRight, viewModel::nextYear, size = 40.dp)
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Hero anual
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer).padding(24.dp),
            ) {
                Text("Balance neto anual", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f))
                Text(
                    formatMoneyCLP(yearBalance),
                    style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black,
                    color = if (yearBalance >= 0) MaterialTheme.colorScheme.onPrimaryContainer else finance.expense,
                )
                Spacer(Modifier.height(18.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column {
                        Text("INGRESOS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        Text("+${formatMoneyCLP(yearIncome)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Column {
                        Text("GASTOS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        Text("-${formatMoneyCLP(yearExpense)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // Bar chart
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(20.dp),
            ) {
                Text("Ingresos vs gastos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    fullYear.forEach { s ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                modifier = Modifier.height(140.dp).width(40.dp),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Bar(fraction = (s.totalIncome ?: 0.0) / maxVal, color = finance.income)
                                Bar(fraction = (s.totalExpenses ?: 0.0) / maxVal, color = finance.expense)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(monthShortLabel(s.month ?: 1), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Legend("Ingresos", finance.income)
                    Spacer(Modifier.width(20.dp))
                    Legend("Gastos", finance.expense)
                }
            }

            // Monthly table
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(14.dp),
                ) {
                    Th("Mes", 0.8f)
                    Th("Ingresos", 1f)
                    Th("Gastos", 1f)
                    Th("Bal.", 0.8f)
                }
                fullYear.forEach { s ->
                    val hasData = (s.totalIncome ?: 0.0) > 0 || (s.totalExpenses ?: 0.0) > 0
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(monthShortLabel(s.month ?: 1), modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        if (hasData) {
                            Td(formatMoneyCLP(s.totalIncome ?: 0.0), 1f, MaterialTheme.colorScheme.onSurface)
                            Td(formatMoneyCLP(s.totalExpenses ?: 0.0), 1f, MaterialTheme.colorScheme.onSurface)
                            val bal = s.balance ?: 0.0
                            Td(
                                (if (bal > 0) "+" else "") + "${(bal / 1000).roundToInt()}k",
                                0.8f, if (bal >= 0) finance.income else finance.expense,
                            )
                        } else {
                            Text(
                                "Sin movimientos", modifier = Modifier.weight(2.8f),
                                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Bar(fraction: Double, color: Color) {
    Box(
        modifier = Modifier
            .width(12.dp)
            .fillMaxHeightFraction(fraction)
            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
            .background(color),
    )
}

// Altura mínima visible para barras en 0 (igual que RN: 4px).
@Composable
private fun Modifier.fillMaxHeightFraction(fraction: Double): Modifier {
    val f = fraction.toFloat().coerceIn(0f, 1f)
    return if (f <= 0f) this.height(4.dp) else this.fillMaxHeight(f)
}

@Composable
private fun Legend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Th(text: String, weight: Float) {
    Text(
        text.uppercase(), modifier = Modifier.weight(weight), textAlign = TextAlign.End,
        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Td(text: String, weight: Float, color: Color) {
    Text(
        text, modifier = Modifier.weight(weight), textAlign = TextAlign.End,
        style = MaterialTheme.typography.bodyMedium, color = color,
    )
}
