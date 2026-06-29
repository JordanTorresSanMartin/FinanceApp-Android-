package com.example.financeapp.ui.viewmodel.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.model.Transaction
import com.example.financeapp.domain.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class TransactionsUiState(
    val transactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val currentYear: Int,
    val currentMonth: Int,
    val searchQuery: String = "",
    val filterType: String = "All",
    val error: String? = null
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TransactionsUiState(
            currentYear = Calendar.getInstance().get(Calendar.YEAR),
            currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        )
    )
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    init {
        loadTransactions()
    }

    fun loadTransactions() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)
            try {
                val transactions = repository.getTransactionsByMonth(state.currentYear, state.currentMonth)
                _uiState.value = _uiState.value.copy(
                    transactions = transactions,
                    isLoading = false
                ).applyFilter()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query).applyFilter()
    }

    fun onFilterTypeChange(type: String) {
        _uiState.value = _uiState.value.copy(filterType = type).applyFilter()
    }

    fun nextMonth() {
        var year = _uiState.value.currentYear
        var month = _uiState.value.currentMonth + 1
        if (month > 12) {
            month = 1
            year++
        }
        _uiState.value = _uiState.value.copy(currentYear = year, currentMonth = month)
        loadTransactions()
    }

    fun previousMonth() {
        var year = _uiState.value.currentYear
        var month = _uiState.value.currentMonth - 1
        if (month < 1) {
            month = 12
            year--
        }
        _uiState.value = _uiState.value.copy(currentYear = year, currentMonth = month)
        loadTransactions()
    }

    private fun TransactionsUiState.applyFilter(): TransactionsUiState {
        val filtered = transactions.filter { transaction ->
            val matchesSearch = transaction.description.contains(searchQuery, ignoreCase = true)
            val matchesType = when (filterType) {
                "Income" -> transaction.type == "income"
                "Expense" -> transaction.type == "expense"
                else -> true
            }
            matchesSearch && matchesType
        }
        return this.copy(filteredTransactions = filtered)
    }
}
