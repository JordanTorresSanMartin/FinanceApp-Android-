package com.example.financeapp.data.repository

import com.example.financeapp.data.model.*
import com.example.financeapp.domain.RECURRING_NOTE
import com.example.financeapp.domain.INSTALLMENT_NOTE
import com.example.financeapp.domain.repository.FinanceRepository
import com.example.financeapp.util.toJavaLocalDate
import com.example.financeapp.util.DataVersion
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.exceptions.RestException
import android.util.Log
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
    private val tracking: com.example.financeapp.data.local.TrackingStore,
) : FinanceRepository {

    override fun currentUserId(): String? = auth.currentUserOrNull()?.id
    override fun currentUserEmail(): String? = auth.currentUserOrNull()?.email
    // Sin red, signOut() falla en el servidor: igual borramos la sesión local.
    override suspend fun signOut() {
        runCatching { auth.signOut() }.onFailure { auth.clearSession() }
    }

    // ── Resúmenes / presupuesto ──────────────────────────────────────────────
    override suspend fun getMonthlySummary(year: Int, month: Int): MonthlySummary? =
        withContext(Dispatchers.IO) {
            // El saldo se ARRASTRA entre meses: julio no parte de 0, hereda lo que
            // sobró de junio. La vista trae una fila por mes con movimientos; el
            // acumulado se resuelve aquí para que un mes sin movimientos también
            // herede el saldo anterior. (Una fila por mes: volumen mínimo.)
            val all = postgrest["monthly_summary"].select().decodeList<MonthlySummary>()
            val upTo = all.filter { s ->
                val y = s.year ?: return@filter false
                val m = s.month ?: return@filter false
                y < year || (y == year && m <= month)
            }.sortedWith(compareBy({ it.year ?: 0 }, { it.month ?: 0 }))
            val target = java.time.YearMonth.of(year, month)
            val start = tracking.start
            // Con fecha de inicio, un mes sin movimientos igual muestra el saldo inicial.
            if (upTo.isEmpty() && (start == null || target.isBefore(start))) return@withContext null

            val cumulative = com.example.financeapp.domain.cumulativeBalance(
                upTo.associate { java.time.YearMonth.of(it.year!!, it.month!!) to (it.balance ?: 0.0) },
                target, start, tracking.initialBalance,
            )
            if (upTo.isEmpty()) return@withContext MonthlySummary(
                year = year, month = month, totalIncome = 0.0, totalExpenses = 0.0,
                balance = 0.0, savingsPct = 0.0, cumulativeBalance = cumulative,
            )
            val latest = upTo.last()
            if (latest.year == year && latest.month == month) {
                latest.copy(cumulativeBalance = cumulative)
            } else {
                // Mes sin movimientos: neto 0 pero con el acumulado heredado.
                MonthlySummary(
                    year = year, month = month,
                    totalIncome = 0.0, totalExpenses = 0.0,
                    balance = 0.0, savingsPct = 0.0,
                    cumulativeBalance = cumulative,
                )
            }
        }

    override suspend fun getBudgetStatus(year: Int, month: Int): List<BudgetStatus> =
        withContext(Dispatchers.IO) {
            // Nota: la vista budget_status NO tiene columna sort_order (sí la tiene
            // `categories`). Ordenamos por category_name para un orden estable.
            postgrest["budget_status"].select {
                filter { eq("year", year); eq("month", month) }
                order("category_name", Order.ASCENDING)
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
        DataVersion.bump()
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

    override suspend fun saveCategory(id: String?, name: String, type: String, icon: String?, color: String?) {
        DataVersion.bump()
        withContext(Dispatchers.IO) {
            val body = buildJsonObject {
                put("name", name); put("type", type); put("icon", icon); put("color", color)
            }
            if (id != null) {
                postgrest["categories"].update(body) { filter { eq("id", id) } }
            } else {
                // Las categorías del usuario llevan user_id; si la tabla no lo tiene, se inserta sin él.
                runCatching {
                    postgrest["categories"].insert(buildJsonObject { body.forEach { (k, v) -> put(k, v) }; put("user_id", requireUserId()) })
                }.recoverCatching { postgrest["categories"].insert(body) }.getOrThrow()
            }
        }
    }

    override suspend fun registerInstallmentExpense(name: String, n: Int, total: Int, amount: Double, date: LocalDate) {
        // Categoría: la que usaste antes para esa compra; si no, una de crédito/deuda/cuotas.
        val cats = runCatching { getCategories() }.getOrDefault(emptyList()).filter { it.type != TxType.INGRESO }
        val category = runCatching { com.example.financeapp.domain.suggestCategory(getCategoryHistory(), name) }.getOrNull()
            ?.takeIf { id -> cats.any { it.id == id } }
            ?: cats.firstOrNull { Regex("cuota|cr[eé]dito|deuda", RegexOption.IGNORE_CASE).containsMatchIn(it.name) }?.id
        insertTransaction(date, "$name · cuota $n/$total", category, TxType.GASTO, amount, INSTALLMENT_NOTE)
    }

    override suspend fun undoInstallmentExpense(name: String) {
        val tx = withContext(Dispatchers.IO) {
            postgrest["transactions"].select {
                filter { eq("notes", INSTALLMENT_NOTE); like("description", "$name · cuota %") }
                order("date", Order.DESCENDING)
                limit(1)
            }.decodeList<Transaction>()
        }.firstOrNull() ?: return
        deleteTransaction(tx.id ?: return)
    }

    @kotlinx.serialization.Serializable
    private data class DescCategory(
        val description: String? = null,
        @kotlinx.serialization.SerialName("category_id") val categoryId: String? = null,
    )

    override suspend fun getCategoryHistory(): Map<String, String> = withContext(Dispatchers.IO) {
        postgrest["transactions"].select(Columns.list("description", "category_id")) {
            order("date", Order.DESCENDING)
            limit(600)
        }.decodeList<DescCategory>()
            .mapNotNull { r -> r.categoryId?.let { c -> r.description?.let { com.example.financeapp.domain.normalizeDescription(it) to c } } }
            .filter { it.first.isNotEmpty() }
            .distinctBy { it.first } // la primera es la más reciente
            .toMap()
    }

    override suspend fun getTransactionsByMonth(year: Int, month: Int): List<Transaction> =
        withContext(Dispatchers.IO) {
            val start = LocalDate(year, month, 1)
            val end = LocalDate(year, month, java.time.YearMonth.of(year, month).lengthOfMonth())
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
        DataVersion.bump()
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
        DataVersion.bump()
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
        DataVersion.bump()
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
        var token = auth.currentAccessTokenOrNull()
        if (token == null) {
            runCatching { auth.refreshCurrentSession() }
            token = auth.currentAccessTokenOrNull()
        }
        if (token == null) {
            return@withContext StatementParseResult(
                ok = false, error = "Tu sesión expiró. Inicia sesión de nuevo.",
            )
        }
        val payload = buildJsonObject {
            put("pdf_base64", pdfBase64)
            put("filename", filename)
            if (password != null) put("password", password)
        }
        // Si la función responde 4xx/5xx, supabase-kt lanza RestException y el mensaje real del
        // servidor ({ok, code, error}) se perdía: el usuario solo veía "No se pudo…". Lo leemos igual.
        val body = try {
            functions.invoke(
                function = "parse-statement",
                body = payload,
                headers = Headers.build {
                    append(HttpHeaders.ContentType, "application/json")
                    // NO agregar Authorization: supabase-kt ya envía "Bearer <jwt de la sesión>".
                    // Duplicarlo llegaba al servidor como "Bearer X, Bearer X" → 401 "No autorizado".
                },
            ).bodyAsText()
        } catch (e: RestException) {
            Log.w("parse-statement", "HTTP ${e.statusCode}: ${e.error} ${e.description.orEmpty()}")
            runCatching { e.response.bodyAsText() }.getOrNull()
                ?.takeIf { it.trimStart().startsWith("{") }
                ?: return@withContext StatementParseResult(
                    ok = false, error = "El servidor no pudo leer el PDF (${e.statusCode}): ${e.description ?: e.error}",
                )
        }
        val obj: JsonObject = runCatching { tolerantJson.parseToJsonElement(body).jsonObject }.getOrElse {
            return@withContext StatementParseResult(ok = false, error = "Respuesta inesperada del servidor al leer el PDF.")
        }
        StatementParseResult(
            ok = obj["ok"]?.jsonPrimitive?.booleanOrNull ?: false,
            code = obj["code"]?.jsonPrimitive?.contentOrNull,
            error = obj["error"]?.jsonPrimitive?.contentOrNull,
            bank = obj["bank"]?.jsonPrimitive?.contentOrNull,
            cardLast4 = obj["card_last4"]?.jsonPrimitive?.contentOrNull,
            installments = obj["installments"]?.jsonPrimitive?.intOrNull ?: 0,
        )
    }

    // ── Pagos recurrentes ────────────────────────────────────────────────────
    override suspend fun getRecurringPayments(): List<RecurringPayment> = withContext(Dispatchers.IO) {
        postgrest["recurring_payments"].select(Columns.raw("*, categories(name,icon,color)")) {
            filter { eq("active", true) }
            order("name", Order.ASCENDING)
        }.decodeList<RecurringPayment>()
    }

    override suspend fun upsertRecurringPayment(
        id: String?, name: String, amount: Double, categoryId: String?,
        frequency: String, billingDay: Int?, notes: String?,
    ) {
        DataVersion.bump()
        withContext(Dispatchers.IO) {
            val userId = requireUserId()
            postgrest["recurring_payments"].upsert(
                buildJsonObject {
                    if (id != null) put("id", id)
                    put("user_id", userId)
                    put("name", name)
                    put("amount", amount)
                    put("category_id", categoryId)
                    put("frequency", frequency)
                    put("billing_day", billingDay)
                    put("notes", notes)
                    put("active", true)
                }
            )
        }
    }

    override suspend fun deleteRecurringPayment(id: String) {
        DataVersion.bump()
        withContext(Dispatchers.IO) {
            postgrest["recurring_payments"].delete { filter { eq("id", id) } }
        }
    }

    override suspend fun getRecurringPaymentDates(since: LocalDate): Map<String, List<java.time.LocalDate>> =
        withContext(Dispatchers.IO) {
            postgrest["transactions"].select {
                filter { eq("notes", RECURRING_NOTE); gte("date", since) }
            }.decodeList<Transaction>()
                .groupBy({ it.description.trim().lowercase() }, { it.date.toJavaLocalDate() })
        }

    /** Si la categoría del pago fijo es de ingreso (ej. Sueldo), se registra como ingreso. */
    override suspend fun registerRecurringPayment(item: RecurringPayment, date: LocalDate) =
        insertTransaction(
            date = date,
            description = item.name,
            categoryId = item.categoryId,
            type = if (categoryType(item.categoryId) == TxType.INGRESO) TxType.INGRESO else TxType.GASTO,
            amount = item.amount,
            notes = RECURRING_NOTE,
        )

    private suspend fun categoryType(id: String?): String? = id?.let {
        withContext(Dispatchers.IO) {
            runCatching {
                postgrest["categories"].select { filter { eq("id", it) } }.decodeList<Category>().firstOrNull()?.type
            }.getOrNull()
        }
    }

    override suspend fun undoRecurringPayment(item: RecurringPayment, date: LocalDate) {
        val tx = withContext(Dispatchers.IO) {
            postgrest["transactions"].select {
                filter { eq("notes", RECURRING_NOTE); eq("date", date.toString()); eq("description", item.name) }
            }.decodeList<Transaction>()
        }.lastOrNull() ?: return
        deleteTransaction(tx.id ?: return)
    }

    private fun requireUserId(): String =
        currentUserId() ?: throw IllegalStateException("Usuario no autenticado")

}
