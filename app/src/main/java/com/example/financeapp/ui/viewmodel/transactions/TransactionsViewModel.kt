package com.example.financeapp.ui.viewmodel.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.model.Transaction
import com.example.financeapp.data.model.TxType
import com.example.financeapp.domain.repository.FinanceRepository
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

const val FILTER_ALL = "todos"

data class TransactionsUiState(
    val transactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val currentYear: Int = LocalDate.now().year,
    val currentMonth: Int = LocalDate.now().monthValue,
    val searchQuery: String = "",
    val filterType: String = FILTER_ALL, // FILTER_ALL | TxType.INGRESO | TxType.GASTO
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val error: String? = null,
) {
    val isFiltering: Boolean get() = searchQuery.isNotBlank() || filterType != FILTER_ALL
}

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    fun load(userRefresh: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = userRefresh, error = null) }
            val (year, month) = _uiState.value.run { currentYear to currentMonth }
            try {
                val txs = repository.getTransactionsByMonth(year, month)
                _uiState.update { it.copy(transactions = txs, isLoading = false, isRefreshing = false).applyFilter() }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = friendlyError(e)) }
            }
        }
    }

    fun onSearchQueryChange(query: String) = _uiState.update { it.copy(searchQuery = query).applyFilter() }
    fun onFilterTypeChange(type: String) = _uiState.update { it.copy(filterType = type).applyFilter() }

    fun nextMonth() = shiftMonth(1)
    fun previousMonth() = shiftMonth(-1)

    private fun shiftMonth(delta: Int) {
        val (y, m) = _uiState.value.run { shiftYearMonth(currentYear, currentMonth, delta) }
        _uiState.update { it.copy(currentYear = y, currentMonth = m, isLoading = true, transactions = emptyList()).applyFilter() }
        load()
    }

    private fun TransactionsUiState.applyFilter(): TransactionsUiState {
        val q = searchQuery.trim()
        val filtered = transactions.filter { tx ->
            val matchesSearch = q.isEmpty() ||
                tx.description.contains(q, ignoreCase = true) ||
                tx.categories?.name?.contains(q, ignoreCase = true) == true
            val matchesType = filterType == FILTER_ALL || tx.type == filterType
            matchesSearch && matchesType
        }
        val income = filtered.filter { it.type == TxType.INGRESO }.sumOf { it.amount }
        val expenses = filtered.filter { it.type == TxType.GASTO }.sumOf { it.amount }
        return copy(filteredTransactions = filtered, totalIncome = income, totalExpenses = expenses)
    }
}
