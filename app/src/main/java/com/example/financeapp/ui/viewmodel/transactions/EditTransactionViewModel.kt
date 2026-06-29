package com.example.financeapp.ui.viewmodel.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.model.Category
import com.example.financeapp.data.model.TxType
import com.example.financeapp.domain.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import javax.inject.Inject

data class EditTransactionUiState(
    val loading: Boolean = true,
    val amount: String = "",
    val description: String = "",
    val type: String = TxType.GASTO,
    val dateText: String = "",
    val selectedCategoryId: String? = null,
    val notes: String = "",
    val source: String? = null,
    val categories: List<Category> = emptyList(),
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isSuccess: Boolean = false,
    val notFound: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class EditTransactionViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditTransactionUiState())
    val uiState: StateFlow<EditTransactionUiState> = _uiState.asStateFlow()

    private var transactionId: String? = null
    private var loaded = false

    fun load(id: String) {
        if (loaded) return
        loaded = true
        transactionId = id
        viewModelScope.launch {
            try {
                val tx = repository.getTransactionById(id)
                val cats = repository.getCategories()
                if (tx == null) {
                    _uiState.value = _uiState.value.copy(loading = false, notFound = true)
                    return@launch
                }
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    amount = tx.amount.toLong().toString(),
                    description = tx.description,
                    type = tx.type,
                    dateText = tx.date.toString(),
                    selectedCategoryId = tx.categoryId,
                    notes = tx.notes.orEmpty(),
                    source = tx.source,
                    categories = cats,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun onAmountChange(raw: String) { _uiState.value = _uiState.value.copy(amount = raw.filter { it.isDigit() }) }
    fun onDescriptionChange(value: String) { _uiState.value = _uiState.value.copy(description = value) }
    fun onTypeChange(type: String) { _uiState.value = _uiState.value.copy(type = type, selectedCategoryId = null) }
    fun onCategorySelected(id: String) { _uiState.value = _uiState.value.copy(selectedCategoryId = id) }
    fun onNotesChange(value: String) { _uiState.value = _uiState.value.copy(notes = value) }
    fun onDateChange(iso: String) { _uiState.value = _uiState.value.copy(dateText = iso) }
    fun consumeError() { _uiState.value = _uiState.value.copy(error = null) }

    fun save() {
        val s = _uiState.value
        val id = transactionId ?: return
        val amountValue = s.amount.toLongOrNull() ?: 0L
        when {
            s.description.isBlank() -> { _uiState.value = s.copy(error = "Ingresa una descripción."); return }
            amountValue <= 0 -> { _uiState.value = s.copy(error = "El monto debe ser mayor a 0."); return }
            s.selectedCategoryId == null -> { _uiState.value = s.copy(error = "Selecciona una categoría."); return }
        }
        val date = try { LocalDate.parse(s.dateText) } catch (e: Exception) {
            _uiState.value = s.copy(error = "Fecha inválida (usa YYYY-MM-DD)."); return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                repository.updateTransaction(
                    id = id,
                    date = date,
                    description = s.description.trim(),
                    categoryId = s.selectedCategoryId,
                    type = s.type,
                    amount = amountValue.toDouble(),
                    notes = s.notes.trim().ifBlank { null },
                )
                _uiState.value = _uiState.value.copy(isSaving = false, isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message ?: "Error al guardar")
            }
        }
    }

    fun delete() {
        val id = transactionId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true, error = null)
            try {
                repository.deleteTransaction(id)
                _uiState.value = _uiState.value.copy(isDeleting = false, isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isDeleting = false, error = e.message ?: "No se pudo eliminar")
            }
        }
    }
}
