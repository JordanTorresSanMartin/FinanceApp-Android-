package com.example.financeapp.util

import kotlinx.datetime.LocalDate
import java.text.NumberFormat
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Utilidades de formato para replicar el comportamiento de la app RN:
 * - Moneda CLP con separador de miles ("$1.500").
 * - Nombres de mes / fechas en español de Chile.
 */

private val esCL: Locale = Locale.forLanguageTag("es-CL")
private val esES: Locale = Locale.forLanguageTag("es-ES")

private val clpFormat: NumberFormat = NumberFormat.getIntegerInstance(esCL)

/** "$1.500" — redondea y agrega separador de miles, igual que RN. */
fun formatMoneyCLP(amount: Double): String = "$" + clpFormat.format(amount.roundToLong())

/** "1.500" — solo dígitos agrupados con separador de miles (campos de monto). */
fun groupThousands(digits: String): String {
    val d = digits.filter { it.isDigit() }
    return if (d.isEmpty()) "" else clpFormat.format(d.toLong())
}

/** "1k" / "1.5M" — abreviado, usado en tablas y forecast. */
fun formatK(amount: Double): String {
    val n = amount
    return when {
        abs(n) >= 1_000_000 -> "$" + String.format(Locale.US, "%.1f", n / 1_000_000) + "M"
        abs(n) >= 1_000 -> "$" + (n / 1_000).roundToLong() + "k"
        else -> "$" + n.roundToLong()
    }
}

/** Fecha de hoy en la zona local, como kotlinx LocalDate. */
fun todayLocalDate(): LocalDate {
    val now = java.time.LocalDate.now()
    return LocalDate(now.year, now.monthValue, now.dayOfMonth)
}

fun LocalDate.toJavaLocalDate(): java.time.LocalDate =
    java.time.LocalDate.of(year, monthNumber, dayOfMonth)

/** "Mayo 2026" — mes capitalizado + año, para cabeceras. */
fun monthYearLabel(year: Int, month: Int): String {
    val name = java.time.Month.of(month).getDisplayName(TextStyle.FULL, esES)
    return name.replaceFirstChar { it.uppercase(esES) } + " $year"
}

/** "May" — nombre corto del mes (gráficos/tablas). */
fun monthShortLabel(month: Int): String =
    java.time.Month.of(month).getDisplayName(TextStyle.SHORT, esES)
        .replaceFirstChar { it.uppercase(esES) }

/** "Hoy" / "Ayer" / "lun, 5 may" — cabecera de grupo de día en transacciones. */
fun dayHeaderLabel(date: LocalDate): String {
    val d = date.toJavaLocalDate()
    val today = java.time.LocalDate.now()
    return when (d) {
        today -> "Hoy"
        today.minusDays(1) -> "Ayer"
        else -> {
            val weekday = d.dayOfWeek.getDisplayName(TextStyle.SHORT, esCL)
            val monthShort = d.month.getDisplayName(TextStyle.SHORT, esCL)
            "${weekday.replaceFirstChar { it.uppercase(esCL) }}, ${d.dayOfMonth} $monthShort"
        }
    }
}

/** "5 may" — fecha corta para detalles. */
fun shortDateLabel(date: LocalDate): String {
    val d = date.toJavaLocalDate()
    val monthShort = d.month.getDisplayName(TextStyle.SHORT, esCL)
    return "${d.dayOfMonth} $monthShort"
}

/** ISO "YYYY-MM-DD" de un LocalDate. */
fun LocalDate.iso(): String = toString()
