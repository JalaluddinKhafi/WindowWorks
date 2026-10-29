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
import com.example.domain.model.CustomerModel
import com.example.domain.model.ExpenseModel
import com.example.domain.model.InvoiceModel
import com.example.domain.model.PaymentModel
import com.example.domain.model.ProjectModel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
  invoices: List<InvoiceModel>,
  payments: List<PaymentModel>,
  expenses: List<ExpenseModel>,
  projects: List<ProjectModel>,
  customers: List<CustomerModel>,
  onRecordPayment: (PaymentModel) -> Unit,
  onRecordExpense: (ExpenseModel) -> Unit
) {
  var selectedTab by remember { mutableIntStateOf(0) } // 0 = Invoices, 1 = Payments, 2 = Expenses
  var showPaymentDialog by remember { mutableStateOf(false) }
  var showExpenseDialog by remember { mutableStateOf(false) }

  val totalInvoiced = invoices.fold(BigDecimal.ZERO) { acc, inv -> acc.add(inv.grandTotal) }
  val totalReceived = payments.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.amount) }
  val totalExpenses = expenses.fold(BigDecimal.ZERO) { acc, e -> acc.add(e.amount) }
  val netCash = totalReceived.subtract(totalExpenses)

  Scaffold(
    floatingActionButton = {
      if (selectedTab == 1) {
        FloatingActionButton(
          onClick = { showPaymentDialog = true },
          containerColor = MaterialTheme.colorScheme.primary
        ) {
          Icon(Icons.Default.Add, contentDescription = "ثبت دریافت وجه", tint = MaterialTheme.colorScheme.onPrimary)
        }
      } else if (selectedTab == 2) {
        FloatingActionButton(
          onClick = { showExpenseDialog = true },
          containerColor = Color(0xFFDC2626)
        ) {
          Icon(Icons.Default.Add, contentDescription = "ثبت هزینه جدید", tint = Color.White)
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

      // Top Financial Overview
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("تراز مالی و گردش نقدینگی کارگاه", style = MaterialTheme.typography.titleSmall)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text("کل فاکتورها", style = MaterialTheme.typography.labelSmall)
              Text(CurrencyHelper.formatMoney(totalInvoiced), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Column {
              Text("کل دریافتی‌ها", style = MaterialTheme.typography.labelSmall)
              Text(CurrencyHelper.formatMoney(totalReceived), style = MaterialTheme.typography.labelLarge, color = Color(0xFF059669))
            }
            Column {
              Text("کل هزینه‌ها", style = MaterialTheme.typography.labelSmall)
              Text(CurrencyHelper.formatMoney(totalExpenses), style = MaterialTheme.typography.labelLarge, color = Color(0xFFDC2626))
            }
            Column {
              Text("خالص صندوق", style = MaterialTheme.typography.labelSmall)
              Text(CurrencyHelper.formatMoney(netCash), style = MaterialTheme.typography.labelLarge, color = if (netCash >= BigDecimal.ZERO) Color(0xFF047857) else Color(0xFFB91C1C))
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Tabs: Invoices, Payments, Expenses
      TabRow(selectedTabIndex = selectedTab) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text("فاکتورها (${CurrencyHelper.toPersianDigits(invoices.size)})") }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("دریافتی‌ها (${CurrencyHelper.toPersianDigits(payments.size)})") }
        )
        Tab(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          text = { Text("هزینه‌ها (${CurrencyHelper.toPersianDigits(expenses.size)})") }
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      when (selectedTab) {
        0 -> {
          if (invoices.isEmpty()) {
            EmptyListPlaceholder("هنوز پیش‌فاکتور یا فاکتوری صادر نشده است.")
          } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
              items(invoices) { inv ->
                InvoiceCard(invoice = inv)
              }
            }
          }
        }
        1 -> {
          if (payments.isEmpty()) {
            EmptyListPlaceholder("هنوز دریافتی یا رسیدی ثبت نشده است.")
          } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
              items(payments) { p ->
                PaymentCard(payment = p)
              }
            }
          }
        }
        2 -> {
          if (expenses.isEmpty()) {
            EmptyListPlaceholder("هنوز هزینه‌ای در این دوره ثبت نشده است.")
          } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
              items(expenses) { exp ->
                ExpenseCard(expense = exp)
              }
            }
          }
        }
      }
    }
  }

  if (showPaymentDialog) {
    RecordPaymentDialog(
      projects = projects,
      customers = customers,
      onDismiss = { showPaymentDialog = false },
      onConfirm = { p ->
        onRecordPayment(p)
        showPaymentDialog = false
      }
    )
  }

  if (showExpenseDialog) {
    RecordExpenseDialog(
      projects = projects,
      onDismiss = { showExpenseDialog = false },
      onConfirm = { e ->
        onRecordExpense(e)
        showExpenseDialog = false
      }
    )
  }
}

@Composable
fun InvoiceCard(invoice: InvoiceModel) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(invoice.invoiceNumber, style = MaterialTheme.typography.titleSmall)
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (invoice.isFinalInvoice) Color(0xFFECFDF5) else Color(0xFFFEF3C7)
          ) {
            Text(
              text = if (invoice.isFinalInvoice) "فاکتور قطعی" else "پیش‌فاکتور",
              style = MaterialTheme.typography.labelSmall,
              color = if (invoice.isFinalInvoice) Color(0xFF047857) else Color(0xFFB45309),
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "مبلغ: ${CurrencyHelper.formatMoney(invoice.grandTotal, invoice.currency)}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.primary
        )
      }
    }
  }
}

@Composable
fun PaymentCard(payment: PaymentModel) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text("رسید دریافت وجه", style = MaterialTheme.typography.titleSmall, color = Color(0xFF059669))
        Spacer(modifier = Modifier.height(4.dp))
        Text("روش پرداخت: ${payment.paymentMethod} ${if (payment.referenceNumber.isNotBlank()) "| شماره پیگیری: ${payment.referenceNumber}" else ""}", style = MaterialTheme.typography.bodySmall)
        if (payment.notes.isNotBlank()) {
          Text("بابت: ${payment.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
      Text(
        text = CurrencyHelper.formatMoney(payment.amount, payment.currency),
        style = MaterialTheme.typography.titleSmall,
        color = Color(0xFF059669)
      )
    }
  }
}

@Composable
fun ExpenseCard(expense: ExpenseModel) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(expense.title, style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text("سرفصل: ${expense.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Text(
        text = CurrencyHelper.formatMoney(expense.amount, expense.currency),
        style = MaterialTheme.typography.titleSmall,
        color = Color(0xFFDC2626)
      )
    }
  }
}

@Composable
fun RecordPaymentDialog(
  projects: List<ProjectModel>,
  customers: List<CustomerModel>,
  onDismiss: () -> Unit,
  onConfirm: (PaymentModel) -> Unit
) {
  var amountStr by remember { mutableStateOf("") }
  var selectedProjectId by remember { mutableStateOf(projects.firstOrNull()?.id ?: "") }
  var paymentMethod by remember { mutableStateOf("نقدی (Cash)") }
  var refNum by remember { mutableStateOf("") }
  var notes by remember { mutableStateOf("قسط مرحله‌ای قرارداد") }
  var error by remember { mutableStateOf("") }

  val project = projects.find { it.id == selectedProjectId }
  val customerId = project?.customerId ?: customers.firstOrNull()?.id ?: ""

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("ثبت دریافت وجه / قسط جدید") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        if (error.isNotBlank()) {
          Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        OutlinedTextField(
          value = amountStr,
          onValueChange = { amountStr = it },
          label = { Text("مبلغ دریافتی (افغانی) *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Text("پروژه مربوطه:", style = MaterialTheme.typography.labelMedium)
        projects.forEach { p ->
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            RadioButton(
              selected = selectedProjectId == p.id,
              onClick = { selectedProjectId = p.id }
            )
            Text("${p.title} (${p.projectNumber})", style = MaterialTheme.typography.bodyMedium)
          }
        }

        OutlinedTextField(
          value = refNum,
          onValueChange = { refNum = it },
          label = { Text("شماره ارجاع / فیش بانکی (اختیاری)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("توضیحات و بابت") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val amt = CurrencyHelper.parseAmount(amountStr)
          if (amt <= BigDecimal.ZERO) {
            error = "مبلغ معتبر نیست."
            return@Button
          }
          onConfirm(
            PaymentModel(
              id = SecurityHelper.generateUuid(),
              customerId = customerId,
              projectId = selectedProjectId,
              amount = amt,
              paymentMethod = paymentMethod,
              referenceNumber = refNum.trim(),
              notes = notes.trim()
            )
          )
        }
      ) { Text("ثبت رسید") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("انصراف") }
    }
  )
}

@Composable
fun RecordExpenseDialog(
  projects: List<ProjectModel>,
  onDismiss: () -> Unit,
  onConfirm: (ExpenseModel) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var category by remember { mutableStateOf("مواد اولیه و ملزومات") }
  var amountStr by remember { mutableStateOf("") }
  var notes by remember { mutableStateOf("") }
  var error by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("ثبت هزینه کارگاهی") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        if (error.isNotBlank()) {
          Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("عنوان هزینه * (مثلاً خرید تیغ‌اره اره)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = amountStr,
          onValueChange = { amountStr = it },
          label = { Text("مبلغ (افغانی) *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("توضیحات") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val amt = CurrencyHelper.parseAmount(amountStr)
          if (title.isBlank() || amt <= BigDecimal.ZERO) {
            error = "عنوان و مبلغ الزامی هستند."
            return@Button
          }
          onConfirm(
            ExpenseModel(
              id = SecurityHelper.generateUuid(),
              title = title.trim(),
              category = category,
              amount = amt,
              notes = notes.trim()
            )
          )
        }
      ) { Text("ثبت هزینه") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("انصراف") }
    }
  )
}

@Composable
private fun EmptyListPlaceholder(message: String) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
  }
}
