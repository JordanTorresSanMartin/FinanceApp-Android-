package com.example.financeapp.ui.viewmodel.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.model.TxType
import com.example.financeapp.domain.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class CalculatedBudget(
    val categoryId: String,
    val categoryName: String,
    val icon: String?,
    val color: String?,
    val budgetAmount: Double,
    val spent: Double,
    val pctUsed: Double,
    val status: String, // "ok" | "advertencia" | "excedido"
)

data class BudgetsUiState(
    val items: List<CalculatedBudget> = emptyList(),
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val isLoading: Boolean = true,
    val savingId: String? = null,
    val currentYear: Int,
    val currentMonth: Int,
    val error: String? = null,
) {
    val totalAvailable: Double get() = totalBudget - totalSpent
}

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        BudgetsUiState(
            currentYear = Calendar.getInstance().get(Calendar.YEAR),
            currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1,
        )
    )
    val uiState: StateFlow<BudgetsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val year = _uiState.value.currentYear
                val month = _uiState.value.currentMonth
                val cats = repository.getCategories().filter { it.type == TxType.GASTO || it.type == "ambos" }
                val budgets = repository.getBudgetsForMonth(year, month)
                val expenses = repository.getTransactionsByMonth(year, month).filter { it.type == TxType.GASTO }

                var totalBudget = 0.0
                var totalSpent = 0.0
                val items = cats.mapNotNull { cat ->
                    val id = cat.id ?: return@mapNotNull null
                    val budgetAmount = budgets.firstOrNull { it.categoryId == id }?.amount ?: 0.0
                    val spent = expenses.filter { it.categoryId == id }.sumOf { it.amount }
                    val pct = when {
                        budgetAmount > 0 -> spent / budgetAmount * 100
                        spent > 0 -> 100.0
                        else -> 0.0
                    }
                    totalBudget += budgetAmount
                    totalSpent += spent
                    CalculatedBudget(
                        categoryId = id,
                        categoryName = cat.name,
                        icon = cat.icon,
                        color = cat.color,
                        budgetAmount = budgetAmount,
                        spent = spent,
                        pctUsed = pct,
                        status = if (pct >= 100) "excedido" else if (pct >= 85) "advertencia" else "ok",
                    )
                }
                _uiState.value = _uiState.value.copy(
                    items = items, totalBudget = totalBudget, totalSpent = totalSpent, isLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun updateBudgetAmount(categoryId: String, amount: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(savingId = categoryId)
            try {
                repository.upsertBudget(categoryId, _uiState.value.currentYear, _uiState.value.currentMonth, amount)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Error al guardar presupuesto")
            } finally {
                _uiState.value = _uiState.value.copy(savingId = null)
                load()
            }
        }
    }

    fun nextMonth() = shiftMonth(1)
    fun previousMonth() = shiftMonth(-1)

    private fun shiftMonth(delta: Int) {
        var year = _uiState.value.currentYear
        var month = _uiState.value.currentMonth + delta
        if (month > 12) { month = 1; year++ }
        if (month < 1) { month = 12; year-- }
        _uiState.value = _uiState.value.copy(currentYear = year, currentMonth = month)
        load()
    }
}
