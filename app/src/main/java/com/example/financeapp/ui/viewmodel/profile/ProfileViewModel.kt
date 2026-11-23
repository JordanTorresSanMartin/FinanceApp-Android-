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
    private val repository: FinanceRepository
) : ViewModel() {

    val uiState: ProfileUiState = repository.currentUserEmail().orEmpty().let { email ->
        ProfileUiState(email = email, name = email.substringBefore('@').ifBlank { "Usuario" })
    }

    /** La navegación al login la hace FinanceNavHost al cambiar la sesión. */
    fun logout() {
        viewModelScope.launch { runCatching { repository.signOut() } }
    }
}
