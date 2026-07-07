package com.example.financeapp.ui.viewmodel.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.model.TxType
import com.example.financeapp.domain.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import java.util.Calendar
import javax.inject.Inject

data class TxLite(val id: String, val date: LocalDate, val amount: Double, val description: String)

data class CatStat(
    val id: String,
    val name: String,
    val icon: String?,
    val color: String?,
    val total: Double,
    val count: Int,
    val pct: Double,
    val budget: Double,
    val txs: List<TxLite>,
)

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val stats: List<CatStat> = emptyList(),
    val total: Double = 0.0,
    val selectedId: String? = null,
    val currentYear: Int,
    val currentMonth: Int,
    val error: String? = null,
)

// Paleta de respaldo si una categoría no trae color (igual que la app RN).
private val FALLBACK_PALETTE = listOf(
    "#4C8DF5", "#E2A33B", "#1CA37E", "#D9534F", "#9B6BDF", "#E0699F", "#3CB1C8", "#C5713B",
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AnalyticsUiState(
            currentYear = Calendar.getInstance().get(Calendar.YEAR),
            currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1,
        )
    )
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val year = _uiState.value.currentYear
                val month = _uiState.value.currentMonth
                val txs = repository.getTransactionsByMonth(year, month).filter { it.type == TxType.GASTO }
                val budgets = repository.getBudgetsForMonth(year, month)

                val grouped = txs.groupBy { it.categoryId ?: "sin" }
                var sum = 0.0
                var paletteIndex = 0
                val stats = grouped.map { (id, list) ->
                    val first = list.first()
                    val total = list.sumOf { it.amount }
                    sum += total
                    val color = first.categories?.color ?: FALLBACK_PALETTE[paletteIndex++ % FALLBACK_PALETTE.size]
                    val budget = budgets.firstOrNull { it.categoryId == id }?.amount ?: 0.0
                    CatStat(
                        id = id,
                        name = first.categories?.name ?: "Sin categoría",
                        icon = first.categories?.icon,
                        color = color,
                        total = total,
                        count = list.size,
                        pct = 0.0,
                        budget = budget,
                        txs = list.take(6).map { TxLite(it.id ?: "", it.date, it.amount, it.description) },
                    )
                }.sortedByDescending { it.total }
                    .map { it.copy(pct = if (sum > 0) it.total / sum * 100 else 0.0) }

                val keepSelected = _uiState.value.selectedId?.takeIf { sel -> stats.any { it.id == sel } }
                _uiState.value = _uiState.value.copy(
                    isLoading = false, stats = stats, total = sum, selectedId = keepSelected,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun toggleSelected(id: String?) {
        _uiState.value = _uiState.value.copy(
            selectedId = if (_uiState.value.selectedId == id) null else id
        )
    }

    fun nextMonth() = shiftMonth(1)
    fun previousMonth() = shiftMonth(-1)

    private fun shiftMonth(delta: Int) {
        var year = _uiState.value.currentYear
        var month = _uiState.value.currentMonth + delta
        if (month > 12) { month = 1; year++ }
        if (month < 1) { month = 12; year-- }
        _uiState.value = _uiState.value.copy(currentYear = year, currentMonth = month, selectedId = null)
        load()
    }
}
