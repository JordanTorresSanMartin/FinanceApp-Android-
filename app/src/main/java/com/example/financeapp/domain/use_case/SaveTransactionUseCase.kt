package com.example.financeapp.domain.use_case

import com.example.financeapp.data.model.Transaction
import com.example.financeapp.domain.repository.FinanceRepository
import javax.inject.Inject

class SaveTransactionUseCase @Inject constructor(
    private val repository: FinanceRepository
) {
    suspend operator fun invoke(transaction: Transaction) {
        repository.insertTransaction(transaction)
    }
}
