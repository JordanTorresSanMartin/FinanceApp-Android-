package com.example.financeapp.ui.screens.upcoming

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.financeapp.data.model.RecurringPayment
import com.example.financeapp.domain.PayState
import com.example.financeapp.domain.RecurringWithStatus
import com.example.financeapp.ui.components.CategoryAvatar
import com.example.financeapp.ui.theme.FinanceTheme
import com.example.financeapp.ui.viewmodel.upcoming.RecurringUiState
import com.example.financeapp.util.categoryIcon
import com.example.financeapp.util.parseHexColor
import java.time.LocalDate

/** Pestaña "Fijos": suscripciones y cuentas que se repiten (sin fecha de término). */
@Composable
internal fun FijosPage(
    state: RecurringUiState,
    contentPadding: PaddingValues,
    onAdd: () -> Unit,
    onEdit: (RecurringPayment) -> Unit,
    onRegister: (RecurringPayment) -> Unit,
) {
    // Confirmación cuando el período ya figura pagado (evita registrar dos veces).
    var confirmDuplicate by remember { mutableStateOf<RecurringPayment?>(null) }
    val today = remember { LocalDate.now() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item(key = "intro") {
            PageIntro("Suscripciones y cuentas que se repiten cada mes o año.") { AddPill(onAdd) }
            Spacer(Modifier.height(8.dp))
        }
        if (state.items.isEmpty() && !state.isLoading) {
            item(key = "empty") {
                EmptyPage(
                    Icons.Filled.Repeat, "Aún no agregas pagos fijos",
                    "Netflix, seguro, plan de celular, gimnasio… todo lo que pagas cada mes o año. " +
                        "Agrégalos y sigue su estado desde el widget \"Pagos fijos\".",
                ) { AddPill(onAdd) }
            }
        }
        itemsIndexed(state.items, key = { _, r -> r.item.id ?: r.item.name }) { i, row ->
            FijoRow(
                row, today, i, state.items.size, Modifier.animateItem(),
                registering = row.item.id in state.registering,
                onEdit = { onEdit(row.item) },
                onRegister = { if (row.status.state == PayState.PAGADO) confirmDuplicate = row.item else onRegister(row.item) },
            )
        }
    }

    confirmDuplicate?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmDuplicate = null },
            title = { Text("Ya está pagado") },
            text = { Text("\"${item.name}\" ya tiene un pago registrado este período. ¿Registrar otro pago igual?") },
            confirmButton = { TextButton(onClick = { confirmDuplicate = null; onRegister(item) }) { Text("Registrar igual") } },
            dismissButton = { TextButton(onClick = { confirmDuplicate = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun FijoRow(
    row: RecurringWithStatus,
    today: LocalDate,
    index: Int,
    count: Int,
    modifier: Modifier,
    registering: Boolean,
    onEdit: () -> Unit,
    onRegister: () -> Unit,
) {
    val item = row.item
    val finance = FinanceTheme.colors
    val color = parseHexColor(item.color ?: item.categories?.color, MaterialTheme.colorScheme.primary)
    val stateColor = when (row.status.state) {
        PayState.PAGADO -> finance.income
        PayState.ATRASADO -> finance.expense
        PayState.POR_VENCER -> finance.warning
        PayState.PENDIENTE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    GroupRow(index, count, modifier, onClick = onEdit, onClickLabel = "Editar ${item.name}") {
        CategoryAvatar(icon = categoryIcon(item.icon ?: item.categories?.icon), color = color, size = 40.dp, corner = 14.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(row.status.label(today), style = MaterialTheme.typography.bodySmall, color = stateColor, maxLines = 1)
            Text(
                buildString {
                    append(money(item.amount)).append(" · ")
                    append(frequencyLabel(item.frequency))
                    item.billingDay?.let { append(" · día $it") }
                    item.categories?.name?.let { append(" · $it") }
                },
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
            )
        }
        Spacer(Modifier.width(12.dp))
        PayButton(
            done = row.status.state == PayState.PAGADO, busy = registering, onClick = onRegister,
            label = "Registrar pago de ${item.name}", doneText = "Pagado",
        )
    }
}
