package com.example.financeapp.ui.viewmodel.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.model.Budget
import com.example.financeapp.data.model.BudgetStatus
import com.example.financeapp.domain.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class BudgetsUiState(
    val budgets: List<BudgetStatus> = emptyList(),
    val isLoading: Boolean = false,
    val currentYear: Int,
    val currentMonth: Int,
    val error: String? = null
)

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        BudgetsUiState(
            currentYear = Calendar.getInstance().get(Calendar.YEAR),
            currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        )
    )
    val uiState: StateFlow<BudgetsUiState> = _uiState.asStateFlow()

    init {
        loadBudgets()
    }

    fun loadBudgets() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val budgets = repository.getBudgetStatus(_uiState.value.currentYear, _uiState.value.currentMonth)
                _uiState.value = _uiState.value.copy(budgets = budgets, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun updateBudgetAmount(categoryId: String, amount: Double) {
        viewModelScope.launch {
            try {
                val budget = Budget(
                    categoryId = categoryId,
                    year = _uiState.value.currentYear,
                    month = _uiState.value.currentMonth,
                    amount = amount
                )
                repository.upsertBudget(budget)
                loadBudgets()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Error al actualizar presupuesto")
            }
        }
    }

    fun nextMonth() {
        var year = _uiState.value.currentYear
        var month = _uiState.value.currentMonth + 1
        if (month > 12) {
            month = 1
            year++
        }
        _uiState.value = _uiState.value.copy(currentYear = year, currentMonth = month)
        loadBudgets()
    }

    fun previousMonth() {
        var year = _uiState.value.currentYear
        var month = _uiState.value.currentMonth - 1
        if (month < 1) {
            month = 12
            year--
        }
        _uiState.value = _uiState.value.copy(currentYear = year, currentMonth = month)
        loadBudgets()
    }
}
