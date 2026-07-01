package com.example.financeapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.financeapp.data.model.TxType
import com.example.financeapp.ui.theme.FinanceTheme

/**
 * FAB expressive de "registro rápido": al tocarlo se despliega con resorte para
 * elegir Gasto o Ingreso. Motion springy, ícono que rota, y squish al presionar.
 */
@Composable
fun QuickAddFab(
    onSelectType: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 135f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "fabRotate",
    )
    val finance = FinanceTheme.colors

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(spring()) +
                scaleIn(spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.7f) +
                slideInVertically(spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)) { it / 3 },
            exit = fadeOut() + scaleOut(targetScale = 0.7f) + slideOutVertically { it / 3 },
        ) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniAction("Ingreso", finance.income, finance.onIncome, Icons.Filled.ArrowDownward) {
                    expanded = false; onSelectType(TxType.INGRESO)
                }
                MiniAction("Gasto", finance.expense, finance.onExpense, Icons.Filled.ArrowUpward) {
                    expanded = false; onSelectType(TxType.GASTO)
                }
            }
        }

        Box(
            modifier = Modifier
                .pressable(onClick = { expanded = !expanded }, strongHaptic = true)
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = if (expanded) "Cerrar" else "Agregar",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp).rotate(rotation),
            )
        }
    }
}

@Composable
private fun MiniAction(
    label: String,
    container: Color,
    onContainer: Color,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .pressable(onClick = onClick, strongHaptic = true)
            .clip(CircleShape)
            .background(container)
            .padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(label, color = onContainer, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(onContainer.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = onContainer, modifier = Modifier.size(18.dp))
        }
    }
}
