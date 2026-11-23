package com.example.financeapp.ui.screens.upcoming

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.financeapp.domain.CuotaState
import com.example.financeapp.domain.InstallmentPlan
import com.example.financeapp.domain.byDueDay
import com.example.financeapp.domain.status
import com.example.financeapp.domain.withPaidCount
import com.example.financeapp.ui.components.pressable
import com.example.financeapp.ui.theme.FinanceTheme
import com.example.financeapp.util.formatMoneyCLP
import com.example.financeapp.util.groupThousands
import java.time.LocalDate
import java.util.UUID

private fun money(v: Long) = formatMoneyCLP(v.toDouble())

@Composable
fun InstallmentSection(
    plans: List<InstallmentPlan>,
    onAdd: () -> Unit,
    onEdit: (InstallmentPlan) -> Unit,
    onPay: (InstallmentPlan) -> Unit,
) {
    val today = remember { LocalDate.now() }
    var confirmAdvance by remember { mutableStateOf<InstallmentPlan?>(null) }
    val sorted = remember(plans) { plans.byDueDay() }
    val active = sorted.filterNot { it.isDone }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Mis cuotas", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            AddPill(onAdd, "Agregar compra en cuotas")
        }

        if (plans.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Sigue tus compras en cuotas", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Indica el monto de cada cuota y cuántas son. Verás cuánto te queda y podrás " +
                        "pagar la del mes con un toque, aquí o desde el widget \"Mis cuotas\".",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            SummaryCard(active, today)
            sorted.forEach { plan ->
                InstallmentCard(
                    plan = plan,
                    today = today,
                    onEdit = { onEdit(plan) },
                    onPay = {
                        if (plan.status(today).state == CuotaState.PAGADA_MES) confirmAdvance = plan else onPay(plan)
                    },
                )
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
private fun SummaryCard(active: List<InstallmentPlan>, today: LocalDate) {
    val onC = MaterialTheme.colorScheme.onPrimaryContainer
    val left = active.sumOf { it.remainingAmount }
    val total = active.sumOf { it.totalAmount }
    val paidMonth = active.count { it.status(today).state == CuotaState.PAGADA_MES }
    val progress by animateFloatAsState(
        targetValue = if (total == 0L) 1f else 1f - left.toFloat() / total,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "summaryProgress",
    )
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.primaryContainer).padding(20.dp),
    ) {
        Text("Te quedan por pagar", style = MaterialTheme.typography.labelLarge, color = onC.copy(alpha = 0.85f))
        Text(
            if (active.isEmpty()) "Nada, ¡estás al día!" else money(left),
            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = onC,
        )
        if (active.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = onC,
                trackColor = onC.copy(alpha = 0.16f),
                strokeCap = StrokeCap.Round,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "${active.sumOf { it.remaining }} cuotas en ${active.size} " +
                    (if (active.size == 1) "compra" else "compras") + " · este mes $paidMonth de ${active.size} pagadas",
                style = MaterialTheme.typography.labelMedium, color = onC.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun InstallmentCard(plan: InstallmentPlan, today: LocalDate, onEdit: () -> Unit, onPay: () -> Unit) {
    val finance = FinanceTheme.colors
    val st = plan.status(today)
    val (fg, bg) = when (st.state) {
        CuotaState.PAGADA_MES, CuotaState.TERMINADA -> finance.income to finance.incomeContainer
        CuotaState.ATRASADA -> finance.expense to finance.expenseContainer
        CuotaState.POR_VENCER -> finance.warning to finance.warningContainer
        CuotaState.PENDIENTE -> MaterialTheme.colorScheme.onSurfaceVariant to MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val barColor = if (plan.isDone) finance.income else MaterialTheme.colorScheme.primary
    // Resorte suave: la barra "avanza" visiblemente al pagar.
    val progress by animateFloatAsState(
        targetValue = plan.progress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "planProgress",
    )

    Column(
        modifier = Modifier.fillMaxWidth()
            .pressable(onClick = onEdit, onClickLabel = "Editar ${plan.name}")
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (plan.isDone) finance.incomeContainer else MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (plan.isDone) Icons.Filled.Celebration else Icons.Filled.CreditCard, null,
                    tint = if (plan.isDone) finance.income else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(plan.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${money(plan.amount)} por cuota", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.clearAndSetSemantics { contentDescription = "${plan.paid} de ${plan.total} cuotas pagadas" },
            ) {
                Text("${plan.paid}/${plan.total}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = barColor)
                Text("cuotas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(14.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(10.dp),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round,
        )
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!plan.isDone) {
                    Text(
                        "Quedan ${plan.remaining} · ${money(plan.remainingAmount)}",
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                    )
                }
                // Estado: color + texto (el color nunca es la única señal).
                Row(
                    modifier = Modifier.clip(CircleShape).background(bg).padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (st.state == CuotaState.PAGADA_MES || st.state == CuotaState.TERMINADA) {
                        Icon(Icons.Filled.CheckCircle, null, tint = fg, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(st.label, style = MaterialTheme.typography.labelMedium, color = fg, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
            when {
                plan.isDone -> Unit
                st.state == CuotaState.PAGADA_MES -> OutlinedButton(
                    onClick = onPay,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text("Adelantar") }
                else -> Button(
                    onClick = onPay,
                    modifier = Modifier.heightIn(min = 48.dp),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                ) {
                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Pagar cuota", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentEditorSheet(
    editing: InstallmentPlan?,
    onDismiss: () -> Unit,
    onSave: (InstallmentPlan) -> Unit,
    onDelete: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by rememberSaveable { mutableStateOf(editing?.name ?: "") }
    var amount by rememberSaveable { mutableStateOf(editing?.amount?.toString() ?: "") }
    var total by rememberSaveable { mutableStateOf(editing?.total?.toString() ?: "") }
    var paid by rememberSaveable { mutableStateOf(editing?.paid?.takeIf { it > 0 }?.toString() ?: "") }
    var dueDay by rememberSaveable { mutableStateOf(editing?.dueDay?.toString() ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }

    val amountV = amount.toLongOrNull() ?: 0L
    val totalV = total.toIntOrNull() ?: 0
    val paidV = paid.toIntOrNull() ?: 0
    val dayV = dueDay.toIntOrNull()
    val totalError = total.isNotEmpty() && totalV !in 1..360
    val paidError = paid.isNotEmpty() && totalV > 0 && paidV > totalV
    val dayError = dueDay.isNotEmpty() && (dayV == null || dayV !in 1..31)
    val canSave = name.isNotBlank() && amountV > 0 && totalV in 1..360 && !paidError && !dayError

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(if (editing == null) "Nueva compra en cuotas" else "Editar cuotas", style = MaterialTheme.typography.titleLarge)

            SheetField(name, { name = it }, "¿Qué compraste? (Notebook, celular…)")
            SheetField(groupThousands(amount), { amount = it.filter(Char::isDigit).take(10) }, "Monto de cada cuota (CLP)", KeyboardType.Number)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) {
                    SheetField(total, { total = it.filter(Char::isDigit).take(3) }, "N° de cuotas", KeyboardType.Number,
                        error = if (totalError) "Entre 1 y 360" else null)
                }
                Box(Modifier.weight(1f)) {
                    SheetField(paid, { paid = it.filter(Char::isDigit).take(3) }, "Ya pagadas", KeyboardType.Number,
                        error = if (paidError) "Máximo $totalV" else null)
                }
            }
            SheetField(
                dueDay, { dueDay = it.filter(Char::isDigit).take(2) }, "Día de vencimiento (opcional)", KeyboardType.Number,
                imeAction = ImeAction.Done,
                error = if (dayError) "Debe ser un día entre 1 y 31" else null,
            )

            // Vista previa en vivo: el usuario ve el resultado antes de guardar.
            if (amountV > 0 && totalV in 1..360 && !paidError) {
                val left = (totalV - paidV).coerceAtLeast(0)
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "$totalV cuotas de ${money(amountV)} = ${money(amountV * totalV)}",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    LinearProgressIndicator(
                        progress = { paidV.toFloat() / totalV },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f),
                        strokeCap = StrokeCap.Round,
                    )
                    Text(
                        if (left == 0) "Ya está pagada completa" else "Te quedan $left por ${money(amountV * left)}",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            Button(
                onClick = {
                    val base = editing ?: InstallmentPlan(id = UUID.randomUUID().toString(), name = "", amount = 0, total = 1)
                    onSave(base.copy(name = name.trim(), amount = amountV, total = totalV, dueDay = dayV).withPaidCount(paidV))
                    onDismiss()
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Guardar", fontWeight = FontWeight.Bold) }

            if (editing != null) {
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Icon(Icons.Filled.DeleteOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (confirmDelete && editing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Eliminar \"${editing.name}\"") },
            text = { Text("Se borrará este seguimiento de cuotas. No afecta tus movimientos.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(editing.id); onDismiss() }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }
}

