package com.example.financeapp.ui.viewmodel.upcoming

import com.example.financeapp.ui.widget.FixedPaymentsWidgetStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.model.Category
import com.example.financeapp.data.model.RecurringPayment
import com.example.financeapp.domain.PayState
import com.example.financeapp.domain.RecurringWithStatus
import com.example.financeapp.domain.repository.FinanceRepository
import com.example.financeapp.domain.use_case.GetRecurringStatusUseCase
import com.example.financeapp.util.friendlyError
import com.example.financeapp.util.todayLocalDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecurringUiState(
    val isLoading: Boolean = true,
    val items: List<RecurringWithStatus> = emptyList(),
    val monthlyTotal: Double = 0.0,
    val categories: List<Category> = emptyList(),
    /** Ids con un registro de pago en curso (evita doble toque). */
    val registering: Set<String> = emptySet(),
    val message: String? = null,
) {
    val paidCount: Int get() = items.count { it.status.state == PayState.PAGADO }
    val pendingAmount: Double get() = items.filter { it.status.state != PayState.PAGADO }.sumOf { it.item.amount }
}

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val repository: FinanceRepository,
    private val getRecurringStatus: GetRecurringStatusUseCase,
    private val widgetStore: FixedPaymentsWidgetStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecurringUiState())
    val uiState: StateFlow<RecurringUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val items = getRecurringStatus()
                val cats = runCatching { repository.getCategories() }.getOrDefault(_uiState.value.categories)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        items = items,
                        monthlyTotal = items.sumOf { r -> r.item.monthlyEquivalent },
                        categories = cats,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, message = friendlyError(e)) }
            }
        }
    }

    /** Recarga la lista y el widget "Pagos fijos". */
    private fun afterChange() {
        load()
        viewModelScope.launch { widgetStore.refresh() }
    }

    fun save(
        id: String?, name: String, amount: Double, categoryId: String?,
        frequency: String, billingDay: Int?, notes: String?,
    ) {
        viewModelScope.launch {
            try {
                repository.upsertRecurringPayment(
                    id, name.trim(), amount, categoryId, frequency,
                    billingDay?.coerceIn(1, 31), notes?.trim()?.ifBlank { null },
                )
                _uiState.update { it.copy(message = if (id == null) "Pago fijo agregado" else "Cambios guardados") }
                afterChange()
            } catch (e: Exception) {
                _uiState.update { it.copy(message = friendlyError(e, "No se pudo guardar")) }
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteRecurringPayment(id)
                _uiState.update { it.copy(message = "Pago fijo eliminado") }
                afterChange()
            } catch (e: Exception) {
                _uiState.update { it.copy(message = friendlyError(e, "No se pudo eliminar")) }
            }
        }
    }

    /** Crea la transacción (gasto) del período. La UI confirma antes si ya estaba pagado. */
    fun registerPayment(item: RecurringPayment) {
        val id = item.id ?: return
        if (id in _uiState.value.registering) return
        viewModelScope.launch {
            _uiState.update { it.copy(registering = it.registering + id) }
            try {
                repository.registerRecurringPayment(item, todayLocalDate())
                _uiState.update { it.copy(message = "Pago de \"${item.name}\" registrado") }
                afterChange()
            } catch (e: Exception) {
                _uiState.update { it.copy(message = friendlyError(e, "No se pudo registrar el pago")) }
            } finally {
                _uiState.update { it.copy(registering = it.registering - id) }
            }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }
}
