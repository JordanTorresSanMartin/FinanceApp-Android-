package com.example.financeapp.ui.screens.transactions

import com.example.financeapp.ui.viewmodel.transactions.FILTER_ALL
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import com.example.financeapp.data.model.Transaction
import com.example.financeapp.data.model.TxType
import com.example.financeapp.ui.components.CategoryAvatar
import com.example.financeapp.ui.components.RoundIconButton
import com.example.financeapp.ui.components.groupedItemShape
import com.example.financeapp.ui.components.pressable
import com.example.financeapp.ui.theme.FinanceTheme
import com.example.financeapp.ui.viewmodel.transactions.TransactionsViewModel
import com.example.financeapp.util.LocalAppHaptics
import com.example.financeapp.util.categoryIcon
import com.example.financeapp.util.dayHeaderLabel
import com.example.financeapp.util.formatMoneyCLP
import com.example.financeapp.util.monthYearLabel
import com.example.financeapp.util.parseHexColor
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigateToNewTransaction: () -> Unit,
    onNavigateToEditTransaction: (String) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }
    val finance = FinanceTheme.colors
    val focusManager = LocalFocusManager.current

    val grouped: List<Pair<LocalDate, List<Transaction>>> =
        state.filteredTransactions.groupBy { it.date }.toList()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // ── Header ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIconButton(Icons.Filled.ChevronLeft, viewModel::previousMonth, "Mes anterior")
                Text(
                    monthYearLabel(state.currentYear, state.currentMonth),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                )
                RoundIconButton(Icons.Filled.ChevronRight, viewModel::nextMonth, "Mes siguiente")
            }
            Spacer(Modifier.height(14.dp))
            // Search
            TextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier.fillMaxWidth().clip(CircleShape),
                placeholder = { Text("Buscar por descripción o categoría") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Close, "Limpiar búsqueda", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
            Spacer(Modifier.height(12.dp))
            // Filter chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill("Todos", state.filterType == FILTER_ALL, MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.onPrimary) { viewModel.onFilterTypeChange(FILTER_ALL) }
                FilterPill("Ingresos", state.filterType == TxType.INGRESO, finance.income,
                    finance.onIncome) { viewModel.onFilterTypeChange(TxType.INGRESO) }
                FilterPill("Gastos", state.filterType == TxType.GASTO, finance.expense,
                    finance.onExpense) { viewModel.onFilterTypeChange(TxType.GASTO) }
            }
        }

        // ── Error visible (diagnóstico) ──
        state.error?.let { err ->
            Text(
                "No se pudieron cargar las transacciones: $err",
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

        // ── List ──
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.load(userRefresh = true) },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            if (state.isLoading && state.transactions.isEmpty()) {
                com.example.financeapp.ui.components.SkeletonScope(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
                    com.example.financeapp.ui.components.SkeletonCardList(rows = 8)
                }
            } else if (state.filteredTransactions.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Filled.ReceiptLong, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(if (state.isFiltering) "Sin resultados" else "Sin transacciones", style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (state.isFiltering) "Ningún movimiento coincide con la búsqueda o el filtro" else "No hay movimientos este mes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp,
                    ),
                ) {
                    grouped.forEach { (date, items) ->
                        val dayTotal = items.sumOf { if (it.type == TxType.INGRESO) it.amount else -it.amount }
                        item(key = "h-$date") {
                            DayHeader(label = dayHeaderLabel(date), total = dayTotal)
                        }
                        items.forEachIndexed { idx, tx ->
                            item(key = tx.id ?: (tx.description + date + idx)) {
                                TransactionRow(tx, shape = groupedItemShape(idx, items.size)) {
                                    tx.id?.let(onNavigateToEditTransaction)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Footer totals ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FooterTotal(Modifier.weight(1f), "Ingresos", formatMoneyCLP(state.totalIncome), finance.income)
            Box(Modifier.width(1.dp).height(36.dp).background(MaterialTheme.colorScheme.outlineVariant))
            FooterTotal(Modifier.weight(1f), "Gastos", formatMoneyCLP(state.totalExpenses), finance.expense)
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .pressable(onClick = onNavigateToNewTransaction, strongHaptic = true)
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, "Agregar", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String, selected: Boolean, selColor: Color, onSel: Color, onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .pressable(onClick = onClick, role = Role.Tab)
            .semantics { this.selected = selected }
            .clip(CircleShape)
            .background(if (selected) selColor else MaterialTheme.colorScheme.surfaceContainer)
            .heightIn(min = 40.dp)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) onSel else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DayHeader(label: String, total: Double) {
    val finance = FinanceTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(Modifier.weight(1f).padding(horizontal = 10.dp).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
        Text(
            (if (total >= 0) "+" else "-") + formatMoneyCLP(kotlin.math.abs(total)),
            style = MaterialTheme.typography.labelMedium,
            color = if (total >= 0) finance.income else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TransactionRow(tx: Transaction, shape: RoundedCornerShape, onClick: () -> Unit) {
    val finance = FinanceTheme.colors
    val isIncome = tx.type == TxType.INGRESO
    val catColor = parseHexColor(tx.categories?.color, MaterialTheme.colorScheme.primary)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .pressable(onClick = onClick, onClickLabel = "Editar")
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryAvatar(icon = categoryIcon(tx.categories?.icon), color = catColor, size = 46.dp)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(tx.description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    tx.categories?.name ?: "Sin categoría",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!tx.source.isNullOrBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Row(
                        modifier = Modifier.clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Mail, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(9.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(tx.source, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        Text(
            (if (isIncome) "+" else "-") + formatMoneyCLP(tx.amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isIncome) finance.income else finance.expense,
        )
    }
}

@Composable
private fun FooterTotal(modifier: Modifier, label: String, amount: String, color: Color) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(amount, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}
