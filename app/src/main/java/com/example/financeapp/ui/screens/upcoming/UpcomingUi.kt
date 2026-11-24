package com.example.financeapp.ui.screens.upcoming

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.financeapp.ui.components.pressable
import com.example.financeapp.ui.theme.FinanceTheme
import com.example.financeapp.util.formatMoneyCLP

/*
 * Piezas compartidas de "Próximos pagos": listas agrupadas (Material 3 Expressive),
 * botón Pagar → Pagada animado, montos que cambian con transición.
 */

internal fun money(v: Double) = formatMoneyCLP(v)

/** Extremos del grupo muy redondeados, filas intermedias casi rectas: se lee como un solo bloque. */
internal fun groupShape(index: Int, count: Int): Shape {
    val big = 24.dp
    val small = 6.dp
    val top = if (index == 0) big else small
    val bottom = if (index == count - 1) big else small
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
internal fun GroupRow(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val base = if (onClick != null) modifier.pressable(onClick = onClick, pressedScale = 0.98f, onClickLabel = onClickLabel) else modifier
    Row(
        modifier = base.fillMaxWidth().heightIn(min = 72.dp)
            .clip(groupShape(index, count))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Una línea que explica qué hay en la pestaña + acción opcional a la derecha. */
@Composable
internal fun PageIntro(text: String, action: (@Composable () -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        if (action != null) {
            Spacer(Modifier.width(12.dp))
            action()
        }
    }
}

@Composable
internal fun EmptyPage(icon: ImageVector, title: String, body: String, action: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        action()
    }
}

/** Título + valor a la derecha, en una línea. */
@Composable
internal fun RowTitle(title: String, trailing: String, trailingColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(trailing, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = trailingColor)
    }
}

@Composable
internal fun ThinProgress(progress: Float, color: Color, modifier: Modifier = Modifier) {
    // Resorte suave: la barra "avanza" visiblemente al pagar.
    val p by animateFloatAsState(
        progress.coerceIn(0f, 1f),
        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "thinProgress",
    )
    LinearProgressIndicator(
        progress = { p },
        modifier = modifier.fillMaxWidth().height(4.dp),
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

/**
 * "Pagar" (tonal) que al registrarse se transforma en un chip "Pagada ✓" con un pequeño rebote.
 * Tocar el chip sigue llamando [onClick] (la pantalla decide si confirma un pago extra).
 */
@Composable
internal fun PayButton(done: Boolean, onClick: () -> Unit, label: String, busy: Boolean = false, doneText: String = "Pagada", actionText: String = "Pagar") {
    val finance = FinanceTheme.colors
    AnimatedContent(
        targetState = done,
        transitionSpec = {
            (fadeIn(tween(200)) + scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)))
                .togetherWith(fadeOut(tween(120)))
        },
        label = "payButton",
    ) { isDone ->
        if (isDone) {
            Row(
                modifier = Modifier.pressable(onClick = onClick, onClickLabel = label)
                    .heightIn(min = 40.dp).clip(CircleShape).background(finance.incomeContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .semantics { contentDescription = "$doneText. $label" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Check, null, tint = finance.income, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(doneText, style = MaterialTheme.typography.labelLarge, color = finance.income, fontWeight = FontWeight.SemiBold)
            }
        } else {
            FilledTonalButton(
                onClick = onClick, enabled = !busy,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.heightIn(min = 40.dp).semantics { contentDescription = label },
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(actionText, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
