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
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import com.example.financeapp.R
import com.example.financeapp.data.local.InstallmentStore
import com.example.financeapp.domain.CuotaState
import com.example.financeapp.domain.InstallmentPlan
import com.example.financeapp.domain.byDueDay
import com.example.financeapp.domain.paidToday
import com.example.financeapp.domain.status
import com.example.financeapp.util.formatMoneyCLP
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate

/**
 * Widget "Mis cuotas": pinta la caché al instante y la actualiza desde Supabase (incluye pagos
 * registrados desde la planilla en pareja). Barra de progreso, cuánto queda y "Pagar"/"Deshacer".
 */
class InstallmentsWidget : GlanceAppWidget() {
    // Exact: LocalSize es el tamaño real, para mostrar solo las filas que caben enteras.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = installmentsEntryPoint(context).installments()
        provideContent {
            val plans by store.plans.collectAsState()
            LaunchedEffect(Unit) { runCatching { store.refresh() } } // sin conexión: queda la caché
            GlanceTheme { InstallmentsContent(plans, LocalDate.now()) }
        }
    }
}

// ponytail: el nombre "PaymentStatus" es histórico; se conserva para no romper widgets ya colocados.
class PaymentStatusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = InstallmentsWidget()
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface InstallmentsEntryPoint {
    fun installments(): InstallmentStore
}

private fun installmentsEntryPoint(context: Context) =
    EntryPointAccessors.fromApplication(context.applicationContext, InstallmentsEntryPoint::class.java)

private val planIdKey = ActionParameters.Key<String>("plan_id")

class PayInstallmentAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        // Sin conexión no cambia nada: el botón sigue en "Pagar", así se nota que no se registró.
        com.example.financeapp.util.AppHaptics(context).success()
        runCatching { installmentsEntryPoint(context).installments().pay(parameters[planIdKey] ?: return) }
        com.example.financeapp.notifications.DueAlerts.checkNow(context)
    }
}

class UndoInstallmentAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        com.example.financeapp.util.AppHaptics(context).undo()
        runCatching { installmentsEntryPoint(context).installments().undoLast(parameters[planIdKey] ?: return) }
        com.example.financeapp.notifications.DueAlerts.checkNow(context)
    }
}

/** InstallmentStore lo llama tras cada cambio. */
suspend fun refreshInstallmentWidgets(context: Context) {
    runCatching { InstallmentsWidget().updateAll(context) }
}

private val ROW_COMFORT = 66.dp
private val ROW_COMPACT = 50.dp

@Composable
private fun InstallmentsContent(plans: List<InstallmentPlan>, today: LocalDate) {
    val context = LocalContext.current
    val active = plans.filterNot { it.isDone }.byDueDay()
    val open = actionStartActivity(openUpcomingIntent(context))
    val d = dimsFor(LocalSize.current.height, active.size, ROW_COMFORT, ROW_COMPACT)
    WidgetFrame(d.pad) {
        when {
            plans.isEmpty() -> FriendlyMessage(
                context, R.drawable.ic_widget_add, "Sigue tus compras en cuotas",
                "Toca para agregar la primera", positive = false,
            )
            active.isEmpty() -> FriendlyMessage(
                context, R.drawable.ic_widget_check, "¡Sin cuotas pendientes!",
                "Pagaste todo. Bien hecho", positive = true,
            )
            else -> {
                val paidMonth = active.count { it.status(today).state == CuotaState.PAGADA_MES }
                val size = LocalSize.current
                val cap = gridCapacity(size.width, size.height, d, active.size)
                val more = if (cap in 1 until active.size) " · +${active.size - cap}" else ""
                WidgetHeader(
                    d = d,
                    title = "Mis cuotas · $paidMonth/${active.size} este mes$more",
                    shortTitle = "$paidMonth/${active.size} este mes$more",
                    value = "Quedan ${formatMoneyCLP(active.sumOf { it.remainingAmount }.toDouble())}",
                    open = open,
                    button = R.drawable.ic_widget_add,
                    buttonLabel = "Abrir la app para agregar o editar cuotas",
                    buttonAction = open,
                )
                if (cap == 0) {
                    WidgetList(d, active, id = { it.id }) { plan -> PlanCard(d, plan, today) }
                } else {
                    // Lo pagado hoy cuenta como urgente para que "Deshacer" no desaparezca.
                    val shown = pickVisible(active, cap) { if (it.paidToday(today)) -1 else it.status(today).state.ordinal }
                    WidgetGrid(d, shown) { plan, mod ->
                        val (kind, label, action) = planAction(plan, today)
                        ItemTile(mod, plan.name, "${plan.paid}/${plan.total}", plan.status(today).label, toneOf(plan, today), kind, label, action)
                    }
                }
            }
        }
    }
}

private fun toneOf(plan: InstallmentPlan, today: LocalDate) = when (plan.status(today).state) {
    CuotaState.ATRASADA -> Tone.LATE
    CuotaState.POR_VENCER -> Tone.SOON
    CuotaState.PAGADA_MES, CuotaState.TERMINADA -> Tone.OK
    CuotaState.PENDIENTE -> Tone.NORMAL
}

private fun planAction(plan: InstallmentPlan, today: LocalDate): Triple<PillKind, String, androidx.glance.action.Action?> {
    val params = actionParametersOf(planIdKey to plan.id)
    return when {
        plan.paidToday(today) -> Triple(PillKind.UNDO, "Deshacer pago de ${plan.name}", actionRunCallback<UndoInstallmentAction>(params))
        plan.status(today).state == CuotaState.PAGADA_MES -> Triple(PillKind.DONE, "Cuota del mes de ${plan.name} pagada", null)
        else -> Triple(PillKind.PAY, "Pagar cuota ${plan.paid + 1} de ${plan.total} de ${plan.name}", actionRunCallback<PayInstallmentAction>(params))
    }
}

@Composable
private fun PlanCard(d: Dims, plan: InstallmentPlan, today: LocalDate) {
    val st = plan.status(today)
    val tone = toneOf(plan, today)
    val status = if (st.state == CuotaState.PAGADA_MES) "Quedan ${plan.remaining} · ${formatMoneyCLP(plan.remainingAmount.toDouble())}"
    else "${st.label} · ${formatMoneyCLP(plan.amount.toDouble())}"
    val (kind, label, action) = planAction(plan, today)
    ItemCard(
        d = d,
        name = plan.name,
        avatar = avatarColor(plan.name),
        trailingTop = "${plan.paid}/${plan.total}",
        status = status,
        tone = tone,
        extra = {
            LinearProgressIndicator(
                progress = plan.progress,
                modifier = GlanceModifier.fillMaxWidth().height(if (d.compact) 3.dp else 5.dp),
                color = GlanceTheme.colors.primary,
                backgroundColor = GlanceTheme.colors.surfaceVariant,
            )
        },
        pill = { ActionPill(d, kind, label, action) },
    )
}
