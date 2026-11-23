package com.example.financeapp.ui.viewmodel.auth

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import com.example.financeapp.ui.widget.FixedPaymentsWidgetStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.util.friendlyError
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthMode { LOGIN, REGISTER }

data class AuthUiState(
    val isLoading: Boolean = false,
    val info: String? = null,
    val error: String? = null,
)

private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: Auth,
    private val fixedWidget: FixedPaymentsWidgetStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * null = aún leyendo la sesión guardada. Un fallo al refrescar el token (sin red)
     * NO cierra la sesión: solo NotAuthenticated manda al login.
     */
    val isLoggedIn: StateFlow<Boolean?> = auth.sessionStatus
        .map {
            when (it) {
                is SessionStatus.Initializing -> null
                is SessionStatus.NotAuthenticated -> false
                else -> true
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // El widget "Pagos fijos" sigue a la sesión: se llena al entrar y se vacía al salir.
        isLoggedIn.filterNotNull().distinctUntilChanged()
            .onEach { if (it) fixedWidget.refresh() else fixedWidget.clear() }
            .launchIn(viewModelScope)
    }

    private fun validate(email: String, password: String?): Boolean {
        val error = when {
            !EMAIL_REGEX.matches(email.trim()) -> "Ingresa un correo válido."
            password != null && password.length < 6 -> "La contraseña debe tener al menos 6 caracteres."
            else -> null
        }
        if (error != null) _uiState.value = AuthUiState(error = error)
        return error == null
    }

    fun login(email: String, password: String) {
        if (!validate(email, password) || _uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                auth.signInWith(Email) {
                    this.email = email.trim().lowercase()
                    this.password = password
                }
                // La navegación reacciona a isLoggedIn.
                _uiState.value = AuthUiState()
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = friendlyError(e, "No se pudo iniciar sesión"))
            }
        }
    }

    fun signUp(email: String, password: String) {
        if (!validate(email, password) || _uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                auth.signUpWith(Email) {
                    this.email = email.trim().lowercase()
                    this.password = password
                }
                // Con confirmación de correo activada, signUp no crea sesión.
                _uiState.value = if (auth.currentSessionOrNull() != null) AuthUiState()
                else AuthUiState(info = "Cuenta creada. Revisa tu correo para confirmar y luego inicia sesión.")
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = friendlyError(e, "No se pudo crear la cuenta"))
            }
        }
    }

    fun resetPassword(email: String) {
        if (!validate(email, null) || _uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                auth.resetPasswordForEmail(email.trim().lowercase())
                _uiState.value = AuthUiState(info = "Te enviamos un correo para restablecer tu contraseña.")
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = friendlyError(e, "No se pudo enviar el correo"))
            }
        }
    }
}
