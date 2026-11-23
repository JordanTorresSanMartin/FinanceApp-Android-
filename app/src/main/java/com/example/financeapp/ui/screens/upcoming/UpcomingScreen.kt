package com.example.financeapp.ui.screens.upcoming

import com.example.financeapp.ui.components.Bone
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.ui.draw.alpha
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.financeapp.notifications.DueAlerts
import com.example.financeapp.ui.components.SkeletonCardList
import com.example.financeapp.ui.components.SkeletonChart
import com.example.financeapp.ui.components.SkeletonHero
import com.example.financeapp.ui.components.SkeletonScope

import com.example.financeapp.util.LocalAppHaptics
import com.example.financeapp.ui.viewmodel.upcoming.InstallmentsViewModel
import com.example.financeapp.domain.InstallmentPlan
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import com.example.financeapp.domain.CuotaState
import com.example.financeapp.domain.PayState
import com.example.financeapp.domain.status
import com.example.financeapp.ui.viewmodel.upcoming.UpcomingUiState

private enum class UpcomingTab(val title: String) { CUOTAS("Cuotas"), FIJOS("Fijos"), TARJETAS("Tarjetas") }

/**
 * Próximos pagos: un resumen de lo que falta este mes y tres pestañas deslizables
 * (Cuotas · Fijos · Tarjetas), cada una con una línea que explica qué contiene.
 */
@Composable
fun UpcomingScreen(
    onBack: () -> Unit,
    viewModel: UpcomingViewModel = hiltViewModel(),
    recurringVm: RecurringViewModel = hiltViewModel(),
    cuotasVm: InstallmentsViewModel = hiltViewModel(),
) {
    val plans by cuotasVm.plans.collectAsStateWithLifecycle()
    val cuotasLoading by cuotasVm.loading.collectAsStateWithLifecycle()
    var planEditorOpen by remember { mutableStateOf(false) }
    var editingPlan by remember { mutableStateOf<InstallmentPlan?>(null) }
    val haptics = LocalAppHaptics.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recurring by recurringVm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var editorOpen by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<RecurringPayment?>(null) }
    val tabs = UpcomingTab.entries
    val pager = rememberPagerState { tabs.size }

    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        // Leer y codificar fuera del hilo principal: un PDF grande trababa la UI.
        scope.launch {
            val result = withContext(Dispatchers.IO) { readPdf(context, uri) }
            result.onSuccess { (b64, name) -> viewModel.uploadPdf(b64, name) }
                .onFailure { snackbar.showSnackbar(it.message ?: "No se pudo leer el PDF.") }
        }
    }
    val pickPdf = { picker.launch(arrayOf("application/pdf")) }

    // Aviso en vivo de pagos de hoy: se pide el permiso aquí, donde se entiende para qué es.
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) DueAlerts.checkNow(context)
    }
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    // Tras pagar (o al cargar) se recalcula el aviso; WorkManager funde llamadas seguidas.
    LaunchedEffect(plans, recurring.items) { if (!recurring.isLoading) DueAlerts.checkNow(context) }

    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); viewModel.consumeMessage() } }
    LaunchedEffect(recurring.message) { recurring.message?.let { snackbar.showSnackbar(it); recurringVm.consumeMessage() } }
    val cuotasMsg by cuotasVm.message.collectAsStateWithLifecycle()
    LaunchedEffect(cuotasMsg) { cuotasMsg?.let { snackbar.showSnackbar(it); cuotasVm.consumeMessage() } }

    val today = remember { java.time.LocalDate.now() }
    val pendingPlans = plans.filter { !it.isDone && it.status(today).state != CuotaState.PAGADA_MES }
    val activePlans = plans.count { !it.isDone }
    val pendingFijos = recurring.items.filter { it.status.state != PayState.PAGADO }
    val totalItems = activePlans + recurring.items.size
    val doneItems = totalItems - pendingPlans.size - pendingFijos.size
    val pendingCuotasAmount = pendingPlans.sumOf { it.amount }.toDouble()
    val counts = mapOf(
        UpcomingTab.CUOTAS to activePlans,
        UpcomingTab.FIJOS to recurring.items.size,
        UpcomingTab.TARJETAS to state.installments.size,
    )
    val pageInsets = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, onBack, "Volver", size = 40.dp)
                Spacer(Modifier.width(16.dp))
                Text("Próximos pagos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            com.example.financeapp.ui.components.LoadingContent(
                loading = recurring.isLoading || (cuotasLoading && plans.isEmpty()),
                skeleton = { SkeletonScope(Modifier.padding(horizontal = 20.dp)) { SkeletonHero(height = 128.dp) } },
            ) { MonthOverview(
                pending = pendingCuotasAmount + recurring.pendingAmount,
                cuotas = pendingCuotasAmount,
                fijos = recurring.pendingAmount,
                done = doneItems,
                total = totalItems,
                modifier = Modifier.padding(horizontal = 20.dp),
            ) }

            Spacer(Modifier.height(8.dp))
            PrimaryTabRow(
                selectedTabIndex = pager.currentPage,
                containerColor = MaterialTheme.colorScheme.background,
                divider = {},
            ) {
                tabs.forEachIndexed { i, tab ->
                    val selected = pager.currentPage == i
                    Tab(
                        selected = selected,
                        onClick = { haptics?.selection(); scope.launch { pager.animateScrollToPage(i) } },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tab.title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                                val n = counts[tab] ?: 0
                                if (n > 0) {
                                    Spacer(Modifier.width(6.dp))
                                    Box(
                                        Modifier.clip(CircleShape).background(
                                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
                                        ).padding(horizontal = 7.dp, vertical = 1.dp),
                                    ) {
                                        Text(
                                            "$n", style = MaterialTheme.typography.labelSmall,
                                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        },
                    )
                }
            }

            HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                beyondViewportPageCount = 1,
            ) { page ->
                when (tabs[page]) {
                    UpcomingTab.CUOTAS -> CuotasPage(
                        plans = plans,
                        loading = cuotasLoading,
                        contentPadding = pageInsets,
                        onAdd = { editingPlan = null; planEditorOpen = true },
                        onEdit = { editingPlan = it; planEditorOpen = true },
                        onPay = { plan ->
                            haptics?.success()
                            cuotasVm.pay(plan.id)
                            val n = plan.paid + 1
                            val msg = if (n >= plan.total) "¡Terminaste de pagar ${plan.name}!"
                            else "Cuota $n de ${plan.total} de ${plan.name} pagada"
                            scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                val r = snackbar.showSnackbar(msg, actionLabel = "Deshacer", duration = SnackbarDuration.Short)
                                if (r == SnackbarResult.ActionPerformed) cuotasVm.undo(plan.id)
                            }
                        },
                    )
                    UpcomingTab.FIJOS -> FijosPage(
                        state = recurring,
                        contentPadding = pageInsets,
                        onAdd = { editingItem = null; editorOpen = true },
                        onEdit = { editingItem = it; editorOpen = true },
                        onRegister = { haptics?.success(); recurringVm.registerPayment(it) },
                    )
                    UpcomingTab.TARJETAS -> TarjetasPage(state, today, pageInsets, onUpload = pickPdf, onSetClosed = viewModel::setCardClosed)
                }
            }
        }
    }

    if (planEditorOpen) {
        InstallmentEditorSheet(
            editing = editingPlan,
            onDismiss = { planEditorOpen = false },
            onSave = cuotasVm::save,
            onDelete = cuotasVm::delete,
        )
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

/** Lo único que importa de un vistazo: cuánto falta pagar este mes y cuánto llevas. */
@Composable
private fun MonthOverview(pending: Double, cuotas: Double, fijos: Double, done: Int, total: Int, modifier: Modifier = Modifier) {
    val onC = MaterialTheme.colorScheme.onPrimaryContainer
    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text(
            if (pending > 0) "Te falta pagar este mes" else "Este mes",
            style = MaterialTheme.typography.labelLarge, color = onC.copy(alpha = 0.85f),
        )
        if (pending > 0) {
            Text(money(com.example.financeapp.ui.components.animatedAmount(pending)), style = MaterialTheme.typography.headlineLarge, color = onC, fontWeight = FontWeight.Black)
        } else {
            Text("Todo al día ✓", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = onC)
        }
        if (total > 0) {
            Spacer(Modifier.height(10.dp))
            ThinProgress(done.toFloat() / total, onC)
            Spacer(Modifier.height(8.dp))
            Text(
                buildString {
                    append("$done de $total pagos listos")
                    if (cuotas > 0) append(" · cuotas ${money(cuotas)}")
                    if (fijos > 0) append(" · fijos ${money(fijos)}")
                },
                style = MaterialTheme.typography.labelMedium, color = onC.copy(alpha = 0.85f), maxLines = 2,
            )
        }
    }
}

/** Pestaña "Tarjetas": lo que detectamos en tus estados de cuenta; cada tarjeta se puede cerrar o reabrir. */
@Composable
private fun TarjetasPage(
    state: UpcomingUiState,
    today: java.time.LocalDate,
    contentPadding: PaddingValues,
    onUpload: () -> Unit,
    onSetClosed: (com.example.financeapp.data.model.Statement, Boolean) -> Unit,
) {
    val finance = FinanceTheme.colors
    var confirmClose by remember { mutableStateOf<com.example.financeapp.data.model.Statement?>(null) }
    val perCard = remember(state.installments) { state.installments.groupBy { it.statementId } }
    val dueJava = state.nextDue?.let { runCatching { java.time.LocalDate.parse(it.toString()) }.getOrNull() }
    val stale = dueJava != null && dueJava.isBefore(today.minusDays(7))

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item(key = "intro") {
            PageIntro(
                if (state.statements.isEmpty()) "Cuotas que vienen en el estado de cuenta de tu tarjeta."
                else "Cuotas que vienen en el estado de cuenta de tus tarjetas.",
            ) { if (state.statements.isNotEmpty()) UploadPill(state.uploading, onUpload) }
            Spacer(Modifier.height(8.dp))
        }
        if (state.isLoading) {
            item(key = "loading") {
                SkeletonScope(Modifier.animateItem()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) { Bone(Modifier.weight(1f).height(64.dp), RoundedCornerShape(20.dp)) }
                    }
                    Spacer(Modifier.height(10.dp))
                    SkeletonChart(bars = 5)
                    Spacer(Modifier.height(10.dp))
                    SkeletonCardList(rows = 4)
                }
            }
            return@LazyColumn
        }
        if (state.statements.isNotEmpty()) {
            item(key = "cards-title") { SectionLabel("Tus tarjetas", Modifier.animateItem()) }
            itemsIndexed(state.statements, key = { _, s -> "card-" + s.id }) { i, s ->
                val list = perCard[s.id].orEmpty()
                CardRow(
                    s, i, state.statements.size, Modifier.animateItem(),
                    subtitle = "${list.size} compras · ${money(list.sumOf { it.monthlyAmount })}/mes" +
                        (s.dueDate?.let { " · vence ${shortDateLabel(it)}" } ?: ""),
                    actionLabel = "Cerrar", onAction = { confirmClose = s },
                )
            }
            item(key = "cards-gap") { Spacer(Modifier.height(16.dp)) }
        }
        if (state.statements.isEmpty() && state.closedCards.isEmpty()) {
            item(key = "empty") {
                EmptyPage(
                    Icons.Filled.CloudUpload, "Sube tu estado de cuenta",
                    "Importa el PDF de tu tarjeta y detectamos tus compras en cuotas. Si tiene contraseña, te la pedimos.",
                ) { UploadPill(state.uploading, onUpload) }
            }
            return@LazyColumn
        }
        if (state.statements.isEmpty()) {
            item(key = "all-closed") {
                PageIntro("Todas tus tarjetas están cerradas. Reabre una o sube un estado de cuenta nuevo.") { UploadPill(state.uploading, onUpload) }
            }
        }
        if (stale && state.statements.isNotEmpty()) {
            item(key = "stale") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(20.dp))
                        .background(finance.warningContainer).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Estos datos son del estado de cuenta que vencía el ${shortDateLabel(state.nextDue!!)}. " +
                            "Sube el más reciente para verlos al día.",
                        style = MaterialTheme.typography.bodyMedium, color = finance.warning, modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        if (state.statements.isNotEmpty()) item(key = "stats") {
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Cuotas al mes", money(state.monthlyTotal), Modifier.weight(1f))
                StatTile("Facturado", money(state.billedTotal), Modifier.weight(1f))
                StatTile("Vence", state.nextDue?.let { shortDateLabel(it) } ?: "—", Modifier.weight(1f))
            }
        }
        if (state.forecast.isNotEmpty()) {
            item(key = "forecast") { ForecastChart(state, Modifier.padding(bottom = 16.dp)) }
        }
        if (state.installments.isNotEmpty()) item(key = "list-title") {
            Text(
                "${state.installments.size} compras en cuotas", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
        }
        itemsIndexed(state.installments, key = { _, it -> it.id }) { i, inst ->
            BankInstallmentRow(inst, i, state.installments.size, Modifier.animateItem())
        }
        if (state.closedCards.isNotEmpty()) {
            item(key = "closed-title") { SectionLabel("Cerradas", Modifier.animateItem().padding(top = 16.dp)) }
            itemsIndexed(state.closedCards, key = { _, s -> "closed-" + s.id }) { i, s ->
                CardRow(
                    s, i, state.closedCards.size, Modifier.animateItem(),
                    subtitle = "No cuenta en totales ni en la lista", actionLabel = "Reabrir",
                    dimmed = true, onAction = { onSetClosed(s, false) },
                )
            }
        }
    }

    confirmClose?.let { s ->
        val n = perCard[s.id].orEmpty().size
        AlertDialog(
            onDismissRequest = { confirmClose = null },
            title = { Text("¿Cerrar ${cardName(s)}?") },
            text = { Text("Sus $n compras en cuotas dejarán de contar en totales, gráfico y lista. No se borra nada: puedes reabrirla cuando quieras.") },
            confirmButton = { TextButton(onClick = { onSetClosed(s, true); confirmClose = null }) { Text("Cerrar tarjeta") } },
            dismissButton = { TextButton(onClick = { confirmClose = null }) { Text("Cancelar") } },
        )
    }
}

private fun cardName(s: com.example.financeapp.data.model.Statement) = bankShort(s.bank) + (s.cardLast4?.let { " ••$it" } ?: "")

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 4.dp, bottom = 6.dp),
    )
}

@Composable
private fun CardRow(
    s: com.example.financeapp.data.model.Statement,
    index: Int,
    count: Int,
    modifier: Modifier,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
    dimmed: Boolean = false,
) {
    GroupRow(index, count, modifier.alpha(if (dimmed) 0.6f else 1f)) {
        com.example.financeapp.ui.components.CategoryAvatar(
            icon = Icons.Filled.CreditCard, color = MaterialTheme.colorScheme.primary, size = 40.dp, corner = 14.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(cardName(s), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        TextButton(onClick = onAction) { Text(actionLabel, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun UploadPill(uploading: Boolean, onUpload: () -> Unit) {
    FilledTonalButton(onClick = onUpload, enabled = !uploading, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
        if (uploading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
        else Icon(Icons.Filled.CloudUpload, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(if (uploading) "Leyendo…" else "Subir PDF", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun ForecastChart(state: UpcomingUiState, modifier: Modifier = Modifier) {
    val maxForecast = (state.forecast.maxOfOrNull { it.amount } ?: 1.0).coerceAtLeast(1.0)
    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(20.dp),
    ) {
        Text("Cuotas por vencer, mes a mes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom,
        ) {
            state.forecast.forEach { f ->
                // Las barras crecen desde abajo al aparecer.
                val h by animateFloatAsState((f.amount / maxForecast).toFloat(), spring(stiffness = Spring.StiffnessLow), label = "bar")
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(formatK(f.amount), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier.width(24.dp).height((6 + h * 62).dp) // deja espacio a montos y meses
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(MaterialTheme.colorScheme.primary),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(monthAbbrev(f.month), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun BankInstallmentRow(it: Installment, index: Int, count: Int, modifier: Modifier) {
    val color = parseHexColor(it.categories?.color, MaterialTheme.colorScheme.primary)
    val remaining = it.installmentTotal - it.installmentCurrent
    val card = bankShort(it.statements?.bank) + (it.statements?.cardLast4?.let { l -> " ••$l" } ?: "")
    GroupRow(index, count, modifier) {
        CategoryAvatar(icon = categoryIcon(it.categories?.icon), color = color, size = 40.dp, corner = 14.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            RowTitle(it.description, "${money(it.monthlyAmount)}/mes")
            Text(
                "$card · cuota ${it.installmentCurrent} de ${it.installmentTotal} · quedan $remaining",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
            )
            Spacer(Modifier.height(6.dp))
            ThinProgress(if (it.installmentTotal > 0) it.installmentCurrent.toFloat() / it.installmentTotal else 0f, color)
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

private const val MAX_PDF_BYTES = 10L * 1024 * 1024

/** (base64, nombre) del PDF elegido, o error legible si es muy grande o ilegible. */
private fun readPdf(context: android.content.Context, uri: Uri): Result<Pair<String, String>> = runCatching {
    val size = context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
        if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
    }
    if (size != null && size > MAX_PDF_BYTES) error("El PDF pesa más de 10 MB.")
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("No se pudo leer el PDF.")
    if (bytes.size > MAX_PDF_BYTES) error("El PDF pesa más de 10 MB.")
    Base64.encodeToString(bytes, Base64.NO_WRAP) to (queryDisplayName(context, uri) ?: "estado.pdf")
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
