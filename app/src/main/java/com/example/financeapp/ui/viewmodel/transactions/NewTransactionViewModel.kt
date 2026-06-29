package com.example.financeapp.ui.viewmodel.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.model.Category
import com.example.financeapp.data.model.Transaction
import com.example.financeapp.domain.repository.FinanceRepository
import com.example.financeapp.domain.use_case.SaveTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import java.util.*
import javax.inject.Inject

data class NewTransactionUiState(
    val amount: String = "",
    val description: String = "",
    val type: String = "expense",
    val selectedCategoryId: String? = null,
    val notes: String = "",
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NewTransactionViewModel @Inject constructor(
    private val repository: FinanceRepository,
    private val saveTransactionUseCase: SaveTransactionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewTransactionUiState())
    val uiState: StateFlow<NewTransactionUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val categories = repository.getCategories()
                _uiState.value = _uiState.value.copy(categories = categories)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Error al cargar categorías")
            }
        }
    }

    fun onAmountChange(amount: String) {
        if (amount.all { it.isDigit() || it == '.' }) {
            _uiState.value = _uiState.value.copy(amount = amount)
        }
    }

    fun onDescriptionChange(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun onTypeChange(type: String) {
        _uiState.value = _uiState.value.copy(type = type, selectedCategoryId = null)
    }

    fun onCategorySelected(categoryId: String) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
    }

    fun onNotesChange(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun saveTransaction() {
        val state = _uiState.value
        if (state.amount.isBlank() || state.description.isBlank() || state.selectedCategoryId == null) {
            _uiState.value = _uiState.value.copy(error = "Completa todos los campos")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val calendar = Calendar.getInstance()
                val transaction = Transaction(
                    date = LocalDate(
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH) + 1,
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ),
                    description = state.description,
                    categoryId = state.selectedCategoryId,
                    type = state.type,
                    amount = state.amount.toDouble(),
                    notes = state.notes
                )
                saveTransactionUseCase(transaction)
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}
