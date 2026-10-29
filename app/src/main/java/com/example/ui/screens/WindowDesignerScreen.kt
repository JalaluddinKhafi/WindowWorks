package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.CurrencyHelper
import com.example.domain.engine.CostCalculator
import com.example.domain.engine.ManufacturingCalculator
import com.example.domain.model.*
import com.example.ui.designer.Elevation2DCanvas
import com.example.ui.designer.Parametric3DCanvas
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

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
  onBack: () -> Unit
) {
  var design by remember { mutableStateOf(initialDesign) }
  var selectedTab by remember { mutableIntStateOf(0) } // 0 = 2D Elevation, 1 = 3D Interactive
  var selectedPanelIndex by remember { mutableStateOf<Int?>(0) }
  var showCostSheet by remember { mutableStateOf(false) }

  val mfgResult = remember(design) {
    ManufacturingCalculator.calculate(design)
  }

  val estimate = remember(design) {
    CostCalculator.estimateCost(design)
  }

  // Cost metrics (exact decimal area / perimeter)
  val areaM2 = BigDecimal(design.widthMm.toLong() * design.heightMm.toLong())
    .divide(BigDecimal(1_000_000), 4, RoundingMode.HALF_UP)
  val perimeterMeters = BigDecimal((design.widthMm + design.heightMm).toLong() * 2)
    .divide(BigDecimal(1000), 4, RoundingMode.HALF_UP)
  val pricePerM2Afn = if (areaM2 > BigDecimal.ZERO) {
    estimate.quotationPrice.divide(areaM2, 2, RoundingMode.HALF_UP)
  } else BigDecimal.ZERO

  val pricePerLinearMeterAfn = if (perimeterMeters > BigDecimal.ZERO) {
    estimate.quotationPrice.divide(perimeterMeters, 2, RoundingMode.HALF_UP)
  } else BigDecimal.ZERO

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(design.name, style = MaterialTheme.typography.titleMedium)
            Text(
              "${CurrencyHelper.toPersianDigits(design.widthMm)} × ${CurrencyHelper.toPersianDigits(design.heightMm)} میلی‌متر | ${design.profileSystem}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowForward, contentDescription = "بازگشت")
          }
        },
        actions = {
          IconButton(onClick = { showCostSheet = true }) {
            Icon(Icons.Default.Calculate, contentDescription = "محاسبه قیمت تمام‌شده", tint = MaterialTheme.colorScheme.primary)
          }
          IconButton(onClick = { onNavigateToManufacturingInstructions(design) }) {
            Icon(Icons.Default.Engineering, contentDescription = "نقشه ساخت کارگاهی")
          }
          if (canSave) {
            IconButton(onClick = { onSaveDesign(design) }) {
              Icon(Icons.Default.Save, contentDescription = "ذخیره طراحی", tint = MaterialTheme.colorScheme.primary)
            }
          }
        }
      )
    },
    bottomBar = {
      Surface(
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          // Quick price summary pill
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Text("قیمت پیش‌فاکتور:", style = MaterialTheme.typography.labelMedium)
              Text(
                CurrencyHelper.formatMoney(estimate.quotationPrice, estimate.currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
            Text(
              "مترمربع: ${CurrencyHelper.formatMoney(pricePerM2Afn, "AFN")}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = { showCostSheet = true },
              modifier = Modifier.weight(1.1f)
            ) {
              Icon(Icons.Default.PriceCheck, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("آنالیز قیمت تمام‌شده")
            }

            OutlinedButton(
              onClick = { onNavigateToManufacturingInstructions(design) },
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("نقشه کارگاهی")
            }

            IconButton(onClick = { onNavigateToCutting(design) }) {
              Icon(Icons.Default.ContentCut, contentDescription = "نقشه برش", tint = MaterialTheme.colorScheme.primary)
            }
          }
        }
      }
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      // 2D vs 3D Tab Switcher
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surface
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.Architecture, contentDescription = null, modifier = Modifier.size(18.dp))
              Text("نمای فنی ۲ بعدی")
            }
          }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(18.dp))
              Text("دید ۳ بعدی تعاملی")
            }
          }
        )
      }

      // Visual Canvas Area (Height 310dp)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(310.dp)
      ) {
        if (selectedTab == 0) {
          Elevation2DCanvas(
            design = design,
            selectedPanelIndex = selectedPanelIndex,
            onSelectPanel = { selectedPanelIndex = it },
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Parametric3DCanvas(
            design = design,
            modifier = Modifier.fillMaxSize()
          )
        }
      }

      // Warning banner if any manufacturing rule is provisional
      if (mfgResult.isProvisional) {
        Surface(
          color = Color(0xFFFEF3C7),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(18.dp))
            Text(
              text = "محاسبه مشروط: ${mfgResult.missingRulesDescription}",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF92400E)
            )
          }
        }
      }

      // Parameter Adjustments Form
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Dimensions Section
        Text("ابعاد اصلی و کتیبه (میلی‌متر):", style = MaterialTheme.typography.titleSmall)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedTextField(
            value = design.widthMm.toString(),
            onValueChange = {
              val w = it.toIntOrNull() ?: design.widthMm
              design = design.copy(widthMm = w)
            },
            label = { Text("عرض کلی (W)") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = design.heightMm.toString(),
            onValueChange = {
              val h = it.toIntOrNull() ?: design.heightMm
              design = design.copy(heightMm = h)
            },
            label = { Text("ارتفاع کلی (H)") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }

        // Transom Toggle & Height
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Switch(
              checked = design.hasTransom,
              onCheckedChange = { design = design.copy(hasTransom = it) }
            )
            Text("کتیبه بالای پنجره", style = MaterialTheme.typography.bodyMedium)
          }

          if (design.hasTransom) {
            OutlinedTextField(
              value = design.transomHeightMm.toString(),
              onValueChange = {
                val th = it.toIntOrNull() ?: design.transomHeightMm
                design = design.copy(transomHeightMm = th)
              },
              label = { Text("ارتفاع کتیبه (mm)") },
              singleLine = true,
              modifier = Modifier.width(130.dp)
            )
          }
        }

        // Opening Direction (Inward / Outward)
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("جهت بازشوی لنگه‌ها:", style = MaterialTheme.typography.bodyMedium)
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
              selected = design.openingDirection == OpeningDirection.INWARD,
              onClick = { design = design.copy(openingDirection = OpeningDirection.INWARD) },
              label = { Text(OpeningDirection.INWARD.titlePersian) }
            )
            FilterChip(
              selected = design.openingDirection == OpeningDirection.OUTWARD,
              onClick = { design = design.copy(openingDirection = OpeningDirection.OUTWARD) },
              label = { Text(OpeningDirection.OUTWARD.titlePersian) }
            )
          }
        }

        HorizontalDivider()

        // Number of Panels Selector
        Text("تعداد قسمت‌ها / لنگه‌ها:", style = MaterialTheme.typography.titleSmall)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf(1, 2, 3, 4).forEach { count ->
            FilterChip(
              selected = design.panels.size == count,
              onClick = {
                val currentPanels = design.panels
                val newPanels = if (count > currentPanels.size) {
                  currentPanels + List(count - currentPanels.size) {
                    PanelConfig(id = "p_${it + currentPanels.size + 1}", openingType = OpeningType.FIXED)
                  }
                } else {
                  currentPanels.take(count)
                }
                design = design.copy(panels = newPanels)
                if (selectedPanelIndex != null && selectedPanelIndex!! >= count) {
                  selectedPanelIndex = 0
                }
              },
              label = { Text("${CurrencyHelper.toPersianDigits(count)} لنگه") },
              modifier = Modifier.weight(1f)
            )
          }
        }

        // Selected Panel Interactive Editor
        if (selectedPanelIndex != null && selectedPanelIndex!! < design.panels.size) {
          val selIdx = selectedPanelIndex!!
          val selPanel = design.panels[selIdx]
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  "تنظیمات لنگه شماره ${CurrencyHelper.toPersianDigits(selIdx + 1)} (انتخاب‌شده در نقشه):",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = { selectedPanelIndex = null }, modifier = Modifier.size(24.dp)) {
                  Icon(Icons.Default.Close, contentDescription = "بستن", modifier = Modifier.size(16.dp))
                }
              }

              // Opening types for this panel
              Text("نوع بازشوی این لنگه:", style = MaterialTheme.typography.labelSmall)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                listOf(OpeningType.FIXED, OpeningType.CASEMENT_RIGHT, OpeningType.CASEMENT_LEFT, OpeningType.TILT_AND_TURN).forEach { ot ->
                  FilterChip(
                    selected = selPanel.openingType == ot,
                    onClick = {
                      val updated = design.panels.toMutableList()
                      updated[selIdx] = selPanel.copy(openingType = ot)
                      design = design.copy(panels = updated)
                    },
                    label = { Text(ot.titlePersian.split(" ").take(2).joinToString(" "), style = MaterialTheme.typography.labelSmall) }
                  )
                }
              }
            }
          }
        }

        // Panel Opening Types List
        Text("فهرست تمام لنگه‌ها و بازشوها:", style = MaterialTheme.typography.titleSmall)
        design.panels.forEachIndexed { idx, p ->
          Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (selectedPanelIndex == idx) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
              else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { selectedPanelIndex = idx }
          ) {
            Row(
              modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "لنگه شماره ${CurrencyHelper.toPersianDigits(idx + 1)}",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = p.openingType.titlePersian,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.primary
                )
              }

              var expandedOpening by remember { mutableStateOf(false) }
              Box {
                OutlinedButton(onClick = { expandedOpening = true }) {
                  Text("تغییر بازشو")
                  Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }

                DropdownMenu(
                  expanded = expandedOpening,
                  onDismissRequest = { expandedOpening = false }
                ) {
                  OpeningType.entries.forEach { ot ->
                    DropdownMenuItem(
                      text = { Text(ot.titlePersian) },
                      onClick = {
                        val updated = design.panels.toMutableList()
                        updated[idx] = p.copy(openingType = ot)
                        design = design.copy(panels = updated)
                        expandedOpening = false
                      }
                    )
                  }
                }
              }
            }
          }
        }

        HorizontalDivider()

        // Profile System & Brand (Series 6000 & 7000 and Aluminum)
        Text("سیستم و برند مقطع پروفیل:", style = MaterialTheme.typography.titleSmall)
        val profileSystems = listOf(
          "Wintech 6000",
          "Wintech 7000",
          "Vistabest 60",
          "Hofmann 60",
          "Akpa Thermal",
          "Saray Aluminum",
          "Aldoks Standard"
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          profileSystems.take(4).forEach { sys ->
            FilterChip(
              selected = design.profileSystem == sys,
              onClick = { design = design.copy(profileSystem = sys) },
              label = { Text(sys, style = MaterialTheme.typography.labelSmall) }
            )
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          profileSystems.drop(4).forEach { sys ->
            FilterChip(
              selected = design.profileSystem == sys,
              onClick = { design = design.copy(profileSystem = sys) },
              label = { Text(sys, style = MaterialTheme.typography.labelSmall) }
            )
          }
        }

        // Distinct Frame Finish vs Sash Finish
        Text("رنگ و پوشش فریم اصلی:", style = MaterialTheme.typography.titleSmall)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          FrameFinish.entries.take(4).forEach { finish ->
            FilterChip(
              selected = design.frameFinish == finish,
              onClick = { design = design.copy(frameFinish = finish) },
              label = { Text(finish.titlePersian.split(" ").first(), style = MaterialTheme.typography.labelSmall) }
            )
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          FrameFinish.entries.drop(4).take(3).forEach { finish ->
            FilterChip(
              selected = design.frameFinish == finish,
              onClick = { design = design.copy(frameFinish = finish) },
              label = { Text(finish.titlePersian.split(" ").first(), style = MaterialTheme.typography.labelSmall) }
            )
          }
        }

        Text("رنگ و پوشش لنگه متحرک (ساش):", style = MaterialTheme.typography.titleSmall)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          FrameFinish.entries.take(4).forEach { finish ->
            FilterChip(
              selected = design.sashFinish == finish,
              onClick = { design = design.copy(sashFinish = finish) },
              label = { Text(finish.titlePersian.split(" ").first(), style = MaterialTheme.typography.labelSmall) }
            )
          }
        }

        // Outer vs Inner Glass Tints
        Text("رنگ لایه بیرونی شیشه:", style = MaterialTheme.typography.titleSmall)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          GlassTint.entries.take(4).forEach { tint ->
            FilterChip(
              selected = design.outerGlassTint == tint,
              onClick = { design = design.copy(outerGlassTint = tint) },
              label = { Text(tint.titlePersian.split(" ").first(), style = MaterialTheme.typography.labelSmall) }
            )
          }
        }

        Text("رنگ لایه درونی شیشه:", style = MaterialTheme.typography.titleSmall)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          GlassTint.entries.take(4).forEach { tint ->
            FilterChip(
              selected = design.innerGlassTint == tint,
              onClick = { design = design.copy(innerGlassTint = tint) },
              label = { Text(tint.titlePersian.split(" ").first(), style = MaterialTheme.typography.labelSmall) }
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))
      }
    }
  }

  // Cost Calculation Bottom Sheet / Dialog
  if (showCostSheet) {
    var markupPctStr by remember { mutableStateOf(defaultMarkupPercent.stripTrailingZeros().toPlainString()) }
    val currentMarkup = CurrencyHelper.parseAmount(markupPctStr)
      .takeIf { it >= BigDecimal.ZERO } ?: defaultMarkupPercent

    val recalculatedEstimate = remember(design, currentMarkup) {
      CostCalculator.estimateCost(design, markupPercent = currentMarkup)
    }

    val dynamicPricePerM2 = if (areaM2 > BigDecimal.ZERO) {
      recalculatedEstimate.quotationPrice.divide(areaM2, 2, RoundingMode.HALF_UP)
    } else BigDecimal.ZERO

    val dynamicPricePerLinearMeter = if (perimeterMeters > BigDecimal.ZERO) {
      recalculatedEstimate.quotationPrice.divide(perimeterMeters, 2, RoundingMode.HALF_UP)
    } else BigDecimal.ZERO

    val usdRate = exchangeRateUsdToAfn.takeIf { it > BigDecimal.ZERO } ?: BigDecimal("72.50")
    val totalUsd = recalculatedEstimate.quotationPrice.divide(usdRate, 2, RoundingMode.HALF_UP)
    val calculatedProfit = recalculatedEstimate.quotationPrice.subtract(recalculatedEstimate.totalBaseCost)

    ModalBottomSheet(
      onDismissRequest = { showCostSheet = false },
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("محاسبه دقیق قیمت تمام‌شده و سود (BOM)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
              "مساحت: ${CurrencyHelper.toPersianDigits(areaM2.setScale(2, RoundingMode.HALF_UP).toPlainString())} m² | محیط: ${CurrencyHelper.toPersianDigits(perimeterMeters.setScale(2, RoundingMode.HALF_UP).toPlainString())} متر",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          IconButton(onClick = { showCostSheet = false }) {
            Icon(Icons.Default.Close, contentDescription = "بستن")
          }
        }

        HorizontalDivider()

        // Cost breakdown table
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ریز بهای تمام‌شده مصالح و تولید:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("هزینه پروفیل UPVC / آلومینیوم:")
              Text(CurrencyHelper.formatMoney(recalculatedEstimate.profileCost, "AFN"), fontWeight = FontWeight.SemiBold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("تقویت گالوانیزه ۱.۲۵mm:")
              Text(CurrencyHelper.formatMoney(recalculatedEstimate.reinforcementCost, "AFN"), fontWeight = FontWeight.SemiBold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("شیشه دوجداره استاندارد:")
              Text(CurrencyHelper.formatMoney(recalculatedEstimate.glassCost, "AFN"), fontWeight = FontWeight.SemiBold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("یراق‌آلات، لولا و قفل:")
              Text(CurrencyHelper.formatMoney(recalculatedEstimate.hardwareCost, "AFN"), fontWeight = FontWeight.SemiBold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("لاستیک درزبندی EPDM:")
              Text(CurrencyHelper.formatMoney(recalculatedEstimate.gasketCost, "AFN"), fontWeight = FontWeight.SemiBold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("دستمزد مونتاژ و ساخت:")
              Text(CurrencyHelper.formatMoney(recalculatedEstimate.laborCost, "AFN"), fontWeight = FontWeight.SemiBold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("هزینه حمل و اقلام متفرقه:")
              Text(CurrencyHelper.formatMoney(recalculatedEstimate.transportCost, "AFN"), fontWeight = FontWeight.SemiBold)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("جمع کل بهای تمام‌شده (Base Cost):", fontWeight = FontWeight.Bold)
              Text(CurrencyHelper.formatMoney(recalculatedEstimate.totalBaseCost, "AFN"), fontWeight = FontWeight.Bold)
            }
          }
        }

        // Markup setting
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = markupPctStr,
            onValueChange = { markupPctStr = it },
            label = { Text("درصد سود کارگاه (%)") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )

          Column(modifier = Modifier.weight(1.5f)) {
            Text("سود پیش‌بینی شده:", style = MaterialTheme.typography.labelSmall)
            Text(
              CurrencyHelper.formatMoney(calculatedProfit, "AFN"),
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF15803D)
            )
          }
        }

        // Per m² and per linear meter analysis
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text("قیمت هر متر مربع:", style = MaterialTheme.typography.labelSmall)
              Text(
                "${CurrencyHelper.formatMoney(dynamicPricePerM2, "AFN")} / m²",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text("قیمت هر متر طول:", style = MaterialTheme.typography.labelSmall)
              Text(
                "${CurrencyHelper.formatMoney(dynamicPricePerLinearMeter, "AFN")} / m",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        // Grand Total Card
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("قیمت نهایی قابل ارائه به مشتری:", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium)
              Text(
                CurrencyHelper.formatMoney(recalculatedEstimate.quotationPrice, "AFN"),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text("معادل دالری:", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
              Text(
                CurrencyHelper.formatMoney(totalUsd, "USD"),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        Button(
          onClick = {
            showCostSheet = false
            onNavigateToQuotation(design)
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.ReceiptLong, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("صدور پیش‌فاکتور رسمی برای مشتری")
        }

        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}
