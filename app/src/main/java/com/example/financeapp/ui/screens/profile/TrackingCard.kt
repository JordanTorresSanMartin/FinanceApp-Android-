package com.example.financeapp.ui.screens.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.financeapp.ui.components.RoundIconButton
import com.example.financeapp.util.LocalAppHaptics
import com.example.financeapp.util.groupThousands
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-CL"))

/**
 * "Llevar la cuenta desde…": mes de inicio + saldo con el que partes. El saldo total y el % de
 * ahorro del inicio ignoran los meses anteriores (los que quedaron sin anotar).
 */
@Composable
internal fun TrackingCard(start: YearMonth?, initial: Long, onSave: (YearMonth?, Long) -> Unit) {
    val haptics = LocalAppHaptics.current
    // Sugerencia: a fin de mes, partir el mes siguiente (ej. 24 nov → diciembre).
    val suggested = YearMonth.now().let { if (LocalDate.now().dayOfMonth >= 20) it.plusMonths(1) else it }
    var month by rememberSaveable { mutableStateOf((start ?: suggested).toString()) }
    var amount by rememberSaveable { mutableStateOf(if (initial != 0L) initial.toString() else "") }
    var saved by rememberSaveable { mutableStateOf(false) }
    val ym = YearMonth.parse(month)

    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Flag, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Llevar la cuenta desde", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (start == null) "Empieza de cero: el saldo y el ahorro no arrastran los meses que no anotaste."
                    else "Saldo y ahorro cuentan desde ${start.format(monthFmt)}.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Icons.Filled.ChevronLeft, { month = ym.minusMonths(1).toString(); saved = false }, "Mes anterior", size = 36.dp)
            Text(
                ym.format(monthFmt).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            RoundIconButton(Icons.Filled.ChevronRight, { month = ym.plusMonths(1).toString(); saved = false }, "Mes siguiente", size = 36.dp)
        }
        OutlinedTextField(
            value = groupThousands(amount),
            onValueChange = { v -> amount = v.filter { it.isDigit() || it == '-' }.take(12); saved = false },
            label = { Text("Saldo con que partes (lo que tienes en tus cuentas)") },
            prefix = { Text("$") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedVisibility(saved, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.weight(1f)) {
                Text("Guardado ✓", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            if (!saved) Spacer(Modifier.weight(1f))
            if (start != null) {
                TextButton(onClick = { haptics?.undo(); onSave(null, 0); amount = ""; saved = true }) { Text("Quitar") }
            }
            FilledTonalButton(onClick = {
                haptics?.success()
                onSave(ym, amount.toLongOrNull() ?: 0L)
                saved = true
            }) { Text("Guardar", fontWeight = FontWeight.SemiBold) }
        }
    }
}
