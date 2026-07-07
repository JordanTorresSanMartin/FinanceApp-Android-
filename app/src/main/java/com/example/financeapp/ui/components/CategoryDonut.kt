package com.example.financeapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.hypot

data class DonutSegment(val key: String, val label: String, val value: Double, val color: Color)

@Composable
fun CategoryDonut(
    data: List<DonutSegment>,
    centerPrimary: String,
    centerSecondary: String,
    modifier: Modifier = Modifier,
    size: Dp = 250.dp,
    thickness: Dp = 40.dp,
    selectedKey: String? = null,
    onSelect: (String?) -> Unit = {},
) {
    val density = LocalDensity.current
    val sizePx = with(density) { size.toPx() }
    val baseStroke = with(density) { thickness.toPx() }
    val selStroke = baseStroke * 1.18f
    val total = data.sumOf { it.value }
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .size(size)
                .pointerInput(data, selectedKey) {
                    detectTapGestures { pos ->
                        if (total <= 0.0) return@detectTapGestures
                        val c = (sizePx / 2f).toDouble()
                        val dx = pos.x.toDouble() - c
                        val dy = pos.y.toDouble() - c
                        val r = hypot(dx, dy)
                        val outerR = c
                        val innerR = outerR - selStroke.toDouble()
                        if (r < innerR - 8.0 || r > outerR + 8.0) return@detectTapGestures
                        var ang = atan2(dx, -dy) // 0 = arriba, sentido horario
                        if (ang < 0) ang += 2.0 * Math.PI
                        val frac = ang / (2.0 * Math.PI)
                        var acc = 0.0
                        for (seg in data) {
                            val f = seg.value / total
                            if (frac >= acc && frac < acc + f) {
                                onSelect(if (selectedKey == seg.key) null else seg.key)
                                return@detectTapGestures
                            }
                            acc += f
                        }
                    }
                },
        ) {
            val inset = selStroke / 2f
            val arcSize = Size(sizePx - selStroke, sizePx - selStroke)
            val topLeft = Offset(inset, inset)
            if (total <= 0.0) {
                drawArc(
                    color = emptyColor, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = topLeft, size = arcSize, style = Stroke(width = baseStroke),
                )
            } else {
                val gap = if (data.size > 1) 2.2f else 0f
                var start = -90f
                for (seg in data) {
                    val sweep = (seg.value / total * 360.0).toFloat()
                    val selected = selectedKey == seg.key
                    val dim = selectedKey != null && !selected
                    drawArc(
                        color = seg.color.copy(alpha = if (dim) 0.35f else 1f),
                        startAngle = start + gap / 2f,
                        sweepAngle = (sweep - gap).coerceAtLeast(0.1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = if (selected) selStroke else baseStroke),
                    )
                    start += sweep
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                centerSecondary,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                centerPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}
