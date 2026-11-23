package com.example.financeapp.ui.viewmodel.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.model.BudgetStatus
import com.example.financeapp.data.model.MonthlySummary
import com.example.financeapp.domain.PayState
import com.example.financeapp.domain.use_case.GetDashboardDataUseCase
import com.example.financeapp.domain.use_case.GetRecurringStatusUseCase
import com.example.financeapp.util.friendlyError
import com.example.financeapp.util.shiftYearMonth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Resumen de pagos fijos del período para la tarjeta del inicio. */
data class RecurringSummary(val total: Int, val paid: Int, val overdue: Int, val dueSoon: Int)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val summary: MonthlySummary? = null,
    val budgetStatus: List<BudgetStatus> = emptyList(),
    val recurring: RecurringSummary? = null,
    val currentYear: Int = LocalDate.now().year,
    val currentMonth: Int = LocalDate.now().monthValue,
    val error: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardDataUseCase: GetDashboardDataUseCase,
    private val getRecurringStatus: GetRecurringStatusUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    /** [userRefresh] = pull-to-refresh: muestra el indicador sin vaciar la pantalla. */
    fun load(userRefresh: Boolean = false) {
        loadJob?.cancel() // la respuesta de un mes anterior nunca pisa al actual
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = userRefresh, error = null) }
            val (year, month) = _uiState.value.run { currentYear to currentMonth }
            try {
                val data = getDashboardDataUseCase(year, month)
                val recurring = runCatching { getRecurringStatus() }.getOrNull()?.let { list ->
                    RecurringSummary(
                        total = list.size,
                        paid = list.count { it.status.state == PayState.PAGADO },
                        overdue = list.count { it.status.state == PayState.ATRASADO },
                        dueSoon = list.count { it.status.state == PayState.POR_VENCER },
                    )
                }
                _uiState.update {
                    it.copy(
                        isLoading = false, isRefreshing = false,
                        summary = data.summary, budgetStatus = data.budgetStatus,
                        recurring = recurring ?: it.recurring,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = friendlyError(e)) }
            }
        }
    }

    fun nextMonth() = shiftMonth(1)
    fun previousMonth() = shiftMonth(-1)

    private fun shiftMonth(delta: Int) {
        val (y, m) = _uiState.value.run { shiftYearMonth(currentYear, currentMonth, delta) }
        // Vaciamos para no mostrar cifras del mes anterior bajo la etiqueta nueva.
        _uiState.update { it.copy(currentYear = y, currentMonth = m, isLoading = true, summary = null, budgetStatus = emptyList()) }
        load()
    }
}
