package com.example.financeapp.ui.navigation

import kotlinx.serialization.Serializable

sealed interface NavRoute {
    @Serializable data object AuthGraph : NavRoute
    @Serializable data object MainGraph : NavRoute
    @Serializable data object Login : NavRoute
    @Serializable data object Dashboard : NavRoute
    @Serializable data object Transactions : NavRoute
    @Serializable data object Budgets : NavRoute
    @Serializable data object AnnualSummary : NavRoute
    @Serializable data object Profile : NavRoute
    @Serializable data object NewTransaction : NavRoute
    @Serializable data object ArchitectureOverview : NavRoute
}
