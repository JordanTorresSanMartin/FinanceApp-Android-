package com.example.financeapp

import com.example.financeapp.data.model.RecurringPayment
import com.example.financeapp.domain.InstallmentPlan
import com.example.financeapp.domain.PayState
import com.example.financeapp.domain.PaymentStatus
import com.example.financeapp.domain.RecurringWithStatus
import com.example.financeapp.notifications.dueItems
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DueItemsTest {
    private val today = LocalDate.of(2026, 11, 25)
    private fun fijo(name: String, state: PayState, due: LocalDate?) =
        RecurringWithStatus(RecurringPayment(name = name, amount = 85_000.0, frequency = "mensual"), PaymentStatus(state, due, null))
    private fun cuota(name: String, dueDay: Int?, paidBefore: Int = 0) =
        InstallmentPlan(name, name, amount = 28_000, total = 12, paidBefore = paidBefore, dueDay = dueDay, createdOn = "2026-01-01")

    @Test fun onlyTodayAndOverdue() {
        val items = dueItems(
            listOf(
                fijo("Seguro", PayState.POR_VENCER, today),            // vence hoy → sí
                fijo("Netflix", PayState.POR_VENCER, today.plusDays(2)), // en 2 días → no
                fijo("Gym", PayState.ATRASADO, today.minusDays(3)),     // atrasado → sí, primero
                fijo("Luz", PayState.PAGADO, null),                     // pagado → no
            ),
            listOf(
                cuota("Celular", dueDay = 25),            // vence hoy → sí
                cuota("Notebook", dueDay = 5),            // ya pasó el 5 sin pagar → atrasada → sí
                cuota("Sin día", dueDay = null),          // sin día → nunca alerta
                cuota("Terminada", dueDay = 25, paidBefore = 12), // terminada → no
            ),
            today,
        )
        assertEquals(listOf("Gym", "Notebook", "Seguro", "Celular"), items.map { it.name })
        assertEquals(listOf(true, true, false, false), items.map { it.overdue })
    }

    @Test fun nothingDue() = assertEquals(emptyList<Any>(), dueItems(emptyList(), listOf(cuota("X", dueDay = 28)), today))
}
