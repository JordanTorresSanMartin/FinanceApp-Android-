package com.example.financeapp.domain.use_case

import com.example.financeapp.domain.repository.FinanceRepository
import kotlinx.datetime.LocalDate
import javax.inject.Inject

class SaveTransactionUseCase @Inject constructor(
    private val repository: FinanceRepository
) {
    suspend operator fun invoke(
        date: LocalDate,
        description: String,
        categoryId: String?,
        type: String,
        amount: Double,
        notes: String?,
    ) = repository.insertTransaction(date, description, categoryId, type, amount, notes)
}
