package com.example.financeapp.ui.screens.analysis

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.financeapp.ui.screens.analytics.AnalyticsScreen
import com.example.financeapp.ui.screens.budgets.BudgetsScreen
import com.example.financeapp.ui.screens.summary.AnnualSummaryScreen
import com.example.financeapp.util.LocalAppHaptics
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Pestaña "Análisis": junta lo que antes estaba repartido (Análisis escondido en Inicio, Presupuesto
 * y Resumen como pestañas propias). Cada vista conserva su selector de mes / año.
 */
@Composable
fun AnalysisHub() {
    val tabs = listOf("Categorías", "Presupuesto", "Anual")
    val pager = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    val haptics = LocalAppHaptics.current
    // Detent físico al cruzar a otra pestaña (igual que en Pagos).
    LaunchedEffect(pager) { snapshotFlow { pager.currentPage }.drop(1).collect { haptics?.tick() } }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Text(
            "Análisis", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
            modifier = Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
        )
        PrimaryTabRow(selectedTabIndex = pager.currentPage, containerColor = MaterialTheme.colorScheme.background, divider = {}) {
            tabs.forEachIndexed { i, t ->
                Tab(
                    selected = pager.currentPage == i,
                    onClick = { scope.launch { pager.animateScrollToPage(i) } },
                    text = { Text(t, fontWeight = if (pager.currentPage == i) FontWeight.Bold else FontWeight.Medium) },
                )
            }
        }
        HorizontalPager(
            state = pager,
            // Las vistas internas traen su propio statusBarsPadding: aquí ya se usó, así no queda un hueco.
            modifier = Modifier.fillMaxWidth().weight(1f).consumeWindowInsets(WindowInsets.statusBars),
            beyondViewportPageCount = 0,
        ) { page ->
            when (page) {
                0 -> AnalyticsScreen(onBack = {}, showBack = false)
                1 -> BudgetsScreen()
                else -> AnnualSummaryScreen()
            }
        }
    }
}
