package com.example.financeapp.domain

import com.example.financeapp.data.model.Frequency
import com.example.financeapp.data.model.RecurringPayment
import com.example.financeapp.util.toJavaLocalDate
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Nota con la que se marcan las transacciones creadas desde un pago fijo. */
const val RECURRING_NOTE = "Pago recurrente"

/** Nota de las transacciones creadas al pagar una cuota propia ("Mis cuotas"). */
const val INSTALLMENT_NOTE = "Pago de cuota"

/** El orden importa: es el orden de urgencia en listas y widget. */
enum class PayState { ATRASADO, POR_VENCER, PENDIENTE, PAGADO }

data class PaymentStatus(
    val state: PayState,
    /** Vencimiento del período actual (null si ya está pagado y no aplica). */
    val due: LocalDate?,
    /** Fecha en que se registró el pago de este período. */
    val paidOn: LocalDate?,
) {
    fun label(today: LocalDate): String = when (state) {
        PayState.PAGADO -> "Pagado"
        PayState.ATRASADO -> {
            val d = ChronoUnit.DAYS.between(due, today)
            if (d == 1L) "Atrasado 1 día" else "Atrasado $d días"
        }
        PayState.POR_VENCER -> when (val d = ChronoUnit.DAYS.between(today, due)) {
            0L -> "Vence hoy"
            1L -> "Vence mañana"
            else -> "Vence en $d días"
        }
        PayState.PENDIENTE -> "Vence el ${due!!.dayOfMonth}/${due.monthValue}"
    }
}

data class RecurringWithStatus(val item: RecurringPayment, val status: PaymentStatus)

/** Días antes del vencimiento en que el pago pasa a "por vencer". */
const val DUE_SOON_DAYS = 3

/** Período de facturación que contiene a [today] según la frecuencia. */
fun periodWindow(frequency: String, today: LocalDate): ClosedRange<LocalDate> = when (frequency) {
    Frequency.ANUAL -> today.withDayOfYear(1)..today.with(TemporalAdjusters.lastDayOfYear())
    Frequency.SEMANAL -> {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        monday..monday.plusDays(6)
    }
    Frequency.QUINCENAL ->
        if (today.dayOfMonth <= 15) today.withDayOfMonth(1)..today.withDayOfMonth(15)
        else today.withDayOfMonth(16)..today.with(TemporalAdjusters.lastDayOfMonth())
    else -> today.withDayOfMonth(1)..today.with(TemporalAdjusters.lastDayOfMonth())
}

/**
 * Estado de pago del período actual.
 * ponytail: "pagado" = existe una transacción con nota [RECURRING_NOTE] y la misma
 * descripción dentro del período. Si renombras el pago se pierde el vínculo con
 * pagos anteriores; agregar `recurring_id` a `transactions` si eso molesta.
 */
fun paymentStatus(item: RecurringPayment, paymentDates: List<LocalDate>, today: LocalDate): PaymentStatus {
    val window = periodWindow(item.frequency, today)
    val paidOn = paymentDates.filter { it in window }.maxOrNull()
    if (paidOn != null) return PaymentStatus(PayState.PAGADO, null, paidOn)

    val due = item.nextDue?.toJavaLocalDate()?.takeIf { it in window }
        ?: item.billingDay
            ?.takeIf { item.frequency == Frequency.MENSUAL }
            ?.let { today.withDayOfMonth(it.coerceIn(1, today.lengthOfMonth())) }
        ?: window.endInclusive

    val days = ChronoUnit.DAYS.between(today, due)
    val state = when {
        days < 0 -> PayState.ATRASADO
        days <= DUE_SOON_DAYS -> PayState.POR_VENCER
        else -> PayState.PENDIENTE
    }
    return PaymentStatus(state, due, null)
}

/** Orden útil para el widget: lo urgente arriba, lo pagado abajo. */
fun List<RecurringWithStatus>.sortedByUrgency(): List<RecurringWithStatus> =
    sortedWith(compareBy({ it.status.state.ordinal }, { it.status.due }))
