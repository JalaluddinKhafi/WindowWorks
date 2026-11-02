package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.AppLanguage
import com.example.core.CurrencyHelper
import com.example.core.LocalAppLanguage
import com.example.core.Localization
import com.example.domain.engine.*
import com.example.domain.model.*
import com.example.ui.designer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode

enum class SpecsTab(val titleFa: String) {
  SPECS("مشخصات"),
  PROFILE_CUTS("برش پروفیل"),
  GLASS_CUTS("برش شیشه"),
  HARDWARE("یراق‌آلات"),
  REPORT("گزارش")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WindowDesignerScreen(
  initialDesign: WindowDesign,
  defaultMarkupPercent: BigDecimal = BigDecimal("25.00"),
  exchangeRateUsdToAfn: BigDecimal = BigDecimal("72.50"),
  canSave: Boolean = true,
  onSaveDesign: (WindowDesign) -> Unit,
  onNavigateToCutting: (WindowDesign) -> Unit,
  onNavigateToQuotation: (WindowDesign) -> Unit,
  onNavigateToManufacturingInstructions: (WindowDesign) -> Unit,
  onSelectLanguage: ((AppLanguage) -> Unit)? = null,
  onBack: () -> Unit
) {
  val lang = LocalAppLanguage.current
  val history = remember(initialDesign.id) { DesignHistory(initialDesign) }
  var design by history

  var activeCadTool by remember { mutableStateOf(CadToolType.SELECT) }
  var currentModeTab by remember { mutableIntStateOf(0) } // 0 = طراحی (2D), 1 = 3D, 2 = محاسبات
  var currentSpecsSubTab by remember { mutableStateOf(SpecsTab.SPECS) }

  var showGrid by remember { mutableStateOf(true) }
  var showDimensions by remember { mutableStateOf(true) }
  var showTemplateDialog by remember { mutableStateOf(false) }
  var editingDimension by remember { mutableStateOf<DimensionEditTarget?>(null) }
  var selectedPanelIndex by remember { mutableIntStateOf(0) }

  val geometry = remember(design) { GeometryEngine.computeGeometry(design) }
  val mfgResult = remember(design) { ManufacturingCalculator.calculate(design) }
  val estimate = remember(design) { CostCalculator.estimateCost(design) }

  val context = LocalContext.current
  val exportScope = rememberCoroutineScope()
  var pendingSvg by remember { mutableStateOf("") }
  val svgExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/svg+xml")) { uri ->
    if (uri != null) {
      val snapshot = pendingSvg
      exportScope.launch {
        val success = withContext(Dispatchers.IO) {
          runCatching {
            requireNotNull(context.contentResolver.openOutputStream(uri)).bufferedWriter(Charsets.UTF_8).use { it.write(snapshot) }
          }.isSuccess
        }
        Toast.makeText(context, if (success) "نقشه با موفقیت صادر شد" else "خطا در صدور نقشه", Toast.LENGTH_SHORT).show()
      }
    }
  }

  val areaM2 = BigDecimal(design.widthMm.toLong() * design.heightMm.toLong())
    .divide(BigDecimal(1_000_000), 4, RoundingMode.HALF_UP)
  val profileMeters = BigDecimal(mfgResult.totalProfileLengthMm)
    .divide(BigDecimal(1000), 2, RoundingMode.HALF_UP)

  Scaffold(
    bottomBar = {
      // Bottom 3 Primary Tabs (Matching Reference Image: [طراحی] [3D] [محاسبات])
      Surface(
        tonalElevation = 8.dp,
        color = Color(0xFF0F172A),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          PrimaryModeTabButton(
            title = "طراحی",
            icon = Icons.Default.Architecture,
            isSelected = currentModeTab == 0,
            onClick = { currentModeTab = 0 },
            modifier = Modifier.weight(1f)
          )
          PrimaryModeTabButton(
            title = "3D",
            icon = Icons.Default.ViewInAr,
            isSelected = currentModeTab == 1,
            onClick = { currentModeTab = 1 },
            modifier = Modifier.weight(1f)
          )
          PrimaryModeTabButton(
            title = "محاسبات",
            icon = Icons.Default.Calculate,
            isSelected = currentModeTab == 2,
            onClick = { currentModeTab = 2 },
            modifier = Modifier.weight(1f)
          )
        }
      }
    }
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      when (currentModeTab) {
        0 -> {
          // ==========================================
          // MODE 0: 2D CAD DESIGNER (TOP-LEFT OF MOCKUP)
          // ==========================================
          Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar (WindowWorks, Home, Undo, Redo, Zoom In, Zoom Out, Grid, Measure, Close)
            Surface(
              color = Color(0xFF0F172A),
              tonalElevation = 4.dp,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                  // Blue circular home button
                  Surface(
                    shape = CircleShape,
                    color = Color(0xFF0284C7),
                    modifier = Modifier
                      .size(38.dp)
                      .clickable(onClick = onBack)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(Icons.Default.Home, contentDescription = "خانه", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                  }
                  Text(
                    text = "WindowWorks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                }

                // Header Utility Icons
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  IconButton(enabled = history.canUndo, onClick = { history.undo() }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Undo, contentDescription = "بازگشت", tint = if (history.canUndo) Color.White else Color.Gray)
                  }
                  IconButton(enabled = history.canRedo, onClick = { history.redo() }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Redo, contentDescription = "تکرار", tint = if (history.canRedo) Color.White else Color.Gray)
                  }
                  IconButton(onClick = { showTemplateDialog = true }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.DashboardCustomize, contentDescription = "قالب‌ها", tint = Color(0xFF38BDF8))
                  }
                  IconButton(onClick = { showGrid = !showGrid }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.GridOn, contentDescription = "شبکه", tint = if (showGrid) Color(0xFF38BDF8) else Color.Gray)
                  }
                  IconButton(onClick = { showDimensions = !showDimensions }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Straighten, contentDescription = "اندازه‌ها", tint = if (showDimensions) Color(0xFF38BDF8) else Color.Gray)
                  }
                  if (canSave) {
                    IconButton(onClick = { onSaveDesign(design) }, modifier = Modifier.size(34.dp)) {
                      Icon(Icons.Default.Save, contentDescription = "ذخیره", tint = Color(0xFF10B981))
                    }
                  }
                }
              }
            }

            // Main Workspace: Left CAD Tools | Center 2D Canvas | Right Element Settings Inspector
            Row(modifier = Modifier.fillMaxSize()) {
              // 1. Left Vertical CAD Toolbar (انتخاب، خط، مستطیل، دایره، چندضلعی، متن، اندازه)
              Surface(
                color = Color(0xFF0F172A),
                modifier = Modifier
                  .width(68.dp)
                  .fillMaxHeight()
                  .border(1.dp, Color(0xFF1E293B))
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  CadToolSidebarButton(
                    title = "انتخاب",
                    icon = Icons.Default.NearMe,
                    isSelected = activeCadTool == CadToolType.SELECT,
                    onClick = { activeCadTool = CadToolType.SELECT }
                  )
                  CadToolSidebarButton(
                    title = "خط",
                    icon = Icons.Default.Edit,
                    isSelected = activeCadTool == CadToolType.LINE,
                    onClick = {
                      activeCadTool = CadToolType.LINE
                      // Add vertical division into currently selected panel
                      design = DesignEditor.split(design, selectedPanelIndex, SplitAxis.VERTICAL)
                    }
                  )
                  CadToolSidebarButton(
                    title = "مستطیل",
                    icon = Icons.Default.CropSquare,
                    isSelected = activeCadTool == CadToolType.RECTANGLE,
                    onClick = {
                      activeCadTool = CadToolType.RECTANGLE
                      design = DesignEditor.setShape(design, WindowShape.RECTANGLE)
                    }
                  )
                  CadToolSidebarButton(
                    title = "دایره",
                    icon = Icons.Default.ChangeHistory,
                    isSelected = activeCadTool == CadToolType.CIRCLE,
                    onClick = {
                      activeCadTool = CadToolType.CIRCLE
                      design = DesignEditor.setShape(design, WindowShape.ARCHED, 350)
                    }
                  )
                  CadToolSidebarButton(
                    title = "چندضلعی",
                    icon = Icons.Default.Hexagon,
                    isSelected = activeCadTool == CadToolType.POLYGON,
                    onClick = {
                      activeCadTool = CadToolType.POLYGON
                      design = DesignEditor.setShape(design, WindowShape.TRIANGLE, 400)
                    }
                  )
                  CadToolSidebarButton(
                    title = "متن",
                    icon = Icons.Default.TextFields,
                    isSelected = activeCadTool == CadToolType.TEXT,
                    onClick = { activeCadTool = CadToolType.TEXT }
                  )
                  CadToolSidebarButton(
                    title = "اندازه",
                    icon = Icons.Default.Straighten,
                    isSelected = activeCadTool == CadToolType.DIMENSION,
                    onClick = {
                      activeCadTool = CadToolType.DIMENSION
                      editingDimension = DimensionEditTarget.OverallWidth(design.widthMm)
                    }
                  )
                }
              }

              // 2. Center 2D CAD Canvas
              Box(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight()
              ) {
                Elevation2DCanvas(
                  design = design,
                  activeTool = activeCadTool,
                  selectedPanelIndex = selectedPanelIndex,
                  onSelectPanel = { selectedPanelIndex = it },
                  showGrid = showGrid,
                  showDimensions = showDimensions,
                  onPlaceHandle = { idx, x, y ->
                    design = design.copy(
                      panels = design.panels.mapIndexed { i, panel ->
                        if (i == idx) panel.copy(hasHandle = true, handleX = x, handleY = y) else panel
                      }
                    )
                  },
                  onEditWidth = { editingDimension = DimensionEditTarget.OverallWidth(design.widthMm) },
                  onEditHeight = { editingDimension = DimensionEditTarget.OverallHeight(design.heightMm) },
                  onEditTransomHeight = if (design.hasTransom) ({
                    editingDimension = DimensionEditTarget.TransomHeight(design.transomHeightMm)
                  }) else null,
                  onEditPanelWidth = { idx ->
                    val w = geometry.panels.getOrNull(idx)?.widthMm?.toInt() ?: 400
                    editingDimension = DimensionEditTarget.PanelWidth(idx, w)
                  },
                  onMoveDivider = { dividerIdx, newFraction ->
                    design = DesignEditor.resizeParent(design, dividerIdx, newFraction)
                  },
                  modifier = Modifier.fillMaxSize()
                )
              }

              // 3. Right Property Inspector ("تنظیمات المان" - Matching Reference Top-Left)
              Surface(
                color = Color(0xFF0F172A),
                modifier = Modifier
                  .width(220.dp)
                  .fillMaxHeight()
                  .border(1.dp, Color(0xFF1E293B))
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState()),
                  verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                  Text(
                    text = "تنظیمات المان",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )

                  // نوع بازشو (Opening Type)
                  Text("نوع بازشو", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                  val selectedPanel = design.panels.getOrNull(selectedPanelIndex) ?: design.panels.firstOrNull()
                  val activeOpening = selectedPanel?.openingType ?: OpeningType.FIXED
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    OpeningTypeCardButton(
                      title = "ثابت",
                      icon = Icons.Default.CropSquare,
                      isSelected = activeOpening == OpeningType.FIXED,
                      onClick = {
                        design = DesignEditor.updatePanel(design, selectedPanelIndex) { it.copy(openingType = OpeningType.FIXED) }
                      },
                      modifier = Modifier.weight(1f)
                    )
                    OpeningTypeCardButton(
                      title = "لولایی",
                      icon = Icons.Default.MeetingRoom,
                      isSelected = activeOpening == OpeningType.CASEMENT_LEFT || activeOpening == OpeningType.CASEMENT_RIGHT,
                      onClick = {
                        design = DesignEditor.updatePanel(design, selectedPanelIndex) { it.copy(openingType = OpeningType.CASEMENT_LEFT) }
                      },
                      modifier = Modifier.weight(1f)
                    )
                    OpeningTypeCardButton(
                      title = "کشویی",
                      icon = Icons.Default.DoorSliding,
                      isSelected = activeOpening == OpeningType.SLIDING_LEFT || activeOpening == OpeningType.SLIDING_RIGHT,
                      onClick = {
                        design = DesignEditor.updatePanel(design, selectedPanelIndex) { it.copy(openingType = OpeningType.SLIDING_LEFT) }
                      },
                      modifier = Modifier.weight(1f)
                    )
                  }

                  HorizontalDivider(color = Color(0xFF1E293B))

                  // پروفیل (Color Swatches Palette)
                  Text("پروفیل", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                  val colorSwatches = listOf(
                    0xFFF8FAFC, 0xFFE2E8F0, 0xFF94A3B8, 0xFF334155, 0xFF1E293B,
                    0xFF5D4037, 0xFF8D6E63, 0xFFB45309
                  )
                  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (chunk in colorSwatches.chunked(4)) {
                      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        chunk.forEach { hex ->
                          val isChosen = (design.customColorHex ?: design.frameFinish.colorHex) == hex
                          Box(
                            modifier = Modifier
                              .size(34.dp)
                              .clip(RoundedCornerShape(6.dp))
                              .background(Color(hex))
                              .border(
                                width = if (isChosen) 2.5f.dp else 1.dp,
                                color = if (isChosen) Color(0xFF0284C7) else Color(0xFF475569),
                                shape = RoundedCornerShape(6.dp)
                              )
                              .clickable {
                                design = design.copy(customColorHex = hex)
                              }
                          )
                        }
                      }
                    }
                  }

                  HorizontalDivider(color = Color(0xFF1E293B))

                  // نوع شیشه (Glass Tints)
                  Text("نوع شیشه", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                  Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                      GlassTint.CLEAR,
                      GlassTint.REFLECTIVE_BLUE,
                      GlassTint.BRONZE,
                      GlassTint.FROSTED
                    ).forEach { tint ->
                      val isSelected = design.outerGlassTint == tint
                      Box(
                        modifier = Modifier
                          .weight(1f)
                          .height(34.dp)
                          .clip(RoundedCornerShape(6.dp))
                          .background(Color(tint.colorHex).copy(alpha = 0.65f))
                        .border(
                          width = if (isSelected) 2.5f.dp else 1.dp,
                          color = if (isSelected) Color(0xFF0284C7) else Color(0xFF475569),
                          shape = RoundedCornerShape(6.dp)
                        )
                        .clickable {
                          design = design.copy(outerGlassTint = tint)
                        }
                      )
                    }
                  }

                  // Glazing Specification Selector
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(
                        text = "دوجداره Low-E",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                      )
                      Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                  }

                  // Decorative French Georgian Grids Toggle
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text("پنجره فرانسوی (شبکه‌دار):", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Switch(
                      checked = design.hasGrids,
                      onCheckedChange = { design = design.copy(hasGrids = it) }
                    )
                  }
                }
              }
            }
          }
        }

        1 -> {
          // ==========================================
          // MODE 1: 3D REALISTIC ARCHITECTURAL PREVIEW
          // ==========================================
          Parametric3DCanvas(
            design = design,
            onUpdateDesign = { design = it },
            modifier = Modifier.fillMaxSize()
          )
        }

        2 -> {
          // ==========================================
          // MODE 2: SPECIFICATIONS & CALCULATIONS (BOTTOM-RIGHT OF MOCKUP)
          // ==========================================
          Column(
            modifier = Modifier
              .fillMaxSize()
              .background(Color(0xFF0F172A))
          ) {
            // Header Bar
            Surface(
              color = Color(0xFF1E293B),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Icon(Icons.Default.Calculate, contentDescription = null, tint = Color(0xFF0284C7))
                  Text(
                    text = "مشخصات و محاسبات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                }
                IconButton(onClick = { currentModeTab = 0 }) {
                  Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                }
              }
            }

            // Top Sub-Tabs: [مشخصات] [برش پروفیل] [برش شیشه] [یراق‌آلات] [گزارش]
            Surface(
              color = Color(0xFF0F172A),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState())
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                SpecsTab.entries.forEach { subTab ->
                  val isSelected = currentSpecsSubTab == subTab
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B),
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .clickable { currentSpecsSubTab = subTab }
                  ) {
                    Text(
                      text = subTab.titleFa,
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                      color = Color.White,
                      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                  }
                }
              }
            }

            // Main Content Area
            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
            ) {
              when (currentSpecsSubTab) {
                SpecsTab.SPECS -> {
                  // Side-by-side: 2D Diagram | 3D Render | Specs Table (Exact Mockup Layout)
                  Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                  ) {
                    // Left: 2D Elevation diagram
                    Surface(
                      shape = RoundedCornerShape(12.dp),
                      color = Color(0xFFF8FAFC),
                      modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                    ) {
                      Elevation2DCanvas(
                        design = design,
                        showGrid = false,
                        showDimensions = true,
                        modifier = Modifier.fillMaxSize()
                      )
                    }

                    // Center: 3D perspective preview
                    Surface(
                      shape = RoundedCornerShape(12.dp),
                      color = Color(0xFF1E293B),
                      modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                    ) {
                      Parametric3DCanvas(
                        design = design,
                        modifier = Modifier.fillMaxSize()
                      )
                    }

                    // Right: Specifications Data Card (Matching Reference Table)
                    Surface(
                      shape = RoundedCornerShape(12.dp),
                      color = Color(0xFF1E293B),
                      modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .padding(4.dp)
                    ) {
                      Column(
                        modifier = Modifier
                          .fillMaxSize()
                          .padding(14.dp)
                          .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                      ) {
                        SpecDataRow("ابعاد کل", "${CurrencyHelper.toPersianDigits(design.widthMm)} × ${CurrencyHelper.toPersianDigits(design.heightMm)} mm")
                        SpecDataRow("مساحت شیشه", "${CurrencyHelper.toPersianDigits(areaM2.setScale(2, RoundingMode.HALF_UP).toString())} m²")
                        SpecDataRow("طول پروفیل", "${CurrencyHelper.toPersianDigits(profileMeters.toString())} m")
                        SpecDataRow("تعداد قطعات", "${CurrencyHelper.toPersianDigits(mfgResult.profileCuts.size + mfgResult.glassPieces.size)}")
                        SpecDataRow("نوع شیشه", design.outerGlassTint.titlePersian)
                        SpecDataRow("رنگ پروفیل", design.frameFinish.titlePersian.take(15))
                        SpecDataRow("رنگ دستگیره", design.handleColor.titlePersian.take(15))
                        SpecDataRow("قیمت نهایی", CurrencyHelper.formatMoney(estimate.quotationPrice, estimate.currency))
                      }
                    }
                  }
                }

                SpecsTab.PROFILE_CUTS -> {
                  LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    items(mfgResult.profileCuts) { cut ->
                      Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        Row(
                          modifier = Modifier.padding(12.dp),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Column {
                            Text(cut.profileName, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                            Text("برش دو سر: ${cut.angleStart}° / ${cut.angleEnd}°", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                          }
                          Text("${CurrencyHelper.toPersianDigits(cut.lengthMm)} mm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                        }
                      }
                    }
                  }
                }

                SpecsTab.GLASS_CUTS -> {
                  LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    items(mfgResult.glassPieces) { glass ->
                      Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        Row(
                          modifier = Modifier.padding(12.dp),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Column {
                            Text("جام شیشه ${glass.tag}", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                            Text(glass.glazingTitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                          }
                          Text("${CurrencyHelper.toPersianDigits(glass.widthMm)} × ${CurrencyHelper.toPersianDigits(glass.heightMm)} mm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                        }
                      }
                    }
                  }
                }

                SpecsTab.HARDWARE -> {
                  LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    items(estimate.bomItems.filter { it.category == "یراق‌آلات" || it.category == "اتصالات" }) { item ->
                      Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        Row(
                          modifier = Modifier.padding(12.dp),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Text(item.name, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                          Text("${CurrencyHelper.toPersianDigits(item.quantity.toInt())} ${item.unit}", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold)
                        }
                      }
                    }
                  }
                }

                SpecsTab.REPORT -> {
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxSize()
                  ) {
                    Column(
                      modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                      verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                      Text("آنالیز مالی و قیمت تمام‌شده", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                      HorizontalDivider(color = Color(0xFF334155))
                      SpecDataRow("هزینه پروفیل:", CurrencyHelper.formatMoney(estimate.profileCost, estimate.currency))
                      SpecDataRow("هزینه شیشه:", CurrencyHelper.formatMoney(estimate.glassCost, estimate.currency))
                      SpecDataRow("هزینه یراق‌آلات:", CurrencyHelper.formatMoney(estimate.hardwareCost, estimate.currency))
                      SpecDataRow("هزینه گالوانیزه:", CurrencyHelper.formatMoney(estimate.reinforcementCost, estimate.currency))
                      SpecDataRow("دستمزد ساخت و نصب:", CurrencyHelper.formatMoney(estimate.laborCost.add(estimate.installationCost), estimate.currency))
                      HorizontalDivider(color = Color(0xFF334155))
                      SpecDataRow("مجموع کل هزینه:", CurrencyHelper.formatMoney(estimate.totalBaseCost, estimate.currency))
                      SpecDataRow("قیمت پیش‌فاکتور (${estimate.markupPercent}% سود):", CurrencyHelper.formatMoney(estimate.quotationPrice, estimate.currency))
                    }
                  }
                }
              }
            }

            // Bottom Action Bar (Matching Reference Image: [اشتراک‌گذاری] [خروجی PDF] [ذخیره پروژه])
            Surface(
              color = Color(0xFF1E293B),
              tonalElevation = 6.dp,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                // ذخیره پروژه (Save Project - Blue)
                Button(
                  onClick = { onSaveDesign(design) },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("ذخیره پروژه", fontWeight = FontWeight.Bold)
                }

                // خروجی PDF (Export PDF - Green)
                Button(
                  onClick = { onNavigateToManufacturingInstructions(design) },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("خروجی PDF", fontWeight = FontWeight.Bold)
                }

                // اشتراک‌گذاری (Share SVG - Light Blue)
                FilledTonalButton(
                  onClick = {
                    pendingSvg = DesignSvgExporter.export(design)
                    svgExport.launch("window-${design.widthMm}x${design.heightMm}.svg")
                  },
                  colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF38BDF8), contentColor = Color(0xFF0F172A)),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("اشتراک‌گذاری", fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }
  }

  // Pre-designed CAD Templates Dialog
  if (showTemplateDialog) {
    CadTemplateDialog(
      onDismiss = { showTemplateDialog = false },
      onSelectTemplate = { template ->
        design = CadTemplates.applyTemplate(design, template)
        showTemplateDialog = false
      }
    )
  }

  // Direct Dimension Edit Modal Dialog (when tapping dimension lines)
  if (editingDimension != null) {
    val target = editingDimension!!
    val allowedRange = when (target) {
      is DimensionEditTarget.OverallWidth -> 400..12000
      is DimensionEditTarget.OverallHeight -> 400..6000
      is DimensionEditTarget.TransomHeight -> 200..(design.heightMm - 300).coerceAtLeast(200)
      is DimensionEditTarget.PanelWidth -> 160..(design.widthMm - 200).coerceAtLeast(160)
    }

    DimensionEditDialog(
      target = target,
      allowedRange = allowedRange,
      onDismiss = { editingDimension = null },
      onApply = { newValue ->
        design = when (target) {
          is DimensionEditTarget.OverallWidth -> design.copy(widthMm = newValue)
          is DimensionEditTarget.OverallHeight -> design.copy(heightMm = newValue)
          is DimensionEditTarget.TransomHeight -> DesignEditor.setTransomHeightMm(design, newValue)
          is DimensionEditTarget.PanelWidth -> DesignEditor.setPanelWidthMm(design, target.panelIndex, newValue)
        }
        editingDimension = null
      }
    )
  }
}

@Composable
private fun PrimaryModeTabButton(
  title: String,
  icon: ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B),
    modifier = modifier
      .height(44.dp)
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier.fillMaxSize(),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(icon, contentDescription = null, tint = if (isSelected) Color.White else Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = if (isSelected) Color.White else Color(0xFFCBD5E1)
      )
    }
  }
}

@Composable
private fun CadToolSidebarButton(
  title: String,
  icon: ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = if (isSelected) Color(0xFF0284C7) else Color.Transparent,
    modifier = Modifier
      .size(54.dp)
      .clip(RoundedCornerShape(8.dp))
      .clickable(onClick = onClick)
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(icon, contentDescription = title, tint = if (isSelected) Color.White else Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = if (isSelected) Color.White else Color(0xFF94A3B8)
      )
    }
  }
}

@Composable
private fun OpeningTypeCardButton(
  title: String,
  icon: ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B),
    modifier = modifier
      .height(56.dp)
      .clip(RoundedCornerShape(8.dp))
      .clickable(onClick = onClick)
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(icon, contentDescription = null, tint = if (isSelected) Color.White else Color.Gray, modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.height(2.dp))
      Text(text = title, style = MaterialTheme.typography.labelSmall, color = if (isSelected) Color.White else Color(0xFF94A3B8))
    }
  }
}

@Composable
private fun SpecDataRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
      .padding(horizontal = 10.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
    Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
  }
}
