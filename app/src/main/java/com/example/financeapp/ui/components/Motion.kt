package com.example.financeapp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import com.example.financeapp.util.LocalAppHaptics

/** Curvas de Material 3 ("emphasized"): arranque rápido y llegada suave. */
val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/**
 * Esqueleto → contenido con "fade through" de Material: el esqueleto se va rápido y el
 * contenido entra con un fundido y una leve subida. Volver a cargar (loading=true) es un fundido corto.
 */
@Composable
fun LoadingContent(
    loading: Boolean,
    modifier: Modifier = Modifier,
    skeleton: @Composable () -> Unit,
    content: @Composable () -> Unit,
) = FadeThrough(loading, modifier) { isLoading -> if (isLoading) skeleton() else content() }

/** Cambio entre estados (cargando / vacío / lista…) con el mismo "fade through". */
@Composable
fun <T> FadeThrough(target: T, modifier: Modifier = Modifier, content: @Composable (T) -> Unit) {
    AnimatedContent(
        targetState = target,
        modifier = modifier,
        contentAlignment = Alignment.TopCenter,
        transitionSpec = {
            (fadeIn(tween(260, delayMillis = 60)) + slideInVertically(tween(380, easing = EmphasizedDecelerate)) { it / 24 })
                .togetherWith(fadeOut(tween(110)))
        },
        label = "fadeThrough",
    ) { content(it) }
}

/**
 * Monto que "cuenta" hasta [target] (desde 0 la primera vez; desde el valor anterior al cambiar
 * de mes). Solo recompone durante la animación (~0,7 s); al volver a la pantalla no repite el conteo.
 */
@Composable
fun animatedAmount(target: Double): Double {
    var from by rememberSaveable { mutableDoubleStateOf(0.0) }
    var to by rememberSaveable { mutableDoubleStateOf(0.0) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(target) {
        if (target == to) return@LaunchedEffect
        from += (to - from) * progress.value // parte desde lo que se ve ahora
        to = target
        progress.snapTo(0f)
        progress.animateTo(1f, tween(700, easing = EmphasizedDecelerate))
    }
    return from + (to - from) * progress.value
}

/**
 * Interacción "expressive": al presionar, el elemento se comprime con un resorte
 * (spring) y da retroalimentación háptica. Colócalo al INICIO de la cadena de
 * modificadores para que la escala envuelva fondo, forma y contenido.
 *
 *   Modifier.pressable(onClick = { ... }).clip(...).background(...).padding(...)
 */
@Composable
fun Modifier.pressable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    pressedScale: Float = 0.95f,
    strongHaptic: Boolean = false,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow),
        label = "pressScale",
    )
    val haptics = LocalAppHaptics.current
    // Física de botón: un roce leve al hundirse (junto con la escala) y el clic al soltar.
    LaunchedEffect(pressed) { if (pressed && enabled) haptics?.pressDown() }
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        // Sin ripple a propósito (el feedback es la escala); el rol sí se anuncia a TalkBack.
        .clickable(
            interactionSource = interaction, indication = null, enabled = enabled,
            role = role, onClickLabel = onClickLabel,
        ) {
            if (strongHaptic) haptics?.click() else haptics?.tick()
            onClick()
        }
}
