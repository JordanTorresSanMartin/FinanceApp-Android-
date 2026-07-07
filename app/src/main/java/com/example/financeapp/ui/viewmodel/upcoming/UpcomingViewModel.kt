package com.example.financeapp.ui.viewmodel.upcoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.model.ForecastMonth
import com.example.financeapp.data.model.Installment
import com.example.financeapp.data.model.Statement
import com.example.financeapp.domain.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import javax.inject.Inject

data class UpcomingUiState(
    val isLoading: Boolean = true,
    val statements: List<Statement> = emptyList(),
    val installments: List<Installment> = emptyList(),
    val monthlyTotal: Double = 0.0,
    val billedTotal: Double = 0.0,
    val nextDue: LocalDate? = null,
    val forecast: List<ForecastMonth> = emptyList(),
    val uploading: Boolean = false,
    val passwordVisible: Boolean = false,
    val passwordError: String? = null,
    val message: String? = null,
)

@HiltViewModel
class UpcomingViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UpcomingUiState())
    val uiState: StateFlow<UpcomingUiState> = _uiState.asStateFlow()

    // PDF pendiente de contraseña (no va en el estado para evitar recomposiciones).
    private var pendingPdf: Pair<String, String>? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val allStatements = repository.getStatements()
                // Último estado de cuenta por banco (snapshot vigente de cada tarjeta).
                val latestByBank = LinkedHashMap<String, Statement>()
                for (s in allStatements) {
                    val key = s.bank ?: "banco"
                    if (!latestByBank.containsKey(key)) latestByBank[key] = s
                }
                val latest = latestByBank.values.toList()

                val installments = if (latest.isEmpty()) emptyList() else {
                    repository.getInstallments(latest.map { it.id })
                        .filter { it.installmentCurrent < it.installmentTotal }
                }

                // Forecast combinado por mes (suma entre tarjetas, preservando orden).
                val order = ArrayList<String>()
                val map = LinkedHashMap<String, Double>()
                for (s in latest) for (f in s.forecast.orEmpty()) {
                    if (!map.containsKey(f.month)) order.add(f.month)
                    map[f.month] = (map[f.month] ?: 0.0) + f.amount
                }
                val forecast = order.map { ForecastMonth(it, map[it] ?: 0.0) }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statements = latest,
                    installments = installments,
                    monthlyTotal = installments.sumOf { it.monthlyAmount },
                    billedTotal = latest.sumOf { it.totalAmount },
                    nextDue = latest.mapNotNull { it.dueDate }.minOrNull(),
                    forecast = forecast,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, message = e.message)
            }
        }
    }

    fun uploadPdf(base64: String, filename: String) = processPdf(base64, filename, null)

    fun submitPassword(password: String) {
        val pending = pendingPdf ?: return
        if (password.isBlank() || _uiState.value.uploading) return
        processPdf(pending.first, pending.second, password)
    }

    fun cancelPassword() {
        pendingPdf = null
        _uiState.value = _uiState.value.copy(passwordVisible = false, passwordError = null)
    }

    fun consumeMessage() { _uiState.value = _uiState.value.copy(message = null) }

    private fun processPdf(base64: String, filename: String, password: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(uploading = true)
            try {
                val result = repository.uploadStatementPdf(base64, filename, password)
                if (!result.ok) {
                    when (result.code) {
                        "PASSWORD_REQUIRED", "PASSWORD_INCORRECT" -> {
                            pendingPdf = base64 to filename
                            _uiState.value = _uiState.value.copy(
                                uploading = false,
                                passwordVisible = true,
                                passwordError = if (result.code == "PASSWORD_INCORRECT")
                                    "Contraseña incorrecta. Inténtalo de nuevo." else null,
                            )
                        }
                        else -> _uiState.value = _uiState.value.copy(
                            uploading = false,
                            message = result.error ?: "No se pudo procesar el PDF.",
                        )
                    }
                    return@launch
                }
                pendingPdf = null
                val tag = buildString {
                    append(result.bank ?: "Banco")
                    if (!result.cardLast4.isNullOrBlank()) append(" ••").append(result.cardLast4)
                }
                _uiState.value = _uiState.value.copy(
                    uploading = false,
                    passwordVisible = false,
                    passwordError = null,
                    message = "$tag: ${result.installments} compras en cuotas detectadas.",
                )
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(uploading = false, message = e.message ?: "No se pudo subir el PDF.")
            }
        }
    }
}
