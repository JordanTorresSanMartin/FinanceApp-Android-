package com.example.financeapp.ui.viewmodel.upcoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.local.InstallmentStore
import com.example.financeapp.domain.InstallmentPlan
import com.example.financeapp.util.friendlyError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InstallmentsViewModel @Inject constructor(private val store: InstallmentStore) : ViewModel() {
    val plans: StateFlow<List<InstallmentPlan>> = store.plans

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init { run("No se pudieron actualizar tus cuotas.") { store.refresh() } }

    fun save(plan: InstallmentPlan) = run("No se pudo guardar.") { store.upsert(plan) }
    fun delete(id: String) = run("No se pudo eliminar.") { store.delete(id) }
    fun pay(id: String) = run("No se pudo marcar la cuota.") { store.pay(id) }
    fun undo(id: String) = run("No se pudo deshacer.") { store.undoLast(id) }
    fun consumeMessage() { _message.value = null }

    private fun run(fallback: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }.onFailure { _message.value = friendlyError(it, fallback) }
        }
    }
}
