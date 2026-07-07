package com.example.financeapp.ui.screens.upcoming

import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.financeapp.data.model.Installment
import com.example.financeapp.data.model.RecurringPayment
import com.example.financeapp.ui.components.AnimatedProgressBar
import com.example.financeapp.ui.components.CategoryAvatar
import com.example.financeapp.ui.components.RoundIconButton
import com.example.financeapp.ui.theme.FinanceTheme
import com.example.financeapp.ui.viewmodel.upcoming.RecurringViewModel
import com.example.financeapp.ui.viewmodel.upcoming.UpcomingViewModel
import com.example.financeapp.util.categoryIcon
import com.example.financeapp.util.formatK
import com.example.financeapp.util.formatMoneyCLP
import com.example.financeapp.util.parseHexColor
import com.example.financeapp.util.shortDateLabel

@Composable
fun UpcomingScreen(
    onBack: () -> Unit,
    viewModel: UpcomingViewModel = hiltViewModel(),
    recurringVm: RecurringViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recurring by recurringVm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var editorOpen by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<RecurringPayment?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val name = queryDisplayName(context, uri) ?: "estado.pdf"
        val bytes = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        if (bytes != null) viewModel.uploadPdf(Base64.encodeToString(bytes, Base64.NO_WRAP), name)
    }
    val pickPdf = { picker.launch(arrayOf("application/pdf")) }

    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); viewModel.consumeMessage() } }
    LaunchedEffect(recurring.message) { recurring.message?.let { snackbar.showSnackbar(it); recurringVm.consumeMessage() } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, onBack, size = 40.dp)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("COMPROMISOS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Próximos pagos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(enabled = !state.uploading) { pickPdf() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.uploading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    } else {
                        Icon(Icons.Filled.CloudUpload, "Subir PDF", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            if (state.isLoading && recurring.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                val finance = FinanceTheme.colors
                val maxForecast = (state.forecast.maxOfOrNull { it.amount } ?: 1.0).coerceAtLeast(1.0)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // ── Suscripciones y pagos fijos ──
                    item {
                        RecurringSection(
                            state = recurring,
                            onAdd = { editingItem = null; editorOpen = true },
                            onEdit = { editingItem = it; editorOpen = true },
                            onRegister = { recurringVm.registerPayment(it) },
                        )
                    }

                    // ── Estados de cuenta / cuotas ──
                    if (state.statements.isEmpty()) {
                        item { UploadPromptCard(uploading = state.uploading, onUpload = pickPdf) }
                    } else {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer).padding(24.dp),
                            ) {
                                Text("Cuota mensual comprometida", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f))
                                Text(formatMoneyCLP(state.monthlyTotal), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(Modifier.height(8.dp))
                                Box(modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                                    Text("${state.installments.size} compras en cuotas activas", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(finance.expenseContainer).padding(16.dp)) {
                                    Text("FACTURADO ESTE MES", style = MaterialTheme.typography.labelMedium, color = finance.onExpenseContainer)
                                    Spacer(Modifier.height(6.dp))
                                    Text(formatMoneyCLP(state.billedTotal), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = finance.onExpenseContainer)
                                }
                                Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp)) {
                                    Text("PRÓXIMO VENCIMIENTO", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(6.dp))
                                    Text(state.nextDue?.let { shortDateLabel(it) } ?: "—", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        if (state.forecast.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(20.dp)) {
                                    Text("Vencimiento próximos meses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(18.dp))
                                    Row(modifier = Modifier.fillMaxWidth().height(130.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                        state.forecast.forEach { f ->
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                                Text(formatK(f.amount), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Spacer(Modifier.height(6.dp))
                                                Box(modifier = Modifier.width(26.dp).height((6 + (f.amount / maxForecast * 90)).dp).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(MaterialTheme.colorScheme.primary))
                                                Spacer(Modifier.height(8.dp))
                                                Text(monthAbbrev(f.month), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Compras en cuotas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("${state.installments.size}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        items(state.installments.size) { idx -> InstallmentCard(state.installments[idx]) }
                        item {
                            Text(
                                state.statements.joinToString("  ·  ") { (it.bank ?: "Banco") + (it.cardLast4?.let { l -> " ••$l" } ?: "") },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }
    }

    if (editorOpen) {
        RecurringEditorSheet(
            editing = editingItem,
            categories = recurring.categories,
            onDismiss = { editorOpen = false },
            onSave = { id, name, amount, categoryId, frequency, billingDay, notes ->
                recurringVm.save(id, name, amount, categoryId, frequency, billingDay, notes)
            },
            onDelete = { recurringVm.delete(it) },
        )
    }

    if (state.passwordVisible) {
        PasswordDialog(
            error = state.passwordError,
            uploading = state.uploading,
            onCancel = viewModel::cancelPassword,
            onSubmit = viewModel::submitPassword,
        )
    }
}

@Composable
private fun UploadPromptCard(uploading: Boolean, onUpload: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Filled.CloudUpload, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
        Text("Sube tu estado de cuenta", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            "Importa el PDF de tu tarjeta y detectamos tus compras en cuotas. Si tiene contraseña, te la pedimos.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary).clickable(enabled = !uploading) { onUpload() }.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (uploading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(Icons.Filled.CloudUpload, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Subir PDF", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun InstallmentCard(it: Installment) {
    val color = parseHexColor(it.categories?.color, MaterialTheme.colorScheme.primary)
    val remaining = it.installmentTotal - it.installmentCurrent
    val pct = if (it.installmentTotal > 0) it.installmentCurrent.toDouble() / it.installmentTotal else 0.0
    val cardTag = bankShort(it.statements?.bank) + (it.statements?.cardLast4?.let { l -> " ••$l" } ?: "")
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryAvatar(icon = categoryIcon(it.categories?.icon), color = color, size = 42.dp, corner = 14.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(it.description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    "$cardTag · ${it.categories?.name ?: "Otros"} · Total ${formatMoneyCLP(it.totalAmount)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatMoneyCLP(it.monthlyAmount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("al mes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedProgressBar(pct = pct * 100, color = color, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.clip(CircleShape).background(color.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text(
                    "$remaining ${if (remaining == 1) "cuota" else "cuotas"} · ${it.installmentCurrent}/${it.installmentTotal}",
                    style = MaterialTheme.typography.labelMedium, color = color,
                )
            }
        }
    }
}

@Composable
private fun PasswordDialog(error: String?, uploading: Boolean, onCancel: () -> Unit, onSubmit: (String) -> Unit) {
    var pw by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("PDF protegido") },
        text = {
            Column {
                Text("Este estado de cuenta tiene contraseña. Ingrésala para leerlo (no se guarda).", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pw, onValueChange = { pw = it },
                    placeholder = { Text("Contraseña del PDF") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true, isError = error != null, modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(pw) }, enabled = !uploading && pw.isNotBlank()) {
                Text("Desbloquear", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancelar") } },
    )
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String? {
    return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
    }
}

private fun bankShort(bank: String?): String = when {
    bank == null -> "Tarjeta"
    bank.contains("falabella", true) -> "Falabella"
    bank.contains("tenpo", true) -> "Tenpo"
    bank.contains("bci", true) -> "BCI"
    else -> bank
}

private fun monthAbbrev(month: String): String {
    if (month.length < 3) return month
    val three = month.substring(0, 3)
    return three[0].uppercase() + three.substring(1).lowercase()
}
