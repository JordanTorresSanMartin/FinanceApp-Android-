package com.example.financeapp.domain.use_case

import com.example.financeapp.data.model.BudgetStatus
import com.example.financeapp.data.model.MonthlySummary
import com.example.financeapp.data.model.Transaction
import com.example.financeapp.domain.repository.FinanceRepository
import javax.inject.Inject

data class DashboardData(
    val summary: MonthlySummary?,
    val budgetStatus: List<BudgetStatus>,
    val recentTransactions: List<Transaction>
)

class GetDashboardDataUseCase @Inject constructor(
    private val repository: FinanceRepository
) {
    suspend operator fun invoke(year: Int, month: Int): DashboardData {
        val summary = repository.getMonthlySummary(year, month)
        val budgetStatus = repository.getBudgetStatus(year, month)
        val recentTransactions = repository.getRecentTransactions(5)
        
        return DashboardData(
            summary = summary,
            budgetStatus = budgetStatus,
            recentTransactions = recentTransactions
        )
    }
}
