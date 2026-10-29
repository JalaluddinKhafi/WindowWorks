package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.core.CurrencyHelper
import com.example.domain.engine.CuttingOptimizer
import com.example.domain.engine.ManufacturingCalculator
import com.example.domain.model.StockBarPlan
import com.example.domain.model.WindowDesign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuttingPlanScreen(
  design: WindowDesign,
  sawKerfMm: Int = CuttingOptimizer.DEFAULT_SAW_KERF_MM,
  endTrimMm: Int = CuttingOptimizer.DEFAULT_END_TRIM_MM,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val mfg = remember(design) { ManufacturingCalculator.calculate(design) }
  val optResult = remember(mfg, sawKerfMm, endTrimMm) {
    CuttingOptimizer.optimize(
      pieces = mfg.profileCuts,
      profileName = "${design.profileSystem} (${design.frameFinish.titlePersian})",
      stockLengthMm = CuttingOptimizer.DEFAULT_STOCK_LENGTH_MM,
      sawKerfMm = sawKerfMm,
      endTrimMm = endTrimMm
    )
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("نقشه و دستور بهینه‌سازی برش شاخه", style = MaterialTheme.typography.titleMedium)
            Text(
              "${design.name} | شاخه‌های ${CurrencyHelper.toPersianDigits(CuttingOptimizer.DEFAULT_STOCK_LENGTH_MM / 1000)} متری با احتساب ${CurrencyHelper.toPersianDigits(sawKerfMm)}mm تیغ‌اره و ${CurrencyHelper.toPersianDigits(endTrimMm)}mm سرشاخه",
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
          IconButton(
            onClick = {
              val shareText = buildCuttingText(design, optResult)
              val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
              }
              context.startActivity(Intent.createChooser(sendIntent, "ارسال دستور برش کارگاهی"))
            }
          ) {
            Icon(Icons.Default.Share, contentDescription = "اشتراک‌گذاری دستور برش")
          }
        }
      )
    }
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      contentPadding = PaddingValues(vertical = 14.dp)
    ) {
      // Summary Statistics Header
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text("خلاصه بهینه‌سازی برش شاخه‌های ۶ متری (1D Cutting Plan)", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceAround
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("تعداد شاخه ۶ متری", style = MaterialTheme.typography.labelSmall)
                Text(
                  "${CurrencyHelper.toPersianDigits(optResult.stockBars.size)} شاخه",
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.primary
                )
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("تعداد قطعات", style = MaterialTheme.typography.labelSmall)
                Text(
                  "${CurrencyHelper.toPersianDigits(optResult.totalPiecesCount)} قطعه",
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.primary
                )
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("درصد کل ضایعات", style = MaterialTheme.typography.labelSmall)
                Text(
                  "${CurrencyHelper.toPersianDigits(String.format("%.1f", optResult.overallWastePercent))}%",
                  style = MaterialTheme.typography.titleMedium,
                  color = if (optResult.overallWastePercent > 12f) Color(0xFFDC2626) else Color(0xFF059669)
                )
              }
            }
          }
        }
      }

      // Visual Stock Bars List
      item {
        Text("چیدمان قطعات روی هر شاخه پروفیل:", style = MaterialTheme.typography.titleSmall)
      }

      items(optResult.stockBars) { bar ->
        StockBarCard(bar = bar)
      }

      // Detailed Cutting List Table for Workshop Operator
      item {
        Spacer(modifier = Modifier.height(6.dp))
        Text("جدول تفکیکی دستور برش کارگاهی (اپراتور اره):", style = MaterialTheme.typography.titleSmall)
      }

      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            optResult.stockBars.forEach { b ->
              Text(
                text = "شاخه شماره ${CurrencyHelper.toPersianDigits(b.barIndex)} (مصرف: ${CurrencyHelper.toPersianDigits(b.usedLengthMm)}mm | باقیمانده: ${CurrencyHelper.toPersianDigits(b.wasteLengthMm)}mm):",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
              )
              b.cuts.forEachIndexed { cIdx, cut ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "${CurrencyHelper.toPersianDigits(cIdx + 1)}. ${cut.tag}",
                    style = MaterialTheme.typography.bodySmall
                  )
                  Text(
                    text = "طول: ${CurrencyHelper.toPersianDigits(cut.lengthMm)}mm | زاویه: ${cut.angleStart}°/${cut.angleEnd}°",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF0F172A)
                  )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
              }
              Spacer(modifier = Modifier.height(4.dp))
            }
          }
        }
      }
    }
  }
}

@Composable
fun StockBarCard(bar: StockBarPlan) {
  val segmentColors = listOf(
    Color(0xFF2563EB),
    Color(0xFF0284C7),
    Color(0xFF059669),
    Color(0xFF7C3AED),
    Color(0xFFD97706),
    Color(0xFF0D9488)
  )

  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "شاخه شماره ${CurrencyHelper.toPersianDigits(bar.barIndex)}",
          style = MaterialTheme.typography.titleSmall
        )
        if (bar.isReusableOffcut) {
          Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFECFDF5)) {
            Text(
              text = "ته‌شاخه مفید: ${CurrencyHelper.toPersianDigits(bar.wasteLengthMm)}mm",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF047857),
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        } else {
          Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFEF2F2)) {
            Text(
              text = "ضایعات دورریز: ${CurrencyHelper.toPersianDigits(bar.wasteLengthMm)}mm",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFFB91C1C),
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Graphical Stock Bar Canvas
      Canvas(
        modifier = Modifier
          .fillMaxWidth()
          .height(36.dp)
      ) {
        val totalStockW = 6000f
        val canvasW = size.width
        val canvasH = size.height

        // Outer bar border
        drawRect(Color(0xFFE2E8F0), Offset.Zero, Size(canvasW, canvasH))

        var currentMm = 20f // 20mm end trim
        val startX = (currentMm / totalStockW) * canvasW
        drawRect(Color(0xFF94A3B8), Offset.Zero, Size(startX, canvasH))

        bar.cuts.forEachIndexed { idx, c ->
          val color = segmentColors[idx % segmentColors.size]
          val cutW = (c.lengthMm.toFloat() / totalStockW) * canvasW
          val cutX = (currentMm / totalStockW) * canvasW

          drawRect(color, Offset(cutX, 0f), Size(cutW, canvasH))
          // Draw thin white kerf separator line
          drawLine(Color.White, Offset(cutX + cutW, 0f), Offset(cutX + cutW, canvasH), strokeWidth = 2.5f)

          currentMm += c.lengthMm + 4f // 4mm kerf
        }

        // Remaining waste section at end
        val remX = (currentMm / totalStockW) * canvasW
        val remW = canvasW - remX
        if (remW > 0) {
          val remColor = if (bar.isReusableOffcut) Color(0xFF34D399) else Color(0xFFF87171)
          drawRect(remColor, Offset(remX, 0f), Size(remW, canvasH))
        }

        // Stroke border
        drawRect(Color(0xFFCBD5E1), Offset.Zero, Size(canvasW, canvasH), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f))
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Piece badges under bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        bar.cuts.forEachIndexed { idx, c ->
          val color = segmentColors[idx % segmentColors.size]
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = color.copy(alpha = 0.12f)
          ) {
            Text(
              text = "${c.tag.split(" ").first()} (${CurrencyHelper.toPersianDigits(c.lengthMm)}mm)",
              style = MaterialTheme.typography.labelSmall,
              color = color,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      }
    }
  }
}

private fun buildCuttingText(design: WindowDesign, opt: com.example.domain.model.CuttingOptimizationResult): String {
  val sb = StringBuilder()
  sb.appendLine("دستور برش کارگاهی در و پنجره دوجداره")
  sb.appendLine("طراحی: ${design.name} (${design.profileSystem})")
  sb.appendLine("تعداد شاخه ۶ متری مورد نیاز: ${opt.stockBars.size} شاخه")
  sb.appendLine("درصد ضایعات کل: ${String.format("%.1f", opt.overallWastePercent)}%")
  sb.appendLine("----------------------------------------")
  opt.stockBars.forEach { b ->
    sb.appendLine("شاخه شماره ${b.barIndex}:")
    b.cuts.forEachIndexed { i, c ->
      sb.appendLine("  ${i + 1}. ${c.tag} | طول: ${c.lengthMm}mm | زاویه: ${c.angleStart}°/${c.angleEnd}°")
    }
    sb.appendLine("  باقیمانده ته شاخه: ${b.wasteLengthMm}mm (${if (b.isReusableOffcut) "مفید قابل مصرف" else "ضایعات"})")
    sb.appendLine()
  }
  return sb.toString()
}
