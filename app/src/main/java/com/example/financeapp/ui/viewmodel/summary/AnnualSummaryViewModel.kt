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
import java.util.Calendar
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
        AnnualSummaryUiState(currentYear = Calendar.getInstance().get(Calendar.YEAR))
    )
    val uiState: StateFlow<AnnualSummaryUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val summaries = repository.getAnnualSummary(_uiState.value.currentYear)
                _uiState.value = _uiState.value.copy(summaries = summaries, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun nextYear() {
        _uiState.value = _uiState.value.copy(currentYear = _uiState.value.currentYear + 1)
        load()
    }

    fun previousYear() {
        _uiState.value = _uiState.value.copy(currentYear = _uiState.value.currentYear - 1)
        load()
    }
}
