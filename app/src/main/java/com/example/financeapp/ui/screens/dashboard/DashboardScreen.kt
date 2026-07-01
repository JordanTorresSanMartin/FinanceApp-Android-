package com.example.financeapp.ui.screens.dashboard

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.financeapp.data.model.BudgetStatus
import com.example.financeapp.ui.components.AlertBanner
import com.example.financeapp.ui.components.AnimatedProgressBar
import com.example.financeapp.ui.components.BudgetAlertItem
import com.example.financeapp.ui.components.CategoryAvatar
import com.example.financeapp.ui.components.QuickAddFab
import com.example.financeapp.ui.components.RoundIconButton
import com.example.financeapp.ui.components.pressable
import com.example.financeapp.ui.theme.FinanceTheme
import com.example.financeapp.ui.viewmodel.dashboard.DashboardUiState
import com.example.financeapp.ui.viewmodel.dashboard.DashboardViewModel
import com.example.financeapp.util.LocalAppHaptics
import com.example.financeapp.util.categoryIcon
import com.example.financeapp.util.formatMoneyCLP
import com.example.financeapp.util.monthYearLabel
import com.example.financeapp.util.parseHexColor
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    onNavigateToNewTransaction: (String) -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToUpcoming: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            DashboardHeader(
                monthLabel = monthYearLabel(state.currentYear, state.currentMonth),
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
                onAnalytics = onNavigateToAnalytics,
            )
            DashboardBody(
                state = state,
                onNavigateToUpcoming = onNavigateToUpcoming,
            )
        }
        QuickAddFab(
            onSelectType = onNavigateToNewTransaction,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        )
    }
}

@Composable
private fun DashboardHeader(
    monthLabel: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onAnalytics: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(Icons.Filled.ChevronLeft, onPrevious)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "PRESUPUESTO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(monthLabel, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Icons.Filled.ChevronRight, onNext)
            Spacer(Modifier.width(8.dp))
            RoundIconButton(
                icon = Icons.Filled.BarChart,
                onClick = onAnalytics,
                container = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun DashboardBody(
    state: DashboardUiState,
    onNavigateToUpcoming: () -> Unit,
) {
    val finance = FinanceTheme.colors
    val summary = state.summary
    val balance = summary?.balance ?: 0.0
    val income = summary?.totalIncome ?: 0.0
    val expenses = summary?.totalExpenses ?: 0.0
    val savings = summary?.savingsPct ?: 0.0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        state.error?.let { err ->
            item {
                Text(
                    "No se pudieron cargar los datos: $err",
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
        // Hero balance
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(24.dp),
            ) {
                Text(
                    "Balance neto",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                )
                Text(
                    if (state.isLoading) "—" else formatMoneyCLP(balance),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = if (balance < 0) finance.expense else MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.TrendingUp, null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (state.isLoading) "—" else "${"%.1f".format(savings)}% de ahorro",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        // Alert banner
        item {
            val alertItems = state.budgetStatus.map {
                BudgetAlertItem(
                    id = it.categoryId ?: "",
                    name = it.categoryName ?: "",
                    budget = it.budgetAmount ?: 0.0,
                    spent = it.spent ?: 0.0,
                )
            }
            Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                AlertBanner(items = alertItems)
            }
        }

        // Income / Expense
        item {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TonalAmountCard(
                    modifier = Modifier.weight(1f),
                    label = "Ingresos",
                    amount = if (state.isLoading) "—" else formatMoneyCLP(income),
                    container = finance.incomeContainer,
                    onContainer = finance.onIncomeContainer,
                    iconBg = finance.income,
                    onIcon = finance.onIncome,
                    icon = Icons.Filled.ArrowDownward,
                )
                TonalAmountCard(
                    modifier = Modifier.weight(1f),
                    label = "Gastos",
                    amount = if (state.isLoading) "—" else formatMoneyCLP(expenses),
                    container = finance.expenseContainer,
                    onContainer = finance.onExpenseContainer,
                    iconBg = finance.expense,
                    onIcon = finance.onExpense,
                    icon = Icons.Filled.ArrowUpward,
                )
            }
        }

        // Próximos pagos access
        item {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .fillMaxWidth()
                    .pressable(onClick = onNavigateToUpcoming)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.CreditCard, null, tint = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text("Próximos pagos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Cuotas de tus estados de cuenta",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp),
                )
            }
        }

        // Section header
        item {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Estado del presupuesto", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${state.budgetStatus.size} categorías",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (state.budgetStatus.isEmpty()) {
            item { EmptyBudget() }
        } else {
            items(state.budgetStatus, key = { it.categoryId ?: it.categoryName ?: "" }) { cat ->
                BudgetStatusCard(cat)
            }
        }
    }
}

@Composable
private fun TonalAmountCard(
    modifier: Modifier,
    label: String,
    amount: String,
    container: Color,
    onContainer: Color,
    iconBg: Color,
    onIcon: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(container)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(30.dp).clip(CircleShape).background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = onIcon, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = onContainer)
        }
        Spacer(Modifier.height(12.dp))
        Text(amount, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = onContainer)
    }
}

@Composable
private fun BudgetStatusCard(cat: BudgetStatus) {
    val finance = FinanceTheme.colors
    val status = cat.status ?: "ok"
    val statusColor = when (status) {
        "excedido" -> finance.expense
        "advertencia" -> finance.warning
        else -> finance.income
    }
    val statusIcon = when (status) {
        "excedido" -> Icons.Filled.Error
        "advertencia" -> Icons.Filled.Warning
        else -> Icons.Filled.CheckCircle
    }
    val catColor = parseHexColor(cat.color, MaterialTheme.colorScheme.primary)
    val spent = cat.spent ?: 0.0
    val budget = cat.budgetAmount ?: 0.0
    val pct = cat.pctUsed ?: 0.0

    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryAvatar(icon = categoryIcon(cat.icon), color = catColor)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    cat.categoryName ?: "Sin categoría",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier.clip(CircleShape).background(statusColor.copy(alpha = 0.14f))
                        .padding(horizontal = 9.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${pct.roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = statusColor)
                }
            }
            Spacer(Modifier.height(10.dp))
            AnimatedProgressBar(pct = pct, color = statusColor)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${formatMoneyCLP(spent)} de ${formatMoneyCLP(budget)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (status == "excedido") {
                    Text(
                        "+${formatMoneyCLP(spent - budget)} excedido",
                        style = MaterialTheme.typography.labelMedium,
                        color = finance.expense,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyBudget() {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Filled.PieChart, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(42.dp),
        )
        Text("Sin presupuestos este mes", style = MaterialTheme.typography.titleMedium)
        Text(
            "Define montos por categoría en la pestaña Presupuesto para ver tu progreso aquí.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
