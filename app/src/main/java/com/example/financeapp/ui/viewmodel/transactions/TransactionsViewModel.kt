package com.example.financeapp.ui.viewmodel.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.model.Transaction
import com.example.financeapp.data.model.TxType
import com.example.financeapp.domain.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class TransactionsUiState(
    val transactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = true,
    val currentYear: Int,
    val currentMonth: Int,
    val searchQuery: String = "",
    val filterType: String = "todos", // "todos" | "ingreso" | "gasto"
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val error: String? = null,
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TransactionsUiState(
            currentYear = Calendar.getInstance().get(Calendar.YEAR),
            currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1,
        )
    )
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val txs = repository.getTransactionsByMonth(_uiState.value.currentYear, _uiState.value.currentMonth)
                _uiState.value = _uiState.value.copy(transactions = txs, isLoading = false).applyFilter()
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

    private fun TransactionsUiState.applyFilter(): TransactionsUiState {
        val filtered = transactions.filter { tx ->
            val matchesSearch = searchQuery.isBlank() ||
                tx.description.contains(searchQuery, ignoreCase = true)
            val matchesType = filterType == "todos" || tx.type == filterType
            matchesSearch && matchesType
        }
        val income = filtered.filter { it.type == TxType.INGRESO }.sumOf { it.amount }
        val expenses = filtered.filter { it.type == TxType.GASTO }.sumOf { it.amount }
        return copy(filteredTransactions = filtered, totalIncome = income, totalExpenses = expenses)
    }
}
