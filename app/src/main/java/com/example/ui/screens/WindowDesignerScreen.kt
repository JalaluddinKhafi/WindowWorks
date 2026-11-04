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
  var inspectorTab by remember { mutableIntStateOf(0) } // 0 = لنگه، 1 = طرح
  var canvasViewMode by remember { mutableStateOf(CanvasViewMode.BODY) }
  var snapStepMm by remember { mutableIntStateOf(10) }
  var zoomFactor by remember { mutableFloatStateOf(1f) }
  var resetZoom by remember { mutableStateOf<() -> Unit>({}) }

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

  val currentSystem = MaterialCatalog.find(design.profileSystem) ?: MaterialCatalog.defaultFor(design.materialFamily)
  val materialAccent = design.materialFamily.accentColor()

  Scaffold(
    bottomBar = {
      // Bottom 3 Primary Tabs (Matching Reference Image: [طراحی] [3D] [محاسبات])
      Surface(
        tonalElevation = 8.dp,
        color = StudioColors.Surface,
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
            // Header Bar: home, design identity + material badge, history and quick utilities
            Surface(
              color = StudioColors.Surface,
              tonalElevation = 4.dp,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                      shape = CircleShape,
                      color = StudioColors.AccentStrong,
                      modifier = Modifier
                        .size(34.dp)
                        .clickable(onClick = onBack)
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Home, contentDescription = "خانه", tint = Color.White, modifier = Modifier.size(18.dp))
                      }
                    }
                    Column {
                      Text(
                        text = design.name.ifBlank { "طرح بدون نام" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = StudioColors.TextPrimary,
                        maxLines = 1
                      )
                      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(
                          shape = RoundedCornerShape(4.dp),
                          color = materialAccent.copy(alpha = 0.18f)
                        ) {
                          Text(
                            text = MaterialCatalog.shortFamilyTitle(design.materialFamily),
                            style = MaterialTheme.typography.labelSmall,
                            color = materialAccent,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                          )
                        }
                        Text(
                          text = "${currentSystem.code} • ${CurrencyHelper.toPersianDigits(design.widthMm)}×${CurrencyHelper.toPersianDigits(design.heightMm)}",
                          style = MaterialTheme.typography.labelSmall,
                          color = StudioColors.TextMuted,
                          maxLines = 1
                        )
                      }
                    }
                  }

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(enabled = history.canUndo, onClick = { history.undo() }, modifier = Modifier.size(32.dp)) {
                      Icon(
                        Icons.Default.Undo,
                        contentDescription = "بازگشت",
                        tint = if (history.canUndo) StudioColors.TextPrimary else StudioColors.TextMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(19.dp)
                      )
                    }
                    IconButton(enabled = history.canRedo, onClick = { history.redo() }, modifier = Modifier.size(32.dp)) {
                      Icon(
                        Icons.Default.Redo,
                        contentDescription = "تکرار",
                        tint = if (history.canRedo) StudioColors.TextPrimary else StudioColors.TextMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(19.dp)
                      )
                    }
                    IconButton(onClick = { showTemplateDialog = true }, modifier = Modifier.size(32.dp)) {
                      Icon(Icons.Default.DashboardCustomize, contentDescription = "قالب‌های آماده", tint = StudioColors.Accent, modifier = Modifier.size(19.dp))
                    }
                    IconButton(onClick = { showGrid = !showGrid }, modifier = Modifier.size(32.dp)) {
                      Icon(
                        Icons.Default.GridOn,
                        contentDescription = "شبکه",
                        tint = if (showGrid) StudioColors.Accent else StudioColors.TextMuted,
                        modifier = Modifier.size(19.dp)
                      )
                    }
                    IconButton(onClick = { showDimensions = !showDimensions }, modifier = Modifier.size(32.dp)) {
                      Icon(
                        Icons.Default.Straighten,
                        contentDescription = "اندازه‌ها",
                        tint = if (showDimensions) StudioColors.Accent else StudioColors.TextMuted,
                        modifier = Modifier.size(19.dp)
                      )
                    }
                    if (canSave) {
                      IconButton(onClick = { onSaveDesign(design) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Save, contentDescription = "ذخیره", tint = StudioColors.Success, modifier = Modifier.size(19.dp))
                      }
                    }
                  }
                }

                // Material selection is always visible: every design is either PVC or aluminum.
                MaterialSelectionBar(
                  design = design,
                  onDesignChange = { design = it },
                  compact = true
                )
              }
            }

            // Main Workspace: Left tool rail | Center 2D canvas | Right property dock
            Row(modifier = Modifier.fillMaxSize()) {
              // 1. Left vertical tool rail: navigation + structural editing tools
              Surface(
                color = StudioColors.Surface,
                modifier = Modifier
                  .width(64.dp)
                  .fillMaxHeight()
                  .border(1.dp, StudioColors.Outline)
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 6.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  CadToolSidebarButton(
                    title = "انتخاب",
                    icon = Icons.Default.NearMe,
                    isSelected = activeCadTool == CadToolType.SELECT,
                    onClick = { activeCadTool = CadToolType.SELECT }
                  )
                  CadToolSidebarButton(
                    title = "تقسیم |",
                    icon = Icons.Default.VerticalSplit,
                    isSelected = activeCadTool == CadToolType.LINE,
                    onClick = {
                      activeCadTool = CadToolType.LINE
                      design = DesignEditor.split(design, selectedPanelIndex, SplitAxis.VERTICAL)
                    }
                  )
                  CadToolSidebarButton(
                    title = "تقسیم —",
                    icon = Icons.Default.HorizontalSplit,
                    isSelected = activeCadTool == CadToolType.RECTANGLE,
                    onClick = {
                      activeCadTool = CadToolType.RECTANGLE
                      design = DesignEditor.split(design, selectedPanelIndex, SplitAxis.HORIZONTAL)
                    }
                  )
                  CadToolSidebarButton(
                    title = "قوس",
                    icon = Icons.Default.Architecture,
                    isSelected = design.windowShape == WindowShape.ARCHED,
                    onClick = {
                      design = DesignEditor.setShape(
                        design,
                        if (design.windowShape == WindowShape.ARCHED) WindowShape.RECTANGLE else WindowShape.ARCHED,
                        350
                      )
                    }
                  )
                  CadToolSidebarButton(
                    title = "شیب‌دار",
                    icon = Icons.Default.Roofing,
                    isSelected = design.windowShape == WindowShape.TRAPEZOIDAL || design.windowShape == WindowShape.TRIANGLE,
                    onClick = {
                      design = DesignEditor.setShape(
                        design,
                        if (design.windowShape == WindowShape.TRAPEZOIDAL) WindowShape.RECTANGLE else WindowShape.TRAPEZOIDAL,
                        400
                      )
                    }
                  )
                  CadToolSidebarButton(
                    title = "کتیبه",
                    icon = Icons.Default.Splitscreen,
                    isSelected = design.hasTransom,
                    onClick = {
                      design = if (design.hasTransom) design.copy(hasTransom = false)
                      else DesignEditor.setTransomHeightMm(design, design.transomHeightMm.coerceAtMost(design.heightMm - 300))
                    }
                  )
                  CadToolSidebarButton(
                    title = "دستگیره",
                    icon = Icons.Default.PanToolAlt,
                    isSelected = activeCadTool == CadToolType.HANDLE,
                    onClick = { activeCadTool = if (activeCadTool == CadToolType.HANDLE) CadToolType.SELECT else CadToolType.HANDLE }
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
                  HorizontalDivider(color = StudioColors.Outline, modifier = Modifier.padding(horizontal = 12.dp))
                  CadToolSidebarButton(
                    title = "قالب",
                    icon = Icons.Default.DashboardCustomize,
                    isSelected = false,
                    onClick = { showTemplateDialog = true }
                  )
                  CadToolSidebarButton(
                    title = "SVG",
                    icon = Icons.Default.Download,
                    isSelected = false,
                    onClick = {
                      pendingSvg = DesignSvgExporter.export(design)
                      svgExport.launch("window-${design.widthMm}x${design.heightMm}.svg")
                    }
                  )
                }
              }

              // 2. Center workspace: toolbar strip, canvas and live info badges
              Column(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight()
                  .background(StudioColors.Background)
              ) {
                CanvasToolStrip(
                  viewMode = canvasViewMode,
                  onChangeViewMode = { canvasViewMode = it },
                  snapStepMm = snapStepMm,
                  onChangeSnap = { snapStepMm = it },
                  zoom = zoomFactor,
                  onResetZoom = { resetZoom() },
                  showGrid = showGrid,
                  onToggleGrid = { showGrid = !showGrid }
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                  Elevation2DCanvas(
                    design = design,
                    activeTool = activeCadTool,
                    selectedPanelIndex = selectedPanelIndex,
                    onSelectPanel = { selectedPanelIndex = it },
                    showGrid = showGrid,
                    showDimensions = showDimensions,
                    snapStepMm = snapStepMm,
                    viewMode = canvasViewMode,
                    onTransformState = { zoom, reset ->
                      zoomFactor = zoom
                      resetZoom = reset
                    },
                    onPlaceHandle = { idx, x, y ->
                      // A tap only relocates the handle while the handle tool is active.
                      if (activeCadTool == CadToolType.HANDLE) {
                        design = DesignEditor.updatePanel(design, idx) {
                          it.copy(hasHandle = true, handleX = x, handleY = y)
                        }
                      }
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

                  // Live dimension badge (top-left of the canvas)
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StudioColors.Surface.copy(alpha = 0.92f),
                    modifier = Modifier
                      .align(Alignment.TopStart)
                      .padding(10.dp)
                  ) {
                    Text(
                      text = "${CurrencyHelper.toPersianDigits(design.widthMm)} × ${CurrencyHelper.toPersianDigits(design.heightMm)} mm  •  " +
                        "${CurrencyHelper.toPersianDigits(areaM2.setScale(2, RoundingMode.HALF_UP).toString())} m²",
                      style = MaterialTheme.typography.labelSmall,
                      color = StudioColors.TextPrimary,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                  }

                  // Material badge (top-right of the canvas) — reminds which rules apply
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = materialAccent.copy(alpha = 0.92f),
                    modifier = Modifier
                      .align(Alignment.TopEnd)
                      .padding(10.dp)
                  ) {
                    Text(
                      text = MaterialCatalog.familyTitle(design.materialFamily),
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = Color.White,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                  }
                }

                // Live summary strip below the canvas
                Surface(color = StudioColors.Surface, modifier = Modifier.fillMaxWidth()) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .horizontalScroll(rememberScrollState())
                      .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                  ) {
                    StudioMetric("طول پروفیل", "${CurrencyHelper.toPersianDigits(profileMeters.toString())} m")
                    StudioMetric("شیشه", "${mfgResult.glassPieces.size} جام")
                    StudioMetric("گالوانیزه", if (design.materialFamily == MaterialFamily.UPVC) "${mfgResult.reinforcementCuts.sumOf { it.lengthMm }} mm" else "ندارد")
                    StudioMetric("لنگه", "${CurrencyHelper.toPersianDigits(design.panels.size)}")
                    StudioMetric("قیمت", CurrencyHelper.formatMoney(estimate.quotationPrice, estimate.currency))
                  }
                }
              }

              // 3. Right property dock: opening inspector / design properties
              Surface(
                color = StudioColors.Surface,
                modifier = Modifier
                  .width(272.dp)
                  .fillMaxHeight()
                  .border(1.dp, StudioColors.Outline)
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("لنگهٔ انتخابی", "طرح").forEachIndexed { index, title ->
                      Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (inspectorTab == index) StudioColors.AccentStrong else StudioColors.SurfaceRaised,
                        modifier = Modifier
                          .weight(1f)
                          .clip(RoundedCornerShape(8.dp))
                          .clickable { inspectorTab = index }
                      ) {
                        Text(
                          text = title,
                          style = MaterialTheme.typography.labelMedium,
                          fontWeight = if (inspectorTab == index) FontWeight.Bold else FontWeight.Normal,
                          color = if (inspectorTab == index) Color.White else StudioColors.TextMuted,
                          modifier = Modifier.padding(vertical = 7.dp),
                          textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                      }
                    }
                  }

                  Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (inspectorTab == 0) {
                      OpeningInspector(
                        design = design,
                        selectedIndex = selectedPanelIndex,
                        onDesignChange = { design = it },
                        onSelect = { selectedPanelIndex = it },
                        onEditPanelWidth = { idx ->
                          val w = geometry.panels.getOrNull(idx)?.widthMm?.toInt() ?: 400
                          editingDimension = DimensionEditTarget.PanelWidth(idx, w)
                        }
                      )
                    } else {
                      DesignPropertiesPanel(
                        design = design,
                        onDesignChange = { design = it },
                        onEditWidth = { editingDimension = DimensionEditTarget.OverallWidth(design.widthMm) },
                        onEditHeight = { editingDimension = DimensionEditTarget.OverallHeight(design.heightMm) }
                      )
                    }
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
              .background(StudioColors.Background)
          ) {
            // Header Bar
            Surface(
              color = StudioColors.SurfaceRaised,
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
                  Icon(Icons.Default.Calculate, contentDescription = null, tint = StudioColors.Accent)
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
              color = StudioColors.Surface,
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
                    color = if (isSelected) StudioColors.AccentStrong else StudioColors.SurfaceRaised,
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
                      color = StudioColors.SurfaceRaised,
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
                      color = StudioColors.SurfaceRaised,
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
                        SpecDataRow("جنس سازه", MaterialCatalog.familyTitle(design.materialFamily))
                        SpecDataRow("سیستم پروفیل", currentSystem.code)
                        SpecDataRow("عرض نمای پروفیل", "${CurrencyHelper.toPersianDigits(currentSystem.faceWidthMm)} mm")
                        if (design.materialFamily == MaterialFamily.ALUMINUM) {
                          SpecDataRow("پل حرارتی", if (design.hasThermalBreak) "دارد" else "ندارد")
                        }
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
                        color = StudioColors.SurfaceRaised,
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
                        color = StudioColors.SurfaceRaised,
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
                        color = StudioColors.SurfaceRaised,
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
                    color = StudioColors.SurfaceRaised,
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
                      SpecDataRow(
                        if (design.materialFamily == MaterialFamily.UPVC) "هزینه گالوانیزه:" else "هزینه پل حرارتی:",
                        CurrencyHelper.formatMoney(estimate.reinforcementCost, estimate.currency)
                      )
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
              color = StudioColors.SurfaceRaised,
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
    color = if (isSelected) StudioColors.AccentStrong else StudioColors.SurfaceRaised,
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
      Icon(icon, contentDescription = null, tint = if (isSelected) Color.White else StudioColors.TextMuted, modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = if (isSelected) Color.White else StudioColors.TextPrimary
      )
    }
  }
}

@Composable
private fun StudioMetric(label: String, value: String) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(label, style = MaterialTheme.typography.labelSmall, color = StudioColors.TextMuted)
    Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = StudioColors.TextPrimary)
  }
}

/**
 * Thin toolbar above the canvas: technical view mode (body / representation / glass heat), snap step
 * and zoom readout. Keeps the canvas itself free of chrome so the drawing stays the focus.
 */
@Composable
private fun CanvasToolStrip(
  viewMode: CanvasViewMode,
  onChangeViewMode: (CanvasViewMode) -> Unit,
  snapStepMm: Int,
  onChangeSnap: (Int) -> Unit,
  zoom: Float,
  onResetZoom: () -> Unit,
  showGrid: Boolean,
  onToggleGrid: () -> Unit
) {
  Surface(color = StudioColors.SurfaceRaised, modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 8.dp, vertical = 5.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      CanvasViewMode.entries.forEach { mode ->
        val icon = when (mode) {
          CanvasViewMode.BODY -> Icons.Default.DoorFront
          CanvasViewMode.REPRESENTATION -> Icons.Default.SquareFoot
          CanvasViewMode.GLASS_HEAT -> Icons.Default.Thermostat
        }
        Surface(
          shape = RoundedCornerShape(7.dp),
          color = if (viewMode == mode) StudioColors.AccentStrong else Color.Transparent,
          modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .clickable { onChangeViewMode(mode) }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              icon,
              contentDescription = mode.titleFa,
              tint = if (viewMode == mode) Color.White else StudioColors.TextMuted,
              modifier = Modifier.size(15.dp)
            )
            Text(
              mode.titleFa,
              style = MaterialTheme.typography.labelSmall,
              color = if (viewMode == mode) Color.White else StudioColors.TextMuted
            )
          }
        }
      }

      VerticalDivider(color = StudioColors.Outline, modifier = Modifier.height(20.dp))

      Text("Snap", style = MaterialTheme.typography.labelSmall, color = StudioColors.TextMuted)
      listOf(1, 10, 50).forEach { step ->
        FilterChip(
          selected = snapStepMm == step,
          onClick = { onChangeSnap(step) },
          label = { Text(if (step == 1) "آزاد" else "${CurrencyHelper.toPersianDigits(step)} mm", style = MaterialTheme.typography.labelSmall) },
          modifier = Modifier.height(28.dp)
        )
      }

      VerticalDivider(color = StudioColors.Outline, modifier = Modifier.height(20.dp))

      TextButton(onClick = onResetZoom, contentPadding = PaddingValues(horizontal = 6.dp)) {
        Icon(Icons.Default.CenterFocusStrong, contentDescription = null, tint = StudioColors.Accent, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text(
          "${CurrencyHelper.toPersianDigits((zoom * 100).toInt())}٪",
          style = MaterialTheme.typography.labelSmall,
          color = StudioColors.TextPrimary
        )
      }
      IconButton(onClick = onToggleGrid, modifier = Modifier.size(30.dp)) {
        Icon(
          Icons.Default.GridOn,
          contentDescription = "شبکه",
          tint = if (showGrid) StudioColors.Accent else StudioColors.TextMuted,
          modifier = Modifier.size(17.dp)
        )
      }
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
private fun SpecDataRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(StudioColors.Background, RoundedCornerShape(6.dp))
      .padding(horizontal = 10.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
    Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
  }
}
