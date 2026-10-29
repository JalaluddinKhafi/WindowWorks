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
import androidx.compose.ui.unit.dp
import com.example.core.CurrencyHelper
import com.example.core.SecurityHelper
import com.example.domain.model.CustomerModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
  customers: List<CustomerModel>,
  onSaveCustomer: (CustomerModel) -> Unit,
  onSelectCustomer: (CustomerModel) -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  var showAddDialog by remember { mutableStateOf(false) }

  val filteredCustomers = remember(customers, searchQuery) {
    if (searchQuery.isBlank()) customers
    else customers.filter {
      it.fullName.contains(searchQuery, ignoreCase = true) ||
          it.phone.contains(searchQuery) ||
          it.code.contains(searchQuery, ignoreCase = true)
    }
  }

  Scaffold(
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showAddDialog = true },
        containerColor = MaterialTheme.colorScheme.primary
      ) {
        Icon(Icons.Default.Add, contentDescription = "افزودن مشتری جدید", tint = MaterialTheme.colorScheme.onPrimary)
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

      // Search field
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("جستجوی نام مشتری، شماره تلفن یا کد...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Default.Clear, contentDescription = "پاک کردن")
            }
          }
        },
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(12.dp))

      if (filteredCustomers.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = if (searchQuery.isBlank()) "هنوز مشتری‌ای ثبت نشده است." else "مشتری با این مشخصات یافت نشد.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
          )
        }
      } else {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(bottom = 80.dp)
        ) {
          items(filteredCustomers) { customer ->
            CustomerCard(customer = customer, onClick = { onSelectCustomer(customer) })
          }
        }
      }
    }
  }

  if (showAddDialog) {
    CustomerDialog(
      onDismiss = { showAddDialog = false },
      onConfirm = { newCust ->
        onSaveCustomer(newCust)
        showAddDialog = false
      }
    )
  }
}

@Composable
fun CustomerCard(customer: CustomerModel, onClick: () -> Unit) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
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
          Text(
            text = customer.fullName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
          )
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
          ) {
            Text(
              text = customer.code,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "تلفن: ${CurrencyHelper.toPersianDigits(customer.phone)}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.primary
        )
        if (customer.address.isNotBlank()) {
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "آدرس: ${customer.address}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Icon(
        Icons.Default.ChevronLeft,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.outline
      )
    }
  }
}

@Composable
fun CustomerDialog(
  customer: CustomerModel? = null,
  onDismiss: () -> Unit,
  onConfirm: (CustomerModel) -> Unit
) {
  var fullName by remember { mutableStateOf(customer?.fullName ?: "") }
  var phone by remember { mutableStateOf(customer?.phone ?: "") }
  var address by remember { mutableStateOf(customer?.address ?: "") }
  var notes by remember { mutableStateOf(customer?.notes ?: "") }
  var errorMessage by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (customer == null) "ثبت مشتری جدید" else "ویرایش مشخصات مشتری") },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        if (errorMessage.isNotBlank()) {
          Text(text = errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        OutlinedTextField(
          value = fullName,
          onValueChange = { fullName = it },
          label = { Text("نام و نام خانوادگی *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("شماره تماس *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = address,
          onValueChange = { address = it },
          label = { Text("آدرس / محل پروژه") },
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("یادداشت‌ها") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (fullName.isBlank() || phone.isBlank()) {
            errorMessage = "لطفاً نام و شماره تماس مشتری را وارد کنید."
            return@Button
          }
          val id = customer?.id ?: SecurityHelper.generateUuid()
          val code = customer?.code ?: "CST-${(100..999).random()}"
          onConfirm(
            CustomerModel(
              id = id,
              code = code,
              fullName = fullName.trim(),
              phone = phone.trim(),
              address = address.trim(),
              notes = notes.trim()
            )
          )
        }
      ) {
        Text("ذخیره")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("انصراف")
      }
    }
  )
}
