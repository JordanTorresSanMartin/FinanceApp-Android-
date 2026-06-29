package com.example.financeapp.ui.screens.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.financeapp.ui.components.AlertBanner
import com.example.financeapp.ui.components.AnimatedProgressBar
import com.example.financeapp.ui.components.BudgetAlertItem
import com.example.financeapp.ui.components.CategoryAvatar
import com.example.financeapp.ui.components.RoundIconButton
import com.example.financeapp.ui.theme.FinanceTheme
import com.example.financeapp.ui.viewmodel.budgets.BudgetsViewModel
import com.example.financeapp.ui.viewmodel.budgets.CalculatedBudget
import com.example.financeapp.util.categoryIcon
import com.example.financeapp.util.formatMoneyCLP
import com.example.financeapp.util.monthYearLabel
import com.example.financeapp.util.parseHexColor
import kotlin.math.roundToInt

@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }
    val finance = FinanceTheme.colors

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(Icons.Filled.ChevronLeft, viewModel::previousMonth, size = 40.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PRESUPUESTO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(monthYearLabel(state.currentYear, state.currentMonth), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            RoundIconButton(Icons.Filled.ChevronRight, viewModel::nextMonth, size = 40.dp)
        }

        // Totals card
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("PRESUPUESTO TOTAL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f))
                Text(
                    if (state.isLoading) "—" else formatMoneyCLP(state.totalBudget),
                    style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Box(Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)))
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text("DISPONIBLE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f))
                Text(
                    if (state.isLoading) "—" else formatMoneyCLP(state.totalAvailable),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (state.totalAvailable < 0) finance.expense else MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    AlertBanner(
                        items = state.items.map {
                            BudgetAlertItem(it.categoryId, it.categoryName, it.budgetAmount, it.spent)
                        },
                    )
                }
                items(state.items, key = { it.categoryId }) { item ->
                    BudgetItemCard(
                        item = item,
                        saving = state.savingId == item.categoryId,
                        onSave = { amount -> viewModel.updateBudgetAmount(item.categoryId, amount) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetItemCard(
    item: CalculatedBudget,
    saving: Boolean,
    onSave: (Double) -> Unit,
) {
    val finance = FinanceTheme.colors
    val statusColor = when (item.status) {
        "excedido" -> finance.expense
        "advertencia" -> finance.warning
        else -> finance.income
    }
    val catColor = parseHexColor(item.color, MaterialTheme.colorScheme.primary)
    var text by remember(item.categoryId, item.budgetAmount) {
        mutableStateOf(if (item.budgetAmount > 0) item.budgetAmount.toLong().toString() else "")
    }
    var wasFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryAvatar(icon = categoryIcon(item.icon), color = catColor, size = 40.dp, corner = 14.dp)
            Spacer(Modifier.width(12.dp))
            Text(item.categoryName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (saving) {
                Spacer(Modifier.width(8.dp))
                CircularProgressIndicator(modifier = Modifier.height(16.dp).width(16.dp), strokeWidth = 2.dp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Presupuesto asignado", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                TextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .onFocusChanged { focus ->
                            if (wasFocused && !focus.isFocused) {
                                onSave((text.toLongOrNull() ?: 0L).toDouble())
                            }
                            wasFocused = focus.isFocused
                        },
                    placeholder = { Text("0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
            }
            Column(Modifier.weight(1f)) {
                Text("Gastado mes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(statusColor.copy(alpha = 0.10f))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        formatMoneyCLP(item.spent),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = if (item.status == "excedido") finance.expense else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { AnimatedProgressBar(pct = item.pctUsed, color = statusColor) }
            Spacer(Modifier.width(12.dp))
            Text("${item.pctUsed.roundToInt()}%", style = MaterialTheme.typography.labelLarge, color = statusColor)
        }
    }
}
