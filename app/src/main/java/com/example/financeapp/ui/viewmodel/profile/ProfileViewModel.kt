package com.example.financeapp.ui.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.domain.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val email: String = "",
    val name: String = "Usuario",
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        val email = repository.currentUserEmail().orEmpty()
        _uiState.value = ProfileUiState(
            email = email,
            name = email.substringBefore('@').ifBlank { "Usuario" },
        )
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            try { repository.signOut() } catch (_: Exception) {}
            onDone()
        }
    }
}
