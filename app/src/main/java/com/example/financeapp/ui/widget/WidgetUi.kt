package com.example.financeapp.ui.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.financeapp.MainActivity
import com.example.financeapp.R
import com.example.financeapp.util.parseHexColor

/*
 * Piezas visuales compartidas por los widgets "Pagos fijos" y "Mis cuotas".
 * Colores dinámicos del sistema (Material You) + tonos suaves por estado.
 */

// Tonos de estado: contenedor suave + texto con contraste ≥ 4.5:1 en claro y oscuro.
internal val okText = ColorProvider(day = Color(0xFF17703A), night = Color(0xFF86DDA0))
internal val okBg = ColorProvider(day = Color(0xFFE2F5E7), night = Color(0xFF1C3524))
internal val warnText = ColorProvider(day = Color(0xFF8A5300), night = Color(0xFFF5C46B))
internal val warnBg = ColorProvider(day = Color(0xFFFFF1D6), night = Color(0xFF3A2D12))
internal val lateText = ColorProvider(day = Color(0xFFB3261E), night = Color(0xFFFFB4AB))
internal val lateBg = ColorProvider(day = Color(0xFFFDE7E5), night = Color(0xFF3F1F1C))

enum class Tone { LATE, SOON, NORMAL, OK }

@Composable
internal fun toneText(t: Tone): ColorProvider = when (t) {
    Tone.LATE -> lateText
    Tone.SOON -> warnText
    Tone.OK -> okText
    Tone.NORMAL -> GlanceTheme.colors.onSurfaceVariant
}

@Composable
internal fun toneBg(t: Tone): ColorProvider = when (t) {
    Tone.LATE -> lateBg
    Tone.SOON -> warnBg
    Tone.OK -> okBg
    Tone.NORMAL -> GlanceTheme.colors.surface
}

private val avatarPalette = listOf(0xFF5B6EE1, 0xFF2E9E8F, 0xFFE0705A, 0xFF8E6CC9, 0xFFC98A1E, 0xFF3D8BD9, 0xFFD15C8A)

/** Color estable por nombre (o el color de la categoría si existe). */
internal fun avatarColor(name: String, hex: String? = null): Color =
    hex?.let { parseHexColor(it, Color.Unspecified) }?.takeIf { it != Color.Unspecified }
        ?: Color(avatarPalette[Math.floorMod(name.lowercase().hashCode(), avatarPalette.size)])

/**
 * Medidas del widget. Cómodo por defecto; si así no caben al menos 2 filas enteras
 * (launchers con celdas bajas, p. ej. OxygenOS en 4×2) pasa a compacto.
 */
internal data class Dims(
    val compact: Boolean,
    val pad: Dp,
    val headerH: Dp,
    val gap: Dp,
    val rowH: Dp,
    val avatar: Dp,
    val pillH: Dp,
    val name: TextUnit,
    val value: TextUnit,
) {
    fun rowsFit(height: Dp): Int = ((height - pad * 2 - headerH) / (rowH + gap)).toInt().coerceAtLeast(1)
}

internal fun dimsFor(height: Dp, items: Int, comfortRow: Dp, compactRow: Dp): Dims {
    val comfort = Dims(false, 10.dp, 46.dp, 6.dp, comfortRow, 36.dp, 40.dp, 15.sp, 20.sp)
    if (comfort.rowsFit(height) >= minOf(items, 2)) return comfort
    return Dims(true, 8.dp, 30.dp, 4.dp, compactRow, 28.dp, 32.dp, 14.sp, 16.sp)
}

/**
 * Elige qué filas mostrar si no caben todas: las más urgentes (lo pagado hoy cuenta como urgente
 * para que "Deshacer" no desaparezca), y las muestra en su orden natural (estable).
 */
internal fun <T> pickVisible(items: List<T>, max: Int, rank: (T) -> Int): List<T> {
    if (items.size <= max) return items
    val keep = items.withIndex().sortedBy { rank(it.value) }.take(max).map { it.index }.toSet()
    return items.filterIndexed { i, _ -> i in keep }
}

internal fun openUpcomingIntent(context: Context) =
    Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        data = Uri.parse("financeapp://upcoming")
        putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_UPCOMING)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }

@Composable
internal fun WidgetFrame(pad: Dp = 10.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(android.R.dimen.system_app_widget_background_radius)
            .padding(pad),
        content = content,
    )
}

/** "Mis cuotas · 2/3 este mes" / "Quedan $904.910" + botón redondo a la derecha. */
@Composable
internal fun WidgetHeader(
    d: Dims,
    title: String,
    /** Versión corta para el encabezado compacto de una línea. */
    shortTitle: String,
    value: String,
    open: Action,
    button: Int,
    buttonLabel: String,
    buttonAction: Action,
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().height(d.headerH).padding(start = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (d.compact) {
            // Una sola línea: el monto manda; el título se recorta si no cabe.
            Row(modifier = GlanceModifier.defaultWeight().clickable(open), verticalAlignment = Alignment.CenterVertically) {
                Text(value, maxLines = 1, style = TextStyle(fontSize = d.value, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface))
                Spacer(GlanceModifier.width(8.dp))
                Text(shortTitle, maxLines = 1, modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = GlanceTheme.colors.onSurfaceVariant))
            }
        } else {
            Column(modifier = GlanceModifier.defaultWeight().clickable(open)) {
                Text(title, maxLines = 1, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = GlanceTheme.colors.onSurfaceVariant))
                Text(value, maxLines = 1, style = TextStyle(fontSize = d.value, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface))
            }
        }
        val b = if (d.compact) 30.dp else 44.dp
        Box(
            modifier = GlanceModifier.size(b).cornerRadius(b / 2)
                .background(GlanceTheme.colors.secondaryContainer)
                .clickable(buttonAction)
                .semantics { contentDescription = buttonLabel },
            contentAlignment = Alignment.Center,
        ) {
            Image(ImageProvider(button), null, colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer), modifier = GlanceModifier.size(if (d.compact) 18.dp else 22.dp))
        }
    }
}

@Composable
private fun LetterAvatar(name: String, color: Color, size: Dp) {
    Box(
        modifier = GlanceModifier.size(size).cornerRadius(size / 2).background(ColorProvider(color)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().firstOrNull()?.uppercase() ?: "•",
            style = TextStyle(fontSize = if (size < 34.dp) 14.sp else 16.sp, fontWeight = FontWeight.Bold, color = ColorProvider(Color.White)),
        )
    }
}

enum class PillKind { PAY, UNDO, DONE }

/** Botón con texto (más claro que un ícono suelto). DONE no es tocable: solo confirma. */
@Composable
internal fun ActionPill(d: Dims, kind: PillKind, label: String, action: Action?, doneText: String = "Pagada") {
    val bg: ColorProvider
    val fg: ColorProvider
    val text: String
    val icon: Int
    when (kind) {
        PillKind.PAY -> { bg = GlanceTheme.colors.primary; fg = GlanceTheme.colors.onPrimary; text = "Pagar"; icon = R.drawable.ic_widget_check }
        PillKind.UNDO -> { bg = GlanceTheme.colors.secondaryContainer; fg = GlanceTheme.colors.onSecondaryContainer; text = "Deshacer"; icon = R.drawable.ic_widget_undo }
        PillKind.DONE -> { bg = okBg; fg = okText; text = doneText; icon = R.drawable.ic_widget_check }
    }
    var mod = GlanceModifier.height(d.pillH).cornerRadius(d.pillH / 2).background(bg).padding(horizontal = if (d.compact) 10.dp else 12.dp)
    if (action != null) mod = mod.clickable(action)
    Row(modifier = mod.semantics { contentDescription = label }, verticalAlignment = Alignment.CenterVertically) {
        Image(ImageProvider(icon), null, colorFilter = ColorFilter.tint(fg), modifier = GlanceModifier.size(16.dp))
        Spacer(GlanceModifier.width(4.dp))
        Text(text, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = fg))
    }
}

/** Tarjeta de una fila: avatar · textos · botón. `extra` va bajo el nombre (ej. barra de progreso). */
@Composable
internal fun ItemCard(
    d: Dims,
    name: String,
    avatar: Color,
    trailingTop: String?,
    status: String,
    tone: Tone,
    pill: @Composable () -> Unit,
    extra: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().height(d.rowH)
            .cornerRadius(android.R.dimen.system_app_widget_inner_radius)
            .background(toneBg(tone))
            .padding(horizontal = if (d.compact) 8.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LetterAvatar(name, avatar, d.avatar)
        Spacer(GlanceModifier.width(if (d.compact) 8.dp else 10.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(name, maxLines = 1, modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(fontSize = d.name, fontWeight = FontWeight.Medium, color = GlanceTheme.colors.onSurface))
                if (trailingTop != null) {
                    Text(trailingTop, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.primary))
                }
            }
            if (extra != null) {
                val g = if (d.compact) 2.dp else 4.dp
                Spacer(GlanceModifier.height(g)); extra(); Spacer(GlanceModifier.height(g))
            }
            // El color acompaña al texto; nunca es la única señal.
            Text(status, maxLines = 1, style = TextStyle(
                fontSize = 12.sp, color = toneText(tone),
                fontWeight = if (tone == Tone.LATE) FontWeight.Bold else FontWeight.Medium,
            ))
        }
        Spacer(GlanceModifier.width(8.dp))
        pill()
    }
}

/** Estado vacío / todo listo / sin sesión: ícono en círculo, título y una línea. */
@Composable
internal fun FriendlyMessage(context: Context, icon: Int, title: String, subtitle: String, positive: Boolean, onClick: Action? = null) {
    val size = LocalSize.current
    Row(
        modifier = GlanceModifier.fillMaxSize().padding(horizontal = 8.dp)
            .clickable(onClick ?: actionStartActivity(openUpcomingIntent(context))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier.size(52.dp).cornerRadius(26.dp)
                .background(if (positive) okBg else GlanceTheme.colors.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Image(ImageProvider(icon), null, modifier = GlanceModifier.size(28.dp),
                colorFilter = ColorFilter.tint(if (positive) okText else GlanceTheme.colors.onSecondaryContainer))
        }
        Spacer(GlanceModifier.width(14.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(title, maxLines = 2, style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface))
            if (size.height > 90.dp) {
                Spacer(GlanceModifier.height(2.dp))
                Text(subtitle, maxLines = 2, style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurfaceVariant))
            }
        }
    }
}
