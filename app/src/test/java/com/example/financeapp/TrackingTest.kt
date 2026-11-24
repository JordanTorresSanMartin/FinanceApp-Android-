package com.example.financeapp

import com.example.financeapp.domain.cumulativeBalance
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

class TrackingTest {
    private val net = mapOf(
        YearMonth.of(2026, 10) to -500_000.0, // meses sin ingresos anotados
        YearMonth.of(2026, 11) to -577_412.0,
        YearMonth.of(2026, 12) to 300_000.0,
        YearMonth.of(2027, 1) to 50_000.0,
    )

    @Test fun withoutStartSumsEverything() =
        assertEquals(-777_412.0, cumulativeBalance(net, YearMonth.of(2026, 12), null, 0.0), 0.01)

    @Test fun startIgnoresOldMonthsAndAddsInitial() {
        val dic = YearMonth.of(2026, 12)
        assertEquals(1_300_000.0, cumulativeBalance(net, dic, dic, 1_000_000.0), 0.01)
        assertEquals(1_350_000.0, cumulativeBalance(net, YearMonth.of(2027, 1), dic, 1_000_000.0), 0.01)
    }

    @Test fun monthsBeforeStartKeepOldBehavior() =
        assertEquals(-1_077_412.0, cumulativeBalance(net, YearMonth.of(2026, 11), YearMonth.of(2026, 12), 1_000_000.0), 0.01)

    @Test fun startMonthWithoutMovementsShowsInitial() =
        assertEquals(800_000.0, cumulativeBalance(emptyMap(), YearMonth.of(2026, 12), YearMonth.of(2026, 12), 800_000.0), 0.01)
}
