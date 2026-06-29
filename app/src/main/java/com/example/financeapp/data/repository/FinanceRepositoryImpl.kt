package com.example.financeapp.data.repository

import com.example.financeapp.data.model.*
import com.example.financeapp.domain.repository.FinanceRepository
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import javax.inject.Inject

class FinanceRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest
) : FinanceRepository {

    override suspend fun getMonthlySummary(year: Int, month: Int): MonthlySummary? = withContext(Dispatchers.IO) {
        val response = postgrest["monthly_summary"]
            .select {
                filter {
                    eq("year", year)
                    eq("month", month)
                }
            }
        response.decodeSingleOrNull<MonthlySummary>()
    }

    override suspend fun getBudgetStatus(year: Int, month: Int): List<BudgetStatus> = withContext(Dispatchers.IO) {
        val response = postgrest["budget_status"]
            .select {
                filter {
                    eq("year", year)
                    eq("month", month)
                }
            }
        response.decodeList<BudgetStatus>()
    }

    override suspend fun getRecentTransactions(limit: Long): List<Transaction> = withContext(Dispatchers.IO) {
        val response = postgrest["transactions"]
            .select {
                range(0, limit - 1)
                order("date", Order.DESCENDING)
            }
        response.decodeList<Transaction>()
    }

    override suspend fun getTransactionsByMonth(year: Int, month: Int): List<Transaction> = withContext(Dispatchers.IO) {
        val startDate = LocalDate(year, month, 1)
        val lastDay = when (month) {
            4, 6, 9, 11 -> 30
            2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
            else -> 31
        }
        val endDate = LocalDate(year, month, lastDay)
        
        val response = postgrest["transactions"]
            .select {
                filter {
                    gte("date", startDate)
                    lte("date", endDate)
                }
                order("date", Order.DESCENDING)
            }
        response.decodeList<Transaction>()
    }

    override suspend fun getCategories(): List<Category> = withContext(Dispatchers.IO) {
        val response = postgrest["categories"].select()
        response.decodeList<Category>()
    }

    override suspend fun insertTransaction(transaction: Transaction) {
        withContext(Dispatchers.IO) {
            postgrest["transactions"].insert(transaction)
        }
    }

    override suspend fun upsertBudget(budget: Budget) {
        withContext(Dispatchers.IO) {
            postgrest["budgets"].upsert(budget) {
                onConflict = "category_id,year,month"
            }
        }
    }

    override suspend fun getAnnualSummary(year: Int): List<MonthlySummary> = withContext(Dispatchers.IO) {
        val response = postgrest["monthly_summary"]
            .select {
                filter {
                    eq("year", year)
                }
                order("month", Order.ASCENDING)
            }
        response.decodeList<MonthlySummary>()
    }
}
