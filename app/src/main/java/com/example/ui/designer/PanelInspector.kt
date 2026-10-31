package com.example.ui.designer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.domain.engine.DesignEditor
import com.example.domain.engine.GeometryEngine
import com.example.domain.model.*
import kotlin.math.roundToInt

@Composable
fun PanelInspector(design: WindowDesign, selectedIndex: Int?, onChange: (WindowDesign) -> Unit,
  onSelect: (Int) -> Unit) {
  val index = selectedIndex?.takeIf { it in design.panels.indices }
  ChoiceRow("نوع محصول", ProductType.entries, design.productType, { it.titlePersian }) {
    onChange(design.copy(productType = it))
  }
  Text("ویرایش بخش‌ها: هر قسمت را در نقشه لمس کنید؛ با تقسیم افقی، کتیبهٔ محلی بسازید.", style = MaterialTheme.typography.bodySmall)
  Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    design.panels.forEachIndexed { i, _ ->
      FilterChip(selected = index == i, onClick = { onSelect(i) }, label = { Text("قسمت ${i + 1}") })
    }
  }
  if (index == null) return
  val panel = design.panels[index]
  fun update(value: PanelConfig) = onChange(design.copy(panels = design.panels.mapIndexed { i, p -> if (i == index) value else p }))
  Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      val bounds = GeometryEngine.computeGeometry(design).panels.firstOrNull { it.panelIndex == index }
      Text("قسمت ${index + 1} • ${bounds?.widthMm?.toInt()} × ${bounds?.heightMm?.toInt()} mm", style = MaterialTheme.typography.titleSmall)
      Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { onChange(DesignEditor.split(design, index, SplitAxis.VERTICAL)) }) { Text("+ تقسیم عمودی") }
        OutlinedButton(onClick = { onChange(DesignEditor.split(design, index, SplitAxis.HORIZONTAL)) }) { Text("+ کتیبه / تقسیم افقی") }
        OutlinedButton(enabled = design.panels.size > 1, onClick = {
          onChange(DesignEditor.remove(design, index)); onSelect((index - 1).coerceAtLeast(0))
        }) { Text("حذف قسمت و ادغام") }
      }
      Text("حذف، فضای قسمت را به بخش همسایه می‌دهد. حداقل اندازه: ۱۶۰ mm؛ حداکثر ۴۸ قسمت.", style = MaterialTheme.typography.labelSmall)
      val parent = DesignEditor.parent(design, index)
      if (parent != null) {
        var fraction by remember(design, index) { mutableFloatStateOf(parent.fraction) }
        Text("موقعیت جداکنندهٔ این بخش: ${(fraction * 100).toInt()}٪", style = MaterialTheme.typography.labelMedium)
        Slider(value = fraction.coerceIn(0.01f, 0.99f), onValueChange = { fraction = it },
          onValueChangeFinished = { onChange(DesignEditor.resizeParent(design, index, fraction)) }, valueRange = 0.01f..0.99f)
        val available = DesignEditor.parentAvailableMm(design, index)
        if (available != null && available >= 320f) {
          DimensionField((available * parent.fraction).roundToInt(),
            if (parent.axis == SplitAxis.VERTICAL) "اندازهٔ بخش اول از چپ (mm)" else "اندازهٔ بخش اول از بالا (mm)",
            160..(available.toInt() - 160), Modifier.fillMaxWidth()) {
              onChange(DesignEditor.resizeParent(design, index, it / available))
            }
        }
      }
      ChoiceRow("نوع بازشو", OpeningType.entries, panel.openingType, { it.titlePersian }) { update(panel.copy(openingType = it)) }
      ChoiceRow("جهت بازشو", OpeningDirection.entries, panel.openingDirection, { it.titlePersian }) { update(panel.copy(openingDirection = it)) }
      ChoiceRow("شیشه", GlazingType.entries, panel.glazingType, { it.titlePersian }) { update(panel.copy(glazingType = it)) }
      ChoiceRow("رنگ شیشهٔ این بخش", GlassTint.entries, panel.glassTint, { it.titlePersian }) { update(panel.copy(glassTint = it)) }
      ChoiceRow("رنگ لنگهٔ این بخش", FrameFinish.entries, panel.sashFinish ?: design.sashFinish, { it.titlePersian }) { update(panel.copy(sashFinish = it)) }
      if (panel.openingType != OpeningType.FIXED) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("دستگیره")
          Switch(checked = panel.hasHandle, onCheckedChange = { update(panel.copy(hasHandle = it)) })
          TextButton(onClick = { update(panel.copy(handleX = null, handleY = 0.5f)) }) { Text("جای پیش‌فرض") }
        }
        if (panel.hasHandle) {
          OutlinedTextField(value = panel.handleModel, onValueChange = { update(panel.copy(handleModel = it)) }, label = { Text("مدل دستگیره") }, modifier = Modifier.fillMaxWidth())
          Text("مکان دستگیره: از چپ / از بالا؛ درصدِ اندازهٔ همین قسمت", style = MaterialTheme.typography.labelSmall)
          val defaultX = when (panel.openingType) { OpeningType.CASEMENT_LEFT -> 0.95f; OpeningType.TOP_HUNG -> 0.5f; else -> 0.05f }
          PositionSlider("افقی", panel.handleX ?: defaultX) { update(panel.copy(handleX = it)) }
          PositionSlider("عمودی", panel.handleY) { update(panel.copy(handleY = it)) }
        }
      }
    }
  }
}

@Composable
private fun PositionSlider(label: String, value: Float, onCommit: (Float) -> Unit) {
  var pending by remember(value) { mutableFloatStateOf(value) }
  Text("$label: ${(pending * 100).toInt()}٪", style = MaterialTheme.typography.labelSmall)
  Slider(value = pending, onValueChange = { pending = it }, onValueChangeFinished = { onCommit(pending) }, valueRange = 0.02f..0.98f)
}

@Composable
private fun <T> ChoiceRow(label: String, values: List<T>, selected: T, title: (T) -> String, onSelect: (T) -> Unit) {
  Text(label, style = MaterialTheme.typography.labelMedium)
  Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    values.forEach { value -> FilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(title(value)) }) }
  }
}
