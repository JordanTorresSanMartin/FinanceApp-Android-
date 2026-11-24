package com.example.financeapp.ui.screens.upcoming

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.financeapp.data.model.Category
import com.example.financeapp.data.model.Frequency
import com.example.financeapp.data.model.RecurringPayment
import com.example.financeapp.data.model.TxType
import com.example.financeapp.domain.PayState
import com.example.financeapp.domain.RecurringWithStatus
import com.example.financeapp.ui.components.CategoryAvatar
import com.example.financeapp.ui.components.pressable
import com.example.financeapp.ui.theme.FinanceTheme
import com.example.financeapp.ui.viewmodel.upcoming.RecurringUiState
import com.example.financeapp.util.categoryIcon
import com.example.financeapp.util.formatMoneyCLP
import com.example.financeapp.util.groupThousands
import com.example.financeapp.util.parseHexColor
import java.time.LocalDate

fun frequencyLabel(freq: String): String = when (freq) {
    Frequency.ANUAL -> "Anual"
    Frequency.SEMANAL -> "Semanal"
    Frequency.QUINCENAL -> "Quincenal"
    else -> "Mensual"
}

@Composable
internal fun AddPill(onAdd: () -> Unit, label: String = "Agregar pago fijo") {
    Row(
        modifier = Modifier
            .pressable(onClick = onAdd, onClickLabel = label)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.height(18.dp))
        Spacer(Modifier.width(4.dp))
        Text("Agregar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecurringEditorSheet(
    editing: RecurringPayment?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (id: String?, name: String, amount: Double, categoryId: String?, frequency: String, billingDay: Int?, notes: String?) -> Unit,
    onDelete: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = com.example.financeapp.util.LocalAppHaptics.current
    var name by rememberSaveable { mutableStateOf(editing?.name ?: "") }
    var amount by rememberSaveable { mutableStateOf(editing?.amount?.toLong()?.toString() ?: "") }
    var frequency by rememberSaveable { mutableStateOf(editing?.frequency ?: Frequency.MENSUAL) }
    var billingDay by rememberSaveable { mutableStateOf(editing?.billingDay?.toString() ?: "") }
    var categoryId by rememberSaveable { mutableStateOf(editing?.categoryId) }
    var notes by rememberSaveable { mutableStateOf(editing?.notes ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }
    // Gastos primero; las de ingreso (Sueldo…) al final: un "pago fijo" de ingreso se registra como ingreso.
    val expenseCats = categories.sortedBy { if (it.type == TxType.INGRESO) 1 else 0 }
    val amountValue = amount.toLongOrNull() ?: 0L
    val dayValue = billingDay.toIntOrNull()
    val dayError = billingDay.isNotEmpty() && (dayValue == null || dayValue !in 1..31)
    val canSave = name.isNotBlank() && amountValue > 0 && !dayError

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(if (editing == null) "Nuevo pago fijo" else "Editar pago fijo", style = MaterialTheme.typography.titleLarge)

            SheetField(name, { name = it }, "Nombre (Netflix, Seguro auto…)")
            SheetField(groupThousands(amount), { amount = it.filter { c -> c.isDigit() }.take(12) }, "Monto (CLP)", KeyboardType.Number)

            Text("Frecuencia", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Frequency.ALL.forEach { f ->
                    ChoiceChip(frequencyLabel(f), frequency == f) { frequency = f }
                }
            }

            if (frequency == Frequency.MENSUAL) {
                SheetField(
                    billingDay, { billingDay = it.filter { c -> c.isDigit() }.take(2) },
                    "Día de cobro del mes (opcional)", KeyboardType.Number,
                    error = if (dayError) "Debe ser un día entre 1 y 31" else null,
                )
            }

            if (expenseCats.isNotEmpty()) {
                Text("Categoría", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    expenseCats.forEach { cat ->
                        val id = cat.id ?: return@forEach
                        val catColor = parseHexColor(cat.color, MaterialTheme.colorScheme.primary)
                        val sel = categoryId == id
                        Row(
                            modifier = Modifier.clip(CircleShape)
                                .background(if (sel) catColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerHigh)
                                .border(1.5.dp, if (sel) catColor else Color.Transparent, CircleShape)
                                .selectable(selected = sel, role = Role.RadioButton) { categoryId = id }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CategoryAvatar(icon = categoryIcon(cat.icon), color = catColor, size = 28.dp, corner = 14.dp)
                            Spacer(Modifier.width(6.dp))
                            Text(cat.name, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        }
                    }
                }
            }

            SheetField(notes, { notes = it }, "Notas (opcional)", imeAction = ImeAction.Done)

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    haptics?.success()
                    onSave(editing?.id, name, amountValue.toDouble(), categoryId, frequency,
                        if (frequency == Frequency.MENSUAL) dayValue else null, notes)
                    onDismiss()
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Guardar", fontWeight = FontWeight.Bold) }

            if (editing?.id != null) {
                TextButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Icon(Icons.Filled.DeleteOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.height(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (confirmDelete && editing?.id != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Eliminar pago fijo") },
            text = { Text("¿Eliminar \"${editing.name}\"? Los pagos ya registrados se mantienen en tu historial.") },
            confirmButton = {
                TextButton(onClick = { haptics?.destructive(); confirmDelete = false; onDelete(editing.id); onDismiss() }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val haptics = com.example.financeapp.util.LocalAppHaptics.current
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier.clip(CircleShape).background(bg)
            .selectable(selected = selected, role = Role.RadioButton, onClick = { haptics?.tick(); onClick() })
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

@Composable
internal fun SheetField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboard: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    error: String? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = imeAction),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}
