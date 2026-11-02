package com.example.ui.designer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import com.example.core.AppLanguage
import com.example.core.LocalAppLanguage
import com.example.core.Localization
import com.example.domain.engine.DesignEditor
import com.example.domain.model.*

enum class DesignSheetType {
  FRAME,
  MULLION,
  TRANSOM,
  SASH,
  GLASS,
  OPENING,
  DIMENSIONS,
  MATERIALS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextualDesignBottomSheet(
  sheetType: DesignSheetType,
  design: WindowDesign,
  selectedPanelIndex: Int,
  availableProfiles: List<String> = emptyList(),
  onUpdateDesign: (WindowDesign) -> Unit,
  onDismiss: () -> Unit
) {
  val lang = LocalAppLanguage.current
  val selectedPanel = design.panels.getOrNull(selectedPanelIndex) ?: design.panels.firstOrNull()

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    dragHandle = { BottomSheetDefaults.DragHandle() },
    containerColor = MaterialTheme.colorScheme.surface
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Header with title and close
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        val title = when (sheetType) {
          DesignSheetType.FRAME -> "تنظیمات فریم و شکل پنجره (Frame)"
          DesignSheetType.MULLION -> "مولیون و تقسیم‌بندی (Mullion)"
          DesignSheetType.TRANSOM -> "کتیبه و پروفیل افقی (Transom)"
          DesignSheetType.SASH -> "لنگه و نوع بازشو (Sash)"
          DesignSheetType.GLASS -> "نوعیت و رنگ شیشه (Glass)"
          DesignSheetType.OPENING -> "جهت بازشو و یراق‌آلات (Opening)"
          DesignSheetType.DIMENSIONS -> "ابعاد دقیق میلی‌متری (Dimensions)"
          DesignSheetType.MATERIALS -> "سیستم پروفیل و متریال (Materials)"
        }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "بستن")
        }
      }

      when (sheetType) {
        DesignSheetType.FRAME -> FrameSheetContent(design, onUpdateDesign, lang)
        DesignSheetType.MULLION -> MullionSheetContent(design, selectedPanelIndex, onUpdateDesign, lang)
        DesignSheetType.TRANSOM -> TransomSheetContent(design, selectedPanelIndex, onUpdateDesign, lang)
        DesignSheetType.SASH -> SashSheetContent(design, selectedPanelIndex, onUpdateDesign, lang)
        DesignSheetType.GLASS -> GlassSheetContent(design, selectedPanelIndex, onUpdateDesign, lang)
        DesignSheetType.OPENING -> OpeningSheetContent(design, selectedPanelIndex, onUpdateDesign, lang)
        DesignSheetType.DIMENSIONS -> DimensionsSheetContent(design, onUpdateDesign, lang)
        DesignSheetType.MATERIALS -> MaterialsSheetContent(design, availableProfiles, onUpdateDesign, lang)
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
private fun FrameSheetContent(
  design: WindowDesign,
  onUpdate: (WindowDesign) -> Unit,
  lang: AppLanguage
) {
  Text("شکل هندسی پنجره / درب:", style = MaterialTheme.typography.labelMedium)
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    WindowShape.entries.forEach { shape ->
      FilterChip(
        selected = design.windowShape == shape,
        onClick = { onUpdate(DesignEditor.setShape(design, shape)) },
        label = { Text(shape.titlePersian) },
        modifier = Modifier.weight(1f)
      )
    }
  }

  if (design.windowShape != WindowShape.RECTANGLE) {
    Text("ارتفاع قوس / شیب: ${design.archHeightMm} mm", style = MaterialTheme.typography.bodySmall)
    Slider(
      value = design.archHeightMm.toFloat(),
      onValueChange = { onUpdate(design.copy(archHeightMm = it.toInt())) },
      valueRange = 100f..(design.heightMm / 2).coerceAtLeast(150).toFloat()
    )
  }

  HorizontalDivider()
  Text("رنگ و روکش پروفیل فریم:", style = MaterialTheme.typography.labelMedium)
  LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    items(FrameFinish.entries) { finish ->
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .clickable { onUpdate(design.copy(frameFinish = finish)) }
          .padding(4.dp)
      ) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(finish.colorHex))
            .border(
              width = if (design.frameFinish == finish) 3.dp else 1.dp,
              color = if (design.frameFinish == finish) MaterialTheme.colorScheme.primary else Color.Gray,
              shape = CircleShape
            )
        )
        Text(finish.titlePersian.take(12), style = MaterialTheme.typography.labelSmall)
      }
    }
  }
}

@Composable
private fun MullionSheetContent(
  design: WindowDesign,
  panelIndex: Int,
  onUpdate: (WindowDesign) -> Unit,
  lang: AppLanguage
) {
  Text("تقسیم‌بندی عمودی (افزودن مولیون):", style = MaterialTheme.typography.labelMedium)
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Button(
      onClick = { onUpdate(DesignEditor.split(design, panelIndex, SplitAxis.VERTICAL, 0.5f)) },
      modifier = Modifier.weight(1f)
    ) {
      Icon(Icons.Default.ViewColumn, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("تقسیم عمودی")
    }
    OutlinedButton(
      onClick = { onUpdate(DesignEditor.remove(design, panelIndex)) },
      enabled = design.panels.size > 1,
      modifier = Modifier.weight(1f)
    ) {
      Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("حذف این لنگه")
    }
  }

  HorizontalDivider()
  Text("تقسیم مساوی در کل عرض پنجره:", style = MaterialTheme.typography.labelMedium)
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    FilledTonalButton(
      onClick = { onUpdate(DesignEditor.equalizeDivisions(design, 2, SplitAxis.VERTICAL)) },
      modifier = Modifier.weight(1f)
    ) { Text("۲ قسمت مساوی") }
    FilledTonalButton(
      onClick = { onUpdate(DesignEditor.equalizeDivisions(design, 3, SplitAxis.VERTICAL)) },
      modifier = Modifier.weight(1f)
    ) { Text("۳ قسمت مساوی") }
    FilledTonalButton(
      onClick = { onUpdate(DesignEditor.equalizeDivisions(design, 4, SplitAxis.VERTICAL)) },
      modifier = Modifier.weight(1f)
    ) { Text("۴ قسمت مساوی") }
  }
}

@Composable
private fun TransomSheetContent(
  design: WindowDesign,
  panelIndex: Int,
  onUpdate: (WindowDesign) -> Unit,
  lang: AppLanguage
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text("کتیبه سراسری بالای پنجره (Transom):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    Switch(
      checked = design.hasTransom,
      onCheckedChange = { onUpdate(design.copy(hasTransom = it)) }
    )
  }

  if (design.hasTransom) {
    Text("ارتفاع کتیبه: ${design.transomHeightMm} mm", style = MaterialTheme.typography.bodySmall)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      listOf(-50, -10, +10, +50).forEach { delta ->
        FilledTonalButton(
          onClick = { onUpdate(DesignEditor.setTransomHeightMm(design, design.transomHeightMm + delta)) },
          modifier = Modifier.weight(1f)
        ) {
          Text(if (delta > 0) "+$delta" else "$delta")
        }
      }
    }
  }

  HorizontalDivider()
  Text("افزودن تیر افقی (ترانسوم) داخل لنگه جاری:", style = MaterialTheme.typography.labelMedium)
  Button(
    onClick = { onUpdate(DesignEditor.split(design, panelIndex, SplitAxis.HORIZONTAL, 0.5f)) },
    modifier = Modifier.fillMaxWidth()
  ) {
    Icon(Icons.Default.TableRows, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(modifier = Modifier.width(6.dp))
    Text("تقسیم افقی لنگه شماره ${panelIndex + 1}")
  }
}

@Composable
private fun SashSheetContent(
  design: WindowDesign,
  panelIndex: Int,
  onUpdate: (WindowDesign) -> Unit,
  lang: AppLanguage
) {
  val panel = design.panels.getOrNull(panelIndex) ?: return
  Text("نوع بازشوی لنگه ${panelIndex + 1}:", style = MaterialTheme.typography.labelMedium)

  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    OpeningType.entries.forEach { opType ->
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (panel.openingType == opType) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onUpdate(DesignEditor.updatePanel(design, panelIndex) { it.copy(openingType = opType) }) }
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          RadioButton(
            selected = panel.openingType == opType,
            onClick = { onUpdate(DesignEditor.updatePanel(design, panelIndex) { it.copy(openingType = opType) }) }
          )
          Text(opType.titlePersian, style = MaterialTheme.typography.bodyMedium)
        }
      }
    }
  }

  HorizontalDivider()
  Text("رنگ و روکش این لنگه بازشو:", style = MaterialTheme.typography.labelMedium)
  LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    items(FrameFinish.entries) { finish ->
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .clickable { onUpdate(DesignEditor.updatePanel(design, panelIndex) { it.copy(sashFinish = finish) }) }
          .padding(4.dp)
      ) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(finish.colorHex))
            .border(
              width = if (panel.sashFinish == finish) 3.dp else 1.dp,
              color = if (panel.sashFinish == finish) MaterialTheme.colorScheme.primary else Color.Gray,
              shape = CircleShape
            )
        )
        Text(finish.titlePersian.take(10), style = MaterialTheme.typography.labelSmall)
      }
    }
  }
}

@Composable
private fun GlassSheetContent(
  design: WindowDesign,
  panelIndex: Int,
  onUpdate: (WindowDesign) -> Unit,
  lang: AppLanguage
) {
  val panel = design.panels.getOrNull(panelIndex) ?: return
  Text("نوعیت شیشه (جداره):", style = MaterialTheme.typography.labelMedium)
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    GlazingType.entries.forEach { gType ->
      FilterChip(
        selected = panel.glazingType == gType,
        onClick = { onUpdate(DesignEditor.updatePanel(design, panelIndex) { it.copy(glazingType = gType) }) },
        label = { Text(gType.titlePersian.take(12)) },
        modifier = Modifier.weight(1f)
      )
    }
  }

  HorizontalDivider()
  Text("رنگ و نوع شیشه بیرونی:", style = MaterialTheme.typography.labelMedium)
  LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    items(GlassTint.entries) { tint ->
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .clickable {
            onUpdate(
              DesignEditor.updatePanel(design, panelIndex) { it.copy(glassTint = tint) }
                .copy(outerGlassTint = tint)
            )
          }
          .padding(4.dp)
      ) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(tint.colorHex))
            .border(
              width = if (panel.glassTint == tint) 3.dp else 1.dp,
              color = if (panel.glassTint == tint) MaterialTheme.colorScheme.primary else Color.Gray,
              shape = CircleShape
            )
        )
        Text(tint.titlePersian.take(10), style = MaterialTheme.typography.labelSmall)
      }
    }
  }
}

@Composable
private fun OpeningSheetContent(
  design: WindowDesign,
  panelIndex: Int,
  onUpdate: (WindowDesign) -> Unit,
  lang: AppLanguage
) {
  val panel = design.panels.getOrNull(panelIndex) ?: return
  Text("جهت بازشو:", style = MaterialTheme.typography.labelMedium)
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    OpeningDirection.entries.forEach { dir ->
      FilterChip(
        selected = panel.openingDirection == dir,
        onClick = {
          onUpdate(
            DesignEditor.updatePanel(design, panelIndex) { it.copy(openingDirection = dir) }
              .copy(openingDirection = dir)
          )
        },
        label = { Text(dir.titlePersian) },
        modifier = Modifier.weight(1f)
      )
    }
  }

  HorizontalDivider()
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text("دستگیره روی لنگه:", style = MaterialTheme.typography.bodyMedium)
    Switch(
      checked = panel.hasHandle,
      onCheckedChange = { onUpdate(DesignEditor.updatePanel(design, panelIndex) { p -> p.copy(hasHandle = it) }) }
    )
  }

  Text("تعداد زبانه‌های قفل (Lock Points): ${panel.lockPoints}", style = MaterialTheme.typography.bodySmall)
  Slider(
    value = panel.lockPoints.toFloat(),
    onValueChange = { onUpdate(DesignEditor.updatePanel(design, panelIndex) { p -> p.copy(lockPoints = it.toInt()) }) },
    valueRange = 1f..6f,
    steps = 4
  )
}

@Composable
private fun DimensionsSheetContent(
  design: WindowDesign,
  onUpdate: (WindowDesign) -> Unit,
  lang: AppLanguage
) {
  Text("عرض کلی پنجره (Width): ${design.widthMm} mm", style = MaterialTheme.typography.titleSmall)
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    listOf(-100, -50, -10, +10, +50, +100).forEach { delta ->
      FilledTonalButton(
        onClick = { onUpdate(design.copy(widthMm = (design.widthMm + delta).coerceIn(400, 12000))) },
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(2.dp)
      ) {
        Text(if (delta > 0) "+$delta" else "$delta", style = MaterialTheme.typography.labelSmall)
      }
    }
  }

  HorizontalDivider()
  Text("ارتفاع کلی پنجره (Height): ${design.heightMm} mm", style = MaterialTheme.typography.titleSmall)
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    listOf(-100, -50, -10, +10, +50, +100).forEach { delta ->
      FilledTonalButton(
        onClick = { onUpdate(design.copy(heightMm = (design.heightMm + delta).coerceIn(400, 6000))) },
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(2.dp)
      ) {
        Text(if (delta > 0) "+$delta" else "$delta", style = MaterialTheme.typography.labelSmall)
      }
    }
  }
}

@Composable
private fun MaterialsSheetContent(
  design: WindowDesign,
  availableProfiles: List<String>,
  onUpdate: (WindowDesign) -> Unit,
  lang: AppLanguage
) {
  Text("خانواده متریال:", style = MaterialTheme.typography.labelMedium)
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    MaterialFamily.entries.forEach { family ->
      FilterChip(
        selected = design.materialFamily == family,
        onClick = { onUpdate(design.copy(materialFamily = family)) },
        label = { Text(family.titlePersian) },
        modifier = Modifier.weight(1f)
      )
    }
  }

  HorizontalDivider()
  Text("سیستم پروفیل کارگاهی:", style = MaterialTheme.typography.labelMedium)
  val systems = (availableProfiles + listOf("Wintech 6000", "Veka 70", "Akpa Thermal 65", "Sarplast 60")).distinct()
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    systems.forEach { sys ->
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (design.profileSystem == sys) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onUpdate(design.copy(profileSystem = sys)) }
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          RadioButton(
            selected = design.profileSystem == sys,
            onClick = { onUpdate(design.copy(profileSystem = sys)) }
          )
          Text(sys, style = MaterialTheme.typography.bodyMedium)
        }
      }
    }
  }
}
