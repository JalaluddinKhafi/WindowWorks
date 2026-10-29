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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.CurrencyHelper
import com.example.core.UserRole
import com.example.data.local.AuditLogEntity
import com.example.data.local.UserEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  workshopName: String,
  baseCurrency: String,
  exchangeRateUsdToAfn: String,
  defaultMarkup: String,
  sawKerfMm: String,
  onSaveSettings: (workshopName: String, baseCurrency: String, rate: String, markup: String, kerf: String) -> Unit,
  onExportBackup: () -> String,
  onNavigateToUsers: () -> Unit,
  onNavigateToMaterialsCatalog: () -> Unit = {},
  onNavigateToProjectCalculator: () -> Unit = {}
) {
  val context = LocalContext.current
  var name by remember { mutableStateOf(workshopName) }
  var currency by remember { mutableStateOf(baseCurrency) }
  var rate by remember { mutableStateOf(exchangeRateUsdToAfn) }
  var markup by remember { mutableStateOf(defaultMarkup) }
  var kerf by remember { mutableStateOf(sawKerfMm) }
  var backupMessage by remember { mutableStateOf("") }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    contentPadding = PaddingValues(vertical = 14.dp)
  ) {
    item {
      Text("تنظیمات عمومی کارگاه و ضرایب مهندسی", style = MaterialTheme.typography.titleMedium)
    }

    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("نام کارگاه تولیدی") },
            modifier = Modifier.fillMaxWidth()
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("ارز پیش‌فرض:", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              FilterChip(
                selected = currency == "AFN",
                onClick = { currency = "AFN" },
                label = { Text("افغانی (AFN)") }
              )
              FilterChip(
                selected = currency == "USD",
                onClick = { currency = "USD" },
                label = { Text("دالر (USD)") }
              )
            }
          }

          OutlinedTextField(
            value = rate,
            onValueChange = { rate = it },
            label = { Text("نرخ برابری دالر به افغانی (۱ USD = X AFN)") },
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = markup,
            onValueChange = { markup = it },
            label = { Text("درصد پیش‌فرض سود کارگاه (مارک‌آپ %)") },
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = kerf,
            onValueChange = { kerf = it },
            label = { Text("ضخامت تیغ‌اره فارسی‌بر اره (Kerf mm)") },
            modifier = Modifier.fillMaxWidth()
          )

          Button(
            onClick = { onSaveSettings(name, currency, rate, markup, kerf) },
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("ذخیره تنظیمات")
          }
        }
      }
    }

    // Direct Modules Navigation
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedCard(
          onClick = onNavigateToMaterialsCatalog,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("اجناس و مواد", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("کاتالوگ پروفیل، شیشه و یراق", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }

        OutlinedCard(
          onClick = onNavigateToProjectCalculator,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Text("محاسبه پروژه", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("پیش‌فاکتور دسته‌جمعی اقلام", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    }

    // User Accounts Navigation
    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("کاربران محلی و سطوح دسترسی (RBAC)", style = MaterialTheme.typography.titleSmall)
            Text("مدیریت نقش‌های مدیر، حسابدار، طراح، فروش و اپراتور کارگاه", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          FilledTonalButton(onClick = onNavigateToUsers) {
            Text("مدیریت")
          }
        }
      }
    }

    // Local Backup & Restore
    item {
      Text("پشتیبان‌گیری محلی و بازیابی داده‌ها (Backup & Restore):", style = MaterialTheme.typography.titleSmall)
    }

    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            "تمامی اطلاعات کارگاه، پروژه‌ها، نقشه‌های برش، فاکتورها و دریافت‌ها به صورت یک فایل ساختاریافته استخراج می‌شود و کاملاً آفلاین قابل نگهداری است.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Button(
            onClick = {
              val jsonBackup = onExportBackup()
              val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, jsonBackup)
                type = "application/json"
              }
              context.startActivity(Intent.createChooser(sendIntent, "ذخیره یا ارسال فایل پشتیبان WindowWorkshop"))
              backupMessage = "فایل پشتیبان با موفقیت استخراج شد."
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("ایجاد و اشتراک‌گذاری فایل پشتیبان")
          }

          if (backupMessage.isNotBlank()) {
            Text(backupMessage, style = MaterialTheme.typography.labelSmall, color = Color(0xFF059669))
          }
        }
      }
    }

    // AI Assistant Information
    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF059669))
            Text("دستیار هوشمند محاسبات کارگاهی", style = MaterialTheme.typography.titleSmall, color = Color(0xFF065F46))
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            "کلیه محاسبات مهندسی و مالی توسط موتور ریاضی قطعی برنامه انجام شده و به هیچ عنوان توسط هوش مصنوعی حدس زده نمی‌شوند. دستیار صرفاً جهت توضیح فرمول‌ها، تحلیل ضایعات برش و ارائه متن پیش‌نویس به مشتریان استفاده می‌شود.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF047857)
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
  users: List<UserEntity>,
  auditLogs: List<AuditLogEntity>,
  onBack: () -> Unit
) {
  var selectedTab by remember { mutableIntStateOf(0) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("کاربران و ردپای تغییرات (Audit Logs)") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowForward, contentDescription = "بازگشت")
          }
        }
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp)
    ) {
      TabRow(selectedTabIndex = selectedTab) {
        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("کاربران کارگاه") })
        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("گزارش ردپای امنیتی") })
      }

      Spacer(modifier = Modifier.height(12.dp))

      when (selectedTab) {
        0 -> {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 30.dp)) {
            items(users) { u ->
              val roleEnum = UserRole.fromCode(u.role)
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
                    Text(u.fullName, style = MaterialTheme.typography.titleSmall)
                    Text("نام کاربری: ${u.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("نقش: ${roleEnum.titlePersian}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                  }
                  Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFECFDF5)) {
                    Text("فعال", style = MaterialTheme.typography.labelSmall, color = Color(0xFF047857), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                  }
                }
              }
            }
          }
        }
        1 -> {
          val sdf = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US) }
          LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 30.dp)) {
            items(auditLogs) { log ->
              Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(log.details, style = MaterialTheme.typography.bodyMedium)
                    Text(CurrencyHelper.toPersianDigits(sdf.format(Date(log.timestamp))), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                  }
                  Text("توسط: ${log.username} | عمل: ${log.action}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }
          }
        }
      }
    }
  }
}
