package com.example.financeapp.domain.repository

import com.example.financeapp.data.model.*

interface FinanceRepository {
    suspend fun getMonthlySummary(year: Int, month: Int): MonthlySummary?
    suspend fun getBudgetStatus(year: Int, month: Int): List<BudgetStatus>
    suspend fun getRecentTransactions(limit: Long = 10): List<Transaction>
    suspend fun getTransactionsByMonth(year: Int, month: Int): List<Transaction>
    suspend fun getCategories(): List<Category>
    suspend fun insertTransaction(transaction: Transaction)
    suspend fun upsertBudget(budget: Budget)
    suspend fun getAnnualSummary(year: Int): List<MonthlySummary>
}
