package com.example.financeapp.ui.screens.upcoming

import com.example.financeapp.ui.components.SkeletonCardList
import com.example.financeapp.ui.components.SkeletonScope

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CreditCard
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.financeapp.domain.CuotaState
import com.example.financeapp.domain.InstallmentPlan
import com.example.financeapp.domain.byDueDay
import com.example.financeapp.domain.status
import com.example.financeapp.ui.components.CategoryAvatar
import com.example.financeapp.ui.theme.FinanceTheme
import java.time.LocalDate

/** Pestaña "Cuotas": compras en cuotas que sigues tú (y las que alguien te debe). */
@Composable
internal fun CuotasPage(
    plans: List<InstallmentPlan>,
    loading: Boolean,
    contentPadding: PaddingValues,
    onAdd: () -> Unit,
    onEdit: (InstallmentPlan) -> Unit,
    onPay: (InstallmentPlan) -> Unit,
) {
    val today = remember { LocalDate.now() }
    var confirmAdvance by remember { mutableStateOf<InstallmentPlan?>(null) }
    val sorted = remember(plans) { plans.byDueDay() }
    val active = sorted.filterNot { it.isDone }
    val done = sorted.filter { it.isDone }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item(key = "intro") {
            PageIntro("Compras en cuotas que sigues tú. Márcalas al pagar.") { AddPill(onAdd, "Agregar compra en cuotas") }
            Spacer(Modifier.height(8.dp))
        }
        if (plans.isEmpty() && loading) {
            item(key = "skeleton") { SkeletonScope { SkeletonCardList(rows = 3) } }
        } else if (plans.isEmpty()) {
            item(key = "empty") {
                EmptyPage(
                    Icons.Filled.CreditCard, "Sigue tus compras en cuotas",
                    "Indica el monto de cada cuota y cuántas son. Verás cuánto te queda y podrás pagar la del mes " +
                        "con un toque, aquí o desde el widget \"Mis cuotas\".",
                ) { AddPill(onAdd, "Agregar compra en cuotas") }
            }
        }
        itemsIndexed(active, key = { _, p -> p.id }) { i, plan ->
            CuotaRow(plan, today, i, active.size, Modifier.animateItem(), onEdit = { onEdit(plan) }) {
                if (plan.status(today).state == CuotaState.PAGADA_MES) confirmAdvance = plan else onPay(plan)
            }
        }
        if (done.isNotEmpty()) {
            item(key = "done-title") {
                Text(
                    "Terminadas", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.animateItem().padding(top = 20.dp, bottom = 6.dp, start = 4.dp),
                )
            }
            itemsIndexed(done, key = { _, p -> p.id }) { i, plan ->
                CuotaRow(plan, today, i, done.size, Modifier.animateItem(), onEdit = { onEdit(plan) }, onPay = null)
            }
        }
    }

    confirmAdvance?.let { plan ->
        AlertDialog(
            onDismissRequest = { confirmAdvance = null },
            title = { Text("¿Adelantar una cuota?") },
            text = { Text("Ya pagaste la cuota de este mes de \"${plan.name}\". ¿Marcar la cuota ${plan.paid + 1} de ${plan.total} como pagada?") },
            confirmButton = { TextButton(onClick = { confirmAdvance = null; onPay(plan) }) { Text("Adelantar") } },
            dismissButton = { TextButton(onClick = { confirmAdvance = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun CuotaRow(
    plan: InstallmentPlan,
    today: LocalDate,
    index: Int,
    count: Int,
    modifier: Modifier,
    onEdit: () -> Unit,
    onPay: (() -> Unit)?,
) {
    val finance = FinanceTheme.colors
    val st = plan.status(today)
    val stateColor = when (st.state) {
        CuotaState.PAGADA_MES, CuotaState.TERMINADA -> finance.income
        CuotaState.ATRASADA -> finance.expense
        CuotaState.POR_VENCER -> finance.warning
        CuotaState.PENDIENTE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    GroupRow(index, count, modifier, onClick = onEdit, onClickLabel = "Editar ${plan.name}") {
        CategoryAvatar(
            icon = if (plan.isDone) Icons.Filled.Celebration else Icons.Filled.CreditCard,
            color = if (plan.isDone) finance.income else MaterialTheme.colorScheme.primary,
            size = 40.dp, corner = 14.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            RowTitle(plan.name, "${plan.paid}/${plan.total}", if (plan.isDone) finance.income else MaterialTheme.colorScheme.primary)
            Text(
                if (plan.isDone) "Pagada completa · ${money(plan.totalAmount.toDouble())}" else st.label,
                style = MaterialTheme.typography.bodySmall, color = stateColor, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (!plan.isDone) {
                Spacer(Modifier.height(6.dp))
                ThinProgress(plan.progress, MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${money(plan.amount.toDouble())} c/u · quedan ${plan.remaining}" + (plan.owedBy?.let { " · paga $it" } ?: ""),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onPay != null) {
            Spacer(Modifier.width(12.dp))
            PayButton(
                done = st.state == CuotaState.PAGADA_MES, onClick = onPay,
                label = "Pagar cuota ${plan.paid + 1} de ${plan.total} de ${plan.name}",
            )
        }
    }
}
