package com.example.financeapp.ui.viewmodel.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.model.BudgetStatus
import com.example.financeapp.data.model.MonthlySummary
import com.example.financeapp.data.model.Transaction
import com.example.financeapp.domain.use_case.GetDashboardDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    data class Success(
        val summary: MonthlySummary?,
        val budgetStatus: List<BudgetStatus>,
        val recentTransactions: List<Transaction>,
        val currentYear: Int,
        val currentMonth: Int
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardDataUseCase: GetDashboardDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var currentYear: Int
    private var currentMonth: Int

    init {
        val calendar = Calendar.getInstance()
        currentYear = calendar.get(Calendar.YEAR)
        currentMonth = calendar.get(Calendar.MONTH) + 1
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            try {
                val data = getDashboardDataUseCase(currentYear, currentMonth)

                _uiState.value = DashboardUiState.Success(
                    summary = data.summary,
                    budgetStatus = data.budgetStatus,
                    recentTransactions = data.recentTransactions,
                    currentYear = currentYear,
                    currentMonth = currentMonth
                )
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun nextMonth() {
        if (currentMonth == 12) {
            currentMonth = 1
            currentYear++
        } else {
            currentMonth++
        }
        loadDashboardData()
    }

    fun previousMonth() {
        if (currentMonth == 1) {
            currentMonth = 12
            currentYear--
        } else {
            currentMonth--
        }
        loadDashboardData()
    }
}
