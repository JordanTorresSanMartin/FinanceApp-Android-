package com.example.financeapp.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
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
 * Widget "Mis cuotas" (datos locales, sin conexión ni sesión): barra de progreso por compra,
 * cuánto queda y botón "Pagar" para la cuota del mes (con "Deshacer").
 */
class InstallmentsWidget : GlanceAppWidget() {
    // Exact: LocalSize es el tamaño real, para mostrar solo las filas que caben enteras.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = installmentsEntryPoint(context).installments()
        provideContent {
            val plans by store.plans.collectAsState()
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
        installmentsEntryPoint(context).installments().pay(parameters[planIdKey] ?: return)
    }
}

class UndoInstallmentAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        installmentsEntryPoint(context).installments().undoLast(parameters[planIdKey] ?: return)
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
                val max = d.rowsFit(LocalSize.current.height)
                val visible = pickVisible(active, max) { if (it.paidToday(today)) -1 else it.status(today).state.ordinal }
                val hidden = active.size - visible.size
                val paidMonth = active.count { it.status(today).state == CuotaState.PAGADA_MES }
                WidgetHeader(
                    d = d,
                    title = "Mis cuotas · $paidMonth/${active.size} este mes" + if (hidden > 0) " · +$hidden más" else "",
                    shortTitle = "$paidMonth/${active.size} este mes" + if (hidden > 0) " · +$hidden" else "",
                    value = "Quedan ${formatMoneyCLP(active.sumOf { it.remainingAmount }.toDouble())}",
                    open = open,
                    button = R.drawable.ic_widget_add,
                    buttonLabel = "Abrir la app para agregar o editar cuotas",
                    buttonAction = open,
                )
                visible.forEach { plan ->
                    Spacer(GlanceModifier.height(d.gap))
                    PlanCard(d, plan, today)
                }
            }
        }
    }
}

@Composable
private fun PlanCard(d: Dims, plan: InstallmentPlan, today: LocalDate) {
    val st = plan.status(today)
    val tone = when (st.state) {
        CuotaState.ATRASADA -> Tone.LATE
        CuotaState.POR_VENCER -> Tone.SOON
        CuotaState.PAGADA_MES, CuotaState.TERMINADA -> Tone.OK
        CuotaState.PENDIENTE -> Tone.NORMAL
    }
    val params = actionParametersOf(planIdKey to plan.id)
    val status = if (st.state == CuotaState.PAGADA_MES) "Quedan ${plan.remaining} · ${formatMoneyCLP(plan.remainingAmount.toDouble())}"
    else "${st.label} · ${formatMoneyCLP(plan.amount.toDouble())}"
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
        pill = {
            when {
                plan.paidToday(today) -> ActionPill(d, PillKind.UNDO, "Deshacer pago de ${plan.name}", actionRunCallback<UndoInstallmentAction>(params))
                st.state == CuotaState.PAGADA_MES -> ActionPill(d, PillKind.DONE, "Cuota del mes de ${plan.name} pagada", null)
                else -> ActionPill(d, PillKind.PAY, "Pagar cuota ${plan.paid + 1} de ${plan.total} de ${plan.name}", actionRunCallback<PayInstallmentAction>(params))
            }
        },
    )
}
