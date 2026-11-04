package com.example.ui.designer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.CurrencyHelper
import com.example.domain.engine.DesignEditor
import com.example.domain.engine.GeometryEngine
import com.example.domain.model.*
import kotlin.math.roundToInt

/** Short studio label + glyph for every opening type. */
data class OpeningPreset(
  val type: OpeningType,
  val shortTitle: String,
  val icon: ImageVector,
  val category: OpeningCategory
)

enum class OpeningCategory(val titleFa: String) { FIXED("ثابت"), CASEMENT("لولایی"), TILT("شیب‌دار / کلنگی"), SLIDING("کشویی") }

val OpeningPresets: List<OpeningPreset> = listOf(
  OpeningPreset(OpeningType.FIXED, "ثابت", Icons.Default.CropSquare, OpeningCategory.FIXED),
  OpeningPreset(OpeningType.CASEMENT_LEFT, "چپ‌باز", Icons.Default.TurnLeft, OpeningCategory.CASEMENT),
  OpeningPreset(OpeningType.CASEMENT_RIGHT, "راست‌باز", Icons.Default.TurnRight, OpeningCategory.CASEMENT),
  OpeningPreset(OpeningType.DOUBLE_CASEMENT, "دولنگه", Icons.Default.MeetingRoom, OpeningCategory.CASEMENT),
  OpeningPreset(OpeningType.TOP_HUNG, "سایه‌بان", Icons.Default.VerticalAlignTop, OpeningCategory.TILT),
  OpeningPreset(OpeningType.TILT, "کلنگی", Icons.Default.VerticalAlignBottom, OpeningCategory.TILT),
  OpeningPreset(OpeningType.TILT_AND_TURN, "دوحالته", Icons.Default.AutoAwesomeMotion, OpeningCategory.TILT),
  OpeningPreset(OpeningType.SLIDING_LEFT, "کشویی چپ", Icons.Default.ArrowBack, OpeningCategory.SLIDING),
  OpeningPreset(OpeningType.SLIDING_RIGHT, "کشویی راست", Icons.Default.ArrowForward, OpeningCategory.SLIDING)
)

fun presetFor(type: OpeningType): OpeningPreset =
  OpeningPresets.firstOrNull { it.type == type } ?: OpeningPresets.first()

@Composable
fun StudioSectionTitle(text: String, icon: ImageVector? = null) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    if (icon != null) {
      Icon(icon, contentDescription = null, tint = StudioColors.Accent, modifier = Modifier.size(15.dp))
    }
    Text(
      text = text,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
      color = StudioColors.TextMuted
    )
  }
}

@Composable
fun OpeningTypeGrid(
  selected: OpeningType,
  allowed: List<OpeningType>,
  onSelect: (OpeningType) -> Unit,
  modifier: Modifier = Modifier
) {
  val visible = OpeningPresets.filter { it.type in allowed }
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
    visible.chunked(3).forEach { row ->
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        row.forEach { preset ->
          OpeningTile(
            preset = preset,
            isSelected = preset.type == selected,
            onClick = { onSelect(preset.type) },
            modifier = Modifier.weight(1f)
          )
        }
        repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f).height(1.dp)) }
      }
    }
  }
}

@Composable
fun OpeningTile(preset: OpeningPreset, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (isSelected) StudioColors.AccentStrong else StudioColors.SurfaceRaised,
    border = BorderStroke(1.dp, if (isSelected) StudioColors.Accent else StudioColors.Outline),
    modifier = modifier
      .height(56.dp)
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(2.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(
        preset.icon,
        contentDescription = preset.shortTitle,
        tint = if (isSelected) Color.White else StudioColors.TextMuted,
        modifier = Modifier.size(18.dp)
      )
      Text(
        text = preset.shortTitle,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = if (isSelected) Color.White else StudioColors.TextPrimary
      )
    }
  }
}

@Composable
fun ColorSwatchRow(
  colors: List<Pair<String, Long>>,
  selectedHex: Long?,
  onSelect: (Long) -> Unit
) {
  // Swatches scroll horizontally so a long finish list never squeezes the inspector column.
  Row(
    horizontalArrangement = Arrangement.spacedBy(5.dp),
    modifier = Modifier.horizontalScroll(rememberScrollState())
  ) {
    colors.forEach { (_, hex) ->
      val chosen = selectedHex == hex
      Box(
        modifier = Modifier
          .size(30.dp)
          .clip(RoundedCornerShape(7.dp))
          .background(Color(hex))
          .border(if (chosen) 2.5.dp else 1.dp, if (chosen) StudioColors.Accent else StudioColors.OutlineSoft, RoundedCornerShape(7.dp))
          .clickable { onSelect(hex) }
      )
    }
  }
}



/**
 * Full inspector for the currently selected opening: opening type, hinge direction, glazing,
 * glass/ sash finishes, hardware and dimension editing (mm or ratio based divider).
 */
@Composable
fun OpeningInspector(
  design: WindowDesign,
  selectedIndex: Int,
  onDesignChange: (WindowDesign) -> Unit,
  onSelect: (Int) -> Unit,
  onEditPanelWidth: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  val index = selectedIndex.coerceIn(0, (design.panels.size - 1).coerceAtLeast(0))
  val panel = design.panels.getOrNull(index) ?: return
  val bounds = GeometryEngine.computeGeometry(design).panels.firstOrNull { it.panelIndex == index }
  val allowed = MaterialCatalog.recommendedOpenings(design.materialFamily)
  val presets = presetFor(panel.openingType)

  fun update(value: PanelConfig) = onDesignChange(DesignEditor.updatePanel(design, index) { value })

  Column(
    modifier = modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 2.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // --- Header -----------------------------------------------------------------------------
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(StudioColors.SurfaceRaised)
        .padding(horizontal = 10.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(Icons.Default.Dashboard, contentDescription = null, tint = StudioColors.Accent, modifier = Modifier.size(16.dp))
          Text(
            "لنگه ${CurrencyHelper.toPersianDigits(index + 1)}/${CurrencyHelper.toPersianDigits(design.panels.size)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = StudioColors.TextPrimary
          )
        }
        Text(
          text = if (bounds != null)
            "${CurrencyHelper.toPersianDigits(bounds.widthMm.roundToInt())} × ${CurrencyHelper.toPersianDigits(bounds.heightMm.roundToInt())} mm"
          else "—",
          style = MaterialTheme.typography.labelSmall,
          color = StudioColors.TextMuted
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(
          enabled = index > 0,
          onClick = { onSelect(index - 1) },
          modifier = Modifier.size(32.dp)
        ) { Icon(Icons.Default.ChevronLeft, contentDescription = "قسمت قبلی", tint = StudioColors.TextPrimary) }
        IconButton(
          enabled = index < design.panels.size - 1,
          onClick = { onSelect(index + 1) },
          modifier = Modifier.size(32.dp)
        ) { Icon(Icons.Default.ChevronRight, contentDescription = "قسمت بعدی", tint = StudioColors.TextPrimary) }
      }
    }

    // --- Division controls -------------------------------------------------------------------
    StudioSectionTitle("تقسیم‌بندی", Icons.Default.GridGoldenratio)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
      StudioMiniButton("+ عمودی", Icons.Default.VerticalSplit, Modifier.weight(1f)) {
        onDesignChange(DesignEditor.split(design, index, SplitAxis.VERTICAL, 0.5f))
        onSelect(design.panels.size)
      }
      StudioMiniButton("+ افقی", Icons.Default.HorizontalSplit, Modifier.weight(1f)) {
        onDesignChange(DesignEditor.split(design, index, SplitAxis.HORIZONTAL, 0.5f))
        onSelect(design.panels.size)
      }
      StudioMiniButton(
        "حذف",
        Icons.Default.DeleteOutline,
        Modifier.weight(1f),
        enabled = design.panels.size > 1,
        tint = StudioColors.Danger
      ) {
        onDesignChange(DesignEditor.remove(design, index))
        onSelect((index - 1).coerceAtLeast(0))
      }
    }

    val parent = DesignEditor.parent(design, index)
    val available = DesignEditor.parentAvailableMm(design, index)
    if (parent != null && available != null && available >= 320f) {
      val currentMm = (available * parent.fraction).roundToInt()
      var text by remember(design, index, currentMm) { mutableStateOf(currentMm.toString()) }
      var sliderValue by remember(parent.fraction) { mutableFloatStateOf(parent.fraction) }
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            if (parent.axis == SplitAxis.VERTICAL) "عرض این لنگه (mm)" else "ارتفاع این لنگه (mm)",
            style = MaterialTheme.typography.labelSmall,
            color = StudioColors.TextMuted
          )
          Text(
            "${CurrencyHelper.toPersianDigits((sliderValue * 100).roundToInt())}٪",
            style = MaterialTheme.typography.labelSmall,
            color = StudioColors.Accent
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          OutlinedTextField(
            value = text,
            onValueChange = { text = it.filter(Char::isDigit).take(5) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(104.dp),
            textStyle = MaterialTheme.typography.bodyMedium
          )
          val parsed = text.toIntOrNull()
          Button(
            onClick = {
              if (parsed != null) onDesignChange(DesignEditor.setPanelWidthMm(design, index, parsed))
            },
            enabled = parsed != null && parsed in 160..(available.roundToInt() - 160),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
          ) { Text("اعمال", style = MaterialTheme.typography.labelMedium) }
          TextButton(onClick = {
            onDesignChange(DesignEditor.resizeParent(design, index, 0.5f))
          }) { Text("مساوی", style = MaterialTheme.typography.labelMedium) }
        }
        Slider(
          value = sliderValue,
          onValueChange = { sliderValue = it },
          onValueChangeFinished = {
            onDesignChange(DesignEditor.resizeParent(design, index, sliderValue))
            text = (available * sliderValue).roundToInt().toString()
          },
          valueRange = 0.05f..0.95f
        )
        Text(
          "حداقل ۱۶۰ mm • فضای قابل تقسیم ${CurrencyHelper.toPersianDigits(available.roundToInt())} mm",
          style = MaterialTheme.typography.labelSmall,
          color = StudioColors.TextMuted
        )
      }
    }

    if (design.panels.size > 1) {
      StudioSectionTitle("تقسیم مساوی کل سازه", Icons.Default.GridView)
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(2, 3, 4, 5).forEach { n ->
          OutlinedButton(
            onClick = { onDesignChange(DesignEditor.equalizeDivisions(design, n)) },
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.weight(1f)
          ) { Text(CurrencyHelper.toPersianDigits(n), style = MaterialTheme.typography.labelMedium) }
        }
      }
    }

    HorizontalDivider(color = StudioColors.Outline)

    // --- Opening type -------------------------------------------------------------------------
    StudioSectionTitle("نوع بازشو (${MaterialCatalog.shortFamilyTitle(design.materialFamily)})", Icons.Default.DoorFront)
    OpeningTypeGrid(
      selected = panel.openingType,
      allowed = allowed,
      onSelect = { type ->
        update(
          panel.copy(
            openingType = type,
            openingDirection = if (type == OpeningType.SLIDING_LEFT || type == OpeningType.SLIDING_RIGHT)
              OpeningDirection.INWARD else panel.openingDirection
          )
        )
      }
    )
    if (panel.openingType !in allowed) {
      Text(
        "این بازشو برای ${MaterialCatalog.familyTitle(design.materialFamily)} در فهرست پیشنهادی نیست؛ یراق‌آلات را بررسی کنید.",
        style = MaterialTheme.typography.labelSmall,
        color = StudioColors.Warning
      )
    }

    if (panel.openingType != OpeningType.FIXED &&
      panel.openingType != OpeningType.SLIDING_LEFT &&
      panel.openingType != OpeningType.SLIDING_RIGHT
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        OpeningDirection.entries.forEach { dir ->
          val selected = panel.openingDirection == dir
          FilterChip(
            selected = selected,
            onClick = { update(panel.copy(openingDirection = dir)) },
            label = { Text(if (dir == OpeningDirection.INWARD) "بازشو به داخل" else "بازشو به خارج", style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.weight(1f)
          )
        }
      }
    }

    HorizontalDivider(color = StudioColors.Outline)

    // --- Glazing ------------------------------------------------------------------------------
    StudioSectionTitle("شیشه و لعاب", Icons.Default.Window)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
      MaterialCatalog.glazings(design.materialFamily).forEach { g ->
        FilterChip(
          selected = panel.glazingType == g,
          onClick = { update(panel.copy(glazingType = g)) },
          label = { Text(g.titlePersian.substringBefore(" ("), style = MaterialTheme.typography.labelSmall) }
        )
      }
    }
    Text(
      "تینت شیشه بیرونی",
      style = MaterialTheme.typography.labelSmall,
      color = StudioColors.TextMuted
    )
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
      GlassTint.entries.forEach { tint ->
        val chosen = panel.glassTint == tint
        Box(
          modifier = Modifier
            .weight(1f)
            .height(30.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color(tint.colorHex).copy(alpha = 0.75f))
            .border(
              if (chosen) 2.5.dp else 1.dp,
              if (chosen) StudioColors.Accent else StudioColors.OutlineSoft,
              RoundedCornerShape(7.dp)
            )
            .clickable {
              onDesignChange(DesignEditor.updatePanel(design, index) { it.copy(glassTint = tint) })
            }
        )
      }
    }
    Text(
      text = panel.glassTint.titlePersian,
      style = MaterialTheme.typography.labelSmall,
      color = StudioColors.TextPrimary
    )

    HorizontalDivider(color = StudioColors.Outline)

    // --- Frame / sash finish ------------------------------------------------------------------
    StudioSectionTitle("رنگ لنگهٔ این قسمت", Icons.Default.FormatPaint)
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
      FrameFinish.entries.forEach { finish ->
        val chosen = (panel.sashFinish ?: design.sashFinish) == finish
        Box(
          modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color(finish.colorHex))
            .border(
              if (chosen) 2.5.dp else 1.dp,
              if (chosen) StudioColors.Accent else StudioColors.OutlineSoft,
              RoundedCornerShape(7.dp)
            )
            .clickable { update(panel.copy(sashFinish = finish)) }
        )
      }
    }

    // --- Hardware -----------------------------------------------------------------------------
    if (panel.openingType != OpeningType.FIXED) {
      HorizontalDivider(color = StudioColors.Outline)
      StudioSectionTitle("یراق و دستگیره", Icons.Default.Build)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("نمایش دستگیره", style = MaterialTheme.typography.bodySmall, color = StudioColors.TextPrimary)
        Switch(checked = panel.hasHandle, onCheckedChange = { update(panel.copy(hasHandle = it)) }, modifier = Modifier.height(26.dp))
      }
      if (panel.hasHandle) {
        var handleModel by remember(panel.handleModel) { mutableStateOf(panel.handleModel) }
        OutlinedTextField(
          value = handleModel,
          onValueChange = { handleModel = it },
          label = { Text("مدل دستگیره", style = MaterialTheme.typography.labelSmall) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          textStyle = MaterialTheme.typography.bodySmall
        )
        var sliderX by remember(panel.handleX) { mutableFloatStateOf(panel.handleX ?: if (presets.category == OpeningCategory.SLIDING) 0.08f else 0.5f) }
        var sliderY by remember(panel.handleY) { mutableFloatStateOf(panel.handleY) }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("موقعیت افقی/عمودی", style = MaterialTheme.typography.labelSmall, color = StudioColors.TextMuted)
          TextButton(onClick = {
            sliderX = 0.5f; sliderY = 0.5f
            update(panel.copy(handleX = null, handleY = 0.5f))
          }) { Text("پیش‌فرض", style = MaterialTheme.typography.labelSmall) }
        }
        Slider(
          value = sliderX,
          onValueChange = { sliderX = it },
          onValueChangeFinished = { update(panel.copy(handleX = sliderX)) },
          valueRange = 0.03f..0.97f
        )
        Slider(
          value = sliderY,
          onValueChange = { sliderY = it },
          onValueChangeFinished = { update(panel.copy(handleY = sliderY)) },
          valueRange = 0.03f..0.97f
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("نقاط قفل چندنقطه‌ای", style = MaterialTheme.typography.labelSmall, color = StudioColors.TextPrimary)
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(1, 2, 3).forEach { n ->
              FilterChip(
                selected = panel.lockPoints == n,
                onClick = { update(panel.copy(lockPoints = n)) },
                label = { Text(CurrencyHelper.toPersianDigits(n), style = MaterialTheme.typography.labelSmall) }
              )
            }
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
          HandleColor.entries.forEach { hc ->
            FilterChip(
              selected = design.handleColor == hc,
              onClick = { onDesignChange(design.copy(handleColor = hc)) },
              label = { Text(hc.titlePersian.substringBefore(" ("), style = MaterialTheme.typography.labelSmall) }
            )
          }
        }
      }
    }

    // --- Per-panel dimension shortcut ---------------------------------------------------------
    OutlinedButton(
      onClick = { onEditPanelWidth(index) },
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(10.dp)
    ) {
      Icon(Icons.Default.Straighten, contentDescription = null, modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("ویرایش ابعاد با دیالوگ دقیق", style = MaterialTheme.typography.labelMedium)
    }
  }
}

@Composable
fun StudioMiniButton(
  label: String,
  icon: ImageVector,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  tint: Color = StudioColors.TextPrimary,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(9.dp),
    color = StudioColors.SurfaceRaised,
    border = BorderStroke(1.dp, StudioColors.Outline),
    modifier = modifier
      .height(40.dp)
      .clip(RoundedCornerShape(9.dp))
      .clickable(enabled = enabled, onClick = onClick)
  ) {
    Row(
      modifier = Modifier.fillMaxSize(),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        icon,
        contentDescription = null,
        tint = if (enabled) tint else StudioColors.TextMuted.copy(alpha = 0.4f),
        modifier = Modifier.size(16.dp)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = if (enabled) tint else StudioColors.TextMuted.copy(alpha = 0.4f)
      )
    }
  }
}
