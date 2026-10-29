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
import com.example.data.local.AccessoryEntity
import com.example.data.local.GlassEntity
import com.example.data.local.InventoryMovementEntity
import com.example.data.local.ProfileEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
  profiles: List<ProfileEntity>,
  glasses: List<GlassEntity>,
  accessories: List<AccessoryEntity>,
  onAddProfile: (ProfileEntity) -> Unit,
  onAddGlass: (GlassEntity) -> Unit,
  onAddAccessory: (AccessoryEntity) -> Unit
) {
  var selectedTab by remember { mutableIntStateOf(0) } // 0 = Profiles, 1 = Glasses, 2 = Accessories
  var showAddProfileDialog by remember { mutableStateOf(false) }

  Scaffold(
    floatingActionButton = {
      if (selectedTab == 0) {
        FloatingActionButton(
          onClick = { showAddProfileDialog = true },
          containerColor = MaterialTheme.colorScheme.primary
        ) {
          Icon(Icons.Default.Add, contentDescription = "افزودن مقطع پروفیل", tint = MaterialTheme.colorScheme.onPrimary)
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
  movements: List<InventoryMovementEntity>,
  onRecordMovement: (InventoryMovementEntity) -> Unit
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

      item {
        Text("گردش اخیر انبار (کاردکس ورود و خروج):", style = MaterialTheme.typography.titleSmall)
      }

      if (movements.isEmpty()) {
        item {
          Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("هنوز تراکنش انباری ثبت نشده است.", color = MaterialTheme.colorScheme.outline)
          }
        }
      } else {
        items(movements) { m ->
          val isEntry = m.movementType.contains("IN")
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
                  text = when (m.movementType) {
                    "PURCHASE_IN" -> "ورود خرید از تامین‌کننده"
                    "PROJECT_OUT" -> "مصرف در پروژه"
                    "SCRAP_OUT" -> "ضایعات کارگاهی"
                    else -> "تعدیل انبار"
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = if (isEntry) Color(0xFF059669) else Color(0xFFDC2626)
                )
                if (m.notes.isNotBlank()) {
                  Text(m.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
              Text(
                text = "${if (isEntry) "+" else "-"}${CurrencyHelper.toPersianDigits(m.quantity)} ${m.unit}",
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
  onDismiss: () -> Unit,
  onConfirm: (InventoryMovementEntity) -> Unit
) {
  var matName by remember { mutableStateOf("شاخه ۶ متری فریم وینتک") }
  var qtyStr by remember { mutableStateOf("10") }
  var unit by remember { mutableStateOf("شاخه") }
  var movementType by remember { mutableStateOf("PURCHASE_IN") }
  var notes by remember { mutableStateOf("ورود بار جدید کارخانه") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("ثبت حواله ورود یا خروج انبار") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = matName,
          onValueChange = { matName = it },
          label = { Text("نام متریال / کالا *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = movementType == "PURCHASE_IN",
            onClick = { movementType = "PURCHASE_IN" },
            label = { Text("ورود خرید (+)") }
          )
          FilterChip(
            selected = movementType == "PROJECT_OUT",
            onClick = { movementType = "PROJECT_OUT" },
            label = { Text("خروج پروژه (-)") }
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = qtyStr,
            onValueChange = { qtyStr = it },
            label = { Text("مقدار *") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = unit,
            onValueChange = { unit = it },
            label = { Text("واحد") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("توضیحات و منبع حواله") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (matName.isNotBlank() && qtyStr.isNotBlank()) {
            onConfirm(
              InventoryMovementEntity(
                id = SecurityHelper.generateUuid(),
                materialId = "mat_${matName.hashCode()}",
                materialName = matName.trim(),
                movementType = movementType,
                quantity = qtyStr.trim(),
                unit = unit.trim(),
                notes = notes.trim()
              )
            )
          }
        }
      ) { Text("ثبت حواله") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("انصراف") }
    }
  )
}
