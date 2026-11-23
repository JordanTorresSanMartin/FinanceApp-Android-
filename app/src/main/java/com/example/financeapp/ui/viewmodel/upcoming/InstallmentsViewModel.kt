package com.example.financeapp.ui.viewmodel.upcoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.local.InstallmentStore
import com.example.financeapp.domain.InstallmentPlan
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InstallmentsViewModel @Inject constructor(private val store: InstallmentStore) : ViewModel() {
    val plans: StateFlow<List<InstallmentPlan>> = store.plans

    fun save(plan: InstallmentPlan) { viewModelScope.launch { store.upsert(plan) } }
    fun delete(id: String) { viewModelScope.launch { store.delete(id) } }
    fun pay(id: String) { viewModelScope.launch { store.pay(id) } }
    fun undo(id: String) { viewModelScope.launch { store.undoLast(id) } }
}
