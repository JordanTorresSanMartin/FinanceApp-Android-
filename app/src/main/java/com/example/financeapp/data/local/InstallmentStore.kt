package com.example.financeapp.data.local

import android.content.Context
import com.example.financeapp.domain.InstallmentPlan
import com.example.financeapp.ui.widget.refreshInstallmentWidgets
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cuotas guardadas en el dispositivo (sin Supabase). App y widget comparten esta instancia,
 * así un pago marcado en el widget aparece al instante en la app y viceversa.
 * Se respalda con el backup de Android (ver res/xml/backup_rules.xml y data_extraction_rules.xml).
 */
// ponytail: JSON en SharedPreferences; pasar a Room si llegan a ser cientos de planes.
@Singleton
class InstallmentStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    private val _plans = MutableStateFlow(read())
    val plans: StateFlow<List<InstallmentPlan>> = _plans.asStateFlow()

    private fun read(): List<InstallmentPlan> = runCatching {
        json.decodeFromString<List<InstallmentPlan>>(prefs.getString(KEY, null) ?: return emptyList())
    }.getOrDefault(emptyList())

    /** Única vía de escritura: persiste, publica y refresca el widget. */
    private suspend fun edit(transform: (List<InstallmentPlan>) -> List<InstallmentPlan>) {
        mutex.withLock {
            val next = transform(_plans.value)
            withContext(Dispatchers.IO) { prefs.edit().putString(KEY, json.encodeToString(next)).commit() }
            _plans.value = next
        }
        refreshInstallmentWidgets(context)
    }

    suspend fun upsert(plan: InstallmentPlan) = edit { list ->
        if (list.any { it.id == plan.id }) list.map { if (it.id == plan.id) plan else it } else list + plan
    }

    suspend fun delete(id: String) = edit { list -> list.filterNot { it.id == id } }

    /** Marca la siguiente cuota como pagada. No hace nada si ya estaba completa. */
    suspend fun pay(id: String, date: LocalDate = LocalDate.now()) = edit { list ->
        list.map { if (it.id == id && !it.isDone) it.copy(payments = it.payments + date.toString()) else it }
    }

    /** Deshace la última cuota marcada desde la app/widget. */
    suspend fun undoLast(id: String) = edit { list ->
        list.map { if (it.id == id && it.payments.isNotEmpty()) it.copy(payments = it.payments.dropLast(1)) else it }
    }

    private companion object {
        const val PREFS = "installments"
        const val KEY = "plans"
    }
}
