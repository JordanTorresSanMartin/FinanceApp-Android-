package com.example.financeapp.ui.viewmodel.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.model.MonthlySummary
import com.example.financeapp.domain.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import com.example.financeapp.util.friendlyError
import javax.inject.Inject

data class AnnualSummaryUiState(
    val summaries: List<MonthlySummary> = emptyList(),
    val isLoading: Boolean = true,
    val currentYear: Int,
    val error: String? = null,
)

@HiltViewModel
class AnnualSummaryViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AnnualSummaryUiState(currentYear = java.time.LocalDate.now().year)
    )
    val uiState: StateFlow<AnnualSummaryUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    private val reload = com.example.financeapp.util.ReloadThrottle()

    /** Al volver a la pantalla: recarga solo si pasaron 30 s o la app cambió datos. */
    fun loadIfStale() { if (reload.ready()) load() }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val year = _uiState.value.currentYear
            _uiState.value = _uiState.value.copy(error = null)
            try {
                val summaries = repository.getAnnualSummary(year)
                _uiState.value = _uiState.value.copy(summaries = summaries, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = friendlyError(e))
            }
        }
    }

    fun nextYear() = shiftYear(1)
    fun previousYear() = shiftYear(-1)

    private fun shiftYear(delta: Int) {
        _uiState.value = _uiState.value.copy(
            currentYear = _uiState.value.currentYear + delta, isLoading = true, summaries = emptyList(),
        )
        load()
    }
}
