package com.example.financeapp.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.example.financeapp.ui.components.AnimatedProgressBar
import com.example.financeapp.ui.components.CategoryAvatar
import com.example.financeapp.ui.components.CategoryDonut
import com.example.financeapp.ui.components.DonutSegment
import com.example.financeapp.ui.components.RoundIconButton
import com.example.financeapp.ui.theme.FinanceTheme
import com.example.financeapp.ui.viewmodel.analytics.AnalyticsViewModel
import com.example.financeapp.ui.viewmodel.analytics.CatStat
import com.example.financeapp.util.categoryIcon
import com.example.financeapp.util.formatMoneyCLP
import com.example.financeapp.util.monthYearLabel
import com.example.financeapp.util.parseHexColor
import com.example.financeapp.util.shortDateLabel
import kotlin.math.roundToInt

@Composable
fun AnalyticsScreen(
    onBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }

    val selected = state.stats.firstOrNull { it.id == state.selectedId }
    val segments = state.stats.map {
        DonutSegment(it.id, it.name, it.total, parseHexColor(it.color, MaterialTheme.colorScheme.primary))
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, onBack, size = 40.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ANÁLISIS POR CATEGORÍA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(monthYearLabel(state.currentYear, state.currentMonth), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Row {
                RoundIconButton(Icons.Filled.ChevronLeft, viewModel::previousMonth, size = 40.dp)
                Spacer(Modifier.width(8.dp))
                RoundIconButton(Icons.Filled.ChevronRight, viewModel::nextMonth, size = 40.dp)
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 40.dp),
        ) {
            // Donut card
            item {
                Column(
                    modifier = Modifier.padding(20.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CategoryDonut(
                        data = segments,
                        centerPrimary = if (selected != null) "${selected.pct.roundToInt()}%" else formatMoneyCLP(state.total),
                        centerSecondary = selected?.name ?: "Gastado",
                        selectedKey = state.selectedId,
                        onSelect = viewModel::toggleSelected,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (selected != null) "Toca el sector de nuevo para volver al total" else "Toca un sector para ver el detalle",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
            }

            if (selected != null) {
                item { CategoryDetail(selected) }
            } else if (state.stats.isEmpty()) {
                item { EmptyAnalytics() }
            } else {
                items(state.stats.size) { idx ->
                    LegendRow(state.stats[idx]) { viewModel.toggleSelected(state.stats[idx].id) }
                }
            }
        }
    }
}

@Composable
private fun LegendRow(stat: CatStat, onClick: () -> Unit) {
    val color = parseHexColor(stat.color, MaterialTheme.colorScheme.primary)
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(12.dp))
        CategoryAvatar(icon = categoryIcon(stat.icon), color = color, size = 36.dp, corner = 12.dp)
        Spacer(Modifier.width(12.dp))
        Text(stat.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1)
        Column(horizontalAlignment = Alignment.End) {
            Text(formatMoneyCLP(stat.total), style = MaterialTheme.typography.titleMedium)
            Text("${"%.1f".format(stat.pct)}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CategoryDetail(stat: CatStat) {
    val finance = FinanceTheme.colors
    val color = parseHexColor(stat.color, MaterialTheme.colorScheme.primary)
    val avg = if (stat.count > 0) stat.total / stat.count else 0.0
    val hasBudget = stat.budget > 0
    val pctBudget = if (hasBudget) stat.total / stat.budget * 100 else 0.0
    val over = hasBudget && stat.total > stat.budget
    val budgetColor = if (over) finance.expense else if (pctBudget >= 85) finance.warning else finance.income

    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryAvatar(icon = categoryIcon(stat.icon), color = color, size = 52.dp, corner = 18.dp)
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(stat.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${"%.1f".format(stat.pct)}% del gasto del mes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(formatMoneyCLP(stat.total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        // Metrics
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Metric(Modifier.weight(1f), "Movimientos", stat.count.toString())
            Metric(Modifier.weight(1f), "Promedio", formatMoneyCLP(avg))
        }
        // Budget
        if (hasBudget) {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(16.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Presupuesto", style = MaterialTheme.typography.titleSmall)
                    Text("${pctBudget.roundToInt()}%", style = MaterialTheme.typography.titleSmall, color = budgetColor)
                }
                Spacer(Modifier.height(10.dp))
                AnimatedProgressBar(pct = pctBudget, color = budgetColor)
                Spacer(Modifier.height(8.dp))
                Text(
                    "${formatMoneyCLP(stat.total)} de ${formatMoneyCLP(stat.budget)}" +
                        if (over) "  ·  excedido ${formatMoneyCLP(stat.total - stat.budget)}"
                        else "  ·  disponible ${formatMoneyCLP(stat.budget - stat.total)}",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Last movements
        Text("Últimos movimientos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(horizontal = 16.dp),
        ) {
            stat.txs.forEachIndexed { i, tx ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tx.description, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                        Text(shortDateLabel(tx.date), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(formatMoneyCLP(tx.amount), style = MaterialTheme.typography.titleMedium)
                }
                if (i < stat.txs.size - 1) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Metric(modifier: Modifier, label: String, value: String) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(16.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun EmptyAnalytics() {
    Column(
        modifier = Modifier.padding(20.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.PieChart, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
        Text("Sin gastos este mes", style = MaterialTheme.typography.titleMedium)
        Text(
            "Registra movimientos para ver el análisis por categoría.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
