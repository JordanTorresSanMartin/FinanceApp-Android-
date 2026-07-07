package com.example.financeapp.ui.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthMode { LOGIN, REGISTER }

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val info: String? = null,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: Auth
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                auth.signInWith(Email) {
                    this.email = email.trim().lowercase()
                    this.password = password
                }
                _uiState.value = AuthUiState(isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = e.message ?: "Error al iniciar sesión")
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                auth.signUpWith(Email) {
                    this.email = email.trim().lowercase()
                    this.password = password
                }
                // Con confirmación de correo activada, signUp NO crea sesión: no
                // navegamos a la app sin sesión (causaría datos vacíos y 401).
                if (auth.currentSessionOrNull() != null) {
                    _uiState.value = AuthUiState(isSuccess = true)
                } else {
                    _uiState.value = AuthUiState(info = "Cuenta creada. Revisa tu correo para confirmar y luego inicia sesión.")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = e.message ?: "Error al registrarse")
            }
        }
    }

    /** Espera a que la sesión persistida termine de cargar antes de decidir la ruta. */
    suspend fun isUserLoggedIn(): Boolean {
        runCatching { auth.awaitInitialization() }
        return auth.currentSessionOrNull() != null
    }

    fun logout() {
        viewModelScope.launch {
            auth.signOut()
        }
    }
}
