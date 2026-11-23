package com.example.financeapp

import com.example.financeapp.data.model.Frequency
import com.example.financeapp.data.model.RecurringPayment
import com.example.financeapp.util.formatK
import com.example.financeapp.util.formatMoneyCLP
import com.example.financeapp.util.shiftYearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test fun montoPositivo() = assertEquals("$1.500", formatMoneyCLP(1500.0))
    @Test fun montoNegativoConSignoAntes() = assertEquals("-$1.500", formatMoneyCLP(-1500.0))
    @Test fun redondea() = assertEquals("$1.501", formatMoneyCLP(1500.6))
    @Test fun abreviadoNegativo() = assertEquals("-$2k", formatK(-2000.0))
    @Test fun cambioDeAnio() = assertEquals(2027 to 1, shiftYearMonth(2026, 12, 1))
    @Test fun equivalenteMensualSemanal() =
        assertEquals(52.0 / 12.0 * 1200, RecurringPayment(name = "x", amount = 1200.0, frequency = Frequency.SEMANAL).monthlyEquivalent, 0.001)
}
