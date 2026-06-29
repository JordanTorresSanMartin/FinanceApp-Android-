@file:OptIn(kotlin.time.ExperimentalTime::class)
package com.example.financeapp.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Instant

@Serializable
data class Transaction(
    val id: String? = null,
    val date: LocalDate,
    val description: String,
    @SerialName("category_id") val categoryId: String? = null,
    val type: String, // "income" or "expense"
    val amount: Double,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: Instant? = null,
    @SerialName("user_id") val userId: String? = null
)

@Serializable
data class Category(
    val id: String? = null,
    val name: String,
    val type: String,
    val icon: String? = null,
    val color: String? = null,
    @SerialName("sort_order") val sortOrder: Int? = null,
    @SerialName("created_at") val createdAt: Instant? = null,
    @SerialName("user_id") val userId: String? = null
)

@Serializable
data class Budget(
    val id: String? = null,
    @SerialName("category_id") val categoryId: String,
    val year: Int,
    val month: Int,
    val amount: Double,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: Instant? = null,
    @SerialName("user_id") val userId: String? = null
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
    val status: String? = null
)

@Serializable
data class MonthlySummary(
    val year: Int? = null,
    val month: Int? = null,
    @SerialName("total_income") val totalIncome: Double? = null,
    @SerialName("total_expenses") val totalExpenses: Double? = null,
    val balance: Double? = null,
    @SerialName("savings_pct") val savingsPct: Double? = null
)

@Serializable
data class UserBankLink(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("fintoc_link_token") val fintocLinkToken: String,
    @SerialName("fintoc_link_id") val fintocLinkId: String,
    @SerialName("institution_name") val institutionName: String? = null,
    @SerialName("created_at") val createdAt: Instant? = null
)
