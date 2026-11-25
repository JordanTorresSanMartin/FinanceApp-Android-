package com.example.financeapp.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import com.example.financeapp.R
import com.example.financeapp.domain.PayState
import com.example.financeapp.domain.PaymentStatus
import com.example.financeapp.domain.repository.FinanceRepository
import com.example.financeapp.domain.use_case.GetRecurringStatusUseCase
import com.example.financeapp.util.formatMoneyCLP
import com.example.financeapp.util.todayLocalDate
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Widget "Pagos fijos" (suscripciones, plan del celular, internet…): estado del período de
 * cada pago con botón "Pagar" (registra el gasto) y "Deshacer".
 * Los datos vienen de Supabase pero se guardan en caché local: el widget se ve al instante
 * y sigue mostrando lo último conocido sin conexión.
 */
class FixedPaymentsWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = fixedEntryPoint(context).fixedPayments()
        provideContent {
            val snap by store.state.collectAsState()
            LaunchedEffect(Unit) { store.refresh() } // pinta la caché y actualiza en paralelo
            GlanceTheme { FixedContent(snap, LocalDate.now()) }
        }
    }
}

class FixedPaymentsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FixedPaymentsWidget()
}

// ── Datos ────────────────────────────────────────────────────────────────────

@Serializable
data class FixedRow(
    val id: String,
    val name: String,
    val amount: Double,
    val color: String? = null,
    val state: PayState,
    val due: String? = null,
    val paidOn: String? = null,
) {
    fun label(today: LocalDate) =
        PaymentStatus(state, due?.let(LocalDate::parse), paidOn?.let(LocalDate::parse)).label(today)
}

@Serializable
data class FixedSnapshot(
    val signedIn: Boolean = true,
    val loaded: Boolean = false,
    val offline: Boolean = false,
    val rows: List<FixedRow> = emptyList(),
)

@Singleton
class FixedPaymentsWidgetStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: Auth,
    private val repo: FinanceRepository,
    private val recurringStatus: GetRecurringStatusUseCase,
) {
    private val prefs = context.getSharedPreferences("fixed_widget", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val _state = MutableStateFlow(
        runCatching { json.decodeFromString<FixedSnapshot>(prefs.getString(KEY, null)!!) }.getOrDefault(FixedSnapshot()),
    )
    val state: StateFlow<FixedSnapshot> = _state.asStateFlow()

    private suspend fun save(s: FixedSnapshot) {
        _state.value = s
        withContext(Dispatchers.IO) { prefs.edit().putString(KEY, json.encodeToString(s)).apply() }
        runCatching { FixedPaymentsWidget().updateAll(context) }
    }

    /** Llamar tras cualquier cambio en pagos fijos o al cambiar la sesión. */
    suspend fun refresh() {
        if (demo()) return
        runCatching { auth.awaitInitialization() }
        when (auth.sessionStatus.value) {
            // Solo esto es "no hay sesión" (nunca entró o cerró sesión).
            is SessionStatus.NotAuthenticated -> return save(FixedSnapshot(signedIn = false, loaded = true))
            // El token venció y no se pudo renovar (en segundo plano Android suele cortar la red): la
            // sesión sigue guardada. Antes esto mostraba "Inicia sesión" aunque la app estuviera logueada.
            is SessionStatus.RefreshFailure -> {
                runCatching { auth.refreshCurrentSession() }
                if (auth.sessionStatus.value !is SessionStatus.Authenticated) {
                    return save(_state.value.copy(signedIn = true, loaded = true, offline = true))
                }
            }
            else -> Unit
        }
        val today = LocalDate.now()
        val items = withTimeoutOrNull(15_000) { runCatching { recurringStatus(today) }.getOrNull() }
            ?: return save(_state.value.copy(loaded = true, offline = true))
        publish(items)
    }

    /** Pinta el widget con pagos ya consultados (la revisión de fondo los comparte: una sola consulta). */
    suspend fun publish(items: List<com.example.financeapp.domain.RecurringWithStatus>) {
        if (demo()) return
        save(FixedSnapshot(loaded = true, rows = items.mapNotNull { r ->
            FixedRow(
                id = r.item.id ?: return@mapNotNull null,
                name = r.item.name, amount = r.item.amount,
                color = r.item.color ?: r.item.categories?.color,
                state = r.status.state, due = r.status.due?.toString(), paidOn = r.status.paidOn?.toString(),
            )
        }))
    }

    /** Al cerrar sesión: no dejar datos personales en el widget. */
    suspend fun clear() { if (!demo()) save(FixedSnapshot(signedIn = false, loaded = true)) }

    // ponytail: modo demo solo en builds debug, para probar el widget en el emulador sin cuenta.
    private fun demo() = context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0 &&
        prefs.getBoolean("demo", false)

    /** Optimista: el widget responde al toque y luego confirma con el servidor. */
    suspend fun pay(id: String) = mutate(id, PayState.PAGADO, LocalDate.now().toString()) { item ->
        repo.registerRecurringPayment(item, todayLocalDate())
    }

    suspend fun undo(id: String) = mutate(id, PayState.PENDIENTE, null) { item ->
        repo.undoRecurringPayment(item, todayLocalDate())
    }

    private suspend fun mutate(
        id: String, optimisticState: PayState, paidOn: String?,
        call: suspend (com.example.financeapp.data.model.RecurringPayment) -> Unit,
    ) {
        val before = _state.value
        save(before.copy(rows = before.rows.map { if (it.id == id) it.copy(state = optimisticState, paidOn = paidOn) else it }))
        val ok = runCatching {
            val item = repo.getRecurringPayments().first { it.id == id }
            call(item)
        }.isSuccess
        if (ok) refresh() else save(before.copy(offline = true))
    }

    private companion object { const val KEY = "snapshot" }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FixedEntryPoint {
    fun fixedPayments(): FixedPaymentsWidgetStore
}

private fun fixedEntryPoint(context: Context) =
    EntryPointAccessors.fromApplication(context.applicationContext, FixedEntryPoint::class.java)

private val rowIdKey = ActionParameters.Key<String>("recurring_id")

class MarkPaidAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        com.example.financeapp.util.AppHaptics(context).success()
        fixedEntryPoint(context).fixedPayments().pay(parameters[rowIdKey] ?: return)
        com.example.financeapp.notifications.DueAlerts.checkNow(context)
    }
}

class UndoPaidAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        com.example.financeapp.util.AppHaptics(context).undo()
        fixedEntryPoint(context).fixedPayments().undo(parameters[rowIdKey] ?: return)
        com.example.financeapp.notifications.DueAlerts.checkNow(context)
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) =
        fixedEntryPoint(context).fixedPayments().refresh()
}

// ── UI ───────────────────────────────────────────────────────────────────────

private val ROW_COMFORT = 58.dp
private val ROW_COMPACT = 44.dp

@Composable
private fun FixedContent(snap: FixedSnapshot, today: LocalDate) {
    val context = LocalContext.current
    val open = actionStartActivity(openUpcomingIntent(context))
    val rows = snap.rows.sortedBy { it.due ?: "9999" }
    val d = dimsFor(LocalSize.current.height, rows.size, ROW_COMFORT, ROW_COMPACT)
    WidgetFrame(d.pad) {
        when {
            !snap.signedIn -> FriendlyMessage(context, R.drawable.ic_widget_person, "Inicia sesión en FinanceApp", "Para ver tus suscripciones y pagos fijos", positive = false)
            !snap.loaded -> FriendlyMessage(context, R.drawable.ic_widget_refresh, "Cargando tus pagos…", "Un momento", positive = false, onClick = actionRunCallback<RefreshAction>())
            rows.isEmpty() && snap.offline -> FriendlyMessage(context, R.drawable.ic_widget_refresh, "Sin conexión", "Toca para reintentar", positive = false, onClick = actionRunCallback<RefreshAction>())
            rows.isEmpty() -> FriendlyMessage(context, R.drawable.ic_widget_add, "Agrega tus pagos fijos", "Netflix, plan del celular, internet…", positive = false)
            else -> {
                val pending = rows.filter { it.state != PayState.PAGADO }
                val paid = rows.size - pending.size
                val size = LocalSize.current
                val cap = gridCapacity(size.width, size.height, d, rows.size)
                val more = if (cap in 1 until rows.size) " · +${rows.size - cap}" else ""
                WidgetHeader(
                    d = d,
                    title = "Pagos fijos · $paid/${rows.size} pagados$more" + (if (snap.offline) " · sin conexión" else ""),
                    shortTitle = "$paid/${rows.size} pagados$more" + (if (snap.offline) " · sin red" else ""),
                    value = if (pending.isEmpty()) "¡Todo al día!" else "Falta ${formatMoneyCLP(pending.sumOf { it.amount })}",
                    open = open,
                    button = R.drawable.ic_widget_refresh,
                    buttonLabel = "Actualizar pagos fijos",
                    buttonAction = actionRunCallback<RefreshAction>(),
                )
                if (cap == 0) {
                    WidgetList(d, rows, id = { it.id }) { row -> FixedCard(d, row, today) }
                } else {
                    val shown = pickVisible(rows, cap) { if (it.paidOn == today.toString()) -1 else it.state.ordinal }
                    WidgetGrid(d, shown) { row, mod ->
                        val (kind, label, action) = fixedAction(row, today)
                        ItemTile(mod, row.name, null, row.label(today), toneOf(row), kind, label, action)
                    }
                }
            }
        }
    }
}

private fun toneOf(row: FixedRow) = when (row.state) {
    PayState.ATRASADO -> Tone.LATE
    PayState.POR_VENCER -> Tone.SOON
    PayState.PAGADO -> Tone.OK
    PayState.PENDIENTE -> Tone.NORMAL
}

private fun fixedAction(row: FixedRow, today: LocalDate): Triple<PillKind, String, androidx.glance.action.Action?> {
    val params = actionParametersOf(rowIdKey to row.id)
    return when {
        row.state == PayState.PAGADO && row.paidOn == today.toString() ->
            Triple(PillKind.UNDO, "Deshacer pago de ${row.name}", actionRunCallback<UndoPaidAction>(params))
        row.state == PayState.PAGADO -> Triple(PillKind.DONE, "${row.name} pagado este período", null)
        else -> Triple(PillKind.PAY, "Pagar ${row.name}", actionRunCallback<MarkPaidAction>(params))
    }
}

@Composable
private fun FixedCard(d: Dims, row: FixedRow, today: LocalDate) {
    val (kind, label, action) = fixedAction(row, today)
    ItemCard(
        d = d,
        name = row.name,
        avatar = avatarColor(row.name, row.color),
        trailingTop = formatMoneyCLP(row.amount),
        status = row.label(today),
        tone = toneOf(row),
        pill = { ActionPill(d, kind, label, action, doneText = "Pagado") },
    )
}
