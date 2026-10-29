package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.CurrencyHelper
import com.example.domain.model.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*

private val LABOR_COST_PER_UNIT_AFN = BigDecimal("1000.00")
private val UPVC_RATE_PER_M2_AFN = BigDecimal("4200.00")
private val ALUMINUM_RATE_PER_M2_AFN = BigDecimal("7800.00")
private val HARDWARE_OPERABLE_AFN = BigDecimal("2800.00")
private val HARDWARE_FIXED_AFN = BigDecimal("400.00")

/** Unit material cost in AFN: area × material rate + hardware allowance. Exact decimal math. */
private fun estimateUnitCost(
  materialFamily: MaterialFamily,
  widthMm: Int,
  heightMm: Int,
  openingType: OpeningType
): BigDecimal {
  val areaM2 = BigDecimal(widthMm.toLong() * heightMm.toLong())
    .divide(BigDecimal(1_000_000), 4, RoundingMode.HALF_UP)
  val rate = if (materialFamily == MaterialFamily.UPVC) UPVC_RATE_PER_M2_AFN else ALUMINUM_RATE_PER_M2_AFN
  val hardware = if (openingType == OpeningType.FIXED) HARDWARE_FIXED_AFN else HARDWARE_OPERABLE_AFN
  return areaM2.multiply(rate).add(hardware).setScale(2, RoundingMode.HALF_UP)
}

/** Selling price = cost × (1 + markup/100). */
private fun applyMarkup(cost: BigDecimal, markupPercent: BigDecimal): BigDecimal =
  cost.multiply(
    BigDecimal.ONE.add(markupPercent.divide(BigDecimal(100), 6, RoundingMode.HALF_UP))
  ).setScale(2, RoundingMode.HALF_UP)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectCalculatorScreen(
  availableProfiles: List<String> = emptyList(),
  defaultExchangeRate: BigDecimal = BigDecimal("72.50"),
  defaultMarkupPercent: BigDecimal = BigDecimal("25.00"),
  defaultCurrency: String = "AFN",
  onNavigateToDesigner: (WindowDesign) -> Unit,
  onSaveSession: (ProjectCalculationSession) -> Unit,
  onBack: () -> Unit
) {
  val context = LocalContext.current

  // Session state (all monetary values stay BigDecimal, never Double)
  var sessionTitle by remember { mutableStateOf("") }
  var customerName by remember { mutableStateOf("") }
  var customerPhone by remember { mutableStateOf("") }
  var currency by remember { mutableStateOf(if (defaultCurrency == "USD") "USD" else "AFN") }
  var exchangeRateStr by remember { mutableStateOf(defaultExchangeRate.toPlainString()) }
  var markupPercentStr by remember { mutableStateOf(defaultMarkupPercent.toPlainString()) }
  var transportFeeStr by remember { mutableStateOf("0") }
  var installFeeStr by remember { mutableStateOf("0") }
  var discountStr by remember { mutableStateOf("0") }

  // Calculation items start empty; users add their own units.
  var items by remember { mutableStateOf(emptyList<ProjectCalculationItem>()) }

  // Dialog state for adding/editing item
  var showItemDialog by remember { mutableStateOf(false) }
  var editingItem by remember { mutableStateOf<ProjectCalculationItem?>(null) }
  var showShareSummaryDialog by remember { mutableStateOf(false) }

  // Computations & Unified Currency Conversion (exact decimal arithmetic)
  val exchangeRate = CurrencyHelper.parseAmount(exchangeRateStr)
    .takeIf { it > BigDecimal.ZERO } ?: BigDecimal("72.50")
  val markupPercent = CurrencyHelper.parseAmount(markupPercentStr).max(BigDecimal.ZERO)
  val transportFee = CurrencyHelper.parseAmount(transportFeeStr)
  val installFee = CurrencyHelper.parseAmount(installFeeStr)
  val discount = CurrencyHelper.parseAmount(discountStr)

  fun switchCurrency(newCurrency: String) {
    if (newCurrency == currency || exchangeRate <= BigDecimal.ZERO) return
    fun convert(input: String): String {
      val converted = CurrencyHelper.convertCurrency(
        CurrencyHelper.parseAmount(input), currency, newCurrency, exchangeRate
      )
      return converted.setScale(if (newCurrency == "USD") 2 else 0, RoundingMode.HALF_UP).toPlainString()
    }
    transportFeeStr = convert(transportFeeStr)
    installFeeStr = convert(installFeeStr)
    discountStr = convert(discountStr)
    currency = newCurrency
  }

  // Item prices are stored in AFN; displayed/subtotal values are converted to the active currency.
  fun toActiveCurrency(afnAmount: BigDecimal): BigDecimal =
    CurrencyHelper.convertCurrency(afnAmount, "AFN", currency, exchangeRate)

  fun toOtherCurrency(activeAmount: BigDecimal): BigDecimal =
    if (currency == "USD") {
      activeAmount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP)
    } else {
      activeAmount.divide(exchangeRate, 2, RoundingMode.HALF_UP)
    }

  val totalUnits = items.sumOf { it.quantity }
  val totalAreaM2 = items.fold(BigDecimal.ZERO) { acc, item ->
    val m2 = BigDecimal(item.widthMm.toLong() * item.heightMm.toLong())
      .divide(BigDecimal(1_000_000), 4, RoundingMode.HALF_UP)
    acc.add(m2.multiply(BigDecimal(item.quantity)))
  }

  val subtotalItemsAfn = items.fold(BigDecimal.ZERO) { acc, itm -> acc.add(itm.lineTotal) }
  val activeSubtotal = toActiveCurrency(subtotalItemsAfn)

  val grandTotalActive = activeSubtotal
    .add(transportFee)
    .add(installFee)
    .subtract(discount)
    .coerceAtLeast(BigDecimal.ZERO)

  val grandTotalOther = toOtherCurrency(grandTotalActive)

  val avgPricePerM2Active = if (totalAreaM2 > BigDecimal.ZERO) {
    activeSubtotal.divide(totalAreaM2, 2, RoundingMode.HALF_UP)
  } else BigDecimal.ZERO

  val currencyLabel = if (currency == "USD") "دالر USD" else "افغانی AFN"
  val otherCurrencyCode = if (currency == "USD") "AFN" else "USD"
  val otherCurrencyLabel = if (currency == "USD") "افغانی" else "دالر"

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("محاسبه پروژه و پیش‌فاکتور دسته‌جمعی", style = MaterialTheme.typography.titleMedium)
            Text(
              "${CurrencyHelper.toPersianDigits(items.size)} ردیف | ${CurrencyHelper.toPersianDigits(totalUnits)} عدد درب و پنجره",
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
            val session = ProjectCalculationSession(
              id = "sess_${System.currentTimeMillis()}",
              sessionCode = "CALC-${System.currentTimeMillis() % 100000}",
              title = sessionTitle,
              customerName = customerName,
              customerPhone = customerPhone,
              currency = currency,
              exchangeRateUsdToAfn = exchangeRate,
              markupPercent = markupPercent,
              items = items,
              totalAreaM2 = totalAreaM2.setScale(2, RoundingMode.HALF_UP),
              totalMaterialCost = items.fold(BigDecimal.ZERO) { acc, itm ->
                acc.add(itm.unitMaterialCost.multiply(BigDecimal(itm.quantity)))
              },
              transportCost = transportFee,
              installationCost = installFee,
              discountAmount = discount,
              suggestedSellingPrice = activeSubtotal,
              totalProjectAmount = grandTotalActive
            )
            onSaveSession(session)
          }) {
            Icon(Icons.Default.Save, contentDescription = "ذخیره جلسه", tint = MaterialTheme.colorScheme.primary)
          }

          IconButton(onClick = { showShareSummaryDialog = true }) {
            Icon(Icons.Default.Share, contentDescription = "اشتراک‌گذاری برآورد")
          }
        }
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = {
          editingItem = null
          showItemDialog = true
        },
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text("افزودن درب یا پنجره") }
      )
    }
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      item {
        Spacer(modifier = Modifier.height(4.dp))
        // Customer & Project Info Card
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("مشخصات پروژه و کارفرما", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                  selected = currency == "AFN",
                  onClick = { switchCurrency("AFN") },
                  label = { Text("افغانی (AFN)") }
                )
                FilterChip(
                  selected = currency == "USD",
                  onClick = { switchCurrency("USD") },
                  label = { Text("دالر (USD)") }
                )
              }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = sessionTitle,
                onValueChange = { sessionTitle = it },
                label = { Text("عنوان برآورد") },
                singleLine = true,
                modifier = Modifier.weight(1.2f)
              )
              OutlinedTextField(
                value = customerName,
                onValueChange = { customerName = it },
                label = { Text("نام مشتری") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = customerPhone,
                onValueChange = { customerPhone = it },
                label = { Text("شماره تماس") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
              OutlinedTextField(
                value = exchangeRateStr,
                onValueChange = { exchangeRateStr = it },
                label = { Text("نرخ دالر به افغانی") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
              OutlinedTextField(
                value = markupPercentStr,
                onValueChange = { markupPercentStr = it },
                label = { Text("درصد سود %") },
                singleLine = true,
                modifier = Modifier.weight(0.8f)
              )
            }
          }
        }
      }

      // Summary Card
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("خلاصه محاسبات و متراژ کل پروژه", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("تعداد کل یونیت‌ها: ${CurrencyHelper.toPersianDigits(totalUnits)} عدد", style = MaterialTheme.typography.bodyMedium)
              Text("مساحت کل: ${CurrencyHelper.toPersianDigits(totalAreaM2.setScale(2, RoundingMode.HALF_UP).toPlainString())} مترمربع", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("جمع اقلام درب و پنجره:", style = MaterialTheme.typography.bodyMedium)
              Text(CurrencyHelper.formatMoney(activeSubtotal, currency), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("میانگین قیمت هر متر مربع:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("${CurrencyHelper.formatMoney(avgPricePerM2Active, currency)} / m²", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Logistics & Extras
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = transportFeeStr,
                onValueChange = { transportFeeStr = it },
                label = { Text("کرایه حمل ($currencyLabel)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
              OutlinedTextField(
                value = installFeeStr,
                onValueChange = { installFeeStr = it },
                label = { Text("هزینه نصب ($currencyLabel)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
              OutlinedTextField(
                value = discountStr,
                onValueChange = { discountStr = it },
                label = { Text("تخفیف ($currencyLabel)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Grand totals banner
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text("مبلغ کل نهایی برآورد ($currency):", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium)
                  Text(CurrencyHelper.formatMoney(grandTotalActive, currency), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                  Text("معادل $otherCurrencyLabel:", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                  Text(CurrencyHelper.formatMoney(grandTotalOther, otherCurrencyCode), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }

      // Items Header
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("اقلام سفارش داده شده (${CurrencyHelper.toPersianDigits(items.size)} نوع):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          Text("قیمت‌ها بر اساس سود ${CurrencyHelper.toPersianDigits(markupPercentStr)}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }

      // Line items list
      itemsIndexed(items) { index, item ->
        val totalAreaItem = BigDecimal(item.widthMm.toLong() * item.heightMm.toLong())
          .divide(BigDecimal(1_000_000), 4, RoundingMode.HALF_UP)
          .multiply(BigDecimal(item.quantity))

        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header: Item name & Badge
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                  Text(
                    text = "${CurrencyHelper.toPersianDigits(index + 1)}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                  )
                }
                Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
              }

              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (item.materialFamily == MaterialFamily.UPVC) Color(0xFFE0F2FE) else Color(0xFFFEF3C7)
              ) {
                Text(
                  text = if (item.materialFamily == MaterialFamily.UPVC) "UPVC" else "آلومینیوم",
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  style = MaterialTheme.typography.labelSmall,
                  color = if (item.materialFamily == MaterialFamily.UPVC) Color(0xFF0369A1) else Color(0xFFB45309)
                )
              }
            }

            // Specs row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                "ابعاد: ${CurrencyHelper.toPersianDigits(item.widthMm)} × ${CurrencyHelper.toPersianDigits(item.heightMm)} mm",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                "تعداد: ${CurrencyHelper.toPersianDigits(item.quantity)} عدد",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                "مساحت: ${CurrencyHelper.toPersianDigits(totalAreaItem.setScale(2, RoundingMode.HALF_UP).toPlainString())} m²",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            // Details tags
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              AssistChip(
                onClick = {},
                label = { Text(item.profileSystem, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.height(28.dp)
              )
              AssistChip(
                onClick = {},
                label = { Text(item.openingType.titlePersian, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.height(28.dp)
              )
              AssistChip(
                onClick = {},
                label = { Text(item.glassTint.titlePersian.split(" ").first(), style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.height(28.dp)
              )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

            // Pricing & actions
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  "واحد: ${CurrencyHelper.formatMoney(toActiveCurrency(item.unitPrice), currency)}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  "جمع ردیف: ${CurrencyHelper.formatMoney(toActiveCurrency(item.lineTotal), currency)}",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
              }

              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Open in 2D/3D designer
                IconButton(onClick = {
                  val design = WindowDesign(
                    id = "des_${item.id}",
                    name = item.title,
                    productType = item.productType,
                    materialFamily = item.materialFamily,
                    profileSystem = item.profileSystem,
                    frameFinish = item.frameFinish,
                    widthMm = item.widthMm,
                    heightMm = item.heightMm,
                    panels = List(item.panelCount) { pIdx ->
                      PanelConfig(
                        id = "p_${pIdx + 1}",
                        openingType = if (pIdx == 0 && item.panelCount > 1) OpeningType.FIXED else item.openingType,
                        glassTint = item.glassTint
                      )
                    }
                  )
                  onNavigateToDesigner(design)
                }) {
                  Icon(Icons.Default.DesignServices, contentDescription = "طراحی در استودیو", tint = MaterialTheme.colorScheme.primary)
                }

                // Duplicate
                IconButton(onClick = {
                  val copy = item.copy(
                    id = "item_${System.currentTimeMillis()}",
                    title = "${item.title} (تکرار)"
                  )
                  items = items + copy
                }) {
                  Icon(Icons.Default.ContentCopy, contentDescription = "تکرار ردیف")
                }

                // Edit
                IconButton(onClick = {
                  editingItem = item
                  showItemDialog = true
                }) {
                  Icon(Icons.Default.Edit, contentDescription = "ویرایش ردیف")
                }

                // Delete
                IconButton(onClick = {
                  items = items.filter { it.id != item.id }
                }) {
                  Icon(Icons.Default.DeleteOutline, contentDescription = "حذف ردیف", tint = MaterialTheme.colorScheme.error)
                }
              }
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(70.dp))
      }
    }
  }

  // Add / Edit Item Dialog
  if (showItemDialog) {
    var title by remember { mutableStateOf(editingItem?.title ?: "پنجره دوجداره استاندارد") }
    var prodType by remember { mutableStateOf(editingItem?.productType ?: ProductType.WINDOW) }
    var matFamily by remember { mutableStateOf(editingItem?.materialFamily ?: MaterialFamily.UPVC) }
    var profSys by remember { mutableStateOf(editingItem?.profileSystem ?: if (matFamily == MaterialFamily.UPVC) "Wintech 6000" else "Akpa Thermal") }
    var widthStr by remember { mutableStateOf((editingItem?.widthMm ?: 1400).toString()) }
    var heightStr by remember { mutableStateOf((editingItem?.heightMm ?: 1600).toString()) }
    var qtyStr by remember { mutableStateOf((editingItem?.quantity ?: 1).toString()) }
    var panelCount by remember { mutableIntStateOf(editingItem?.panelCount ?: 2) }
    var opType by remember { mutableStateOf(editingItem?.openingType ?: OpeningType.CASEMENT_RIGHT) }
    var glassTint by remember { mutableStateOf(editingItem?.glassTint ?: GlassTint.CLEAR) }
    var frameFinish by remember { mutableStateOf(editingItem?.frameFinish ?: FrameFinish.WHITE) }

    val defaultUpvc = listOf("Wintech 6000", "Wintech 7000", "Vistabest 60", "Hofmann 60", "Kastamonu 60", "Real Win 60")
    val defaultAlum = listOf("Akpa Thermal", "Saray Aluminum", "Aldoks Standard", "Alumax 50")
    val profileOptions = remember(matFamily, availableProfiles) {
      val defaults = if (matFamily == MaterialFamily.UPVC) defaultUpvc else defaultAlum
      (availableProfiles + defaults).filter { it.isNotBlank() }.distinct()
    }

    val dialogScrollState = rememberScrollState()

    AlertDialog(
      onDismissRequest = { showItemDialog = false },
      title = { Text(if (editingItem == null) "افزودن یونیت جدید به محاسبه" else "ویرایش یونیت") },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 500.dp)
            .verticalScroll(dialogScrollState),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("نام / مشخصه یونیت") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = prodType == ProductType.WINDOW,
              onClick = { prodType = ProductType.WINDOW },
              label = { Text("پنجره") },
              modifier = Modifier.weight(1f)
            )
            FilterChip(
              selected = prodType == ProductType.DOOR,
              onClick = { prodType = ProductType.DOOR },
              label = { Text("درب") },
              modifier = Modifier.weight(1f)
            )
            FilterChip(
              selected = matFamily == MaterialFamily.UPVC,
              onClick = {
                matFamily = MaterialFamily.UPVC
                if (!profileOptions.contains(profSys)) {
                  profSys = "Wintech 6000"
                }
              },
              label = { Text("UPVC") },
              modifier = Modifier.weight(1f)
            )
            FilterChip(
              selected = matFamily == MaterialFamily.ALUMINUM,
              onClick = {
                matFamily = MaterialFamily.ALUMINUM
                if (!profileOptions.contains(profSys)) {
                  profSys = "Akpa Thermal"
                }
              },
              label = { Text("آلومینیوم") },
              modifier = Modifier.weight(1.1f)
            )
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = widthStr,
              onValueChange = { widthStr = it },
              label = { Text("عرض (mm)") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = heightStr,
              onValueChange = { heightStr = it },
              label = { Text("ارتفاع (mm)") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = qtyStr,
              onValueChange = { qtyStr = it },
              label = { Text("تعداد") },
              singleLine = true,
              modifier = Modifier.weight(0.8f)
            )
          }

          // Dynamic Profile System selector from User's Catalog
          Text("سیستم پروفیل:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
          OutlinedTextField(
            value = profSys,
            onValueChange = { profSys = it },
            label = { Text("نام / سیستم پروفیل انتخاب‌شده") },
            placeholder = { Text("انتخاب از کاتالوگ یا تایپ دستی...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Text("پروفیل‌های ثبت شده در کاتالوگ کارگاه (لمس جهت انتخاب):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            profileOptions.forEach { opt ->
              FilterChip(
                selected = profSys.trim().equals(opt.trim(), ignoreCase = true),
                onClick = { profSys = opt },
                label = { Text(opt, style = MaterialTheme.typography.labelSmall) }
              )
            }
          }

          // Number of panels
          Text("تعداد قسمت (لنگه):", style = MaterialTheme.typography.labelSmall)
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(1, 2, 3, 4).forEach { pc ->
              FilterChip(
                selected = panelCount == pc,
                onClick = { panelCount = pc },
                label = { Text("${CurrencyHelper.toPersianDigits(pc)} لنگه") }
              )
            }
          }

          // Opening type
          Text("نوع بازشو:", style = MaterialTheme.typography.labelSmall)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf(OpeningType.FIXED, OpeningType.CASEMENT_RIGHT, OpeningType.CASEMENT_LEFT, OpeningType.TILT_AND_TURN, OpeningType.SLIDING_LEFT).forEach { ot ->
              FilterChip(
                selected = opType == ot,
                onClick = { opType = ot },
                label = { Text(ot.titlePersian, style = MaterialTheme.typography.labelSmall) }
              )
            }
          }

          // Glass Tint
          Text("رنگ و نوع شیشه:", style = MaterialTheme.typography.labelSmall)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf(GlassTint.CLEAR, GlassTint.BRONZE, GlassTint.REFLECTIVE_BLUE, GlassTint.FROSTED).forEach { gt ->
              FilterChip(
                selected = glassTint == gt,
                onClick = { glassTint = gt },
                label = { Text(gt.titlePersian.split(" ").first(), style = MaterialTheme.typography.labelSmall) }
              )
            }
          }

          // Live estimation banner in active currency
          val wVal = widthStr.toIntOrNull() ?: 1200
          val hVal = heightStr.toIntOrNull() ?: 1400
          val qVal = qtyStr.toIntOrNull()?.coerceAtLeast(1) ?: 1
          val previewCostAfn = estimateUnitCost(matFamily, wVal, hVal, opType)
          val previewPriceAfn = applyMarkup(previewCostAfn, markupPercent)
          val previewTotalAfn = previewPriceAfn.multiply(BigDecimal(qVal))
          val previewPriceActive = toActiveCurrency(previewPriceAfn)
          val previewTotalActive = toActiveCurrency(previewTotalAfn)

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                "برآورد به واحد پولی انتخابی ($currency):",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("فی واحد: ${CurrencyHelper.formatMoney(previewPriceActive, currency)}", style = MaterialTheme.typography.bodySmall)
                Text("جمع ردیف (${qVal} عدد): ${CurrencyHelper.formatMoney(previewTotalActive, currency)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      },
      confirmButton = {
        Button(onClick = {
          val w = widthStr.toIntOrNull() ?: 1200
          val h = heightStr.toIntOrNull() ?: 1400
          val q = qtyStr.toIntOrNull()?.coerceAtLeast(1) ?: 1

          // Unit cost from the engineering engines (area + hardware rate), exact decimal math.
          val unitCost = estimateUnitCost(matFamily, w, h, opType)
          val unitPrice = applyMarkup(unitCost, markupPercent)
          val totalLine = unitPrice.multiply(BigDecimal(q))

          val newItem = ProjectCalculationItem(
            id = editingItem?.id ?: "item_${System.currentTimeMillis()}",
            title = title,
            productType = prodType,
            materialFamily = matFamily,
            profileSystem = profSys.ifBlank { if (matFamily == MaterialFamily.UPVC) "Wintech 6000" else "Akpa Thermal" },
            frameFinish = frameFinish,
            widthMm = w,
            heightMm = h,
            quantity = q,
            panelCount = panelCount,
            openingType = opType,
            glazingType = GlazingType.DOUBLE_STANDARD,
            glassTint = glassTint,
            unitMaterialCost = unitCost,
            unitLaborCost = LABOR_COST_PER_UNIT_AFN,
            unitPrice = unitPrice,
            lineTotal = totalLine
          )

          items = if (editingItem == null) {
            items + newItem
          } else {
            items.map { if (it.id == newItem.id) newItem else it }
          }
          showItemDialog = false
        }) {
          Text("تأیید و ذخیره")
        }
      },
      dismissButton = {
        TextButton(onClick = { showItemDialog = false }) {
          Text("انصراف")
        }
      }
    )
  }

  // Share / Print Summary Dialog
  if (showShareSummaryDialog) {
    val dateStr = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date()) }
    val quotationText = remember(items, currency, grandTotalActive, grandTotalOther, transportFee, installFee, discount, totalAreaM2) {
      buildString {
        appendLine("══════════════════════════════════════")
        appendLine("       پیش‌فاکتور رسمی کارگاه در و پنجره دوجداره")
        appendLine("══════════════════════════════════════")
        appendLine("عنوان پروژه: $sessionTitle")
        appendLine("نام مشتری: $customerName | تلفن: $customerPhone")
        appendLine("تاریخ صدور: $dateStr")
        appendLine("واحد پولی پیش‌فاکتور: $currencyLabel")
        appendLine("--------------------------------------")
        items.forEachIndexed { i, itm ->
          val area = BigDecimal(itm.widthMm.toLong() * itm.heightMm.toLong())
            .divide(BigDecimal(1_000_000), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal(itm.quantity))
            .setScale(2, RoundingMode.HALF_UP)
          val itemUnitPriceActive = toActiveCurrency(itm.unitPrice)
          val itemLineTotalActive = toActiveCurrency(itm.lineTotal)
          appendLine("${i + 1}. ${itm.title}")
          appendLine("   ابعاد: ${itm.widthMm} × ${itm.heightMm} mm | تعداد: ${itm.quantity} عدد (${area.toPlainString()} m²)")
          appendLine("   سیستم: ${itm.profileSystem} | بازشو: ${itm.openingType.titlePersian}")
          appendLine("   قیمت فی: ${CurrencyHelper.formatMoney(itemUnitPriceActive, currency)} | جمع ردیف: ${CurrencyHelper.formatMoney(itemLineTotalActive, currency)}")
          appendLine()
        }
        appendLine("--------------------------------------")
        appendLine("مساحت کل: ${totalAreaM2.setScale(2, RoundingMode.HALF_UP).toPlainString()} مترمربع")
        appendLine("تعداد کل یونیت‌ها: $totalUnits عدد")
        appendLine("کرایه حمل: ${CurrencyHelper.formatMoney(transportFee, currency)}")
        appendLine("هزینه نصب: ${CurrencyHelper.formatMoney(installFee, currency)}")
        if (discount > BigDecimal.ZERO) {
          appendLine("تخفیف: ${CurrencyHelper.formatMoney(discount, currency)}")
        }
        appendLine("══════════════════════════════════════")
        appendLine("مبلغ نهایی قابل پرداخت: ${CurrencyHelper.formatMoney(grandTotalActive, currency)}")
        appendLine("معادل $otherCurrencyLabel: ${CurrencyHelper.formatMoney(grandTotalOther, otherCurrencyCode)} (نرخ برابری: $exchangeRateStr)")
        appendLine("══════════════════════════════════════")
      }
    }

    val shareScrollState = rememberScrollState()

    AlertDialog(
      onDismissRequest = { showShareSummaryDialog = false },
      title = { Text("پیش‌نمایش پیش‌فاکتور پروژه ($currency)") },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp)
            .verticalScroll(shareScrollState)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(12.dp)
        ) {
          Text(
            text = quotationText,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
          )
        }
      },
      confirmButton = {
        Button(onClick = {
          val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, quotationText)
            type = "text/plain"
          }
          val shareIntent = Intent.createChooser(sendIntent, "ارسال پیش‌فاکتور به مشتری")
          context.startActivity(shareIntent)
          showShareSummaryDialog = false
        }) {
          Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("ارسال و اشتراک‌گذاری")
        }
      },
      dismissButton = {
        TextButton(onClick = { showShareSummaryDialog = false }) {
          Text("بستن")
        }
      }
    )
  }
}
