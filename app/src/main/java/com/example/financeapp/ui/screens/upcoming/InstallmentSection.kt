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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentEditorSheet(
    editing: InstallmentPlan?,
    onDismiss: () -> Unit,
    onSave: (InstallmentPlan) -> Unit,
    onDelete: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = com.example.financeapp.util.LocalAppHaptics.current
    var name by rememberSaveable { mutableStateOf(editing?.name ?: "") }
    var amount by rememberSaveable { mutableStateOf(editing?.amount?.toString() ?: "") }
    var total by rememberSaveable { mutableStateOf(editing?.total?.toString() ?: "") }
    var paid by rememberSaveable { mutableStateOf(editing?.paid?.takeIf { it > 0 }?.toString() ?: "") }
    var dueDay by rememberSaveable { mutableStateOf(editing?.dueDay?.toString() ?: "") }
    var owedBy by rememberSaveable { mutableStateOf(editing?.owedBy ?: "") }
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
                error = if (dayError) "Debe ser un día entre 1 y 31" else null,
            )
            SheetField(owedBy, { owedBy = it.take(40) }, "¿Alguien te la debe? Su nombre (opcional)", imeAction = ImeAction.Done)
            if (owedBy.isNotBlank()) {
                Text(
                    "Escribe el nombre igual que en la planilla en pareja. Cada mes se le cobra la cuota allá " +
                        "y cuando la pague aparecerá marcada aquí.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

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
                    haptics?.success()
                    onSave(
                        base.copy(name = name.trim(), amount = amountV, total = totalV, dueDay = dayV,
                            owedBy = owedBy.trim().ifEmpty { null }).withPaidCount(paidV)
                    )
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
                TextButton(onClick = { haptics?.destructive(); confirmDelete = false; onDelete(editing.id); onDismiss() }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }
}

