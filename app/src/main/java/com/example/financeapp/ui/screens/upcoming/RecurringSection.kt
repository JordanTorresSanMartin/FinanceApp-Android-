package com.example.financeapp.ui.screens.upcoming

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.financeapp.data.model.Category
import com.example.financeapp.data.model.Frequency
import com.example.financeapp.data.model.RecurringPayment
import com.example.financeapp.data.model.TxType
import com.example.financeapp.ui.components.CategoryAvatar
import com.example.financeapp.ui.components.pressable
import com.example.financeapp.ui.theme.FinanceTheme
import com.example.financeapp.ui.viewmodel.upcoming.RecurringUiState
import com.example.financeapp.util.LocalAppHaptics
import com.example.financeapp.util.categoryIcon
import com.example.financeapp.util.formatMoneyCLP
import com.example.financeapp.util.groupThousands
import com.example.financeapp.util.parseHexColor

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
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Suscripciones y pagos fijos", style = MaterialTheme.typography.titleLarge)
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
                    "Netflix, seguro, plan de celular, gimnasio, tus pastillas… todo lo que pagas cada mes o año.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            // Total mensual comprometido
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer).padding(20.dp),
            ) {
                Text("Total fijo mensual (equivalente)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f))
                Text(formatMoneyCLP(state.monthlyTotal), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("${state.items.size} pagos activos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
            }
            state.items.forEach { item -> RecurringItemCard(item, onEdit, onRegister) }
        }
    }
}

@Composable
private fun AddPill(onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .pressable(onClick = onAdd)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.height(18.dp))
        Spacer(Modifier.width(4.dp))
        Text("Agregar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun RecurringItemCard(
    item: RecurringPayment,
    onEdit: (RecurringPayment) -> Unit,
    onRegister: (RecurringPayment) -> Unit,
) {
    val color = parseHexColor(item.color ?: item.categories?.color, MaterialTheme.colorScheme.primary)
    Column(
        modifier = Modifier.fillMaxWidth()
            .pressable(onClick = { onEdit(item) })
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
            Column(horizontalAlignment = Alignment.End) {
                Text(formatMoneyCLP(item.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(frequencyLabel(item.frequency).lowercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth()
                .pressable(onClick = { onRegister(item) }, strongHaptic = true)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.14f))
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Repeat, null, tint = color, modifier = Modifier.height(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Registrar pago", style = MaterialTheme.typography.labelLarge, color = color)
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
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var amount by remember { mutableStateOf(editing?.amount?.toLong()?.toString() ?: "") }
    var frequency by remember { mutableStateOf(editing?.frequency ?: Frequency.MENSUAL) }
    var billingDay by remember { mutableStateOf(editing?.billingDay?.toString() ?: "") }
    var categoryId by remember { mutableStateOf(editing?.categoryId) }
    var notes by remember { mutableStateOf(editing?.notes ?: "") }
    val expenseCats = categories.filter { it.type == TxType.GASTO || it.type == "ambos" }
    val amountValue = amount.filter { it.isDigit() }.toLongOrNull() ?: 0L
    val canSave = name.isNotBlank() && amountValue > 0

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(if (editing == null) "Nuevo pago fijo" else "Editar pago fijo", style = MaterialTheme.typography.titleLarge)

            SheetField(name, { name = it }, "Nombre (Netflix, Seguro auto…)")
            SheetField(groupThousands(amount), { amount = it.filter { c -> c.isDigit() } }, "Monto (CLP)", KeyboardType.Number)

            Text("Frecuencia", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Frequency.ALL.forEach { f ->
                    ChoiceChip(frequencyLabel(f), frequency == f) { frequency = f }
                }
            }

            SheetField(billingDay, { billingDay = it.filter { c -> c.isDigit() }.take(2) }, "Día de cobro del mes (opcional)", KeyboardType.Number)

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
                                .clickable { categoryId = id }
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

            SheetField(notes, { notes = it }, "Notas (opcional)")

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(52.dp).clip(CircleShape)
                    .background(if (canSave) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(enabled = canSave) {
                        onSave(editing?.id, name, amountValue.toDouble(), categoryId, frequency, billingDay.toIntOrNull(), notes)
                        onDismiss()
                    },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Guardar", fontWeight = FontWeight.Bold,
                    color = if (canSave) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (editing?.id != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(48.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .clickable { onDelete(editing.id); onDismiss() },
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.DeleteOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.height(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Eliminar", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier.clip(CircleShape).background(bg).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

@Composable
private fun SheetField(value: String, onValueChange: (String) -> Unit, placeholder: String, keyboard: KeyboardType = KeyboardType.Text) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}
