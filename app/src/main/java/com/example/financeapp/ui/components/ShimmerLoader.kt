package com.example.financeapp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Skeletons de carga. Una sola animación por pantalla (SkeletonScope) y cada hueso la lee
 * SOLO al dibujar: no hay recomposición por fotograma (la versión anterior con `composed`
 * recomponía cada bloque 60 veces por segundo). Al terminar de cargar, el scope sale de la
 * composición y la animación se detiene sola.
 */

private val LocalShimmer = staticCompositionLocalOf<State<Float>?> { null }

@Composable
fun SkeletonScope(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val progress = rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "skeletonProgress",
    )
    CompositionLocalProvider(LocalShimmer provides progress) {
        Column(modifier.semantics { contentDescription = "Cargando" }) { content() }
    }
}

/**
 * Bloque con un brillo que lo cruza. Usar dentro de [SkeletonScope].
 * Sobre tarjetas de color pasa [color] (ej. onContainer con alpha bajo) para que no se vea gris pesado.
 */
@Composable
fun Bone(modifier: Modifier, shape: Shape = RoundedCornerShape(8.dp), color: Color? = null) {
    val progress = LocalShimmer.current
    val base = color ?: MaterialTheme.colorScheme.surfaceContainerHighest
    val shine = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    Spacer(
        modifier.clip(shape).drawWithCache {
            val w = size.width
            onDrawBehind {
                drawRect(base)
                val x = (progress?.value ?: 0f) * (w * 2.5f) - w
                drawRect(Brush.linearGradient(listOf(Color.Transparent, shine, Color.Transparent), Offset(x, 0f), Offset(x + w * 0.8f, size.height)))
            }
        },
    )
}

/** Fila típica: avatar + dos líneas + valor a la derecha. */
@Composable
fun SkeletonRow(avatar: Dp = 40.dp, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Bone(Modifier.size(avatar), RoundedCornerShape(14.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Bone(Modifier.fillMaxWidth(0.55f).height(14.dp))
            Bone(Modifier.fillMaxWidth(0.35f).height(11.dp))
        }
        Spacer(Modifier.width(12.dp))
        Bone(Modifier.width(64.dp).height(14.dp))
    }
}

/** Tarjeta de lista agrupada con [rows] filas esqueleto. */
@Composable
fun SkeletonCardList(rows: Int, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(horizontal = 16.dp, vertical = 6.dp),
    ) { repeat(rows) { SkeletonRow() } }
}

/** Tarjeta grande de resumen (título corto + monto grande + línea). */
@Composable
fun SkeletonHero(modifier: Modifier = Modifier, height: Dp = 140.dp) {
    Column(
        modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Bone(Modifier.width(120.dp).height(12.dp))
        Bone(Modifier.width(200.dp).height(34.dp), RoundedCornerShape(10.dp))
        Bone(Modifier.fillMaxWidth(0.7f).height(10.dp), CircleShape)
    }
}

/** Gráfico de barras esqueleto. */
@Composable
fun SkeletonChart(modifier: Modifier = Modifier, bars: Int = 6) {
    Row(
        modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom,
    ) {
        val heights = listOf(0.55f, 0.8f, 0.65f, 0.95f, 0.5f, 0.7f, 0.6f, 0.85f)
        repeat(bars) { i -> Bone(Modifier.width(22.dp).fillMaxHeightFraction(heights[i % heights.size]), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)) }
    }
}

private fun Modifier.fillMaxHeightFraction(f: Float) = this.then(Modifier.height((110 * f).dp))
