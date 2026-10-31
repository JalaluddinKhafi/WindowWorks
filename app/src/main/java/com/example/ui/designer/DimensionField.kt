package com.example.ui.designer

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

/** Keep incomplete input local so replacing a dimension never applies a partial number. */
@Composable
fun DimensionField(value: Int, label: String, range: IntRange, modifier: Modifier = Modifier, onCommit: (Int) -> Unit) {
  var text by remember(value) { mutableStateOf(value.toString()) }
  var wasFocused by remember { mutableStateOf(false) }
  val focus = LocalFocusManager.current
  val parsed = text.toIntOrNull()
  val valid = parsed != null && parsed in range
  OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text(label) }, singleLine = true,
    isError = !valid,
    supportingText = if (!valid) ({ Text("${range.first}–${range.last} mm") }) else null,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
    keyboardActions = KeyboardActions(onDone = { if (valid) { onCommit(parsed!!); focus.clearFocus() } }),
    modifier = modifier.onFocusChanged {
      if (wasFocused && !it.isFocused && valid) onCommit(parsed!!)
      wasFocused = it.isFocused
    })
}
