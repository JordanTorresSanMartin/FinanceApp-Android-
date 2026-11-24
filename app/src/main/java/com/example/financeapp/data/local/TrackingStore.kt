package com.example.financeapp.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Empezar de cero": desde qué mes se lleva la cuenta y con cuánto se parte. El saldo total y el
 * % de ahorro ignoran los meses anteriores (que quedaron sin anotar) y arrancan en [initialBalance].
 */
// ponytail: preferencias locales; pasar a Supabase si se usa la app en varios teléfonos.
@Singleton
class TrackingStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("tracking", Context.MODE_PRIVATE)

    val start: YearMonth?
        get() = prefs.getString(KEY_START, null)?.let { runCatching { YearMonth.parse(it) }.getOrNull() }

    val initialBalance: Double
        get() = prefs.getLong(KEY_INITIAL, 0L).toDouble()

    fun save(start: YearMonth?, initialBalance: Long) {
        prefs.edit().putString(KEY_START, start?.toString()).putLong(KEY_INITIAL, initialBalance).apply()
    }

    private companion object {
        const val KEY_START = "start"       // "2026-12"
        const val KEY_INITIAL = "initial"   // CLP
    }
}
