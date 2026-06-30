package com.example.financeapp.ui.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.financeapp.ui.theme.FinanceAppTheme
import com.example.financeapp.ui.viewmodel.auth.AuthMode
import com.example.financeapp.ui.viewmodel.auth.AuthUiState
import com.example.financeapp.ui.viewmodel.auth.AuthViewModel
import com.example.financeapp.util.LocalAppHaptics

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LoginContent(
        uiState = uiState,
        onLogin = viewModel::login,
        onSignUp = viewModel::signUp,
        onLoginSuccess = onLoginSuccess,
    )
}

@Composable
fun LoginContent(
    uiState: AuthUiState,
    onLogin: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onLoginSuccess: () -> Unit,
) {
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val haptics = LocalAppHaptics.current

    androidx.compose.runtime.LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onLoginSuccess()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.AccountBalanceWallet, null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            AnimatedContent(
                targetState = authMode,
                transitionSpec = {
                    slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut()
                },
                label = "title_anim",
            ) { mode ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (mode == AuthMode.LOGIN) "¡Hola de nuevo!" else "Únete a nosotros",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = if (mode == AuthMode.LOGIN) "Tu libertad financiera empieza aquí" else "La mejor forma de gestionar tu dinero",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            TextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("Correo electrónico") },
                leadingIcon = { Icon(Icons.Filled.Email, null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )

            Spacer(Modifier.height(16.dp))

            TextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("Contraseña") },
                leadingIcon = { Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { haptics?.selection(); passwordVisible = !passwordVisible }) {
                        Icon(if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )

            Spacer(Modifier.height(40.dp))

            val buttonScale by animateFloatAsState(
                targetValue = if (uiState.isLoading) 0.95f else 1f,
                animationSpec = spring(dampingRatio = 0.5f),
                label = "button_scale",
            )
            Button(
                onClick = {
                    haptics?.medium()
                    if (authMode == AuthMode.LOGIN) onLogin(email, password) else onSignUp(email, password)
                },
                modifier = Modifier.fillMaxWidth().height(64.dp).scale(buttonScale),
                shape = RoundedCornerShape(24.dp),
                enabled = !uiState.isLoading,
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(
                        text = if (authMode == AuthMode.LOGIN) "Iniciar sesión" else "Registrarse",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                }
            }

            uiState.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            uiState.info?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(24.dp))

            TextButton(onClick = {
                haptics?.selection()
                authMode = if (authMode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN
            }) {
                Text(
                    text = if (authMode == AuthMode.LOGIN) "¿Nuevo por aquí? Crea una cuenta" else "¿Ya eres miembro? Inicia sesión",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    FinanceAppTheme {
        LoginContent(uiState = AuthUiState(), onLogin = { _, _ -> }, onSignUp = { _, _ -> }, onLoginSuccess = {})
    }
}
