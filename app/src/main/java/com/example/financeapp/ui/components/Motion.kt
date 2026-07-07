package com.example.financeapp.ui.components

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
import com.example.financeapp.util.LocalAppHaptics

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
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow),
        label = "pressScale",
    )
    val haptics = LocalAppHaptics.current
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(interactionSource = interaction, indication = null, enabled = enabled) {
            if (strongHaptic) haptics?.medium() else haptics?.selection()
            onClick()
        }
}
