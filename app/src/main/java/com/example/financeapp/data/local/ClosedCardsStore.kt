package com.example.financeapp.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tarjetas que el usuario cerró: sus estados de cuenta y cuotas dejan de contar en "Tarjetas".
 * Se guarda en el teléfono y es reversible (no borra nada en Supabase).
 */
// ponytail: preferencias locales; pasar a una columna en Supabase si se usa la app en varios teléfonos.
@Singleton
class ClosedCardsStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("cards", Context.MODE_PRIVATE)

    fun closed(): Set<String> = prefs.getStringSet(KEY, emptySet()).orEmpty().toSet()

    fun setClosed(cardKey: String, closed: Boolean) {
        val next = closed().toMutableSet().apply { if (closed) add(cardKey) else remove(cardKey) }
        prefs.edit().putStringSet(KEY, next).apply()
    }

    companion object {
        private const val KEY = "closed"
        /** Identifica una tarjeta física: banco + últimos 4 dígitos. */
        fun keyOf(bank: String?, last4: String?) = "${bank.orEmpty().lowercase()}|${last4.orEmpty()}"
    }
}
