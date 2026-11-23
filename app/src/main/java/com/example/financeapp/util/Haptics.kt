package com.example.financeapp.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.VibrationEffect.Composition
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Vocabulario háptico de la app, con "física": cada acción tiene una sensación distinta y
 * coherente con su animación (resortes de pressable, Pagar → Pagada, cerrar/borrar).
 *
 * Usa primitivas compuestas (Android 11+) cuando el motor del teléfono las soporta —se sienten
 * como un clic físico, no como un zumbido— y si no, efectos predefinidos o un pulso corto.
 * Respeta "Vibración al tocar" del sistema.
 */
class AppHaptics(private val context: Context) {

    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    private fun enabled(): Boolean {
        val v = vibrator ?: return false
        return v.hasVibrator() &&
            Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0
    }

    /** Primitivas (id, escala 0..1, retraso ms). Si no hay soporte, usa [fallback]. */
    private fun compose(vararg parts: Triple<Int, Float, Int>, fallback: () -> Unit) {
        if (!enabled()) return
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && v.areAllPrimitivesSupported(*parts.map { it.first }.toIntArray())) {
            val c = VibrationEffect.startComposition()
            parts.forEach { (id, scale, delay) -> c.addPrimitive(id, scale, delay) }
            v.vibrate(c.compose())
        } else fallback()
    }

    private fun predefined(effect: Int, ms: Long, amp: Int) {
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) v.vibrate(VibrationEffect.createPredefined(effect))
        else v.vibrate(VibrationEffect.createOneShot(ms, amp))
    }

    /**
     * Respaldo para motores sin primitivas pero con control de intensidad (p. ej. OnePlus):
     * una onda de intensidades imita subidas, caídas y "ka-chunks". Sin control de intensidad,
     * usa el efecto predefinido.
     */
    private fun wave(ms: LongArray, amps: IntArray, effect: Int) {
        val v = vibrator ?: return
        if (v.hasAmplitudeControl()) v.vibrate(VibrationEffect.createWaveform(ms, amps, -1))
        else predefined(effect, ms.sum(), amps.max())
    }

    // ── Vocabulario ──────────────────────────────────────────────────────────

    /** Elegir algo pequeño: pestaña, chip, sector del gráfico, opción. */
    fun tick() = compose(Triple(Composition.PRIMITIVE_TICK, 0.55f, 0)) {
        wave(longArrayOf(0, 8), intArrayOf(0, 90), VibrationEffect.EFFECT_TICK)
    }

    /** Al hundir un botón (pressable): apenas perceptible, como el recorrido de una tecla. */
    fun pressDown() = compose(Triple(lowTick(), 0.35f, 0)) {
        wave(longArrayOf(0, 5), intArrayOf(0, 45), VibrationEffect.EFFECT_TICK)
    }

    /** Soltar un botón / acción normal: clic seco. */
    fun click() = compose(Triple(Composition.PRIMITIVE_CLICK, 0.7f, 0)) { predefined(VibrationEffect.EFFECT_CLICK, 18, 160) }

    /** Interruptor: sube al encender, baja al apagar. */
    fun toggle(on: Boolean) = if (on) {
        compose(Triple(Composition.PRIMITIVE_QUICK_RISE, 0.45f, 0), Triple(Composition.PRIMITIVE_CLICK, 0.8f, 20)) {
            wave(longArrayOf(0, 8, 28, 14), intArrayOf(0, 70, 0, 210), VibrationEffect.EFFECT_CLICK)
        }
    } else {
        compose(Triple(quickFall(), 0.45f, 0), Triple(Composition.PRIMITIVE_TICK, 0.5f, 10)) {
            wave(longArrayOf(0, 14, 28, 8), intArrayOf(0, 190, 0, 60), VibrationEffect.EFFECT_TICK)
        }
    }

    /** Algo se guardó / se pagó: "ka-chunk" ascendente, acompaña el rebote de Pagar → Pagada. */
    fun success() = compose(
        Triple(Composition.PRIMITIVE_CLICK, 0.5f, 0),
        Triple(Composition.PRIMITIVE_CLICK, 1f, 70),
    ) { wave(longArrayOf(0, 12, 60, 22), intArrayOf(0, 110, 0, 255), VibrationEffect.EFFECT_HEAVY_CLICK) }

    /** Terminaste de pagar algo completo: subida lenta que remata en clic fuerte. */
    fun celebrate() = compose(
        Triple(Composition.PRIMITIVE_SLOW_RISE, 0.6f, 0),
        Triple(Composition.PRIMITIVE_CLICK, 1f, 40),
    ) { wave(longArrayOf(0, 25, 25, 25, 25, 40, 30), intArrayOf(0, 40, 80, 130, 180, 0, 255), VibrationEffect.EFFECT_HEAVY_CLICK) }

    /** Deshacer: lo contrario de success, una caída breve. */
    fun undo() = compose(Triple(quickFall(), 0.6f, 0)) {
        wave(longArrayOf(0, 18, 18), intArrayOf(0, 170, 60), VibrationEffect.EFFECT_TICK)
    }

    /** Acción destructiva confirmada (borrar, cerrar tarjeta): golpe sordo. */
    fun destructive() = compose(Triple(thud(), 0.9f, 0)) {
        wave(longArrayOf(0, 45), intArrayOf(0, 255), VibrationEffect.EFFECT_HEAVY_CLICK)
    }

    /** Umbral alcanzado (deslizar para actualizar). */
    fun threshold() = compose(Triple(Composition.PRIMITIVE_QUICK_RISE, 0.5f, 0)) {
        wave(longArrayOf(0, 10, 18, 12), intArrayOf(0, 80, 0, 170), VibrationEffect.EFFECT_CLICK)
    }

    /** Algo falló. */
    fun error() = compose(
        Triple(Composition.PRIMITIVE_CLICK, 0.8f, 0),
        Triple(Composition.PRIMITIVE_CLICK, 0.8f, 90),
    ) { predefined(VibrationEffect.EFFECT_DOUBLE_CLICK, 45, 255) }

    // Primitivas de Android 12+ (con reemplazo compatible en Android 11).
    private fun lowTick() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Composition.PRIMITIVE_LOW_TICK else Composition.PRIMITIVE_TICK
    private fun quickFall() = Composition.PRIMITIVE_QUICK_FALL
    private fun thud() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Composition.PRIMITIVE_THUD else Composition.PRIMITIVE_CLICK

    // Nombres anteriores (se mantienen para no tocar código que ya los usa).
    fun selection() = tick()
    fun light() = click()
    fun medium() = click()
}

/** Provisto en FinanceAppTheme. Null-safe: si no hay provider, no hace nada. */
val LocalAppHaptics = staticCompositionLocalOf<AppHaptics?> { null }
