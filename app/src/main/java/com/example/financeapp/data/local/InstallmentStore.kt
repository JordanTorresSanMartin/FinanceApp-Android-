package com.example.financeapp.data.local

import android.content.Context
import com.example.financeapp.domain.InstallmentPlan
import com.example.financeapp.domain.paymentDiff
import com.example.financeapp.ui.widget.refreshInstallmentWidgets
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cuotas en Supabase (fuente de verdad) con caché local para mostrar al instante y sin conexión.
 * App y widget comparten esta instancia. Escribir requiere conexión: así los pagos registrados desde
 * la planilla en pareja nunca chocan con cambios offline. Los errores se propagan al que llama.
 */
@Singleton
class InstallmentStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val postgrest: Postgrest,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    private val _plans = MutableStateFlow(readCache())
    val plans: StateFlow<List<InstallmentPlan>> = _plans.asStateFlow()

    private fun readCache(): List<InstallmentPlan> = runCatching {
        json.decodeFromString<List<InstallmentPlan>>(prefs.getString(KEY, null) ?: return emptyList())
    }.getOrDefault(emptyList())

    /** Trae las cuotas del servidor y actualiza caché + widget. La primera vez sube las cuotas locales antiguas. */
    suspend fun refresh() = mutex.withLock { pullLocked() }

    suspend fun upsert(plan: InstallmentPlan) = write {
        postgrest[PLANS].upsert(
            buildJsonObject {
                put("id", plan.id); put("name", plan.name); put("amount", plan.amount); put("total", plan.total)
                put("paid_before", plan.paidBefore); put("due_day", plan.dueDay)
                put("created_on", plan.createdOn); put("owed_by", plan.owedBy?.trim()?.ifEmpty { null })
            }
        )
        val remote = postgrest[PAYMENTS].select { filter { eq("plan_id", plan.id) } }.decodeList<PaymentRow>()
        val (toDelete, toInsert) = paymentDiff(remote.map { it.id to it.paidOn }, plan.payments)
        toDelete.forEach { id -> postgrest[PAYMENTS].delete { filter { eq("id", id) } } }
        toInsert.forEach { insertPayment(plan.id, it) }
    }

    suspend fun delete(id: String) = write { postgrest[PLANS].delete { filter { eq("id", id) } } } // pagos: on delete cascade

    /** Marca la siguiente cuota como pagada. No hace nada si ya estaba completa. */
    suspend fun pay(id: String, date: LocalDate = LocalDate.now()) = write {
        if (_plans.value.find { it.id == id }?.isDone != false) return@write
        insertPayment(id, date.toString())
    }

    /** Deshace el último pago registrado (desde la app, el widget o la planilla). */
    suspend fun undoLast(id: String) = write {
        val last = postgrest[PAYMENTS].select {
            filter { eq("plan_id", id) }
            order("paid_on", Order.DESCENDING); order("created_at", Order.DESCENDING)
            limit(1)
        }.decodeList<PaymentRow>().firstOrNull() ?: return@write
        postgrest[PAYMENTS].delete { filter { eq("id", last.id) } }
    }

    /** Ejecuta el cambio en el servidor y vuelve a leer, todo bajo el mismo candado. */
    private suspend fun write(block: suspend () -> Unit) = mutex.withLock {
        withContext(Dispatchers.IO) { block() }
        pullLocked()
    }

    private suspend fun insertPayment(planId: String, date: String) {
        postgrest[PAYMENTS].insert(buildJsonObject { put("plan_id", planId); put("paid_on", date) })
    }

    private suspend fun pullLocked() {
        val next = withContext(Dispatchers.IO) {
            if (!prefs.getBoolean(KEY_MIGRATED, false)) {
                uploadLegacy()
                prefs.edit().putBoolean(KEY_MIGRATED, true).commit()
            }
            val rows = postgrest[PLANS].select { order("created_on", Order.ASCENDING) }.decodeList<PlanRow>()
            val pays = postgrest[PAYMENTS].select {
                order("paid_on", Order.ASCENDING); order("created_at", Order.ASCENDING)
            }.decodeList<PaymentRow>().groupBy({ it.planId }, { it.paidOn })
            rows.map { it.toPlan(pays[it.id].orEmpty()) }
        }
        if (next == _plans.value) return // sin cambios: no redibuja el widget (evita bucles con su refresh)
        withContext(Dispatchers.IO) { prefs.edit().putString(KEY, json.encodeToString(next)).commit() }
        _plans.value = next
        refreshInstallmentWidgets(context)
    }

    /** Sube una sola vez las cuotas que antes vivían solo en el teléfono (sin duplicar las que ya existan). */
    private suspend fun uploadLegacy() {
        val local = readCache()
        if (local.isEmpty()) return
        val existing = postgrest[PLANS].select().decodeList<PlanRow>().map { it.id }.toSet()
        local.filterNot { it.id in existing }.forEach { plan ->
            postgrest[PLANS].insert(
                buildJsonObject {
                    put("id", plan.id); put("name", plan.name); put("amount", plan.amount); put("total", plan.total)
                    put("paid_before", plan.paidBefore); put("due_day", plan.dueDay); put("created_on", plan.createdOn)
                }
            )
            plan.payments.forEach { insertPayment(plan.id, it) }
        }
    }

    @Serializable
    private data class PlanRow(
        val id: String,
        val name: String,
        val amount: Long,
        val total: Int,
        @SerialName("paid_before") val paidBefore: Int = 0,
        @SerialName("due_day") val dueDay: Int? = null,
        @SerialName("created_on") val createdOn: String,
        @SerialName("owed_by") val owedBy: String? = null,
    ) {
        fun toPlan(payments: List<String>) =
            InstallmentPlan(id, name, amount, total, paidBefore, payments, dueDay, createdOn, owedBy)
    }

    @Serializable
    private data class PaymentRow(
        val id: String,
        @SerialName("plan_id") val planId: String,
        @SerialName("paid_on") val paidOn: String,
    )

    private companion object {
        const val PREFS = "installments"
        const val KEY = "plans"
        const val KEY_MIGRATED = "migrated_to_supabase"
        const val PLANS = "installment_plans"
        const val PAYMENTS = "installment_payments"
    }
}
