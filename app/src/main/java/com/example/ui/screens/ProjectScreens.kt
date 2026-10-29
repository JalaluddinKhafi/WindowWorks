package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import com.example.domain.model.*
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
  projects: List<ProjectModel>,
  customers: List<CustomerModel>,
  onSelectProject: (String) -> Unit,
  onSaveProject: (ProjectModel) -> Unit
) {
  var showAddDialog by remember { mutableStateOf(false) }
  var selectedStatusFilter by remember { mutableStateOf<ProjectStatus?>(null) }
  var searchQuery by remember { mutableStateOf("") }

  val filteredProjects = remember(projects, selectedStatusFilter, searchQuery) {
    projects.filter { p ->
      val matchesStatus = selectedStatusFilter == null || p.status == selectedStatusFilter
      val matchesSearch = searchQuery.isBlank() ||
          p.title.contains(searchQuery, ignoreCase = true) ||
          p.projectNumber.contains(searchQuery, ignoreCase = true) ||
          p.siteAddress.contains(searchQuery, ignoreCase = true)
      matchesStatus && matchesSearch
    }
  }

  Scaffold(
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showAddDialog = true },
        containerColor = MaterialTheme.colorScheme.primary
      ) {
        Icon(Icons.Default.Add, contentDescription = "پروژه جدید", tint = MaterialTheme.colorScheme.onPrimary)
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

      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("جستجوی نام یا شماره پروژه...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Status Filter Chips
      ScrollableTabRow(
        selectedTabIndex = if (selectedStatusFilter == null) 0 else selectedStatusFilter!!.ordinal + 1,
        edgePadding = 0.dp,
        divider = {}
      ) {
        Tab(
          selected = selectedStatusFilter == null,
          onClick = { selectedStatusFilter = null },
          text = { Text("همه") }
        )
        ProjectStatus.entries.forEach { status ->
          Tab(
            selected = selectedStatusFilter == status,
            onClick = { selectedStatusFilter = status },
            text = { Text(status.titlePersian) }
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      if (filteredProjects.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "پروژه‌ای مطابق با فیلتر یافت نشد.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
          )
        }
      } else {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(bottom = 80.dp)
        ) {
          items(filteredProjects) { project ->
            ProjectCard(project = project, onClick = { onSelectProject(project.id) })
          }
        }
      }
    }
  }

  if (showAddDialog) {
    CreateProjectDialog(
      customers = customers,
      onDismiss = { showAddDialog = false },
      onConfirm = { newProj ->
        onSaveProject(newProj)
        showAddDialog = false
      }
    )
  }
}

@Composable
fun CreateProjectDialog(
  customers: List<CustomerModel>,
  onDismiss: () -> Unit,
  onConfirm: (ProjectModel) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var siteAddress by remember { mutableStateOf("") }
  var selectedCustomerId by remember { mutableStateOf(customers.firstOrNull()?.id ?: "") }
  var selectedCurrency by remember { mutableStateOf("AFN") }
  var notes by remember { mutableStateOf("") }
  var error by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("تعریف پروژه کارگاهی جدید") },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        if (error.isNotBlank()) {
          Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("عنوان پروژه *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = siteAddress,
          onValueChange = { siteAddress = it },
          label = { Text("نشانی محل کارگاه یا نصب") },
          modifier = Modifier.fillMaxWidth()
        )

        // Customer selection
        if (customers.isNotEmpty()) {
          Text("مشتری کارفرما:", style = MaterialTheme.typography.labelMedium)
          var expanded by remember { mutableStateOf(false) }
          val selectedCust = customers.find { it.id == selectedCustomerId } ?: customers.first()

          OutlinedCard(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { expanded = true }
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(selectedCust.fullName, style = MaterialTheme.typography.bodyMedium)
              Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
          }
          DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
          ) {
            customers.forEach { c ->
              DropdownMenuItem(
                text = { Text("${c.fullName} (${c.phone})") },
                onClick = {
                  selectedCustomerId = c.id
                  expanded = false
                }
              )
            }
          }
        }

        // Currency selection
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text("ارز قرارداد:", style = MaterialTheme.typography.labelMedium)
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

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("توضیحات و شرایط قرارداد") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isBlank()) {
            error = "عنوان پروژه الزامی است."
            return@Button
          }
          val id = SecurityHelper.generateUuid()
          val projNum = "PRJ-${(1000..9999).random()}"
          onConfirm(
            ProjectModel(
              id = id,
              projectNumber = projNum,
              customerId = selectedCustomerId,
              title = title.trim(),
              siteAddress = siteAddress.trim(),
              contractCurrency = selectedCurrency,
              notes = notes.trim()
            )
          )
        }
      ) {
        Text("ثبت پروژه")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("انصراف") }
    }
  )
}

@Composable
fun ProjectDetailScreen(
  project: ProjectModel,
  items: List<ProjectItemModel>,
  customer: CustomerModel?,
  onStatusChange: (ProjectStatus) -> Unit,
  onAddItem: (ProjectItemModel) -> Unit,
  onOpenDesignerForItem: (ProjectItemModel) -> Unit,
  onOpenCuttingPlan: () -> Unit,
  onOpenQuotation: () -> Unit,
  onOpenPaymentDialog: () -> Unit,
  onBack: () -> Unit
) {
  var showAddItemDialog by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    contentPadding = PaddingValues(vertical = 16.dp)
  ) {
    // Header Info Card
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(project.title, style = MaterialTheme.typography.titleMedium)
              Text("شماره: ${project.projectNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusBadge(project.status)
          }

          Spacer(modifier = Modifier.height(12.dp))
          HorizontalDivider()
          Spacer(modifier = Modifier.height(10.dp))

          // Customer and address info
          Text("کارفرما: ${customer?.fullName ?: "نامشخص"} | تلفن: ${customer?.phone ?: "-"}", style = MaterialTheme.typography.bodyMedium)
          if (project.siteAddress.isNotBlank()) {
            Text("محل نصب: ${project.siteAddress}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Financial Summary
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              horizontalArrangement = Arrangement.SpaceAround
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("مبلغ کل", style = MaterialTheme.typography.labelSmall)
                Text(CurrencyHelper.formatMoney(project.totalContractAmount, project.contractCurrency), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("دریافتی", style = MaterialTheme.typography.labelSmall)
                Text(CurrencyHelper.formatMoney(project.totalPaidAmount, project.contractCurrency), style = MaterialTheme.typography.labelLarge, color = Color(0xFF059669))
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("مانده بدهی", style = MaterialTheme.typography.labelSmall)
                Text(
                  CurrencyHelper.formatMoney(project.balanceDue, project.contractCurrency),
                  style = MaterialTheme.typography.labelLarge,
                  color = if (project.balanceDue > BigDecimal.ZERO) Color(0xFFDC2626) else Color(0xFF059669)
                )
              }
            }
          }
        }
      }
    }

    // Workflow Actions Buttons
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = { showAddItemDialog = true },
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("افزودن پنجره/درب", style = MaterialTheme.typography.labelSmall)
        }

        OutlinedButton(
          onClick = onOpenQuotation,
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("پیش‌فاکتور", style = MaterialTheme.typography.labelSmall)
        }

        OutlinedButton(
          onClick = onOpenCuttingPlan,
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("نقشه برش", style = MaterialTheme.typography.labelSmall)
        }
      }
    }

    // Payment Record Action
    item {
      FilledTonalButton(
        onClick = onOpenPaymentDialog,
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("ثبت دریافت قسط یا پیش‌پرداخت جدید")
      }
    }

    // Status Advance Selector
    item {
      var statusMenuExpanded by remember { mutableStateOf(false) }
      OutlinedCard(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { statusMenuExpanded = true }
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("تغییر مرحله و وضعیت پروژه:", style = MaterialTheme.typography.bodyMedium)
          Row(verticalAlignment = Alignment.CenterVertically) {
            StatusBadge(project.status)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
          }
        }
      }

      DropdownMenu(
        expanded = statusMenuExpanded,
        onDismissRequest = { statusMenuExpanded = false }
      ) {
        ProjectStatus.entries.forEach { st ->
          DropdownMenuItem(
            text = { Text(st.titlePersian) },
            onClick = {
              onStatusChange(st)
              statusMenuExpanded = false
            }
          )
        }
      }
    }

    // Items Section Header
    item {
      Text(
        text = "اقلام پنجره و درب پروژه (${CurrencyHelper.toPersianDigits(items.size)} قلم)",
        style = MaterialTheme.typography.titleSmall
      )
    }

    if (items.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
            Text("هنوز پنجره یا دربی برای این پروژه تعریف نشده است. از دکمه «افزودن پنجره/درب» استفاده کنید.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
          }
        }
      }
    } else {
      items(items) { item ->
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDesignerForItem(item) }
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(item.description, style = MaterialTheme.typography.titleSmall)
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "ابعاد: ${CurrencyHelper.toPersianDigits(item.widthMm)} × ${CurrencyHelper.toPersianDigits(item.heightMm)} mm | تعداد: ${CurrencyHelper.toPersianDigits(item.quantity)} عدد",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = "نوع: ${item.productType.titlePersian} (${item.materialFamily.titlePersian})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Button(
              onClick = { onOpenDesignerForItem(item) },
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Icon(Icons.Default.DesignServices, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("طراحی ۲D/3D", style = MaterialTheme.typography.labelSmall)
            }
          }
        }
      }
    }
  }

  if (showAddItemDialog) {
    AddItemDialog(
      projectId = project.id,
      onDismiss = { showAddItemDialog = false },
      onConfirm = { newItem ->
        onAddItem(newItem)
        showAddItemDialog = false
      }
    )
  }
}

@Composable
fun AddItemDialog(
  projectId: String,
  onDismiss: () -> Unit,
  onConfirm: (ProjectItemModel) -> Unit
) {
  var desc by remember { mutableStateOf("پنجره دوجداره سالن") }
  var productType by remember { mutableStateOf(ProductType.WINDOW) }
  var materialFamily by remember { mutableStateOf(MaterialFamily.UPVC) }
  var widthStr by remember { mutableStateOf("1400") }
  var heightStr by remember { mutableStateOf("1600") }
  var quantityStr by remember { mutableStateOf("1") }
  var error by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("تعریف پنجره یا درب جدید") },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        if (error.isNotBlank()) {
          Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        OutlinedTextField(
          value = desc,
          onValueChange = { desc = it },
          label = { Text("عنوان / موقعیت (مثلاً پنجره اتاق خواب)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = productType == ProductType.WINDOW,
            onClick = { productType = ProductType.WINDOW },
            label = { Text("پنجره") }
          )
          FilterChip(
            selected = productType == ProductType.DOOR,
            onClick = { productType = ProductType.DOOR },
            label = { Text("درب") }
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = materialFamily == MaterialFamily.UPVC,
            onClick = { materialFamily = MaterialFamily.UPVC },
            label = { Text("UPVC") }
          )
          FilterChip(
            selected = materialFamily == MaterialFamily.ALUMINUM,
            onClick = { materialFamily = MaterialFamily.ALUMINUM },
            label = { Text("آلومینیوم") }
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        }

        OutlinedTextField(
          value = quantityStr,
          onValueChange = { quantityStr = it },
          label = { Text("تعداد (عدد)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val w = widthStr.toIntOrNull() ?: 1400
          val h = heightStr.toIntOrNull() ?: 1600
          val q = quantityStr.toIntOrNull() ?: 1
          if (w < 400 || h < 400) {
            error = "ابعاد باید حداقل ۴۰۰ میلی‌متر باشد."
            return@Button
          }
          val id = SecurityHelper.generateUuid()
          val code = "ITM-${(100..999).random()}"
          onConfirm(
            ProjectItemModel(
              id = id,
              projectId = projectId,
              itemCode = code,
              description = desc.trim(),
              productType = productType,
              materialFamily = materialFamily,
              widthMm = w,
              heightMm = h,
              quantity = q
            )
          )
        }
      ) { Text("ایجاد قلم") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("انصراف") }
    }
  )
}
