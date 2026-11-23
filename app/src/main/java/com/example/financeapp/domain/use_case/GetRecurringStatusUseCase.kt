package com.example.financeapp.domain.use_case

import com.example.financeapp.domain.RecurringWithStatus
import com.example.financeapp.domain.paymentStatus
import com.example.financeapp.domain.repository.FinanceRepository
import com.example.financeapp.domain.sortedByUrgency
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.LocalDate
import javax.inject.Inject

/** Pagos fijos activos con su estado del período actual. Lo usan la app y el widget. */
class GetRecurringStatusUseCase @Inject constructor(
    private val repository: FinanceRepository,
) {
    suspend operator fun invoke(today: java.time.LocalDate = java.time.LocalDate.now()): List<RecurringWithStatus> =
        coroutineScope {
            val items = async { repository.getRecurringPayments() }
            // Desde el 1 de enero: cubre el período más largo (anual).
            val paid = async { repository.getRecurringPaymentDates(LocalDate(today.year, 1, 1)) }
            val dates = paid.await()
            items.await().map { item ->
                RecurringWithStatus(
                    item = item,
                    status = paymentStatus(item, dates[item.name.trim().lowercase()].orEmpty(), today),
                )
            }.sortedByUrgency()
        }
}
