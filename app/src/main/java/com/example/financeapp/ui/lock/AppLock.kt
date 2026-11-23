package com.example.financeapp.ui.lock

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/** Preferencia local del bloqueo (no es dato sensible: solo on/off). */
object LockPrefs {
    private fun prefs(context: Context) = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    fun isEnabled(context: Context) = prefs(context).getBoolean("app_lock", false)
    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean("app_lock", enabled) }
        (context as? Activity)?.applyRecentsPrivacy(enabled)
    }

    /** true si el dispositivo tiene huella/rostro o PIN/patrón configurado. */
    fun isAvailable(context: Context) =
        BiometricManager.from(context).canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL) ==
            BiometricManager.BIOMETRIC_SUCCESS
}

/** Oculta el contenido en la vista de apps recientes (Android 13+). */
private fun Activity.applyRecentsPrivacy(enabled: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) setRecentsScreenshotEnabled(!enabled)
}

/** Tiempo en segundo plano tolerado antes de volver a pedir desbloqueo. */
private const val RELOCK_AFTER_MS = 60_000L

/**
 * Si el bloqueo está activo, cubre la app hasta autenticar con huella o PIN.
 * El contenido se sigue componiendo debajo para no perder la navegación.
 * ponytail: tolerancia fija de 60 s (cubre elegir un PDF); hacerla configurable si molesta.
 */
@Composable
fun AppLockGate(activity: FragmentActivity, content: @Composable () -> Unit) {
    var locked by rememberSaveable { mutableStateOf(LockPrefs.isEnabled(activity)) }
    var stoppedAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) { activity.applyRecentsPrivacy(LockPrefs.isEnabled(activity)) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { stoppedAt = SystemClock.elapsedRealtime() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (LockPrefs.isEnabled(activity) && stoppedAt > 0 &&
            SystemClock.elapsedRealtime() - stoppedAt > RELOCK_AFTER_MS
        ) locked = true
    }

    Box(Modifier.fillMaxSize()) {
        content()
        if (locked) {
            val prompt = { authenticate(activity) { locked = false } }
            LaunchedEffect(Unit) { prompt() }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    // Consume toques para que no lleguen a la app de abajo.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
                Icon(Icons.Filled.Lock, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Text("FinanceApp está bloqueada", style = MaterialTheme.typography.titleLarge)
                Button(onClick = prompt) { Text("Desbloquear") }
            }
        }
    }
}

private fun authenticate(activity: FragmentActivity, onSuccess: () -> Unit) {
    if (!LockPrefs.isAvailable(activity)) { onSuccess(); return } // sin método de bloqueo: no dejar al usuario afuera
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
        },
    )
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquear FinanceApp")
            .setSubtitle("Usa tu huella, rostro o PIN")
            .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .build(),
    )
}
