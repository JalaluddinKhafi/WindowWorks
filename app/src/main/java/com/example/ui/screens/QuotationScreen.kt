package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.core.CurrencyHelper
import com.example.core.SecurityHelper
import com.example.domain.engine.CostCalculator
import com.example.domain.model.InvoiceModel
import com.example.domain.model.WindowDesign
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotationScreen(
  design: WindowDesign,
  projectId: String,
  exchangeRateUsdToAfn: BigDecimal = BigDecimal("72.50"),
  defaultMarkupPercent: BigDecimal = BigDecimal("25.00"),
  paymentTerms: String = "۵۰٪ پیش‌پرداخت هنگام عقد قرارداد، ۴۰٪ هنگام تحویل، ۱۰٪ پس از نصب و تحویل نهایی.",
  onSaveInvoice: (InvoiceModel) -> Unit,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  var markupPercent by remember { mutableStateOf(defaultMarkupPercent) }
  var selectedCurrency by remember { mutableStateOf("AFN") }

  val estimate = remember(design, markupPercent, selectedCurrency) {
    val estAfn = CostCalculator.estimateCost(
      design = design,
      projectId = projectId,
      markupPercent = markupPercent.setScale(2, RoundingMode.HALF_UP).max(BigDecimal.ZERO),
      currency = "AFN"
    )

    if (selectedCurrency == "USD") {
      estAfn.copy(
        currency = "USD",
        profileCost = CurrencyHelper.convertCurrency(estAfn.profileCost, "AFN", "USD", exchangeRateUsdToAfn),
        glassCost = CurrencyHelper.convertCurrency(estAfn.glassCost, "AFN", "USD", exchangeRateUsdToAfn),
        hardwareCost = CurrencyHelper.convertCurrency(estAfn.hardwareCost, "AFN", "USD", exchangeRateUsdToAfn),
        reinforcementCost = CurrencyHelper.convertCurrency(estAfn.reinforcementCost, "AFN", "USD", exchangeRateUsdToAfn),
        gasketCost = CurrencyHelper.convertCurrency(estAfn.gasketCost, "AFN", "USD", exchangeRateUsdToAfn),
        consumablesCost = CurrencyHelper.convertCurrency(estAfn.consumablesCost, "AFN", "USD", exchangeRateUsdToAfn),
        laborCost = CurrencyHelper.convertCurrency(estAfn.laborCost, "AFN", "USD", exchangeRateUsdToAfn),
        transportCost = CurrencyHelper.convertCurrency(estAfn.transportCost, "AFN", "USD", exchangeRateUsdToAfn),
        installationCost = CurrencyHelper.convertCurrency(estAfn.installationCost, "AFN", "USD", exchangeRateUsdToAfn),
        totalBaseCost = CurrencyHelper.convertCurrency(estAfn.totalBaseCost, "AFN", "USD", exchangeRateUsdToAfn),
        quotationPrice = CurrencyHelper.convertCurrency(estAfn.quotationPrice, "AFN", "USD", exchangeRateUsdToAfn)
      )
    } else {
      estAfn
    }
  }

  var isSaved by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("آنالیز قیمت و پیش‌فاکتور مهندسی", style = MaterialTheme.typography.titleMedium)
            Text(design.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
              val shareText = buildQuotationShareText(design, estimate, paymentTerms)
              val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
              }
              context.startActivity(Intent.createChooser(sendIntent, "ارسال پیش‌فاکتور به مشتری"))
            }
          ) {
            Icon(Icons.Default.Share, contentDescription = "اشتراک‌گذاری پیش‌فاکتور")
          }
        }
      )
    },
    bottomBar = {
      Surface(tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("مبلغ نهایی پیش‌فاکتور:", style = MaterialTheme.typography.labelSmall)
            Text(
              text = CurrencyHelper.formatMoney(estimate.quotationPrice, estimate.currency),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.primary
            )
          }

          Button(
            onClick = {
              val invId = SecurityHelper.generateUuid()
              val invNum = "QUT-${(1000..9999).random()}"
              onSaveInvoice(
                InvoiceModel(
                  id = invId,
                  projectId = projectId,
                  invoiceNumber = invNum,
                  isFinalInvoice = false,
                  currency = estimate.currency,
                  subtotal = estimate.quotationPrice,
                  grandTotal = estimate.quotationPrice
                )
              )
              isSaved = true
            },
            enabled = !isSaved
          ) {
            Icon(if (isSaved) Icons.Default.Check else Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isSaved) "ثبت شد" else "ثبت در پروژه")
          }
        }
      }
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
      // Currency and Markup Controls Card
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("ارز پیش‌فاکتور:", style = MaterialTheme.typography.labelMedium)
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                  selected = selectedCurrency == "AFN",
                  onClick = { selectedCurrency = "AFN" },
                  label = { Text("افغانی (AFN)") }
                )
                FilterChip(
                  selected = selectedCurrency == "USD",
                  onClick = { selectedCurrency = "USD" },
                  label = { Text("دالر (USD)") }
                )
              }
            }

            HorizontalDivider()

            // Markup slider
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                "درصد سود کارگاهی (مارک‌آپ): ${CurrencyHelper.toPersianDigits(markupPercent.setScale(0, RoundingMode.HALF_UP).toInt())}%",
                style = MaterialTheme.typography.bodyMedium
              )
              Text(
                "حاشیه سود: ${CurrencyHelper.toPersianDigits(estimate.profitMarginPercent.toInt())}%",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF059669)
              )
            }
            Slider(
              value = markupPercent.toFloat().coerceIn(5f, 60f),
              onValueChange = { markupPercent = BigDecimal(it.toInt()) },
              valueRange = 5f..60f
            )
          }
        }
      }

      // Cost Breakdown Summary Card
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("تفکیک بهای تمام‌شده و هزینه‌های ساخت:", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))

            CostRow("پروفیل‌های فریم، سش و مولیون", estimate.profileCost, estimate.currency)
            CostRow("گالوانیزه تقویتی داخل مقاطع", estimate.reinforcementCost, estimate.currency)
            CostRow("شیشه دوجداره و گاز آرگون", estimate.glassCost, estimate.currency)
            CostRow("یراق‌آلات (دستگیره، لولا، اسپانیولت)", estimate.hardwareCost, estimate.currency)
            CostRow("لاستیک درزبندی EPDM", estimate.gasketCost, estimate.currency)
            CostRow("چسب، پیچ و اتصالات", estimate.consumablesCost, estimate.currency)
            CostRow("دستمزد ساخت و مونتاژ", estimate.laborCost, estimate.currency)
            CostRow("هزینه حمل تا محل پروژه", estimate.transportCost, estimate.currency)
            CostRow("دستمزد نصب و رگلاژ نهایی", estimate.installationCost, estimate.currency)

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            CostRow("بهای تمام‌شده کل کارگاه", estimate.totalBaseCost, estimate.currency, isBold = true)
            CostRow(
              "سود برآورد شده (${CurrencyHelper.toPersianDigits(markupPercent.setScale(0, RoundingMode.HALF_UP).toInt())}%)",
              estimate.quotationPrice.subtract(estimate.totalBaseCost),
              estimate.currency,
              isBold = true,
              color = Color(0xFF059669)
            )
          }
        }
      }

      // Itemized BOM (Bill of Materials)
      item {
        Text("لیست کامل اقلام مصرفی (Bill of Materials):", style = MaterialTheme.typography.titleSmall)
      }

      items(estimate.bomItems) { bom ->
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(bom.name, style = MaterialTheme.typography.bodyMedium)
              Text(
                "دسته‌بندی: ${bom.category} | کد: ${bom.code}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                "${CurrencyHelper.toPersianDigits(bom.quantity.toPlainString())} ${bom.unit}",
                style = MaterialTheme.typography.labelMedium
              )
              Text(
                CurrencyHelper.formatMoney(bom.lineTotal, bom.currency),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CostRow(
  title: String,
  amount: BigDecimal,
  currency: String,
  isBold: Boolean = false,
  color: Color = Color.Unspecified
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = title,
      style = if (isBold) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodySmall,
      color = color
    )
    Text(
      text = CurrencyHelper.formatMoney(amount, currency),
      style = if (isBold) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodySmall,
      color = color
    )
  }
}

private fun buildQuotationShareText(
  design: WindowDesign,
  est: com.example.domain.model.CostEstimate,
  paymentTerms: String
): String {
  val sb = StringBuilder()
  sb.appendLine("پیش‌فاکتور رسمی ساخت در و پنجره دوجداره")
  sb.appendLine("کارگاه تخصصی در و پنجره")
  sb.appendLine("عنوان قلم: ${design.name}")
  sb.appendLine("ابعاد: ${design.widthMm} × ${design.heightMm} میلی‌متر")
  sb.appendLine("سیستم پروفیل: ${design.profileSystem}")
  sb.appendLine("----------------------------------------")
  sb.appendLine("بهای تمام‌شده: ${CurrencyHelper.formatMoney(est.totalBaseCost, est.currency)}")
  sb.appendLine("مبلغ کل پیش‌فاکتور: ${CurrencyHelper.formatMoney(est.quotationPrice, est.currency)}")
  sb.appendLine("شرایط پرداخت: $paymentTerms")
  return sb.toString()
}
