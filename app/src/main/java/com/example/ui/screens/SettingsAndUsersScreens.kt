package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.core.AppSettings
import com.example.core.CurrencyHelper
import com.example.core.SecurityHelper
import com.example.core.UserRole
import com.example.domain.model.AuditLogModel
import com.example.domain.model.RestoreResult
import com.example.domain.model.UserModel
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  settings: AppSettings,
  currentRole: UserRole,
  onSaveSettings: suspend (AppSettings) -> String?,
  onExportBackup: suspend () -> String,
  onRestoreBackup: suspend (String) -> RestoreResult,
  canBackup: Boolean,
  onChangePassword: () -> Unit,
  onLogout: () -> Unit,
  onNavigateToUsers: () -> Unit,
  onNavigateToMaterialsCatalog: () -> Unit = {},
  onNavigateToProjectCalculator: () -> Unit = {}
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var name by remember { mutableStateOf("") }
  var currency by remember { mutableStateOf("AFN") }
  var rate by remember { mutableStateOf("") }
  var markup by remember { mutableStateOf("") }
  var kerf by remember { mutableStateOf("") }
  var endTrim by remember { mutableStateOf("") }

  var message by remember { mutableStateOf("") }
  var isError by remember { mutableStateOf(false) }
  var isBusy by remember { mutableStateOf(false) }
  var pendingRestoreJson by remember { mutableStateOf<String?>(null) }

  val canEditSettings = currentRole.canEditCatalogAndPrices()

  fun showMessage(text: String, error: Boolean = false) {
    message = text
    isError = error
  }

  // Re-sync the form whenever the stored settings change (initial load, save, restore).
  LaunchedEffect(settings) {
    name = settings.workshopName
    currency = settings.baseCurrency
    rate = settings.exchangeRateUsdToAfn.toPlainString()
    markup = settings.defaultMarkupPercent.toPlainString()
    kerf = settings.sawKerfMm.toString()
    endTrim = settings.endTrimMm.toString()
  }

  // System document picker used by "بازیابی از فایل پشتیبان".
  val restorePicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri == null) {
      showMessage("فایلی انتخاب نشد.", true)
      return@rememberLauncherForActivityResult
    }
    isBusy = true
    scope.launch {
      val json = runCatching {
        withContext(Dispatchers.IO) {
          context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }
      }.getOrNull()
      isBusy = false
      if (json.isNullOrBlank()) {
        showMessage("محتوای فایل قابل خواندن نبود یا فایل خالی است.", true)
      } else {
        pendingRestoreJson = json
      }
    }
  }

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

    if (message.isNotBlank()) {
      item {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (isError) MaterialTheme.colorScheme.errorContainer else Color(0xFFECFDF5),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF047857),
            modifier = Modifier.padding(12.dp)
          )
        }
      }
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
            enabled = canEditSettings,
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
                onClick = { if (canEditSettings) currency = "AFN" },
                enabled = canEditSettings,
                label = { Text("افغانی (AFN)") }
              )
              FilterChip(
                selected = currency == "USD",
                onClick = { if (canEditSettings) currency = "USD" },
                enabled = canEditSettings,
                label = { Text("دالر (USD)") }
              )
            }
          }

          OutlinedTextField(
            value = rate,
            onValueChange = { rate = it },
            label = { Text("نرخ برابری دالر به افغانی (۱ USD = X AFN)") },
            enabled = canEditSettings,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = markup,
            onValueChange = { markup = it },
            label = { Text("درصد پیش‌فرض سود کارگاه (مارک‌آپ %)") },
            enabled = canEditSettings,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = kerf,
              onValueChange = { kerf = it },
              label = { Text("ضخامت تیغ‌اره (Kerf mm)") },
              enabled = canEditSettings,
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = endTrim,
              onValueChange = { endTrim = it },
              label = { Text("ضایعات ته شاخه (mm)") },
              enabled = canEditSettings,
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
          }

          Button(
            onClick = {
              val rateValue = CurrencyHelper.parseAmount(rate)
              if (rateValue <= BigDecimal.ZERO) {
                showMessage("نرخ برابری دالر باید بزرگتر از صفر باشد.", true)
                return@Button
              }
              isBusy = true
              scope.launch {
                val error = onSaveSettings(
                  settings.copy(
                    workshopName = name.trim().ifBlank { settings.workshopName },
                    baseCurrency = currency,
                    exchangeRateUsdToAfn = rateValue,
                    defaultMarkupPercent = CurrencyHelper.parseAmount(markup).max(BigDecimal.ZERO),
                    sawKerfMm = CurrencyHelper.parseAmount(kerf).toInt().coerceIn(0, 50),
                    endTrimMm = CurrencyHelper.parseAmount(endTrim).toInt().coerceIn(0, 500)
                  )
                )
                isBusy = false
                if (error == null) showMessage("تنظیمات با موفقیت ذخیره شد.")
                else showMessage(error, true)
              }
            },
            enabled = canEditSettings && !isBusy,
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("ذخیره تنظیمات")
          }

          if (!canEditSettings) {
            Text(
              "نقش «${currentRole.titlePersian}» فقط اجازه مشاهده تنظیمات را دارد.",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.error
            )
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
          enabled = currentRole.canEditCatalogAndPrices(),
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
          enabled = currentRole.canCreateInvoices(),
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

    // Account section: password + logout
    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("حساب کاربری جاری", style = MaterialTheme.typography.titleSmall)
          Text(
            "نقش شما: ${currentRole.titlePersian}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onChangePassword, modifier = Modifier.weight(1f)) {
              Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("تغییر رمز عبور")
            }
            OutlinedButton(onClick = onLogout, modifier = Modifier.weight(1f)) {
              Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("خروج از حساب")
            }
          }
        }
      }
    }

    // User Accounts Navigation
    if (currentRole.canManageUsers()) {
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
            Column(modifier = Modifier.weight(1f)) {
              Text("کاربران محلی و سطوح دسترسی (RBAC)", style = MaterialTheme.typography.titleSmall)
              Text("مدیریت نقش‌های مدیر، حسابدار، طراح، فروش و اپراتور کارگاه", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalButton(onClick = onNavigateToUsers) {
              Text("مدیریت")
            }
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
            "تمامی اطلاعات کارگاه (مشتریان، پروژه‌ها، طراحی‌ها، کالاها، فاکتورها، دریافت‌ها، هزینه‌ها، انبار، کاربران و تنظیمات) در یک فایل JSON استخراج می‌شود و بدون نیاز به اینترنت قابل نگهداری و بازیابی است.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Button(
            enabled = canBackup && !isBusy,
            onClick = {
              isBusy = true
              scope.launch {
                val jsonBackup = onExportBackup()
                isBusy = false
                val sendIntent = Intent().apply {
                  action = Intent.ACTION_SEND
                  putExtra(Intent.EXTRA_TEXT, jsonBackup)
                  type = "application/json"
                }
                context.startActivity(
                  Intent.createChooser(sendIntent, "ذخیره یا ارسال فایل پشتیبان WindowWorkshop")
                )
                showMessage("فایل پشتیبان با موفقیت استخراج شد.")
              }
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("ایجاد و اشتراک‌گذاری فایل پشتیبان")
          }

          FilledTonalButton(
            enabled = canBackup && !isBusy,
            onClick = {
              // *.json and generic text are both accepted by many file providers.
              restorePicker.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"))
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("بازیابی از فایل پشتیبان")
          }

          if (!canBackup) {
            Text(
              "فقط مدیر کارگاه اجازه پشتیبان‌گیری و بازیابی اطلاعات را دارد.",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.error
            )
          }
        }
      }
    }

    // Data model notes
    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF059669))
            Text("اصالت محاسبات", style = MaterialTheme.typography.titleSmall, color = Color(0xFF065F46))
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            "همه محاسبات مهندسی و مالی صرفاً توسط موتورهای قطعی برنامه (هندسه، برش، BOM و هزینه) و با دقت BigDecimal انجام می‌شوند و هیچ عددی به صورت تخمینی یا تصادفی ذخیره نمی‌شود. برنامه کاملاً آفلاین و بدون اتصال به اینترنت کار می‌کند.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF047857)
          )
        }
      }
    }
  }

  // Restore confirmation (destructive: replaces every record)
  pendingRestoreJson?.let { json ->
    AlertDialog(
      onDismissRequest = { pendingRestoreJson = null },
      title = { Text("تایید بازیابی اطلاعات") },
      text = {
        Text(
          "با بازیابی، تمام اطلاعات فعلی برنامه حذف و با محتوای فایل پشتیبان جایگزین می‌شود. این عملیات قابل بازگشت نیست. ادامه می‌دهید؟"
        )
      },
      confirmButton = {
        Button(
          onClick = {
            pendingRestoreJson = null
            isBusy = true
            scope.launch {
              when (val result = onRestoreBackup(json)) {
                is RestoreResult.Success -> {
                  isBusy = false
                  showMessage(result.summary)
                }
                is RestoreResult.Failure -> {
                  isBusy = false
                  showMessage(result.message, true)
                }
              }
            }
          }
        ) { Text("بله، بازیابی کن") }
      },
      dismissButton = {
        TextButton(onClick = { pendingRestoreJson = null }) { Text("انصراف") }
      }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
  users: List<UserModel>,
  auditLogs: List<AuditLogModel>,
  currentUserId: String?,
  onSaveUser: suspend (UserModel, String?) -> String?,
  onDeleteUser: suspend (String) -> String?,
  onBack: () -> Unit
) {
  val scope = rememberCoroutineScope()
  var selectedTab by remember { mutableIntStateOf(0) }
  var editingUser by remember { mutableStateOf<UserModel?>(null) }
  var showUserDialog by remember { mutableStateOf(false) }
  var feedback by remember { mutableStateOf<String?>(null) }
  var feedbackIsError by remember { mutableStateOf(false) }

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
    },
    floatingActionButton = {
      if (selectedTab == 0) {
        FloatingActionButton(onClick = {
          editingUser = null
          showUserDialog = true
        }) {
          Icon(Icons.Default.PersonAdd, contentDescription = "افزودن کاربر")
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
      TabRow(selectedTabIndex = selectedTab) {
        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("کاربران کارگاه") })
        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("گزارش ردپای امنیتی") })
      }

      feedback?.let {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          it,
          style = MaterialTheme.typography.bodySmall,
          color = if (feedbackIsError) MaterialTheme.colorScheme.error else Color(0xFF047857)
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      when (selectedTab) {
        0 -> {
          if (users.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
              Text("کاربری ثبت نشده است.", color = MaterialTheme.colorScheme.outline)
            }
          } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
              items(users, key = { it.id }) { u ->
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
                      Text("نقش: ${u.role.titlePersian}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                      if (u.id == currentUserId) {
                        Text("(حساب جاری شما)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                      }
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (u.isActive) Color(0xFFECFDF5) else Color(0xFFFEE2E2)
                      ) {
                        Text(
                          if (u.isActive) "فعال" else "غیرفعال",
                          style = MaterialTheme.typography.labelSmall,
                          color = if (u.isActive) Color(0xFF047857) else Color(0xFFB91C1C),
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                      }
                      Row {
                        IconButton(onClick = {
                          editingUser = u
                          showUserDialog = true
                        }) {
                          Icon(Icons.Default.Edit, contentDescription = "ویرایش کاربر")
                        }
                        if (u.id != currentUserId) {
                          IconButton(onClick = {
                            scope.launch {
                              val error = onDeleteUser(u.id)
                              feedbackIsError = error != null
                              feedback = error ?: "کاربر ${u.username} حذف شد."
                            }
                          }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "حذف کاربر", tint = MaterialTheme.colorScheme.error)
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
        1 -> {
          val sdf = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US) }
          if (auditLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
              Text("رکوردی در گزارش ثبت نشده است.", color = MaterialTheme.colorScheme.outline)
            }
          } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 30.dp)) {
              items(auditLogs, key = { it.id }) { log ->
                Card(
                  shape = RoundedCornerShape(10.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                  Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                      Text(log.details, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
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

  if (showUserDialog) {
    UserEditorDialog(
      user = editingUser,
      isSelf = editingUser?.id == currentUserId,
      onDismiss = { showUserDialog = false },
      onConfirm = { user, password ->
        scope.launch {
          val error = onSaveUser(user, password)
          if (error == null) {
            showUserDialog = false
            feedbackIsError = false
            feedback = if (editingUser == null) "کاربر جدید ایجاد شد." else "تغییرات کاربر ذخیره شد."
          } else {
            feedbackIsError = true
            feedback = error
          }
        }
      }
    )
  }
}

@Composable
private fun UserEditorDialog(
  user: UserModel?,
  isSelf: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (UserModel, String?) -> Unit
) {
  var fullName by remember { mutableStateOf(user?.fullName ?: "") }
  var username by remember { mutableStateOf(user?.username ?: "") }
  var role by remember { mutableStateOf(user?.role ?: UserRole.SALES) }
  var isActive by remember { mutableStateOf(user?.isActive ?: true) }
  var password by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var error by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (user == null) "افزودن کاربر جدید" else "ویرایش کاربر") },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 480.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = fullName,
          onValueChange = { fullName = it; error = null },
          label = { Text("نام و نام خانوادگی *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = username,
          onValueChange = { username = it; error = null },
          label = { Text("نام کاربری *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Text("نقش کاربر:", style = MaterialTheme.typography.labelMedium)
        UserRole.entries.forEach { r ->
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            RadioButton(
              selected = role == r,
              onClick = { if (!isSelf || r == UserRole.OWNER_ADMIN) role = r }
            )
            Column {
              Text(r.titlePersian, style = MaterialTheme.typography.bodyMedium)
              Text(r.descriptionPersian, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }

        OutlinedTextField(
          value = password,
          onValueChange = { password = it; error = null },
          label = {
            Text(if (user == null) "رمز عبور *" else "رمز عبور جدید (خالی = بدون تغییر)")
          },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = confirmPassword,
          onValueChange = { confirmPassword = it; error = null },
          label = { Text("تکرار رمز عبور") },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          modifier = Modifier.fillMaxWidth()
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
          Switch(
            checked = isActive,
            onCheckedChange = { isActive = it },
            enabled = !isSelf
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("حساب فعال باشد", style = MaterialTheme.typography.bodyMedium)
        }

        error?.let {
          Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
      }
    },
    confirmButton = {
      Button(onClick = {
        if (fullName.isBlank() || username.isBlank()) {
          error = "نام و نام کاربری الزامی است."
          return@Button
        }
        if (password.isNotBlank() && password != confirmPassword) {
          error = "رمز عبور و تکرار آن یکسان نیست."
          return@Button
        }
        if (user == null && password.length < 6) {
          error = "رمز عبور باید حداقل ۶ کاراکتر باشد."
          return@Button
        }
        onConfirm(
          UserModel(
            id = user?.id ?: SecurityHelper.generateUuid(),
            username = username.trim(),
            fullName = fullName.trim(),
            role = role,
            isActive = isActive,
            createdAt = user?.createdAt ?: System.currentTimeMillis()
          ),
          password.ifBlank { null }
        )
      }) { Text("ذخیره") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("انصراف") }
    }
  )
}


