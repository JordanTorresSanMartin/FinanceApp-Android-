package com.example.financeapp.ui.viewmodel.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.model.TxType
import com.example.financeapp.domain.budgetStatus
import com.example.financeapp.domain.repository.FinanceRepository
import com.example.financeapp.util.friendlyError
import com.example.financeapp.util.shiftYearMonth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class CalculatedBudget(
    val categoryId: String,
    val categoryName: String,
    val icon: String?,
    val color: String?,
    val budgetAmount: Double,
    val spent: Double,
    val pctUsed: Double,
    val status: String, // BUDGET_OK | BUDGET_WARNING | BUDGET_EXCEEDED
)

data class BudgetsUiState(
    val items: List<CalculatedBudget> = emptyList(),
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val isLoading: Boolean = true,
    val savingId: String? = null,
    val currentYear: Int = LocalDate.now().year,
    val currentMonth: Int = LocalDate.now().monthValue,
    val error: String? = null,
    /** Presupuesto total del mes anterior (si este mes está vacío, se ofrece copiarlo). */
    val previousTotal: Double = 0.0,
    val copying: Boolean = false,
) {
    val totalAvailable: Double get() = totalBudget - totalSpent
}

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetsUiState())
    val uiState: StateFlow<BudgetsUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    /** Refresca sin vaciar la lista: así guardar un monto no pierde scroll ni foco. */
    private val reload = com.example.financeapp.util.ReloadThrottle()

    /** Al volver a la pantalla: recarga solo si pasaron 30 s o la app cambió datos. */
    fun loadIfStale() { if (reload.ready()) load() }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            val (year, month) = _uiState.value.run { currentYear to currentMonth }
            try {
                // coroutineScope: si una consulta falla, el error llega al catch en vez de tumbar la app.
                val items = coroutineScope {
                    val catsD = async { repository.getCategories().filter { it.type == TxType.GASTO || it.type == "ambos" } }
                    val budgetsD = async { repository.getBudgetsForMonth(year, month) }
                    val expensesD = async { repository.getTransactionsByMonth(year, month).filter { it.type == TxType.GASTO } }
                    val budgets = budgetsD.await().associateBy { it.categoryId }
                    val spentByCat = expensesD.await().groupBy { it.categoryId }.mapValues { (_, l) -> l.sumOf { it.amount } }

                    catsD.await().mapNotNull { cat ->
                        val id = cat.id ?: return@mapNotNull null
                        val budgetAmount = budgets[id]?.amount ?: 0.0
                        val spent = spentByCat[id] ?: 0.0
                        val pct = when {
                            budgetAmount > 0 -> spent / budgetAmount * 100
                            spent > 0 -> 100.0
                            else -> 0.0
                        }
                        CalculatedBudget(id, cat.name, cat.icon, cat.color, budgetAmount, spent, pct, budgetStatus(pct))
                    }
                }
                val total = items.sumOf { b -> b.budgetAmount }
                val previousTotal = if (total > 0) 0.0 else runCatching {
                    val (py, pm) = shiftYearMonth(year, month, -1)
                    repository.getBudgetsForMonth(py, pm).sumOf { it.amount }
                }.getOrDefault(0.0)
                _uiState.update {
                    it.copy(
                        previousTotal = previousTotal,
                        items = items,
                        totalBudget = items.sumOf { b -> b.budgetAmount },
                        totalSpent = items.sumOf { b -> b.spent },
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = friendlyError(e)) }
            }
        }
    }

    fun updateBudgetAmount(categoryId: String, amount: Double) {
        val current = _uiState.value.items.firstOrNull { it.categoryId == categoryId }
        if (current?.budgetAmount == amount) return // sin cambios: no tocar la red
        val (year, month) = _uiState.value.run { currentYear to currentMonth }
        viewModelScope.launch {
            _uiState.update { it.copy(savingId = categoryId) }
            try {
                repository.upsertBudget(categoryId, year, month, amount)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = friendlyError(e, "No se pudo guardar el presupuesto")) }
            } finally {
                _uiState.update { it.copy(savingId = null) }
                load()
            }
        }
    }

    /** Copia los montos del mes anterior a este (para no configurarlo de nuevo cada mes). */
    fun copyFromPrevious() {
        val (year, month) = _uiState.value.run { currentYear to currentMonth }
        viewModelScope.launch {
            _uiState.update { it.copy(copying = true) }
            try {
                val (py, pm) = shiftYearMonth(year, month, -1)
                repository.getBudgetsForMonth(py, pm).filter { it.amount > 0 }
                    .forEach { repository.upsertBudget(it.categoryId, year, month, it.amount) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = friendlyError(e, "No se pudo copiar el presupuesto")) }
            } finally {
                _uiState.update { it.copy(copying = false) }
                load()
            }
        }
    }

    fun consumeError() = _uiState.update { it.copy(error = null) }

    fun nextMonth() = shiftMonth(1)
    fun previousMonth() = shiftMonth(-1)

    private fun shiftMonth(delta: Int) {
        val (y, m) = _uiState.value.run { shiftYearMonth(currentYear, currentMonth, delta) }
        _uiState.update { it.copy(currentYear = y, currentMonth = m, isLoading = true, items = emptyList()) }
        load()
    }
}
