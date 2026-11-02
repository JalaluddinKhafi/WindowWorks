package com.example.ui.designer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.LocalAppLanguage
import com.example.core.Localization

sealed class DimensionEditTarget {
  data class OverallWidth(val currentMm: Int) : DimensionEditTarget()
  data class OverallHeight(val currentMm: Int) : DimensionEditTarget()
  data class TransomHeight(val currentMm: Int) : DimensionEditTarget()
  data class PanelWidth(val panelIndex: Int, val currentMm: Int) : DimensionEditTarget()
}

@Composable
fun DimensionEditDialog(
  target: DimensionEditTarget,
  allowedRange: IntRange,
  onDismiss: () -> Unit,
  onApply: (Int) -> Unit
) {
  val lang = LocalAppLanguage.current
  val currentVal = when (target) {
    is DimensionEditTarget.OverallWidth -> target.currentMm
    is DimensionEditTarget.OverallHeight -> target.currentMm
    is DimensionEditTarget.TransomHeight -> target.currentMm
    is DimensionEditTarget.PanelWidth -> target.currentMm
  }

  var textVal by remember(currentVal) { mutableStateOf(currentVal.toString()) }
  val parsed = textVal.toIntOrNull()
  val isValid = parsed != null && parsed in allowedRange

  val title = when (target) {
    is DimensionEditTarget.OverallWidth -> "${Localization.get("width", lang)} (W)"
    is DimensionEditTarget.OverallHeight -> "${Localization.get("height", lang)} (H)"
    is DimensionEditTarget.TransomHeight -> Localization.get("transom_height", lang)
    is DimensionEditTarget.PanelWidth -> "${Localization.get("panel", lang)} ${target.panelIndex + 1} - ${Localization.get("width", lang)}"
  }

  fun stepValue(delta: Int) {
    val current = parsed ?: currentVal
    val next = (current + delta).coerceIn(allowedRange.first, allowedRange.last)
    textVal = next.toString()
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(Icons.Default.Straighten, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Text(
          text = Localization.get("enter_mm", lang),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
          value = textVal,
          onValueChange = { textVal = it },
          singleLine = true,
          isError = !isValid,
          supportingText = {
            if (!isValid) {
              Text(
                "${allowedRange.first} – ${allowedRange.last} mm",
                color = MaterialTheme.colorScheme.error
              )
            } else {
              Text("${allowedRange.first} – ${allowedRange.last} mm")
            }
          },
          trailingIcon = { Text("mm", modifier = Modifier.padding(end = 12.dp)) },
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
          ),
          keyboardActions = KeyboardActions(
            onDone = {
              if (isValid && parsed != null) {
                onApply(parsed)
                onDismiss()
              }
            }
          ),
          modifier = Modifier.fillMaxWidth()
        )

        // Quick Stepper Buttons (+/- 10mm, +/- 50mm, +/- 100mm)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "تنظیم سریع میلی‌متری:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilledTonalButton(
              onClick = { stepValue(-100) },
              modifier = Modifier.weight(1f),
              contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) { Text("-100") }
            FilledTonalButton(
              onClick = { stepValue(-50) },
              modifier = Modifier.weight(1f),
              contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) { Text("-50") }
            FilledTonalButton(
              onClick = { stepValue(-10) },
              modifier = Modifier.weight(1f),
              contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) { Text("-10") }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilledTonalButton(
              onClick = { stepValue(+10) },
              modifier = Modifier.weight(1f),
              contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) { Text("+10") }
            FilledTonalButton(
              onClick = { stepValue(+50) },
              modifier = Modifier.weight(1f),
              contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) { Text("+50") }
            FilledTonalButton(
              onClick = { stepValue(+100) },
              modifier = Modifier.weight(1f),
              contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) { Text("+100") }
          }
        }
      }
    },
    confirmButton = {
      Button(
        enabled = isValid && parsed != null,
        onClick = {
          if (isValid && parsed != null) {
            onApply(parsed)
            onDismiss()
          }
        }
      ) {
        Text(Localization.get("apply", lang))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(Localization.get("cancel", lang))
      }
    }
  )
}
