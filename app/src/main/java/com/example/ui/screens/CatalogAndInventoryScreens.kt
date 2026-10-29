package com.example.ui.screens

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
import androidx.compose.ui.unit.dp
import com.example.core.CurrencyHelper
import com.example.core.SecurityHelper
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.data.local.AccessoryEntity
import com.example.data.local.GlassEntity
import com.example.data.local.ProfileEntity
import com.example.domain.model.InventoryMovementModel
import com.example.domain.model.MaterialProductModel
import com.example.domain.model.ProjectModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
  profiles: List<ProfileEntity>,
  glasses: List<GlassEntity>,
  accessories: List<AccessoryEntity>,
  canEdit: Boolean = true,
  onAddProfile: (ProfileEntity) -> Unit,
  onAddGlass: (GlassEntity) -> Unit,
  onAddAccessory: (AccessoryEntity) -> Unit
) {
  var selectedTab by remember { mutableIntStateOf(0) } // 0 = Profiles, 1 = Glasses, 2 = Accessories
  var showAddProfileDialog by remember { mutableStateOf(false) }
  var showAddGlassDialog by remember { mutableStateOf(false) }
  var showAddAccessoryDialog by remember { mutableStateOf(false) }

  Scaffold(
    floatingActionButton = {
      if (canEdit) {
        FloatingActionButton(
          onClick = {
            when (selectedTab) {
              0 -> showAddProfileDialog = true
              1 -> showAddGlassDialog = true
              else -> showAddAccessoryDialog = true
            }
          },
          containerColor = MaterialTheme.colorScheme.primary
        ) {
          Icon(Icons.Default.Add, contentDescription = "افزودن به کاتالوگ", tint = MaterialTheme.colorScheme.onPrimary)
        }
      }
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp)
    ) {
      Spacer(modifier = Modifier.height(12.dp))

      TabRow(selectedTabIndex = selectedTab) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text("پروفیل‌ها (${CurrencyHelper.toPersianDigits(profiles.size)})") }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("شیشه‌ها (${CurrencyHelper.toPersianDigits(glasses.size)})") }
        )
        Tab(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          text = { Text("یراق‌آلات (${CurrencyHelper.toPersianDigits(accessories.size)})") }
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      when (selectedTab) {
        0 -> {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
            items(profiles) { p ->
              Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(p.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                      "کد: ${p.code} | سیستم: ${p.brandSystem} (${p.materialFamily}) | نوع مقطع: ${p.sectionType}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      "${CurrencyHelper.toPersianDigits(p.purchasePrice)} ${p.currency}",
                      style = MaterialTheme.typography.labelLarge,
                      color = MaterialTheme.colorScheme.primary
                    )
                    Text("هر متر طول", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                  }
                }
              }
            }
          }
        }
        1 -> {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
            items(glasses) { g ->
              Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(g.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                      "نوع: ${g.glazingType} | ضخامت: ${CurrencyHelper.toPersianDigits(g.thicknessMm)}mm | رنگ: ${g.tint}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      "${CurrencyHelper.toPersianDigits(g.pricePerM2)} ${g.currency}",
                      style = MaterialTheme.typography.labelLarge,
                      color = MaterialTheme.colorScheme.primary
                    )
                    Text("هر مترمربع", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                  }
                }
              }
            }
          }
        }
        2 -> {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
            items(accessories) { a ->
              Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(a.name, style = MaterialTheme.typography.titleSmall)
                    Text("دسته: ${a.category} | کد: ${a.code}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      "${CurrencyHelper.toPersianDigits(a.unitPrice)} ${a.currency}",
                      style = MaterialTheme.typography.labelLarge,
                      color = MaterialTheme.colorScheme.primary
                    )
                    Text("هر ${a.unit}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (showAddProfileDialog) {
    AddProfileDialog(
      onDismiss = { showAddProfileDialog = false },
      onConfirm = { prof ->
        onAddProfile(prof)
        showAddProfileDialog = false
      }
    )
  }

  if (showAddGlassDialog) {
    AddGlassDialog(
      onDismiss = { showAddGlassDialog = false },
      onConfirm = { g ->
        onAddGlass(g)
        showAddGlassDialog = false
      }
    )
  }

  if (showAddAccessoryDialog) {
    AddAccessoryDialog(
      onDismiss = { showAddAccessoryDialog = false },
      onConfirm = { a ->
        onAddAccessory(a)
        showAddAccessoryDialog = false
      }
    )
  }
}

@Composable
fun AddGlassDialog(
  onDismiss: () -> Unit,
  onConfirm: (GlassEntity) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var glazingType by remember { mutableStateOf("دوجداره") }
  var thickness by remember { mutableStateOf("20") }
  var tint by remember { mutableStateOf("ساده شفاف") }
  var price by remember { mutableStateOf("650.00") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("افزودن نوع شیشه به کاتالوگ") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("عنوان شیشه (مثلاً دوجداره ۴+۱۶+۴) *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = thickness,
            onValueChange = { thickness = it },
            label = { Text("ضخامت کل (mm)") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = tint,
            onValueChange = { tint = it },
            label = { Text("رنگ / نوع") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }
        OutlinedTextField(
          value = price,
          onValueChange = { price = it },
          label = { Text("قیمت خرید هر مترمربع (افغانی) *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        enabled = title.isNotBlank() && CurrencyHelper.parseAmount(price) > java.math.BigDecimal.ZERO,
        onClick = {
          onConfirm(
            GlassEntity(
              id = SecurityHelper.generateUuid(),
              title = title.trim(),
              glazingType = glazingType,
              thicknessMm = CurrencyHelper.parseAmount(thickness).toInt().coerceAtLeast(1),
              tint = tint.trim(),
              pricePerM2 = CurrencyHelper.parseAmount(price).toPlainString()
            )
          )
        }
      ) { Text("ذخیره در کاتالوگ") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
  )
}

@Composable
fun AddAccessoryDialog(
  onDismiss: () -> Unit,
  onConfirm: (AccessoryEntity) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var category by remember { mutableStateOf("دستگیره") }
  var unit by remember { mutableStateOf("عدد") }
  var price by remember { mutableStateOf("150.00") }

  val categories = listOf("دستگیره", "لولا", "قفل", "غلطک", "لاستیک", "پیچ", "گالوانیزه", "اتصالات", "سایر")
  val units = listOf("عدد", "دست", "متر", "بسته", "شاخه")

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("افزودن یراق‌آلات به کاتالوگ") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("نام کالا *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        Text("دسته:", style = MaterialTheme.typography.labelMedium)
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          categories.forEach { c ->
            FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c, style = MaterialTheme.typography.labelSmall) })
          }
        }
        Text("واحد:", style = MaterialTheme.typography.labelMedium)
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          units.forEach { u ->
            FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(u, style = MaterialTheme.typography.labelSmall) })
          }
        }
        OutlinedTextField(
          value = price,
          onValueChange = { price = it },
          label = { Text("قیمت خرید هر $unit (افغانی) *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        enabled = name.isNotBlank() && CurrencyHelper.parseAmount(price) > java.math.BigDecimal.ZERO,
        onClick = {
          onConfirm(
            AccessoryEntity(
              id = SecurityHelper.generateUuid(),
              code = "ACC-${SecurityHelper.generateUuid().take(6).uppercase()}",
              name = name.trim(),
              category = category,
              unit = unit,
              unitPrice = CurrencyHelper.parseAmount(price).toPlainString()
            )
          )
        }
      ) { Text("ذخیره در کاتالوگ") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
  )
}

@Composable
fun AddProfileDialog(
  onDismiss: () -> Unit,
  onConfirm: (ProfileEntity) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var brand by remember { mutableStateOf("Wintech 6000") }
  var sectionType by remember { mutableStateOf("فریم") }
  var priceStr by remember { mutableStateOf("200.00") }
  var matFamily by remember { mutableStateOf("UPVC") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("افزودن مقطع پروفیل جدید به کاتالوگ") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("نام مقطع (مثلاً فریم بازسازی وینتک)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = brand,
          onValueChange = { brand = it },
          label = { Text("برند / سری پروفیل") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = priceStr,
          onValueChange = { priceStr = it },
          label = { Text("قیمت خرید هر متر (افغانی)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank()) {
            val id = SecurityHelper.generateUuid()
            onConfirm(
              ProfileEntity(
                id = id,
                code = "PRF-${(100..999).random()}",
                name = name.trim(),
                materialFamily = matFamily,
                brandSystem = brand.trim(),
                sectionType = sectionType,
                purchasePrice = priceStr.trim()
              )
            )
          }
        }
      ) { Text("ذخیره در کاتالوگ") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("انصراف") }
    }
  )
}

@Composable
fun InventoryScreen(
  movements: List<InventoryMovementModel>,
  products: List<MaterialProductModel>,
  projects: List<ProjectModel> = emptyList(),
  canRecord: Boolean = true,
  onRecordMovement: (InventoryMovementModel) -> Unit
) {
  var showMovementDialog by remember { mutableStateOf(false) }

  Scaffold(
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showMovementDialog = true },
        containerColor = MaterialTheme.colorScheme.primary
      ) {
        Icon(Icons.Default.Add, contentDescription = "ثبت ورود/خروج کالا", tint = MaterialTheme.colorScheme.onPrimary)
      }
    }
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(vertical = 14.dp)
    ) {
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text("کنترل موجودی انبار کارگاه", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              "ثبت ورود شاخه‌های پروفیل و متریال از تامین‌کننده، خروج جهت ساخت پروژه‌ها و پایش ضایعات",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
        }
      }

      // Current stock levels + low-stock warning
      item {
        Text("موجودی فعلی کالاها:", style = MaterialTheme.typography.titleSmall)
      }

      if (products.isEmpty()) {
        item {
          Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
            Text("برای کنترل انبار ابتدا در بخش «اجناس و مواد» کالا ثبت کنید.", color = MaterialTheme.colorScheme.outline)
          }
        }
      } else {
        items(products, key = { it.id }) { p ->
          val stock = p.stockQty
          val isLow = stock <= p.minStockLevel
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (isLow) Color(0xFFFFF7ED) else MaterialTheme.colorScheme.surface
            )
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.titleSmall)
                Text(
                  "کد: ${p.code} | ${p.category}" + if (isLow) " | ⚠ زیر حد موجودی" else "",
                  style = MaterialTheme.typography.labelSmall,
                  color = if (isLow) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Text(
                "${CurrencyHelper.toPersianDigits(stock.stripTrailingZeros().toPlainString())} ${p.unit}",
                style = MaterialTheme.typography.titleSmall,
                color = if (isLow) Color(0xFFB45309) else MaterialTheme.colorScheme.primary
              )
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(6.dp))
        Text("گردش اخیر انبار (کاردکس ورود و خروج):", style = MaterialTheme.typography.titleSmall)
      }

      if (movements.isEmpty()) {
        item {
          Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("هنوز تراکنش انباری ثبت نشده است.", color = MaterialTheme.colorScheme.outline)
          }
        }
      } else {
        items(movements, key = { it.id }) { m ->
          val isEntry = m.isInbound
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(m.materialName, style = MaterialTheme.typography.titleSmall)
                Text(
                  text = InventoryMovementModel.titleFor(m.movementType),
                  style = MaterialTheme.typography.bodySmall,
                  color = if (isEntry) Color(0xFF059669) else Color(0xFFDC2626)
                )
                if (m.notes.isNotBlank()) {
                  Text(m.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
              Text(
                text = "${if (isEntry) "+" else "-"}${CurrencyHelper.toPersianDigits(m.quantity.stripTrailingZeros().toPlainString())} ${m.unit}",
                style = MaterialTheme.typography.titleMedium,
                color = if (isEntry) Color(0xFF059669) else Color(0xFFDC2626)
              )
            }
          }
        }
      }
    }
  }

  if (showMovementDialog) {
    RecordMovementDialog(
      products = products,
      projects = projects,
      onDismiss = { showMovementDialog = false },
      onConfirm = { m ->
        onRecordMovement(m)
        showMovementDialog = false
      }
    )
  }
}

@Composable
fun RecordMovementDialog(
  products: List<MaterialProductModel>,
  projects: List<ProjectModel> = emptyList(),
  onDismiss: () -> Unit,
  onConfirm: (InventoryMovementModel) -> Unit
) {
  var selectedProduct by remember { mutableStateOf(products.firstOrNull()) }
  var qtyStr by remember { mutableStateOf("") }
  var movementType by remember { mutableStateOf(InventoryMovementModel.TYPE_PURCHASE_IN) }
  var selectedProjectId by remember { mutableStateOf<String?>(null) }
  var notes by remember { mutableStateOf("") }
  var error by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("ثبت حواله ورود یا خروج انبار") },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 480.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        if (products.isEmpty()) {
          Text(
            "هیچ کالایی در انبار تعریف نشده است. ابتدا از بخش «اجناس و مواد» کالا ثبت کنید.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
          )
        }

        Text("کالا:", style = MaterialTheme.typography.labelMedium)
        Column {
          products.forEach { p ->
            Row(verticalAlignment = Alignment.CenterVertically) {
              RadioButton(
                selected = selectedProduct?.id == p.id,
                onClick = { selectedProduct = p }
              )
              Text(
                "${p.name} (موجودی: ${p.stockQty.stripTrailingZeros().toPlainString()} ${p.unit})",
                style = MaterialTheme.typography.bodySmall
              )
            }
          }
        }

        Text("نوع حواله:", style = MaterialTheme.typography.labelMedium)
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf(
            InventoryMovementModel.TYPE_PURCHASE_IN to "ورود خرید (+)",
            InventoryMovementModel.TYPE_PROJECT_OUT to "خروج پروژه (-)",
            InventoryMovementModel.TYPE_SCRAP_OUT to "ضایعات (-)"
          ).forEach { (type, label) ->
            FilterChip(
              selected = movementType == type,
              onClick = { movementType = type },
              label = { Text(label, style = MaterialTheme.typography.labelSmall) }
            )
          }
        }

        OutlinedTextField(
          value = qtyStr,
          onValueChange = { qtyStr = it; error = null },
          label = { Text("مقدار (${selectedProduct?.unit ?: "واحد"}) *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        if (movementType == InventoryMovementModel.TYPE_PROJECT_OUT) {
          Text("مصرف در پروژه:", style = MaterialTheme.typography.labelMedium)
          Column {
            projects.forEach { p ->
              Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                  selected = selectedProjectId == p.id,
                  onClick = { selectedProjectId = p.id }
                )
                Text("${p.projectNumber} - ${p.title}", style = MaterialTheme.typography.bodySmall)
              }
            }
          }
        }

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("توضیحات و منبع حواله") },
          modifier = Modifier.fillMaxWidth()
        )

        error?.let {
          Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
      }
    },
    confirmButton = {
      Button(
        enabled = selectedProduct != null,
        onClick = {
          val product = selectedProduct ?: return@Button
          val qty = CurrencyHelper.parseAmount(qtyStr)
          if (qty <= java.math.BigDecimal.ZERO) {
            error = "مقدار باید بزرگتر از صفر باشد."
            return@Button
          }
          if (movementType != InventoryMovementModel.TYPE_PURCHASE_IN &&
            qty > product.stockQty
          ) {
            error = "موجودی کافی نیست. موجودی فعلی: ${product.stockQty.stripTrailingZeros().toPlainString()} ${product.unit}"
            return@Button
          }
          onConfirm(
            InventoryMovementModel(
              id = SecurityHelper.generateUuid(),
              materialId = product.id,
              materialName = product.name,
              movementType = movementType,
              quantity = qty,
              unit = product.unit,
              projectId = if (movementType == InventoryMovementModel.TYPE_PROJECT_OUT) selectedProjectId else null,
              notes = notes.trim()
            )
          )
        }
      ) { Text("ثبت حواله") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("انصراف") }
    }
  )
}
