package com.example.ui.designer

import androidx.compose.runtime.*
import com.example.domain.model.WindowDesign
import kotlin.reflect.KProperty

class DesignHistory(initial: WindowDesign) {
  var current by mutableStateOf(initial)
    private set
  private val past = mutableStateListOf<WindowDesign>()
  private val future = mutableStateListOf<WindowDesign>()
  val canUndo get() = past.isNotEmpty()
  val canRedo get() = future.isNotEmpty()
  operator fun getValue(owner: Any?, property: KProperty<*>) = current
  operator fun setValue(owner: Any?, property: KProperty<*>, value: WindowDesign) = commit(value)
  fun commit(value: WindowDesign) {
    if (value == current) return
    past.add(current)
    if (past.size > 100) past.removeAt(0)
    future.clear()
    current = value
  }
  fun undo() { if (canUndo) { future.add(current); current = past.removeAt(past.lastIndex) } }
  fun redo() { if (canRedo) { past.add(current); current = future.removeAt(future.lastIndex) } }
}
