package com.example.financeapp.ui.viewmodel.upcoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.model.Category
import com.example.financeapp.data.model.RecurringPayment
import com.example.financeapp.data.model.TxType
import com.example.financeapp.domain.repository.FinanceRepository
import com.example.financeapp.util.todayLocalDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecurringUiState(
    val isLoading: Boolean = true,
    val items: List<RecurringPayment> = emptyList(),
    val monthlyTotal: Double = 0.0,
    val categories: List<Category> = emptyList(),
    val message: String? = null,
)

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecurringUiState())
    val uiState: StateFlow<RecurringUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val items = repository.getRecurringPayments()
                val cats = runCatching { repository.getCategories() }.getOrDefault(emptyList())
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    items = items,
                    monthlyTotal = items.sumOf { it.monthlyEquivalent },
                    categories = cats,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, message = e.message)
            }
        }
    }

    fun save(
        id: String?, name: String, amount: Double, categoryId: String?,
        frequency: String, billingDay: Int?, notes: String?,
    ) {
        viewModelScope.launch {
            try {
                repository.upsertRecurringPayment(id, name.trim(), amount, categoryId, frequency, billingDay, notes?.trim()?.ifBlank { null })
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = e.message ?: "No se pudo guardar")
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteRecurringPayment(id)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = e.message ?: "No se pudo eliminar")
            }
        }
    }

    /** Crea la transacción real (gasto) del pago de este período. */
    fun registerPayment(item: RecurringPayment) {
        viewModelScope.launch {
            try {
                repository.insertTransaction(
                    date = todayLocalDate(),
                    description = item.name,
                    categoryId = item.categoryId,
                    type = TxType.GASTO,
                    amount = item.amount,
                    notes = "Pago recurrente",
                )
                _uiState.value = _uiState.value.copy(message = "Pago de \"${item.name}\" registrado")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = e.message ?: "No se pudo registrar el pago")
            }
        }
    }

    fun consumeMessage() { _uiState.value = _uiState.value.copy(message = null) }
}
