package com.example.financeapp.ui.navigation

import kotlinx.serialization.Serializable

sealed interface NavRoute {
    @Serializable data object AuthGraph : NavRoute
    @Serializable data object MainGraph : NavRoute
    @Serializable data object Login : NavRoute

    @Serializable data object Dashboard : NavRoute
    @Serializable data object Transactions : NavRoute
    /** Pestaña "Pagos": cuotas, pagos fijos y tarjetas. */
    @Serializable data object Payments : NavRoute
    /** Pestaña "Análisis": por categoría, presupuesto y anual. */
    @Serializable data object AnalysisHub : NavRoute
    @Serializable data object Profile : NavRoute

    @Serializable data class NewTransaction(val type: String = "gasto") : NavRoute
    @Serializable data class EditTransaction(val id: String) : NavRoute
    @Serializable data object Analytics : NavRoute
    @Serializable data object Upcoming : NavRoute
}
