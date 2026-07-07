package com.example.financeapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Roles de color semánticos de finanzas (ingreso / gasto / advertencia) que MD3
 * no define. Replican exactamente la paleta de la app RN (theme/ThemeProvider).
 */
data class FinanceColors(
    val income: Color, val onIncome: Color, val incomeContainer: Color, val onIncomeContainer: Color,
    val expense: Color, val onExpense: Color, val expenseContainer: Color, val onExpenseContainer: Color,
    val warning: Color, val onWarning: Color, val warningContainer: Color, val onWarningContainer: Color,
)

val financeLightColors = FinanceColors(
    income = Color(0xFF1B873F), onIncome = Color(0xFFFFFFFF),
    incomeContainer = Color(0xFFC8F2D2), onIncomeContainer = Color(0xFF06351A),
    expense = Color(0xFFC0322B), onExpense = Color(0xFFFFFFFF),
    expenseContainer = Color(0xFFFFDAD6), onExpenseContainer = Color(0xFF410002),
    warning = Color(0xFF9A6700), onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFFE9B0), onWarningContainer = Color(0xFF2A1800),
)

val financeDarkColors = FinanceColors(
    income = Color(0xFF7BD995), onIncome = Color(0xFF06351A),
    incomeContainer = Color(0xFF0F5026), onIncomeContainer = Color(0xFFC8F2D2),
    expense = Color(0xFFFFB4AB), onExpense = Color(0xFF690005),
    expenseContainer = Color(0xFF93000A), onExpenseContainer = Color(0xFFFFDAD6),
    warning = Color(0xFFF2C14E), onWarning = Color(0xFF3A2A00),
    warningContainer = Color(0xFF5A4200), onWarningContainer = Color(0xFFFFE9B0),
)

val LocalFinanceColors = staticCompositionLocalOf { financeLightColors }

/** Acceso a los colores de finanzas: `FinanceTheme.colors.income`. */
object FinanceTheme {
    val colors: FinanceColors
        @Composable @ReadOnlyComposable
        get() = LocalFinanceColors.current
}
