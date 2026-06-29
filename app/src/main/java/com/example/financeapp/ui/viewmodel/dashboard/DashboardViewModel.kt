package com.example.financeapp.ui.viewmodel.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.model.BudgetStatus
import com.example.financeapp.data.model.MonthlySummary
import com.example.financeapp.domain.use_case.GetDashboardDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val summary: MonthlySummary? = null,
    val budgetStatus: List<BudgetStatus> = emptyList(),
    val currentYear: Int = 0,
    val currentMonth: Int = 0,
    val error: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardDataUseCase: GetDashboardDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            currentYear = Calendar.getInstance().get(Calendar.YEAR),
            currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1,
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val data = getDashboardDataUseCase(_uiState.value.currentYear, _uiState.value.currentMonth)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    summary = data.summary,
                    budgetStatus = data.budgetStatus,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Error")
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
