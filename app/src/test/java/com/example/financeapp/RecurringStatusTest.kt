package com.example.financeapp

import com.example.financeapp.data.model.Frequency
import com.example.financeapp.data.model.RecurringPayment
import com.example.financeapp.domain.PayState
import com.example.financeapp.domain.paymentStatus
import com.example.financeapp.domain.periodWindow
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RecurringStatusTest {
    private fun item(freq: String = Frequency.MENSUAL, day: Int? = 10) =
        RecurringPayment(id = "1", name = "Netflix", amount = 9990.0, frequency = freq, billingDay = day)

    private val nov17 = LocalDate.of(2026, 11, 17)

    @Test fun pagadoSiHayPagoEnElMes() =
        assertEquals(PayState.PAGADO, paymentStatus(item(), listOf(LocalDate.of(2026, 11, 3)), nov17).state)

    @Test fun pagoDelMesAnteriorNoCuenta() =
        assertEquals(PayState.ATRASADO, paymentStatus(item(), listOf(LocalDate.of(2026, 10, 10)), nov17).state)

    @Test fun porVencerDentroDe3Dias() =
        assertEquals(PayState.POR_VENCER, paymentStatus(item(day = 20), emptyList(), nov17).state)

    @Test fun pendienteSiFaltaMucho() =
        assertEquals(PayState.PENDIENTE, paymentStatus(item(day = 28), emptyList(), nov17).state)

    @Test fun dia31EnMesDe30SeAjusta() {
        val s = paymentStatus(item(day = 31), emptyList(), nov17)
        assertEquals(LocalDate.of(2026, 11, 30), s.due)
    }

    @Test fun anualCubreTodoElAnio() =
        assertEquals(PayState.PAGADO, paymentStatus(item(Frequency.ANUAL, null), listOf(LocalDate.of(2026, 2, 1)), nov17).state)

    @Test fun ventanaQuincenal() {
        assertEquals(LocalDate.of(2026, 11, 16), periodWindow(Frequency.QUINCENAL, nov17).start)
        assertEquals(LocalDate.of(2026, 11, 30), periodWindow(Frequency.QUINCENAL, nov17).endInclusive)
    }

    @Test fun ventanaSemanalEmpiezaLunes() =
        assertEquals(LocalDate.of(2026, 11, 16), periodWindow(Frequency.SEMANAL, nov17).start) // 17-nov-2026 es martes

    @Test fun etiquetaAtraso() =
        assertEquals("Atrasado 7 días", paymentStatus(item(), emptyList(), nov17).label(nov17))
}
