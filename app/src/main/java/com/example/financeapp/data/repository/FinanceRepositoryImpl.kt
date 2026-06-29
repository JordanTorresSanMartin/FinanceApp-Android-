package com.example.financeapp.data.repository

import com.example.financeapp.data.model.*
import com.example.financeapp.domain.repository.FinanceRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.invoke
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject

private const val TX_WITH_CATEGORY = "*, categories(name,icon,color)"
private val tolerantJson = Json { ignoreUnknownKeys = true }

class FinanceRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth,
    private val functions: Functions,
) : FinanceRepository {

    override fun currentUserId(): String? = auth.currentUserOrNull()?.id
    override fun currentUserEmail(): String? = auth.currentUserOrNull()?.email
    override suspend fun signOut() = auth.signOut()

    // ── Resúmenes / presupuesto ──────────────────────────────────────────────
    override suspend fun getMonthlySummary(year: Int, month: Int): MonthlySummary? =
        withContext(Dispatchers.IO) {
            postgrest["monthly_summary"].select {
                filter { eq("year", year); eq("month", month) }
            }.decodeSingleOrNull<MonthlySummary>()
        }

    override suspend fun getBudgetStatus(year: Int, month: Int): List<BudgetStatus> =
        withContext(Dispatchers.IO) {
            postgrest["budget_status"].select {
                filter { eq("year", year); eq("month", month) }
                order("sort_order", Order.ASCENDING)
            }.decodeList<BudgetStatus>()
        }

    override suspend fun getAnnualSummary(year: Int): List<MonthlySummary> =
        withContext(Dispatchers.IO) {
            postgrest["monthly_summary"].select {
                filter { eq("year", year) }
                order("month", Order.ASCENDING)
            }.decodeList<MonthlySummary>()
        }

    override suspend fun getBudgetsForMonth(year: Int, month: Int): List<Budget> =
        withContext(Dispatchers.IO) {
            postgrest["budgets"].select {
                filter { eq("year", year); eq("month", month) }
            }.decodeList<Budget>()
        }

    override suspend fun upsertBudget(categoryId: String, year: Int, month: Int, amount: Double) {
        withContext(Dispatchers.IO) {
            val userId = requireUserId()
            postgrest["budgets"].upsert(
                buildJsonObject {
                    put("user_id", userId)
                    put("category_id", categoryId)
                    put("year", year)
                    put("month", month)
                    put("amount", amount)
                }
            ) { onConflict = "user_id,category_id,year,month" }
        }
    }

    // ── Categorías / transacciones ───────────────────────────────────────────
    override suspend fun getCategories(): List<Category> = withContext(Dispatchers.IO) {
        postgrest["categories"].select {
            order("sort_order", Order.ASCENDING)
        }.decodeList<Category>()
    }

    override suspend fun getTransactionsByMonth(year: Int, month: Int): List<Transaction> =
        withContext(Dispatchers.IO) {
            val start = LocalDate(year, month, 1)
            val end = LocalDate(year, month, lastDayOfMonth(year, month))
            postgrest["transactions"].select(Columns.raw(TX_WITH_CATEGORY)) {
                filter { gte("date", start); lte("date", end) }
                order("date", Order.DESCENDING)
            }.decodeList<Transaction>()
        }

    override suspend fun getTransactionById(id: String): Transaction? =
        withContext(Dispatchers.IO) {
            postgrest["transactions"].select(Columns.raw(TX_WITH_CATEGORY)) {
                filter { eq("id", id) }
            }.decodeSingleOrNull<Transaction>()
        }

    override suspend fun insertTransaction(
        date: LocalDate, description: String, categoryId: String?,
        type: String, amount: Double, notes: String?,
    ) {
        withContext(Dispatchers.IO) {
            val userId = requireUserId()
            postgrest["transactions"].insert(
                buildJsonObject {
                    put("user_id", userId)
                    put("date", date.toString())
                    put("description", description)
                    put("category_id", categoryId)
                    put("type", type)
                    put("amount", amount)
                    put("notes", notes)
                }
            )
        }
    }

    override suspend fun updateTransaction(
        id: String, date: LocalDate, description: String, categoryId: String?,
        type: String, amount: Double, notes: String?,
    ) {
        withContext(Dispatchers.IO) {
            postgrest["transactions"].update(
                buildJsonObject {
                    put("date", date.toString())
                    put("description", description)
                    put("category_id", categoryId)
                    put("type", type)
                    put("amount", amount)
                    put("notes", notes)
                }
            ) { filter { eq("id", id) } }
        }
    }

    override suspend fun deleteTransaction(id: String) {
        withContext(Dispatchers.IO) {
            postgrest["transactions"].delete { filter { eq("id", id) } }
        }
    }

    // ── Estados de cuenta (PDF) ──────────────────────────────────────────────
    override suspend fun getStatements(): List<Statement> = withContext(Dispatchers.IO) {
        postgrest["statements"].select {
            order("statement_date", Order.DESCENDING)
        }.decodeList<Statement>()
    }

    override suspend fun getInstallments(statementIds: List<String>): List<Installment> =
        withContext(Dispatchers.IO) {
            if (statementIds.isEmpty()) return@withContext emptyList()
            postgrest["installments"].select(
                Columns.raw("*, categories(name,icon,color), statements(bank,card_last4)")
            ) {
                filter { isIn("statement_id", statementIds) }
                order("monthly_amount", Order.DESCENDING)
            }.decodeList<Installment>()
        }

    override suspend fun uploadStatementPdf(
        pdfBase64: String, filename: String, password: String?,
    ): StatementParseResult = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            put("pdf_base64", pdfBase64)
            put("filename", filename)
            if (password != null) put("password", password)
        }
        val response = functions.invoke(
            function = "parse-statement",
            body = payload,
            headers = Headers.build { append(HttpHeaders.ContentType, "application/json") },
        )
        val obj: JsonObject = tolerantJson.parseToJsonElement(response.bodyAsText()).jsonObject
        StatementParseResult(
            ok = obj["ok"]?.jsonPrimitive?.booleanOrNull ?: false,
            code = obj["code"]?.jsonPrimitive?.contentOrNull,
            error = obj["error"]?.jsonPrimitive?.contentOrNull,
            bank = obj["bank"]?.jsonPrimitive?.contentOrNull,
            cardLast4 = obj["card_last4"]?.jsonPrimitive?.contentOrNull,
            installments = obj["installments"]?.jsonPrimitive?.intOrNull ?: 0,
        )
    }

    private fun requireUserId(): String =
        currentUserId() ?: throw IllegalStateException("Usuario no autenticado")

    private fun lastDayOfMonth(year: Int, month: Int): Int = when (month) {
        4, 6, 9, 11 -> 30
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        else -> 31
    }
}
