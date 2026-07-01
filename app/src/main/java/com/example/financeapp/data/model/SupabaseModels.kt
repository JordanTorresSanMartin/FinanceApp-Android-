package com.example.financeapp.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDate

/**
 * Modelos que reflejan el esquema real de Supabase usado por la app RN.
 * IMPORTANTE: el campo `type` usa los valores en español de la BD: "ingreso" / "gasto".
 */

object TxType {
    const val INGRESO = "ingreso"
    const val GASTO = "gasto"
}

/** Categoría embebida en joins (transactions/installments → categories). */
@Serializable
data class CategoryRef(
    val name: String? = null,
    val icon: String? = null,
    val color: String? = null,
)

@Serializable
data class Transaction(
    val id: String? = null,
    val date: LocalDate,
    val description: String,
    @SerialName("category_id") val categoryId: String? = null,
    val type: String, // "ingreso" | "gasto"
    val amount: Double,
    val notes: String? = null,
    val source: String? = null,
    @SerialName("email_id") val emailId: String? = null,
    // Embebido al pedir `*, categories(name,icon,color)`
    val categories: CategoryRef? = null,
)

@Serializable
data class Category(
    val id: String? = null,
    val name: String,
    val type: String, // "ingreso" | "gasto" | "ambos"
    val icon: String? = null,
    val color: String? = null,
    @SerialName("sort_order") val sortOrder: Int? = null,
)

@Serializable
data class Budget(
    val id: String? = null,
    @SerialName("category_id") val categoryId: String,
    val year: Int,
    val month: Int,
    val amount: Double,
    val notes: String? = null,
)

@Serializable
data class BudgetStatus(
    val year: Int? = null,
    val month: Int? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("category_name") val categoryName: String? = null,
    val icon: String? = null,
    val color: String? = null,
    @SerialName("budget_amount") val budgetAmount: Double? = null,
    val spent: Double? = null,
    val available: Double? = null,
    @SerialName("pct_used") val pctUsed: Double? = null,
    val status: String? = null,
    @SerialName("sort_order") val sortOrder: Int? = null,
)

@Serializable
data class MonthlySummary(
    val year: Int? = null,
    val month: Int? = null,
    @SerialName("total_income") val totalIncome: Double? = null,
    @SerialName("total_expenses") val totalExpenses: Double? = null,
    val balance: Double? = null,
    @SerialName("savings_pct") val savingsPct: Double? = null,
)

// ── Estados de cuenta (PDF) → "Próximos pagos" ───────────────────────────────

@Serializable
data class ForecastMonth(
    val month: String,
    val amount: Double,
)

/** Tarjeta de origen embebida en installments → statements. */
@Serializable
data class StatementRef(
    val bank: String? = null,
    @SerialName("card_last4") val cardLast4: String? = null,
)

@Serializable
data class Statement(
    val id: String,
    val bank: String? = null,
    @SerialName("card_last4") val cardLast4: String? = null,
    @SerialName("statement_date") val statementDate: LocalDate? = null,
    @SerialName("due_date") val dueDate: LocalDate? = null,
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("minimum_amount") val minimumAmount: Double = 0.0,
    val forecast: List<ForecastMonth>? = null,
    val filename: String? = null,
)

@Serializable
data class Installment(
    val id: String,
    @SerialName("statement_id") val statementId: String,
    val description: String,
    @SerialName("operation_date") val operationDate: LocalDate? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("monthly_amount") val monthlyAmount: Double = 0.0,
    @SerialName("installment_current") val installmentCurrent: Int = 0,
    @SerialName("installment_total") val installmentTotal: Int = 0,
    val categories: CategoryRef? = null,
    val statements: StatementRef? = null,
)

// ── Pagos recurrentes (suscripciones, seguros, plan celular, etc.) ───────────

object Frequency {
    const val MENSUAL = "mensual"
    const val ANUAL = "anual"
    const val SEMANAL = "semanal"
    const val QUINCENAL = "quincenal"
    val ALL = listOf(MENSUAL, ANUAL, QUINCENAL, SEMANAL)
}

@Serializable
data class RecurringPayment(
    val id: String? = null,
    val name: String,
    val amount: Double,
    @SerialName("category_id") val categoryId: String? = null,
    val frequency: String = Frequency.MENSUAL,
    @SerialName("billing_day") val billingDay: Int? = null,
    @SerialName("next_due") val nextDue: LocalDate? = null,
    val icon: String? = null,
    val color: String? = null,
    val active: Boolean = true,
    val notes: String? = null,
    val categories: CategoryRef? = null,
) {
    /** Equivalente mensual (para totalizar montos de distinta frecuencia). */
    val monthlyEquivalent: Double
        get() = when (frequency) {
            Frequency.ANUAL -> amount / 12.0
            Frequency.SEMANAL -> amount * 52.0 / 12.0
            Frequency.QUINCENAL -> amount * 2.0
            else -> amount
        }
}

/** Resultado de invocar la Edge Function `parse-statement`. */
data class StatementParseResult(
    val ok: Boolean,
    val code: String? = null,
    val error: String? = null,
    val bank: String? = null,
    val cardLast4: String? = null,
    val installments: Int = 0,
)
