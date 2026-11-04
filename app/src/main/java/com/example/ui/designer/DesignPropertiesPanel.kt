package com.example.ui.designer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.CurrencyHelper
import com.example.domain.engine.DesignEditor
import com.example.domain.engine.GeometryEngine
import com.example.domain.model.*
import kotlin.math.roundToInt

/**
 * Design-level inspector (overall size, shape, transom, frame/sash finishes, mullion grids, hardware)
 * shown next to the opening inspector in the studio's right-side property dock.
 */
@Composable
fun DesignPropertiesPanel(
  design: WindowDesign,
  onDesignChange: (WindowDesign) -> Unit,
  onEditWidth: () -> Unit,
  onEditHeight: () -> Unit,
  modifier: Modifier = Modifier
) {
  val system = MaterialCatalog.find(design.profileSystem) ?: MaterialCatalog.defaultFor(design.materialFamily)
  val geometry = remember(design) { GeometryEngine.computeGeometry(design) }
  val area = (design.widthMm.toLong() * design.heightMm.toLong()) / 1_000_000.0

  Column(
    modifier = modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // --- Identity --------------------------------------------------------------------------
    var nameField by remember(design.id) { mutableStateOf(design.name) }
    OutlinedTextField(
      value = nameField,
      onValueChange = {
        nameField = it
        onDesignChange(design.copy(name = it))
      },
      label = { Text("نام طرح", style = MaterialTheme.typography.labelSmall) },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
      textStyle = MaterialTheme.typography.bodyMedium
    )

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
      ProductType.entries.forEach { type ->
        FilterChip(
          selected = design.productType == type,
          onClick = { onDesignChange(design.copy(productType = type)) },
          label = { Text(type.titlePersian, style = MaterialTheme.typography.labelSmall) },
          modifier = Modifier.weight(1f)
        )
      }
    }

    // --- Overall size ----------------------------------------------------------------------
    StudioSectionTitle("ابعاد کل", Icons.Default.SquareFoot)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
      SizeShortcut("عرض", design.widthMm, Modifier.weight(1f), onEditWidth)
      SizeShortcut("ارتفاع", design.heightMm, Modifier.weight(1f), onEditHeight)
    }
    Text(
      "مساحت ${CurrencyHelper.toPersianDigits(String.format(java.util.Locale.US, "%.2f", area))} m² • " +
        "حد عملی سیستم ${CurrencyHelper.toPersianDigits(system.maxWidthMm)} × ${CurrencyHelper.toPersianDigits(system.maxHeightMm)} mm",
      style = MaterialTheme.typography.labelSmall,
      color = StudioColors.TextMuted
    )
    if (design.widthMm > system.maxWidthMm || design.heightMm > system.maxHeightMm) {
      Text(
        "ابعاد انتخاب‌شده از محدودهٔ عملی «${system.code}» فراتر است؛ «${MaterialCatalog.forFamily(design.materialFamily).last().code}» را بررسی کنید.",
        style = MaterialTheme.typography.labelSmall,
        color = StudioColors.Warning
      )
    }

    HorizontalDivider(color = StudioColors.Outline)

    // --- Shape -----------------------------------------------------------------------------
    StudioSectionTitle("فرم قاب", Icons.Default.Architecture)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
      WindowShape.entries.forEach { shape ->
        val icon = when (shape) {
          WindowShape.RECTANGLE -> Icons.Default.CropSquare
          WindowShape.ARCHED -> Icons.Default.Architecture
          WindowShape.TRIANGLE -> Icons.Default.ChangeHistory
          WindowShape.TRAPEZOIDAL -> Icons.Default.Roofing
        }
        FilterChip(
          selected = design.windowShape == shape,
          onClick = { onDesignChange(DesignEditor.setShape(design, shape, design.archHeightMm)) },
          label = { Text(shape.titlePersian.substringBefore(" ("), style = MaterialTheme.typography.labelSmall) },
          leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp)) }
        )
      }
    }
    if (design.windowShape != WindowShape.RECTANGLE) {
      ExactMmField(
        label = "ارتفاع قوس/شیب (mm)",
        value = design.archHeightMm,
        range = 100..(design.heightMm / 2).coerceAtLeast(120),
        modifier = Modifier.fillMaxWidth()
      ) { onDesignChange(design.copy(archHeightMm = it)) }
    }

    HorizontalDivider(color = StudioColors.Outline)

    // --- Transom (کتیبه) -------------------------------------------------------------------
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("کتیبه سراسری (Transom)", style = MaterialTheme.typography.bodySmall, color = StudioColors.TextPrimary)
      Switch(
        checked = design.hasTransom,
        onCheckedChange = { on ->
          onDesignChange(
            if (on) DesignEditor.setTransomHeightMm(design, design.transomHeightMm.coerceAtMost(design.heightMm - 300))
            else design.copy(hasTransom = false)
          )
        },
        modifier = Modifier.height(26.dp)
      )
    }
    if (design.hasTransom) {
      ExactMmField(
        label = "ارتفاع کتیبه (mm)",
        value = design.transomHeightMm,
        range = 200..(design.heightMm - 300).coerceAtLeast(200),
        modifier = Modifier.fillMaxWidth()
      ) { onDesignChange(DesignEditor.setTransomHeightMm(design, it)) }
    }

    HorizontalDivider(color = StudioColors.Outline)

    // --- Finishes --------------------------------------------------------------------------
    StudioSectionTitle("رنگ پروفیل فریم", Icons.Default.FormatPaint)
    ColorSwatchRow(
      colors = FrameFinish.entries.map { it.titlePersian to it.colorHex },
      selectedHex = design.customColorHex ?: design.frameFinish.colorHex
    ) { hex ->
      val match = FrameFinish.entries.firstOrNull { it.colorHex == hex }
      onDesignChange(design.copy(frameFinish = match ?: design.frameFinish, customColorHex = null))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
      CoatingType.entries.forEach { coating ->
        FilterChip(
          selected = design.coatingType == coating,
          onClick = { onDesignChange(design.copy(coatingType = coating)) },
          label = { Text(coating.titlePersian.substringBefore(" "), style = MaterialTheme.typography.labelSmall) }
        )
      }
    }

    StudioSectionTitle("رنگ لنگه", Icons.Default.FilterFrames)
    ColorSwatchRow(
      colors = FrameFinish.entries.map { it.titlePersian to it.colorHex },
      selectedHex = design.sashFinish.colorHex
    ) { hex ->
      FrameFinish.entries.firstOrNull { it.colorHex == hex }?.let {
        onDesignChange(design.copy(sashFinish = it))
      }
    }

    HorizontalDivider(color = StudioColors.Outline)

    // --- Mullion grids (Georgian) ------------------------------------------------------------
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("شبکه/بازکردن (شبکه فرانسوی)", style = MaterialTheme.typography.bodySmall, color = StudioColors.TextPrimary)
      Switch(
        checked = design.hasGrids,
        onCheckedChange = { onDesignChange(design.copy(hasGrids = it)) },
        modifier = Modifier.height(26.dp)
      )
    }
    if (design.hasGrids) {
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        ExactMmField(
          label = "ردیف",
          value = design.gridRows,
          range = 1..6,
          modifier = Modifier.weight(1f)
        ) { onDesignChange(design.copy(gridRows = it)) }
        ExactMmField(
          label = "ستون",
          value = design.gridCols,
          range = 1..6,
          modifier = Modifier.weight(1f)
        ) { onDesignChange(design.copy(gridCols = it)) }
      }
    }

    HorizontalDivider(color = StudioColors.Outline)

    // --- Door threshold ---------------------------------------------------------------------
    if (design.productType == ProductType.DOOR) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("آستانه کم‌ارتفاع (Low Threshold)", style = MaterialTheme.typography.bodySmall, color = StudioColors.TextPrimary)
        Switch(
          checked = design.hasLowThreshold,
          onCheckedChange = { onDesignChange(design.copy(hasLowThreshold = it)) },
          modifier = Modifier.height(26.dp)
        )
      }
    }

    // --- Validation -----------------------------------------------------------------------
    if (geometry.validationErrors.isNotEmpty() || geometry.validationWarnings.isNotEmpty()) {
      HorizontalDivider(color = StudioColors.Outline)
      StudioSectionTitle("بررسی فنی", Icons.Default.FactCheck)
      geometry.validationErrors.take(3).forEach { error ->
        AlertRow(error, StudioColors.Danger)
      }
      geometry.validationWarnings.take(3).forEach { warning ->
        AlertRow(warning, StudioColors.Warning)
      }
    }
  }
}

@Composable
private fun AlertRow(text: String, color: Color) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(color.copy(alpha = 0.1f))
      .padding(horizontal = 8.dp, vertical = 6.dp),
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Icon(Icons.Default.Info, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
    Text(text, style = MaterialTheme.typography.labelSmall, color = StudioColors.TextPrimary)
  }
}

@Composable
private fun SizeShortcut(label: String, valueMm: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = StudioColors.SurfaceRaised,
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
      Text(label, style = MaterialTheme.typography.labelSmall, color = StudioColors.TextMuted)
      Text(
        "${CurrencyHelper.toPersianDigits(valueMm)} mm",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = StudioColors.TextPrimary
      )
    }
  }
}

/** Exact numeric entry (mm or counts) with an apply button — used for CNC-grade input. */
@Composable
fun ExactMmField(
  label: String,
  value: Int,
  range: IntRange,
  modifier: Modifier = Modifier,
  onCommit: (Int) -> Unit
) {
  var text by remember(value) { mutableStateOf(value.toString()) }
  val parsed = text.toIntOrNull()
  OutlinedTextField(
    value = text,
    onValueChange = { text = it.filter(Char::isDigit).take(5) },
    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
    singleLine = true,
    isError = parsed == null || parsed !in range,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    trailingIcon = {
      IconButton(onClick = { if (parsed != null && parsed in range) onCommit(parsed) }) {
        Icon(Icons.Default.Check, contentDescription = "اعمال", tint = StudioColors.Success)
      }
    },
    modifier = modifier,
    textStyle = MaterialTheme.typography.bodySmall
  )
}
