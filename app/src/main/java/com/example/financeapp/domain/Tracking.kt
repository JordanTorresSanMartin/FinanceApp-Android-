package com.example.financeapp.domain

import java.time.YearMonth

/**
 * Saldo acumulado al cierre de [target]. Sin [start] (o viendo un mes anterior a él) suma todo lo
 * previo, como siempre. Con [start]: parte en [initial] y solo suma los meses desde [start].
 * [monthlyNet] = neto (ingresos − gastos) de cada mes con movimientos.
 */
fun cumulativeBalance(monthlyNet: Map<YearMonth, Double>, target: YearMonth, start: YearMonth?, initial: Double): Double {
    val from = start?.takeIf { !target.isBefore(it) }
    val net = monthlyNet.filterKeys { !it.isAfter(target) && (from == null || !it.isBefore(from)) }.values.sum()
    return net + if (from != null) initial else 0.0
}
