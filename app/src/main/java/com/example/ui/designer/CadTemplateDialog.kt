package com.example.ui.designer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.CurrencyHelper
import com.example.core.LocalAppLanguage
import com.example.core.Localization
import com.example.domain.engine.CadTemplate
import com.example.domain.engine.CadTemplateCategory
import com.example.domain.engine.CadTemplates
import com.example.domain.model.WindowShape

@Composable
fun CadTemplateDialog(
  onDismiss: () -> Unit,
  onSelectTemplate: (CadTemplate) -> Unit
) {
  val lang = LocalAppLanguage.current
  var selectedCategory by remember { mutableStateOf(CadTemplateCategory.ALL) }
  var selectedTemplate by remember { mutableStateOf(CadTemplates.templates.first()) }
  var searchQuery by remember { mutableStateOf("") }
  var showSearchField by remember { mutableStateOf(false) }

  val filteredTemplates = remember(selectedCategory, searchQuery) {
    var list = if (selectedCategory == CadTemplateCategory.ALL) {
      CadTemplates.templates
    } else {
      CadTemplates.templates.filter { it.category == selectedCategory }
    }
    if (searchQuery.isNotBlank()) {
      list = list.filter {
        it.id.contains(searchQuery, ignoreCase = true) ||
            Localization.get(it.nameKey, lang).contains(searchQuery, ignoreCase = true)
      }
    }
    list
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = Color(0xFF0F172A),
      tonalElevation = 12.dp,
      modifier = Modifier
        .fillMaxWidth(0.96f)
        .fillMaxHeight(0.92f)
        .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Header (Matching Reference Bottom-Left)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B))
            .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFF0284C7),
              modifier = Modifier.size(36.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.DashboardCustomize, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
              }
            }
            Text(
              text = "قالب‌های آماده پنجره و درب",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            if (showSearchField) {
              OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("جستجو...", style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                modifier = Modifier
                  .width(180.dp)
                  .height(46.dp)
              )
            }
            IconButton(onClick = { showSearchField = !showSearchField }) {
              Icon(Icons.Default.Search, contentDescription = "جستجو", tint = Color.White)
            }
            IconButton(onClick = onDismiss) {
              Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
            }
          }
        }

        // 3-Pane Body: Left Category Sidebar | Center Grid | Right Detail Pane
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        ) {
          // 1. Left Category Sidebar
          Column(
            modifier = Modifier
              .width(180.dp)
              .fillMaxHeight()
              .background(Color(0xFF0B132B))
              .padding(8.dp)
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            CategorySidebarItem(
              title = "همه قالب‌ها",
              icon = Icons.Default.AllInclusive,
              isSelected = selectedCategory == CadTemplateCategory.ALL,
              onClick = { selectedCategory = CadTemplateCategory.ALL }
            )
            CategorySidebarItem(
              title = "پنجره ساده",
              icon = Icons.Default.CropSquare,
              isSelected = selectedCategory == CadTemplateCategory.FIXED,
              onClick = { selectedCategory = CadTemplateCategory.FIXED }
            )
            CategorySidebarItem(
              title = "پنجره کشویی",
              icon = Icons.Default.DoorSliding,
              isSelected = selectedCategory == CadTemplateCategory.SLIDING,
              onClick = { selectedCategory = CadTemplateCategory.SLIDING }
            )
            CategorySidebarItem(
              title = "پنجره لولایی",
              icon = Icons.Default.MeetingRoom,
              isSelected = selectedCategory == CadTemplateCategory.CASEMENT,
              onClick = { selectedCategory = CadTemplateCategory.CASEMENT }
            )
            CategorySidebarItem(
              title = "در و پنجره",
              icon = Icons.Default.DoorFront,
              isSelected = selectedCategory == CadTemplateCategory.DOOR,
              onClick = { selectedCategory = CadTemplateCategory.DOOR }
            )
            CategorySidebarItem(
              title = "نمای بزرگ",
              icon = Icons.Default.Window,
              isSelected = selectedCategory == CadTemplateCategory.COMBO,
              onClick = { selectedCategory = CadTemplateCategory.COMBO }
            )
            CategorySidebarItem(
              title = "قوس‌دار",
              icon = Icons.Default.ChangeHistory,
              isSelected = selectedCategory == CadTemplateCategory.ARCHED,
              onClick = { selectedCategory = CadTemplateCategory.ARCHED }
            )
          }

          // 2. Center Grid of Template Cards
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .padding(12.dp)
          ) {
            LazyVerticalGrid(
              columns = GridCells.Adaptive(minSize = 130.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.fillMaxSize()
            ) {
              items(filteredTemplates) { template ->
                val isSelected = selectedTemplate.id == template.id
                TemplateGridCard(
                  template = template,
                  isSelected = isSelected,
                  onClick = { selectedTemplate = template }
                )
              }
            }
          }

          // 3. Right Detail Pane ("جزئیات قالب" - Matching Reference Bottom-Left)
          Surface(
            color = Color(0xFF1E293B),
            tonalElevation = 6.dp,
            modifier = Modifier
              .width(260.dp)
              .fillMaxHeight()
              .border(1.dp, Color(0xFF334155))
          ) {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Text(
                text = "جزئیات قالب",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )

              // Large Technical Preview Box
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F172A),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(180.dp)
                  .border(1.dp, Color(0xFF0284C7), RoundedCornerShape(12.dp))
              ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                  TemplateDrawingCanvas(
                    template = selectedTemplate,
                    modifier = Modifier
                      .fillMaxSize()
                      .padding(12.dp)
                  )
                }
              }

              Text(
                text = Localization.get(selectedTemplate.nameKey, lang),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8)
              )

              // Metrics table
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                  .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("عرض:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                  Text("${CurrencyHelper.toPersianDigits(selectedTemplate.defaultWidthMm)} mm", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("ارتفاع:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                  Text("${CurrencyHelper.toPersianDigits(selectedTemplate.defaultHeightMm)} mm", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("تعداد لنگه:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                  Text("${CurrencyHelper.toPersianDigits(selectedTemplate.panelCount)}", color = Color.White, fontWeight = FontWeight.Bold)
                }
                if (selectedTemplate.hasTransom) {
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("کتیبه بالا:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                    Text("${CurrencyHelper.toPersianDigits(selectedTemplate.defaultTransomHeightMm)} mm", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold)
                  }
                }
              }

              Spacer(modifier = Modifier.weight(1f))

              // Primary Button: + استفاده از قالب
              Button(
                onClick = {
                  onSelectTemplate(selectedTemplate)
                  onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "+ استفاده از قالب",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CategorySidebarItem(
  title: String,
  icon: ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (isSelected) Color(0xFF0284C7) else Color.Transparent,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Icon(icon, contentDescription = null, tint = if (isSelected) Color.White else Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = if (isSelected) Color.White else Color(0xFFCBD5E1)
      )
    }
  }
}

@Composable
private fun TemplateGridCard(
  template: CadTemplate,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .aspectRatio(1f)
      .clip(RoundedCornerShape(12.dp))
      .background(Color(0xFF1E293B))
      .border(
        width = if (isSelected) 2.5f.dp else 1.dp,
        color = if (isSelected) Color(0xFF0284C7) else Color(0xFF334155),
        shape = RoundedCornerShape(12.dp)
      )
      .clickable(onClick = onClick)
      .padding(8.dp)
  ) {
    // Technical CAD preview in center
    TemplateDrawingCanvas(
      template = template,
      modifier = Modifier.fillMaxSize()
    )

    // Selection Badge (blue checkmark)
    if (isSelected) {
      Surface(
        shape = CircleShape,
        color = Color(0xFF0284C7),
        modifier = Modifier
          .align(Alignment.TopEnd)
          .size(22.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
      }
    }
  }
}

@Composable
private fun TemplateDrawingCanvas(
  template: CadTemplate,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val pad = 6f
    val dw = w - (pad * 2)
    val dh = h - (pad * 2)

    val frameColor = Color(0xFF334155)
    val glassColor = Color(0xFF38BDF8).copy(alpha = 0.35f)
    val mullionColor = Color(0xFF64748B)

    // Outer frame
    if (template.category == CadTemplateCategory.ARCHED) {
      val archPath = Path().apply {
        moveTo(pad, pad + dh * 0.35f)
        quadraticTo(pad + dw / 2f, pad, pad + dw, pad + dh * 0.35f)
        lineTo(pad + dw, pad + dh)
        lineTo(pad, pad + dh)
        close()
      }
      drawPath(archPath, glassColor)
      drawPath(archPath, frameColor, style = Stroke(width = 3f))
    } else {
      drawRect(glassColor, Offset(pad, pad), Size(dw, dh))
      drawRect(frameColor, Offset(pad, pad), Size(dw, dh), style = Stroke(width = 3f))
    }

    // Transom if applicable
    if (template.hasTransom) {
      val tH = dh * 0.28f
      drawLine(frameColor, Offset(pad, pad + tH), Offset(pad + dw, pad + tH), strokeWidth = 2.5f)
    }

    // Dividers for multi-panel templates
    val pCount = template.panelCount
    if (pCount > 1) {
      val step = dw / pCount
      for (i in 1 until pCount) {
        val divX = pad + (step * i)
        val startY = if (template.hasTransom) pad + (dh * 0.28f) else pad
        drawLine(mullionColor, Offset(divX, startY), Offset(divX, pad + dh), strokeWidth = 2.5f)
      }
    }

    // Simple opening diagonal dashed lines for visual recognition
    if (template.category == CadTemplateCategory.CASEMENT || template.category == CadTemplateCategory.DOOR) {
      val midY = pad + (dh / 2f)
      drawLine(Color(0xFF0284C7), Offset(pad + dw, midY), Offset(pad, pad), strokeWidth = 1f)
      drawLine(Color(0xFF0284C7), Offset(pad + dw, midY), Offset(pad, pad + dh), strokeWidth = 1f)
    }
  }
}
