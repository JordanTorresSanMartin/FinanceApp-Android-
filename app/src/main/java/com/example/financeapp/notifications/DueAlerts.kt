package com.example.financeapp.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.financeapp.R
import com.example.financeapp.data.local.InstallmentStore
import com.example.financeapp.domain.CuotaState
import com.example.financeapp.domain.InstallmentPlan
import com.example.financeapp.domain.PayState
import com.example.financeapp.domain.RecurringWithStatus
import com.example.financeapp.domain.status
import com.example.financeapp.domain.use_case.GetRecurringStatusUseCase
import com.example.financeapp.ui.widget.openUpcomingIntent
import com.example.financeapp.util.formatK
import com.example.financeapp.util.formatMoneyCLP
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** Algo que hay que pagar hoy (o que ya se atrasó). */
data class DueItem(val name: String, val amount: Double, val overdue: Boolean)

/** Pagos fijos y cuotas que vencen hoy o están atrasados. Pura: se testea sin Android. */
fun dueItems(recurring: List<RecurringWithStatus>, plans: List<InstallmentPlan>, today: LocalDate): List<DueItem> {
    val fijos = recurring.mapNotNull { r ->
        when {
            r.status.state == PayState.ATRASADO -> DueItem(r.item.name, r.item.amount, overdue = true)
            r.status.state == PayState.POR_VENCER && r.status.due == today -> DueItem(r.item.name, r.item.amount, overdue = false)
            else -> null
        }
    }
    val cuotas = plans.filterNot { it.isDone }.mapNotNull { p ->
        val st = p.status(today)
        when {
            st.state == CuotaState.ATRASADA -> DueItem(p.name, p.amount.toDouble(), overdue = true)
            // Sin día de vencimiento no hay "hoy": no se alerta (evita avisos todo el mes).
            st.state == CuotaState.POR_VENCER && p.dueDay != null &&
                today.dayOfMonth == p.dueDay.coerceAtMost(today.lengthOfMonth()) -> DueItem(p.name, p.amount.toDouble(), overdue = false)
            else -> null
        }
    }
    return (fijos + cuotas).sortedByDescending { it.overdue }
}

/**
 * Live Update (Android 16+): con pagos para hoy aparece un chip en la barra de estado, junto a la
 * cámara ("$85k"), y la notificación queda arriba mientras no se pague. Se va sola al pagar.
 * En versiones anteriores es una notificación persistente normal.
 */
object DueAlerts {
    private const val CHANNEL = "pagos_hoy"
    private const val NOTIFICATION_ID = 4100

    /** Revisión periódica (2 veces al día, con internet). Idempotente: se llama al abrir la app. */
    fun schedule(context: Context) {
        val req = PeriodicWorkRequestBuilder<DueWorker>(12, TimeUnit.HOURS)
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("due-alerts", ExistingPeriodicWorkPolicy.KEEP, req)
    }

    /** Revisa ahora (tras pagar o cambiar algo). Llamadas seguidas se funden en una. */
    fun checkNow(context: Context) {
        WorkManager.getInstance(context)
            .enqueueUniqueWork("due-now", ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<DueWorker>().build())
    }

    fun show(context: Context, items: List<DueItem>) {
        val nm = NotificationManagerCompat.from(context)
        if (items.isEmpty()) { nm.cancel(NOTIFICATION_ID); return }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            android.os.Build.VERSION.SDK_INT >= 33
        ) return
        ensureChannel(context)

        val total = items.sumOf { it.amount }
        val first = items.first()
        val title = when {
            items.size > 1 -> "${items.size} pagos para hoy · ${formatMoneyCLP(total)}"
            first.overdue -> "${first.name} está atrasado"
            else -> "Hoy vence ${first.name}"
        }
        val text = if (items.size == 1) "${formatMoneyCLP(first.amount)} · toca para registrarlo"
        else items.joinToString(" · ") { it.name + if (it.overdue) " (atrasado)" else "" }
        val open = PendingIntent.getActivity(
            context, 0, openUpcomingIntent(context), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_finance)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setRequestPromotedOngoing(true) // Live Update en Android 16+
            .setShortCriticalText(if (items.size == 1) formatK(first.amount) else "${items.size} pagos") // chip junto a la cámara
            .build()
        nm.notify(NOTIFICATION_ID, n)
    }

    private fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) != null) return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Pagos de hoy", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Aviso en vivo de pagos fijos y cuotas que vencen hoy o están atrasados"
            },
        )
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DueEntryPoint {
    fun recurringStatus(): GetRecurringStatusUseCase
    fun installments(): InstallmentStore
    fun fixedWidget(): com.example.financeapp.ui.widget.FixedPaymentsWidgetStore
}

class DueWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ep = EntryPointAccessors.fromApplication(applicationContext, DueEntryPoint::class.java)
        // Sin sesión o sin red: se reintenta en la próxima revisión (no insiste en bucle).
        val recurring = runCatching { ep.recurringStatus()() }.getOrElse { return Result.success() }
        runCatching { ep.installments().refresh() }
        // Corre con red garantizada: también recupera el widget "Pagos fijos" si quedó sin conexión.
        runCatching { ep.fixedWidget().refresh() }
        DueAlerts.show(applicationContext, dueItems(recurring, ep.installments().plans.value, LocalDate.now()))
        return Result.success()
    }
}
