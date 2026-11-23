package com.example.financeapp.ui.viewmodel.transactions

import com.example.financeapp.util.friendlyError
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.model.Category
import com.example.financeapp.data.model.TxType
import com.example.financeapp.domain.repository.FinanceRepository
import com.example.financeapp.util.todayLocalDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import javax.inject.Inject

data class NewTransactionUiState(
    val amount: String = "",            // solo dígitos
    val description: String = "",
    val type: String = TxType.GASTO,    // "gasto" | "ingreso"
    val dateText: String = todayLocalDate().toString(), // ISO YYYY-MM-DD
    val selectedCategoryId: String? = null,
    val notes: String = "",
    val categories: List<Category> = emptyList(),
    val loadingCategories: Boolean = true,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class NewTransactionViewModel @Inject constructor(
    private val repository: FinanceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewTransactionUiState())
    val uiState: StateFlow<NewTransactionUiState> = _uiState.asStateFlow()

    private var initialTypeApplied = false

    init {
        loadCategories()
    }

    /** Tipo inicial provisto por el widget (?type=gasto|ingreso). Se aplica una vez. */
    fun setInitialType(type: String?) {
        if (initialTypeApplied) return
        initialTypeApplied = true
        if (type == TxType.INGRESO || type == TxType.GASTO) {
            _uiState.value = _uiState.value.copy(type = type)
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val cats = repository.getCategories()
                _uiState.value = _uiState.value.copy(categories = cats, loadingCategories = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loadingCategories = false, error = "Error al cargar categorías")
            }
        }
    }

    fun onAmountChange(raw: String) {
        _uiState.value = _uiState.value.copy(amount = raw.filter { it.isDigit() })
    }

    fun onDescriptionChange(value: String) { _uiState.value = _uiState.value.copy(description = value) }
    fun onTypeChange(type: String) { _uiState.value = _uiState.value.copy(type = type, selectedCategoryId = null) }
    fun onCategorySelected(id: String) { _uiState.value = _uiState.value.copy(selectedCategoryId = id) }
    fun onNotesChange(value: String) { _uiState.value = _uiState.value.copy(notes = value) }
    fun onDateChange(iso: String) { _uiState.value = _uiState.value.copy(dateText = iso) }
    fun setToday() { _uiState.value = _uiState.value.copy(dateText = todayLocalDate().toString()) }
    fun setYesterday() { _uiState.value = _uiState.value.copy(dateText = java.time.LocalDate.now().minusDays(1).toString()) }

    fun consumeError() { _uiState.value = _uiState.value.copy(error = null) }

    /** true si el usuario escribió algo (para confirmar antes de descartar). */
    val hasChanges: Boolean
        get() = _uiState.value.let { it.amount.isNotEmpty() || it.description.isNotBlank() || it.notes.isNotBlank() }

    fun save() {
        val s = _uiState.value
        val amountValue = s.amount.toLongOrNull() ?: 0L
        when {
            s.description.isBlank() -> { _uiState.value = s.copy(error = "Ingresa una descripción."); return }
            amountValue <= 0 -> { _uiState.value = s.copy(error = "El monto debe ser mayor a 0."); return }
            s.selectedCategoryId == null -> { _uiState.value = s.copy(error = "Selecciona una categoría."); return }
        }
        val date = try { LocalDate.parse(s.dateText) } catch (e: Exception) {
            _uiState.value = s.copy(error = "Fecha inválida (usa YYYY-MM-DD)."); return
        }

        if (s.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                repository.insertTransaction(
                    date = date,
                    description = s.description.trim(),
                    categoryId = s.selectedCategoryId,
                    type = s.type,
                    amount = amountValue.toDouble(),
                    notes = s.notes.trim().ifBlank { null },
                )
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = friendlyError(e, "No se pudo guardar"))
            }
        }
    }
}
