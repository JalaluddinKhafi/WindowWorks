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
import com.example.domain.model.MaterialProductModel
import java.math.BigDecimal

val MATERIAL_CATEGORIES = listOf(
  "همه دسته‌ها",
  "پروفیل UPVC",
  "پروفیل آلومینیوم",
  "شیشه",
  "دستگیره",
  "لولا",
  "قفل و یراق",
  "غلطک",
  "گالوانیزه",
  "لاستیک EPDM",
  "چسب و درزگیر",
  "پیچ و اتصالات",
  "خدمات کارگاهی"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialsCatalogScreen(
  products: List<MaterialProductModel>,
  onSaveProduct: (MaterialProductModel) -> Unit,
  onDeleteProduct: (String) -> Unit,
  onBack: () -> Unit = {}
) {
  var selectedCategory by remember { mutableStateOf("همه دسته‌ها") }
  var searchQuery by remember { mutableStateOf("") }
  var showAddDialog by remember { mutableStateOf(false) }
  var editingProduct by remember { mutableStateOf<MaterialProductModel?>(null) }

  val filteredProducts = remember(products, selectedCategory, searchQuery) {
    products.filter { p ->
      val matchesCat = selectedCategory == "همه دسته‌ها" || p.category == selectedCategory
      val matchesSearch = searchQuery.isBlank() ||
          p.name.contains(searchQuery, ignoreCase = true) ||
          p.code.contains(searchQuery, ignoreCase = true) ||
          p.brandModel.contains(searchQuery, ignoreCase = true)
      matchesCat && matchesSearch
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("کاتالوگ اجناس، مواد و متریال کارگاه") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowForward, contentDescription = "بازگشت")
          }
        }
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          editingProduct = null
          showAddDialog = true
        },
        containerColor = MaterialTheme.colorScheme.primary
      ) {
        Icon(Icons.Default.Add, contentDescription = "افزودن کالا یا متریال جدید", tint = MaterialTheme.colorScheme.onPrimary)
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

      // Search
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("جستجوی نام کالا، کد، برند (مثلاً وینتک، روتو، ۶۰۰۰)...") },
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

      Spacer(modifier = Modifier.height(8.dp))

      // Category Horizontal Tabs
      ScrollableTabRow(
        selectedTabIndex = MATERIAL_CATEGORIES.indexOf(selectedCategory).coerceAtLeast(0),
        edgePadding = 0.dp,
        divider = {}
      ) {
        MATERIAL_CATEGORIES.forEach { cat ->
          Tab(
            selected = selectedCategory == cat,
            onClick = { selectedCategory = cat },
            text = { Text(cat, style = MaterialTheme.typography.labelSmall) }
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Products Count & Stats
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "اقلام ثبت شده: ${CurrencyHelper.toPersianDigits(filteredProducts.size)} کالا",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      if (filteredProducts.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "کالایی با این مشخصات یافت نشد.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
          )
        }
      } else {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(bottom = 80.dp)
        ) {
          items(filteredProducts) { item ->
            MaterialProductCard(
              product = item,
              onEdit = {
                editingProduct = item
                showAddDialog = true
              },
              onDelete = { onDeleteProduct(item.id) }
            )
          }
        }
      }
    }
  }

  if (showAddDialog) {
    MaterialProductDialog(
      initialProduct = editingProduct,
      onDismiss = {
        showAddDialog = false
        editingProduct = null
      },
      onConfirm = { saved ->
        onSaveProduct(saved)
        showAddDialog = false
        editingProduct = null
      }
    )
  }
}

@Composable
fun MaterialProductCard(
  product: MaterialProductModel,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(product.name, style = MaterialTheme.typography.titleSmall)
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.surfaceVariant
            ) {
              Text(
                text = product.category,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "کد: ${product.code} | برند: ${product.brandModel.ifBlank { "عمومی" }} | واحد: ${product.unit}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          if (product.specifications.isNotBlank()) {
            Text(
              text = "مشخصات: ${product.specifications}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.outline
            )
          }
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = CurrencyHelper.formatMoney(product.purchasePriceAfn, "AFN"),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
          )
          if (product.purchasePriceUsd > BigDecimal.ZERO) {
            Text(
              text = CurrencyHelper.formatMoney(product.purchasePriceUsd, "USD"),
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF059669)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "موجودی انبار: ${CurrencyHelper.toPersianDigits(product.stockQty.toInt())} ${product.unit}",
          style = MaterialTheme.typography.labelSmall,
          color = if (product.stockQty <= product.minStockLevel) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Edit, contentDescription = "ویرایش", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
          }
          IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
          }
        }
      }
    }
  }
}

@Composable
fun MaterialProductDialog(
  initialProduct: MaterialProductModel?,
  onDismiss: () -> Unit,
  onConfirm: (MaterialProductModel) -> Unit
) {
  var name by remember { mutableStateOf(initialProduct?.name ?: "") }
  var code by remember { mutableStateOf(initialProduct?.code ?: "MAT-${(100..999).random()}") }
  var category by remember { mutableStateOf(initialProduct?.category ?: "پروفیل UPVC") }
  var brandModel by remember { mutableStateOf(initialProduct?.brandModel ?: "Wintech 6000") }
  var unit by remember { mutableStateOf(initialProduct?.unit ?: "شاخه") }
  var priceAfnStr by remember { mutableStateOf(initialProduct?.purchasePriceAfn?.toPlainString() ?: "1100.00") }
  var priceUsdStr by remember { mutableStateOf(initialProduct?.purchasePriceUsd?.toPlainString() ?: "15.00") }
  var specs by remember { mutableStateOf(initialProduct?.specifications ?: "") }
  var stockLengthStr by remember { mutableStateOf(initialProduct?.stockLengthMm?.toString() ?: "6000") }
  var stockQtyStr by remember { mutableStateOf(initialProduct?.stockQty?.toPlainString() ?: "20") }
  var error by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initialProduct == null) "ثبت کالا یا متریال جدید" else "ویرایش مشخصات کالا") },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        if (error.isNotBlank()) {
          Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("نام متریال / کالا *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = { Text("کد کالا *") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = brandModel,
            onValueChange = { brandModel = it },
            label = { Text("برند / سری") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }

        // Category Selector
        Text("دسته‌بندی کالا:", style = MaterialTheme.typography.labelMedium)
        var catExpanded by remember { mutableStateOf(false) }
        OutlinedCard(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { catExpanded = true }
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(category, style = MaterialTheme.typography.bodyMedium)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
          }
        }
        DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
          MATERIAL_CATEGORIES.filter { it != "همه دسته‌ها" }.forEach { c ->
            DropdownMenuItem(
              text = { Text(c) },
              onClick = {
                category = c
                catExpanded = false
              }
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = priceAfnStr,
            onValueChange = { priceAfnStr = it },
            label = { Text("قیمت خرید (افغانی) *") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = priceUsdStr,
            onValueChange = { priceUsdStr = it },
            label = { Text("قیمت خرید (دالر)") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = unit,
            onValueChange = { unit = it },
            label = { Text("واحد اندازه‌گیری") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = stockQtyStr,
            onValueChange = { stockQtyStr = it },
            label = { Text("موجودی فعلی انبار") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }

        OutlinedTextField(
          value = specs,
          onValueChange = { specs = it },
          label = { Text("مشخصات فنی و ابعاد مقطع") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isBlank() || code.isBlank()) {
            error = "نام و کد کالا الزامی است."
            return@Button
          }
          val id = initialProduct?.id ?: SecurityHelper.generateUuid()
          onConfirm(
            MaterialProductModel(
              id = id,
              code = code.trim(),
              name = name.trim(),
              category = category,
              brandModel = brandModel.trim(),
              unit = unit.trim(),
              purchasePriceAfn = CurrencyHelper.parseAmount(priceAfnStr),
              purchasePriceUsd = CurrencyHelper.parseAmount(priceUsdStr),
              stockLengthMm = stockLengthStr.toIntOrNull() ?: 6000,
              stockQty = CurrencyHelper.parseAmount(stockQtyStr),
              specifications = specs.trim()
            )
          )
        }
      ) { Text("ذخیره در کاتالوگ") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("انصراف") }
    }
  )
}
