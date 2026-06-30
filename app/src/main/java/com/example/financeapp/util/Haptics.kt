package com.example.financeapp.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Háptica perceptible basada en el Vibrator del sistema, para replicar el feedback
 * de `expo-haptics` de la app RN (impacto / selección / éxito / error). El
 * `LocalHapticFeedback` de Compose es demasiado sutil en muchos dispositivos.
 */
class AppHaptics(context: Context) {

    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    private fun oneShot(ms: Long, amplitude: Int) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(ms, amplitude))
        } else {
            @Suppress("DEPRECATION") v.vibrate(ms)
        }
    }

    private fun predefined(effect: Int, fallbackMs: Long, fallbackAmp: Int) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            v.vibrate(VibrationEffect.createPredefined(effect))
        } else {
            oneShot(fallbackMs, fallbackAmp)
        }
    }

    fun selection() = oneShot(10, 60)
    fun light() = oneShot(12, 90)
    fun medium() = predefined(VibrationEffect.EFFECT_CLICK, 20, 170)
    fun success() = predefined(VibrationEffect.EFFECT_HEAVY_CLICK, 28, 220)
    fun error() = predefined(VibrationEffect.EFFECT_DOUBLE_CLICK, 45, 255)
}

/** Provisto en FinanceAppTheme. Null-safe: si no hay provider, no hace nada. */
val LocalAppHaptics = staticCompositionLocalOf<AppHaptics?> { null }
