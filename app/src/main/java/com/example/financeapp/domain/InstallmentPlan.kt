package com.example.financeapp.domain

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Compra o deuda en cuotas, guardada solo en el dispositivo.
 * `paid` = cuotas pagadas antes de agregarla a la app + las marcadas desde la app.
 */
@Serializable
data class InstallmentPlan(
    val id: String,
    val name: String,
    /** Monto de cada cuota (CLP). */
    val amount: Long,
    /** Cuotas totales. */
    val total: Int,
    /** Cuotas que ya estaban pagadas al agregarla. */
    val paidBefore: Int = 0,
    /** Fecha ISO de cada cuota marcada como pagada desde la app o el widget. */
    val payments: List<String> = emptyList(),
    /** Día de vencimiento mensual (1–31), opcional. */
    val dueDay: Int? = null,
    /** Fecha ISO en que se agregó; evita marcar "atrasada" un mes previo al seguimiento. */
    val createdOn: String = LocalDate.now().toString(),
) {
    val paid: Int get() = (paidBefore + payments.size).coerceAtMost(total)
    val remaining: Int get() = (total - paid).coerceAtLeast(0)
    val remainingAmount: Long get() = remaining * amount
    val totalAmount: Long get() = total * amount
    val isDone: Boolean get() = remaining == 0
    val progress: Float get() = if (total <= 0) 0f else paid.toFloat() / total
}

/** Orden = urgencia. */
enum class CuotaState { ATRASADA, POR_VENCER, PENDIENTE, PAGADA_MES, TERMINADA }

data class CuotaStatus(val state: CuotaState, val label: String)

private const val SOON_DAYS = 3
private val es = Locale.forLanguageTag("es-CL")
private val dayMonth = DateTimeFormatter.ofPattern("d MMM", es)
private val monthName = DateTimeFormatter.ofPattern("MMMM", es)

private fun dueIn(month: YearMonth, day: Int): LocalDate = month.atDay(day.coerceAtMost(month.lengthOfMonth()))

fun InstallmentPlan.status(today: LocalDate = LocalDate.now()): CuotaStatus {
    if (isDone) return CuotaStatus(CuotaState.TERMINADA, "¡Pagada completa!")
    val month = YearMonth.from(today)
    val mes = today.format(monthName)
    if (payments.any { runCatching { YearMonth.from(LocalDate.parse(it)) == month }.getOrDefault(false) }) {
        val next = dueDay?.let { " · próxima ${dueIn(month.plusMonths(1), it).format(dayMonth)}" } ?: ""
        return CuotaStatus(CuotaState.PAGADA_MES, "Cuota de $mes pagada$next")
    }
    val day = dueDay ?: return CuotaStatus(CuotaState.PENDIENTE, "Cuota de $mes pendiente")
    var due = dueIn(month, day)
    // Agregada después del vencimiento de este mes: el seguimiento parte el mes siguiente.
    val created = runCatching { LocalDate.parse(createdOn) }.getOrNull()
    if (created != null && YearMonth.from(created) == month && created.isAfter(due)) due = dueIn(month.plusMonths(1), day)
    val days = ChronoUnit.DAYS.between(today, due)
    return when {
        days < 0 -> CuotaStatus(CuotaState.ATRASADA, if (days == -1L) "Atrasada 1 día" else "Atrasada ${-days} días")
        days == 0L -> CuotaStatus(CuotaState.POR_VENCER, "Vence hoy")
        days == 1L -> CuotaStatus(CuotaState.POR_VENCER, "Vence mañana")
        days <= SOON_DAYS -> CuotaStatus(CuotaState.POR_VENCER, "Vence en $days días")
        else -> CuotaStatus(CuotaState.PENDIENTE, "Vence el ${due.format(dayMonth)}")
    }
}

fun InstallmentPlan.paidToday(today: LocalDate = LocalDate.now()): Boolean = payments.lastOrNull() == today.toString()

/**
 * Orden estable por día de vencimiento (terminadas al final). No se reordena al pagar:
 * el botón ✓ se vuelve "deshacer" en el mismo lugar, bajo el dedo. La urgencia la dan color y texto.
 */
fun List<InstallmentPlan>.byDueDay(): List<InstallmentPlan> =
    sortedWith(compareBy<InstallmentPlan>({ it.isDone }, { it.dueDay ?: 32 }, { it.name.lowercase() }))

/** Aplica un nuevo "cuotas pagadas" editado a mano, conservando las fechas registradas que quepan. */
fun InstallmentPlan.withPaidCount(newPaid: Int): InstallmentPlan {
    val p = newPaid.coerceIn(0, total)
    val kept = payments.takeLast(p)
    return copy(payments = kept, paidBefore = p - kept.size)
}
