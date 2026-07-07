package com.example.financeapp.ui.screens.transactions

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.financeapp.data.model.Category
import com.example.financeapp.data.model.TxType
import com.example.financeapp.ui.components.CategoryAvatar
import com.example.financeapp.ui.components.pressable
import com.example.financeapp.ui.theme.FinanceTheme
import com.example.financeapp.util.categoryIcon
import com.example.financeapp.util.groupThousands
import com.example.financeapp.util.parseHexColor

/** Cuerpo compartido del formulario de transacción (nueva y editar). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionFormBody(
    type: String,
    onTypeChange: (String) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    dateText: String,
    onDateChange: (String) -> Unit,
    onToday: () -> Unit,
    onYesterday: () -> Unit,
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    source: String? = null,
) {
    val finance = FinanceTheme.colors
    val activeColor = if (type == TxType.GASTO) finance.expense else finance.income
    val filtered = categories.filter { it.type == type || it.type == "ambos" }

    Column(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (!source.isNullOrBlank()) {
            Text(
                "Importada automáticamente desde $source",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }

        // Type toggle — segmentado tonal M3: el seleccionado usa su container
        // semántico (suave), no un relleno saturado, y llena toda la altura.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToggleHalf(
                modifier = Modifier.weight(1f),
                label = "Gasto",
                icon = Icons.Filled.ArrowUpward,
                selected = type == TxType.GASTO,
                container = finance.expenseContainer,
                onContainer = finance.onExpenseContainer,
            ) { onTypeChange(TxType.GASTO) }
            ToggleHalf(
                modifier = Modifier.weight(1f),
                label = "Ingreso",
                icon = Icons.Filled.ArrowDownward,
                selected = type == TxType.INGRESO,
                container = finance.incomeContainer,
                onContainer = finance.onIncomeContainer,
            ) { onTypeChange(TxType.INGRESO) }
        }

        // Amount hero
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("CLP", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextField(
                value = groupThousands(amount),
                onValueChange = onAmountChange,
                textStyle = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black, textAlign = TextAlign.Center, color = activeColor,
                ),
                placeholder = {
                    Text(
                        "0", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
        }

        // Description
        FormTextField(
            value = description, onValueChange = onDescriptionChange, placeholder = "Descripción",
        )

        // Date
        Text("Fecha", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                FormTextField(value = dateText, onValueChange = onDateChange, placeholder = "YYYY-MM-DD")
            }
            DateChip("Hoy", onToday)
            DateChip("Ayer", onYesterday)
        }

        // Categories
        Text("Categoría", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (filtered.isEmpty()) {
            Text(
                "No hay categorías disponibles.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filtered.forEach { cat ->
                    val id = cat.id ?: return@forEach
                    val selected = selectedCategoryId == id
                    val catColor = parseHexColor(cat.color, MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier
                            .pressable(onClick = { onCategorySelected(id) })
                            .clip(CircleShape)
                            .background(if (selected) catColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerLow)
                            .border(1.5.dp, if (selected) catColor else Color.Transparent, CircleShape)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CategoryAvatar(icon = categoryIcon(cat.icon), color = catColor, size = 34.dp, corner = 17.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(cat.name, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        if (selected) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Filled.CheckCircle, null, tint = catColor, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Notes
        FormTextField(
            value = notes, onValueChange = onNotesChange, placeholder = "Notas (opcional)",
            minLines = 3,
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ToggleHalf(
    modifier: Modifier,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    container: Color,
    onContainer: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxHeight()
            .pressable(onClick = onClick)
            .clip(CircleShape)
            .background(if (selected) container else Color.Transparent),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon, null,
            tint = if (selected) onContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) onContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DateChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int = 1,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
        minLines = minLines,
        singleLine = minLines == 1,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}
