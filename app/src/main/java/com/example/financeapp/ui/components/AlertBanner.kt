package com.example.financeapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.financeapp.ui.theme.FinanceTheme
import com.example.financeapp.util.formatK
import kotlin.math.roundToInt

data class BudgetAlertItem(val id: String, val name: String, val budget: Double, val spent: Double)

@Composable
fun AlertBanner(
    items: List<BudgetAlertItem>,
    modifier: Modifier = Modifier,
) {
    val finance = FinanceTheme.colors
    val excedidas = items.filter { it.budget > 0 && it.spent / it.budget >= 1.0 }
    val advertencia = items.filter { it.budget > 0 && it.spent / it.budget >= 0.85 && it.spent / it.budget < 1.0 }

    if (excedidas.isEmpty() && advertencia.isEmpty()) return

    var dismissed by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(true) }
    if (dismissed) return

    val hasExcedidas = excedidas.isNotEmpty()
    val accent = if (hasExcedidas) finance.expense else finance.warning
    val bannerBg = if (hasExcedidas) finance.expenseContainer else finance.warningContainer
    val onBanner = if (hasExcedidas) finance.onExpenseContainer else finance.onWarningContainer

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(bannerBg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (hasExcedidas) Icons.Filled.Error else Icons.Filled.Warning, null, tint = accent)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                if (hasExcedidas) {
                    Text(
                        "${excedidas.size} categoría(s) excedieron su presupuesto",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = onBanner,
                    )
                }
                if (advertencia.isNotEmpty()) {
                    Text(
                        "${advertencia.size} categoría(s) cerca del límite",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = onBanner,
                    )
                }
            }
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null, tint = accent,
            )
            Icon(
                Icons.Filled.Close,
                contentDescription = "Cerrar",
                tint = accent,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clip(CircleShape)
                    .clickable { dismissed = true },
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                excedidas.forEach { cat ->
                    AlertRow(
                        name = cat.name,
                        sub = "Pres. ${formatK(cat.budget)} · Gastado ${formatK(cat.spent)}",
                        pct = (cat.spent / cat.budget * 100).roundToInt(),
                        extra = "+${formatK(cat.spent - cat.budget)}",
                        rowBg = finance.expense.copy(alpha = 0.10f),
                        pctColor = finance.expense,
                        extraColor = finance.expense,
                        onBanner = onBanner,
                    )
                }
                advertencia.forEach { cat ->
                    AlertRow(
                        name = cat.name,
                        sub = "Pres. ${formatK(cat.budget)} · Gastado ${formatK(cat.spent)}",
                        pct = (cat.spent / cat.budget * 100).roundToInt(),
                        extra = "${formatK(cat.budget - cat.spent)} disp.",
                        rowBg = finance.warning.copy(alpha = 0.10f),
                        pctColor = finance.warning,
                        extraColor = finance.income,
                        onBanner = onBanner,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertRow(
    name: String,
    sub: String,
    pct: Int,
    extra: String,
    rowBg: Color,
    pctColor: Color,
    extraColor: Color,
    onBanner: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(rowBg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = onBanner)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = onBanner.copy(alpha = 0.75f))
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("$pct%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = pctColor)
            Text(extra, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = extraColor)
        }
    }
}
