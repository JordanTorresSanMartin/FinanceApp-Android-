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
fun RecurringSection(
    state: RecurringUiState,
    onAdd: () -> Unit,
    onEdit: (RecurringPayment) -> Unit,
    onRegister: (RecurringPayment) -> Unit,
) {
    // Confirmación cuando el período ya figura pagado (evita registrar dos veces).
    var confirmDuplicate by remember { mutableStateOf<RecurringPayment?>(null) }
    val today = remember { LocalDate.now() }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Suscripciones y pagos fijos", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            AddPill(onAdd)
        }

        if (state.items.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Aún no agregas pagos fijos", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Netflix, seguro, plan de celular, gimnasio… todo lo que pagas cada mes o año. " +
                        "Agrégalos y sigue su estado desde el widget \"Pagos fijos\".",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer).padding(20.dp),
            ) {
                Text(
                    "Este período · ${state.paidCount} de ${state.items.size} pagados",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                )
                Text(
                    if (state.pendingAmount > 0) "Falta ${formatMoneyCLP(state.pendingAmount)}" else "Todo pagado",
                    style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Total fijo mensual equivalente: ${formatMoneyCLP(state.monthlyTotal)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
            state.items.forEach { row ->
                RecurringItemCard(
                    row = row,
                    today = today,
                    registering = row.item.id in state.registering,
                    onEdit = onEdit,
                    onRegister = {
                        if (row.status.state == PayState.PAGADO) confirmDuplicate = row.item else onRegister(row.item)
                    },
                )
            }
        }
    }

    confirmDuplicate?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmDuplicate = null },
            title = { Text("Ya está pagado") },
            text = { Text("\"${item.name}\" ya tiene un pago registrado este período. ¿Registrar otro pago igual?") },
            confirmButton = {
                TextButton(onClick = { confirmDuplicate = null; onRegister(item) }) { Text("Registrar igual") }
            },
            dismissButton = { TextButton(onClick = { confirmDuplicate = null }) { Text("Cancelar") } },
        )
    }
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

@Composable
private fun RecurringItemCard(
    row: RecurringWithStatus,
    today: LocalDate,
    registering: Boolean,
    onEdit: (RecurringPayment) -> Unit,
    onRegister: () -> Unit,
) {
    val item = row.item
    val finance = FinanceTheme.colors
    val color = parseHexColor(item.color ?: item.categories?.color, MaterialTheme.colorScheme.primary)
    val state = row.status.state
    val (stateColor, stateBg) = when (state) {
        PayState.PAGADO -> finance.income to finance.incomeContainer
        PayState.ATRASADO -> finance.expense to finance.expenseContainer
        PayState.POR_VENCER -> finance.warning to finance.warningContainer
        PayState.PENDIENTE -> MaterialTheme.colorScheme.onSurfaceVariant to MaterialTheme.colorScheme.surfaceContainerHigh
    }
    Column(
        modifier = Modifier.fillMaxWidth()
            .pressable(onClick = { onEdit(item) }, onClickLabel = "Editar ${item.name}")
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryAvatar(icon = categoryIcon(item.icon ?: item.categories?.icon), color = color, size = 42.dp, corner = 14.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    buildString {
                        append(frequencyLabel(item.frequency))
                        item.billingDay?.let { append(" · día $it") }
                        item.categories?.name?.let { append(" · $it") }
                    },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                )
            }
            Text(formatMoneyCLP(item.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Estado: color + texto (el color nunca es la única señal).
            Row(
                modifier = Modifier.clip(CircleShape).background(stateBg).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state == PayState.PAGADO) {
                    Icon(Icons.Filled.CheckCircle, null, tint = stateColor, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(row.status.label(today), style = MaterialTheme.typography.labelMedium, color = stateColor, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .pressable(onClick = onRegister, enabled = !registering, strongHaptic = true, onClickLabel = "Registrar pago de ${item.name}")
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.14f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (registering) {
                    CircularProgressIndicator(Modifier.size(16.dp), color = color, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Repeat, null, tint = color, modifier = Modifier.height(16.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text(if (state == PayState.PAGADO) "Registrar otro" else "Registrar pago", style = MaterialTheme.typography.labelLarge, color = color)
            }
        }
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
    var name by rememberSaveable { mutableStateOf(editing?.name ?: "") }
    var amount by rememberSaveable { mutableStateOf(editing?.amount?.toLong()?.toString() ?: "") }
    var frequency by rememberSaveable { mutableStateOf(editing?.frequency ?: Frequency.MENSUAL) }
    var billingDay by rememberSaveable { mutableStateOf(editing?.billingDay?.toString() ?: "") }
    var categoryId by rememberSaveable { mutableStateOf(editing?.categoryId) }
    var notes by rememberSaveable { mutableStateOf(editing?.notes ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }
    val expenseCats = categories.filter { it.type == TxType.GASTO || it.type == "ambos" }
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
                TextButton(onClick = { confirmDelete = false; onDelete(editing.id); onDismiss() }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier.clip(CircleShape).background(bg)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
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
