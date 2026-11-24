package com.example.financeapp.ui.screens.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.financeapp.data.model.Category
import com.example.financeapp.data.model.TxType
import com.example.financeapp.ui.components.CategoryAvatar
import com.example.financeapp.ui.screens.upcoming.GroupRow
import com.example.financeapp.util.LocalAppHaptics
import com.example.financeapp.util.categoryIcon
import com.example.financeapp.util.parseHexColor

private val ICONS = listOf(
    "cart", "food", "cafe", "car", "fuel", "bus", "home", "flash", "wifi", "phone", "medical", "fitness",
    "shirt", "film", "game", "school", "flight", "paw", "gift", "subscri", "card", "deuda", "seguro", "bank",
    "cash", "ahorro", "trending", "otros",
)
private val COLORS = listOf("#5B6EE1", "#2E9E8F", "#E0705A", "#8E6CC9", "#C98A1E", "#3D8BD9", "#D15C8A", "#4CAF50", "#78909C")

/** Lista de categorías (gastos e ingresos) para crear o editar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoriesSheet(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (id: String?, name: String, type: String, icon: String, color: String) -> Unit,
) {
    var editing by remember { mutableStateOf<Category?>(null) }
    var creating by remember { mutableStateOf(false) }
    val haptics = LocalAppHaptics.current

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Categorías", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Divide las muy grandes (ej. \"Deudas/Créditos\" → Crédito auto, Crédito consumo) para ver en qué se va la plata.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            FilledTonalButton(onClick = { haptics?.click(); creating = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Nueva categoría", fontWeight = FontWeight.SemiBold)
            }
            listOf("Gastos" to categories.filter { it.type != TxType.INGRESO }, "Ingresos" to categories.filter { it.type == TxType.INGRESO })
                .forEach { (title, list) ->
                    if (list.isEmpty()) return@forEach
                    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp, start = 4.dp))
                    list.forEachIndexed { i, c ->
                        GroupRow(i, list.size, onClick = { editing = c }, onClickLabel = "Editar ${c.name}") {
                            CategoryAvatar(icon = categoryIcon(c.icon), color = parseHexColor(c.color, MaterialTheme.colorScheme.primary), size = 36.dp, corner = 12.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(c.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text("Editar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
        }
    }

    if (creating || editing != null) {
        CategoryEditor(
            initial = editing,
            onDismiss = { creating = false; editing = null },
            onSave = { name, type, icon, color -> onSave(editing?.id, name, type, icon, color); creating = false; editing = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditor(initial: Category?, onDismiss: () -> Unit, onSave: (String, String, String, String) -> Unit) {
    val haptics = LocalAppHaptics.current
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var type by rememberSaveable { mutableStateOf(initial?.type?.takeIf { it == TxType.INGRESO } ?: TxType.GASTO) }
    var icon by rememberSaveable { mutableStateOf(initial?.icon ?: "otros") }
    var color by rememberSaveable { mutableStateOf(initial?.color ?: COLORS.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nueva categoría" else "Editar categoría") },
        text = {
            Column(Modifier.imePadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it.take(30) }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(TxType.GASTO to "Gasto", TxType.INGRESO to "Ingreso").forEach { (t, label) ->
                        Chip(label, type == t) { haptics?.tick(); type = t }
                    }
                }
                Text("Ícono", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ICONS.forEach { key ->
                        val sel = icon == key
                        CategoryAvatar(
                            icon = categoryIcon(key), color = if (sel) parseHexColor(color, MaterialTheme.colorScheme.primary) else MaterialTheme.colorScheme.outline,
                            size = 36.dp, corner = 12.dp,
                            modifier = Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                .border(if (sel) 2.dp else 0.dp, if (sel) parseHexColor(color, Color.Gray) else Color.Transparent, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                .selectable(sel, role = Role.RadioButton) { haptics?.tick(); icon = key },
                        )
                    }
                }
                Text("Color", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    COLORS.forEach { hex ->
                        val sel = color == hex
                        Spacer(
                            Modifier.size(30.dp).clip(CircleShape).background(parseHexColor(hex, Color.Gray))
                                .border(if (sel) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                .selectable(sel, role = Role.RadioButton) { haptics?.tick(); color = hex },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { haptics?.success(); onSave(name, type, icon, color) }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}
