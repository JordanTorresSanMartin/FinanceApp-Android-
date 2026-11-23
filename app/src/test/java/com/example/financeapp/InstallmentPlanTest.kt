package com.example.financeapp

import com.example.financeapp.domain.CuotaState
import com.example.financeapp.domain.InstallmentPlan
import com.example.financeapp.domain.paymentDiff
import com.example.financeapp.domain.status
import com.example.financeapp.domain.withPaidCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InstallmentPlanTest {
    private val today = LocalDate.of(2026, 11, 18)
    private fun plan(dueDay: Int? = null, paidBefore: Int = 0, payments: List<String> = emptyList(), created: String = "2026-01-01") =
        InstallmentPlan("1", "Notebook", amount = 25_000, total = 12, paidBefore = paidBefore, payments = payments, dueDay = dueDay, createdOn = created)

    @Test fun counts() {
        val p = plan(paidBefore = 3, payments = listOf("2026-10-05"))
        assertEquals(4, p.paid); assertEquals(8, p.remaining); assertEquals(200_000L, p.remainingAmount)
    }

    @Test fun paidThisMonth() = assertEquals(CuotaState.PAGADA_MES, plan(dueDay = 5, payments = listOf("2026-11-02")).status(today).state)

    @Test fun overdue() {
        val s = plan(dueDay = 10).status(today)
        assertEquals(CuotaState.ATRASADA, s.state); assertEquals("Atrasada 8 días", s.label)
    }

    @Test fun dueSoonAndPending() {
        assertEquals("Vence en 2 días", plan(dueDay = 20).status(today).label)
        assertEquals(CuotaState.PENDIENTE, plan(dueDay = 28).status(today).state)
        assertEquals(CuotaState.PENDIENTE, plan().status(today).state)
    }

    @Test fun addedAfterDueDayIsNotOverdue() =
        assertEquals(CuotaState.PENDIENTE, plan(dueDay = 10, created = "2026-11-15").status(today).state)

    @Test fun dueDayClampedToMonthEnd() =
        assertEquals("Vence hoy", plan(dueDay = 31).status(LocalDate.of(2026, 2, 28)).label)

    @Test fun done() = assertTrue(plan(paidBefore = 12).isDone)

    @Test fun editPaidCountKeepsRecentDates() {
        val p = plan(paidBefore = 2, payments = listOf("2026-09-01", "2026-10-01")).withPaidCount(1)
        assertEquals(1, p.paid); assertEquals(listOf("2026-10-01"), p.payments); assertEquals(0, p.paidBefore)
        assertEquals(6, plan(payments = listOf("2026-10-01")).withPaidCount(6).paidBefore + 1)
    }

    @Test fun paymentDiffSyncsByDate() {
        val remote = listOf("a" to "2026-09-01", "b" to "2026-10-01", "c" to "2026-10-01")
        // Sin cambios
        assertEquals(emptyList<String>() to emptyList<String>(), paymentDiff(remote, listOf("2026-09-01", "2026-10-01", "2026-10-01")))
        // Bajó "ya pagadas": se borra el más antiguo
        assertEquals(listOf("a") to emptyList<String>(), paymentDiff(remote, listOf("2026-10-01", "2026-10-01")))
        // Pago nuevo local
        assertEquals(emptyList<String>() to listOf("2026-11-05"), paymentDiff(remote.take(1), listOf("2026-09-01", "2026-11-05")))
        // Todo nuevo
        assertEquals(emptyList<String>() to listOf("2026-01-01", "2026-02-01"), paymentDiff(emptyList(), listOf("2026-02-01", "2026-01-01")))
    }
}
