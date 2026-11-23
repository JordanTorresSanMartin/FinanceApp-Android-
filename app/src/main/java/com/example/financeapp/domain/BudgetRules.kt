package com.example.financeapp.domain

/** % de uso desde el que un presupuesto se marca "cerca del límite". */
const val BUDGET_WARNING_PCT = 85.0

const val BUDGET_OK = "ok"
const val BUDGET_WARNING = "advertencia"
const val BUDGET_EXCEEDED = "excedido"

/** Mismos valores que la vista `budget_status` de la BD. */
fun budgetStatus(pctUsed: Double): String = when {
    pctUsed >= 100 -> BUDGET_EXCEEDED
    pctUsed >= BUDGET_WARNING_PCT -> BUDGET_WARNING
    else -> BUDGET_OK
}
