package com.example.financeapp.domain.repository

import com.example.financeapp.data.model.*
import kotlinx.datetime.LocalDate

interface FinanceRepository {
    // Sesión
    fun currentUserId(): String?
    fun currentUserEmail(): String?
    suspend fun signOut()

    // Resúmenes / presupuesto
    suspend fun getMonthlySummary(year: Int, month: Int): MonthlySummary?
    suspend fun getBudgetStatus(year: Int, month: Int): List<BudgetStatus>
    suspend fun getAnnualSummary(year: Int): List<MonthlySummary>
    suspend fun getBudgetsForMonth(year: Int, month: Int): List<Budget>
    suspend fun upsertBudget(categoryId: String, year: Int, month: Int, amount: Double)

    // Categorías / transacciones
    suspend fun getCategories(): List<Category>
    suspend fun getTransactionsByMonth(year: Int, month: Int): List<Transaction>
    suspend fun getTransactionById(id: String): Transaction?
    suspend fun insertTransaction(
        date: LocalDate, description: String, categoryId: String?,
        type: String, amount: Double, notes: String?,
    )
    suspend fun updateTransaction(
        id: String, date: LocalDate, description: String, categoryId: String?,
        type: String, amount: Double, notes: String?,
    )
    suspend fun deleteTransaction(id: String)

    // Estados de cuenta (PDF) → próximos pagos
    suspend fun getStatements(): List<Statement>
    suspend fun getInstallments(statementIds: List<String>): List<Installment>
    suspend fun uploadStatementPdf(pdfBase64: String, filename: String, password: String?): StatementParseResult

    // Pagos recurrentes (suscripciones, seguros, etc.)
    suspend fun getRecurringPayments(): List<RecurringPayment>
    suspend fun upsertRecurringPayment(
        id: String?, name: String, amount: Double, categoryId: String?,
        frequency: String, billingDay: Int?, notes: String?,
    )
    suspend fun deleteRecurringPayment(id: String)
}
