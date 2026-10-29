package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.CurrencyHelper
import com.example.domain.engine.CostCalculator
import com.example.domain.engine.ManufacturingCalculator
import com.example.domain.model.WindowDesign
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManufacturingInstructionsScreen(
  design: WindowDesign,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val mfg = remember(design) { ManufacturingCalculator.calculate(design) }
  val estimate = remember(design) { CostCalculator.estimateCost(design) }
  val dateStr = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date()) }

  // 10 Steps Data
  val stepList = remember(design, mfg, estimate) {
    listOf(
      WorkshopStep(
        stepNumber = 1,
        title = "مشخصات فنی و شناسنامه یونیت",
        icon = Icons.Default.Badge,
        content = listOf(
          "نام طرح: ${design.name}",
          "کد طراحی: ${design.id}",
          "نوع محصول: ${if (design.productType.name == "WINDOW") "پنجره دوجداره" else "درب دوجداره"}",
          "جنس و متریال: ${if (design.materialFamily.name == "UPVC") "پروفیل UPVC استاندارد" else "آلومینیوم اختصاصی / ترمال‌بریک"}",
          "رنگ و پوشش فریم: ${design.frameFinish.titlePersian}",
          "رنگ و پوشش لنگه: ${design.sashFinish.titlePersian}"
        )
      ),
      WorkshopStep(
        stepNumber = 2,
        title = "ابعاد کلی و موقعیت کتیبه",
        icon = Icons.Default.Straighten,
        content = listOf(
          "عرض نهایی (W): ${CurrencyHelper.toPersianDigits(design.widthMm)} میلی‌متر",
          "ارتفاع نهایی (H): ${CurrencyHelper.toPersianDigits(design.heightMm)} میلی‌متر",
          "وضعیت کتیبه: ${if (design.hasTransom) "دارد (ارتفاع: ${CurrencyHelper.toPersianDigits(design.transomHeightMm)} mm)" else "ندارد (تمام قد)"}",
          "تعداد قسمت‌ها: ${CurrencyHelper.toPersianDigits(design.panels.size)} لنگه مجزا",
          "جهت بازشو: ${design.openingDirection.titlePersian}"
        )
      ),
      WorkshopStep(
        stepNumber = 3,
        title = "سیستم پروفیل و مقاطع انتخابی",
        icon = Icons.Default.Layers,
        content = listOf(
          "برند پروفیل: ${design.profileSystem}",
          "سری مقطع: ${if (design.profileSystem.contains("70")) "سری ۷۰۰۰ (عرض نشیمن ۷۰mm، ۵ کاناله)" else "سری ۶۰۰۰ (عرض نشیمن ۶۰mm، ۴ کاناله)"}",
          "ضخامت دیواره: استاندارد کلاس A (۲.۸ میلی‌متر)",
          "محل تزریق پلی‌آمید: ${if (design.materialFamily.name == "ALUMINUM") "تیغه پلی‌آمید ۲۴mm ضد اتلاف حرارت" else "بدون نیاز (عایق ذاتی UPVC)"}"
        )
      ),
      WorkshopStep(
        stepNumber = 4,
        title = "تفکیک قطعات فریم، لنگه و مولیون",
        icon = Icons.Default.DashboardCustomize,
        content = run {
          val sashCount = design.panels.count { it.openingType != com.example.domain.model.OpeningType.FIXED }
          val mullionCount = (design.panels.size - 1).coerceAtLeast(0) + (if (design.hasTransom) 1 else 0)
          listOf(
            "قطعات فریم اصلی: ۴ شاخه (۲ افقی، ۲ عمودی)",
            "تعداد لنگه‌های متحرک: ${CurrencyHelper.toPersianDigits(sashCount)} لنگه",
            "تعداد وادار میانی (مولیون): ${CurrencyHelper.toPersianDigits(mullionCount)} عدد",
            "تعداد قاب‌های شیشه ثابت: ${CurrencyHelper.toPersianDigits(design.panels.size - sashCount)} قسمت"
          )
        }
      ),
      WorkshopStep(
        stepNumber = 5,
        title = "ریز متره و مصالح خام مصرفی (BOM)",
        icon = Icons.Default.ReceiptLong,
        content = run {
          val profMeters = mfg.totalProfileLengthMm / 1000.0
          val rfMeters = mfg.totalReinforcementLengthMm / 1000.0
          val screwEstimate = ((mfg.totalReinforcementLengthMm / 250) + 16).coerceAtLeast(16)
          listOf(
            "طول کل پروفیل مصرفی: ${CurrencyHelper.toPersianDigits(String.format(Locale.US, "%.2f", profMeters))} متر",
            "تقویت گالوانیزه ۱.۲۵mm: ${CurrencyHelper.toPersianDigits(String.format(Locale.US, "%.2f", rfMeters))} متر",
            "لاستیک درزبندی EPDM: ${CurrencyHelper.toPersianDigits(mfg.totalGasketMeters.toPlainString())} متر",
            "تعداد پیچ گالوانیزه سرمته: ${CurrencyHelper.toPersianDigits(screwEstimate)} عدد (فاصله هر ۲۵cm)",
            "مساحت کل شیشه دوجداره: ${CurrencyHelper.toPersianDigits(mfg.totalGlassAreaM2.toPlainString())} مترمربع"
          )
        }
      ),
      WorkshopStep(
        stepNumber = 6,
        title = "جدول برش دقیق با زوایای فارسی‌بر (Cutting List)",
        icon = Icons.Default.ContentCut,
        content = mfg.profileCuts.map { cut ->
          "• ${cut.sectionType}: طول ${CurrencyHelper.toPersianDigits(cut.lengthMm)} mm | زاویه: ${CurrencyHelper.toPersianDigits(cut.angleStart)}° - ${CurrencyHelper.toPersianDigits(cut.angleEnd)}° (${CurrencyHelper.toPersianDigits(cut.quantity)} عدد) - اورلب جوش +3mm"
        }
      ),
      WorkshopStep(
        stepNumber = 7,
        title = "ابعاد دقیق شیشه دوجداره و زهوارها",
        icon = Icons.Default.GridGoldenratio,
        content = if (mfg.glassPieces.isEmpty()) {
          listOf("شیشه‌ای برای این طرح محاسبه نشده است.")
        } else {
          mfg.glassPieces.mapIndexed { idx, gu ->
            "جام شماره ${CurrencyHelper.toPersianDigits(idx + 1)}: ${CurrencyHelper.toPersianDigits(gu.widthMm)} × ${CurrencyHelper.toPersianDigits(gu.heightMm)} mm | نوع: ${gu.glazingTitle} | لایه بیرونی: ${gu.tintTitle}"
          } + listOf("کسر بادخور استاندارد: ۴۴ میلی‌متر از لبه فریم / لنگه")
        }
      ),
      WorkshopStep(
        stepNumber = 8,
        title = "چک‌لیست یراق‌آلات، دستگیره و قطعات مونتاژ",
        icon = Icons.Default.Handyman,
        content = run {
          val sashCount = design.panels.count { it.openingType != com.example.domain.model.OpeningType.FIXED }
          val mullionCount = (design.panels.size - 1).coerceAtLeast(0) + (if (design.hasTransom) 1 else 0)
          listOf(
            "دستگیره آلومینیومی استاندارد: ${CurrencyHelper.toPersianDigits(mfg.hardwarePcs["دستگیره"] ?: sashCount)} عدد",
            "لولا قابل تنظیم ۳ بعدی / بازشو: ${CurrencyHelper.toPersianDigits(mfg.hardwarePcs["لولا"] ?: (sashCount * 2))} عدد",
            "اسپانیولت و مکانیزم قفل مرکزی: ${CurrencyHelper.toPersianDigits(mfg.hardwarePcs["اسپانیولت"] ?: sashCount)} دست",
            "کانکتور اتصال مولیون زاماک: ${CurrencyHelper.toPersianDigits(mullionCount * 2)} جفت",
            "کلاهک تخلیه آب باران (Drainage Cap): ۴ عدد برای ریل پایین",
            "لقمه تراز زیر شیشه (تاکوز پلاستیکی): به تعداد کافی برای ۴ گوشه شیشه‌ها"
          )
        }
      ),
      WorkshopStep(
        stepNumber = 9,
        title = "دستورالعمل ۱۰ مرحله‌ای مونتاژ کارگاهی",
        icon = Icons.Default.ChecklistRtl,
        content = listOf(
          "۱. برش قطعات پروفیل با اره دو کله دقیق مطابق زوایای ۴۵ و ۹۰ درجه",
          "۲. ایجاد شیارهای تخلیه آب باران و سوراخ‌کاری جای دستگیره و اسپانیولت",
          "۳. جاگذاری گالوانیزه U شکل درون محفظه پروفیل و پیچ‌بندی محکم در فواصل ۲۵cm",
          "۴. جوش چهارگوش در دستگاه جوش حرارتی در دمای ۲۵۰ درجه سانتی‌گراد",
          "۵. پلیسه‌گیری و تمیزکاری درزهای گوشه جوش با شابلون و فرز دستی",
          "۶. نصب مولیون‌ها و فیتینگ‌های اتصال با پیچ‌های مقاوم گالوانیزه",
          "۷. تعبیه نوارهای درزبندی EPDM در شیارهای مربوطه بدون کشیدگی یا پیچیدگی",
          "۸. نصب لولاها، اسپانیولت، زاماک‌ها، دستگیره و اجرای رگلاژ اولیه لنگه‌ها",
          "۹. استقرار تاکوزهای شیشه، جااندازی جام شیشه دوجداره و جا زدن زهوار با چکش پلاستیکی",
          "۱۰. رگلاژ نهایی، کنترل آب‌بندی، بررسی شاقول و گونیا، پاکسازی و بسته‌بندی سلفونی"
        )
      ),
      WorkshopStep(
        stepNumber = 10,
        title = "آنالیز هزینه و برآورد زمان ساخت",
        icon = Icons.Default.PriceCheck,
        content = run {
          val profitAmount = estimate.quotationPrice.subtract(estimate.totalBaseCost)
          listOf(
            "قیمت تمام‌شده مصالح (BOM): ${CurrencyHelper.formatMoney(estimate.totalBaseCost.subtract(estimate.laborCost).subtract(estimate.transportCost), estimate.currency)}",
            "دستمزد مونتاژ و ساخت: ${CurrencyHelper.formatMoney(estimate.laborCost, estimate.currency)}",
            "هزینه حمل و اقلام متفرقه: ${CurrencyHelper.formatMoney(estimate.transportCost, estimate.currency)}",
            "سود کارگاه (${CurrencyHelper.toPersianDigits(estimate.markupPercent.toInt())}%): ${CurrencyHelper.formatMoney(profitAmount, estimate.currency)}",
            "قیمت نهایی پیش‌فاکتور: ${CurrencyHelper.formatMoney(estimate.quotationPrice, estimate.currency)}",
            "زمان تخمینی ساخت در خط تولید: ۲.۵ الی ۳.۵ ساعت کارگاهی"
          )
        }
      )
    )
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("نقشه و دستورالعمل ساخت کارگاهی", style = MaterialTheme.typography.titleMedium)
            Text(
              "${design.name} (${CurrencyHelper.toPersianDigits(design.widthMm)} × ${CurrencyHelper.toPersianDigits(design.heightMm)} mm)",
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
          IconButton(onClick = {
            val printText = buildString {
              appendLine("═══════════════════════════════════════════")
              appendLine("     دستورالعمل ساخت و نقشه کارگاهی ۱۰ مرحله‌ای")
              appendLine("═══════════════════════════════════════════")
              appendLine("طرح: ${design.name} | تاریخ: $dateStr")
              appendLine("ابعاد: ${design.widthMm} × ${design.heightMm} میلی‌متر")
              appendLine("سیستم پروفیل: ${design.profileSystem}")
              appendLine("-------------------------------------------")
              stepList.forEach { step ->
                appendLine("${step.stepNumber}. ${step.title}:")
                step.content.forEach { line ->
                  appendLine("   $line")
                }
                appendLine()
              }
              appendLine("═══════════════════════════════════════════")
              appendLine("امضای مسئول خط تولید: .....................")
              appendLine("امضای کنترل کیفیت (QC): ...................")
            }

            val sendIntent = Intent().apply {
              action = Intent.ACTION_SEND
              putExtra(Intent.EXTRA_TEXT, printText)
              type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "چاپ یا اشتراک نقشه کارگاهی"))
          }) {
            Icon(Icons.Default.Share, contentDescription = "چاپ و اشتراک‌گذاری", tint = MaterialTheme.colorScheme.primary)
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
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      item {
        Spacer(modifier = Modifier.height(4.dp))
        // Top banner
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(Icons.Default.Engineering, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Column {
              Text("برگه هدایت تولید و مونتاژ کارگاه", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              Text("شامل ۱۰ گام استاندارد از برش تا کنترل نهایی کیفیت", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }

      items(stepList.size) { index ->
        val step = stepList[index]
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary
              ) {
                Text(
                  text = "گام ${CurrencyHelper.toPersianDigits(step.stepNumber)}",
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimary,
                  fontWeight = FontWeight.Bold
                )
              }
              Icon(step.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
              Text(step.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

            step.content.forEach { line ->
              Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
              )
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

private data class WorkshopStep(
  val stepNumber: Int,
  val title: String,
  val icon: androidx.compose.ui.graphics.vector.ImageVector,
  val content: List<String>
)
