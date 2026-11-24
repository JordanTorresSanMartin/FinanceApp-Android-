package com.example.financeapp.ui.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.domain.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val email: String = "",
    val name: String = "Usuario",
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: FinanceRepository,
    private val tracking: com.example.financeapp.data.local.TrackingStore,
) : ViewModel() {

    /** Desde qué mes se lleva la cuenta y con cuánto se parte (null = desde siempre). */
    val trackingStart: java.time.YearMonth? get() = tracking.start
    val initialBalance: Long get() = tracking.initialBalance.toLong()

    private val _categories = kotlinx.coroutines.flow.MutableStateFlow<List<com.example.financeapp.data.model.Category>>(emptyList())
    val categories: kotlinx.coroutines.flow.StateFlow<List<com.example.financeapp.data.model.Category>> = _categories
    private val _message = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val message: kotlinx.coroutines.flow.StateFlow<String?> = _message
    fun consumeMessage() { _message.value = null }

    fun loadCategories() {
        viewModelScope.launch {
            runCatching { repository.getCategories() }
                .onSuccess { _categories.value = it }
                .onFailure { _message.value = com.example.financeapp.util.friendlyError(it, "No se pudieron cargar las categorías.") }
        }
    }

    fun saveCategory(id: String?, name: String, type: String, icon: String, color: String) {
        viewModelScope.launch {
            runCatching { repository.saveCategory(id, name.trim(), type, icon, color) }
                .onSuccess { _message.value = if (id == null) "Categoría \"${name.trim()}\" creada" else "Categoría actualizada"; loadCategories() }
                .onFailure { _message.value = com.example.financeapp.util.friendlyError(it, "No se pudo guardar la categoría.") }
        }
    }

    fun saveTracking(start: java.time.YearMonth?, initial: Long) {
        tracking.save(start, initial)
        com.example.financeapp.util.DataVersion.bump() // el inicio recalcula el saldo al volver
    }

    val uiState: ProfileUiState = repository.currentUserEmail().orEmpty().let { email ->
        ProfileUiState(email = email, name = email.substringBefore('@').ifBlank { "Usuario" })
    }

    /** La navegación al login la hace FinanceNavHost al cambiar la sesión. */
    fun logout() {
        viewModelScope.launch { runCatching { repository.signOut() } }
    }
}
