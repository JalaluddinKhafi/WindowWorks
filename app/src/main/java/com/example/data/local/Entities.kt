package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "users")
data class UserEntity(
  @PrimaryKey val id: String,
  val username: String,
  val fullName: String,
  val passwordHash: String,
  val salt: String,
  val role: String, // OWNER_ADMIN, SALES, DESIGNER, ACCOUNTANT, OPERATOR, READ_ONLY
  val isActive: Boolean = true,
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class CustomerEntity(
  @PrimaryKey val id: String,
  val code: String,
  val fullName: String,
  val phone: String,
  val address: String,
  val notes: String,
  val isActive: Boolean = true,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "projects")
data class ProjectEntity(
  @PrimaryKey val id: String,
  val projectNumber: String,
  val customerId: String,
  val title: String,
  val siteAddress: String,
  val status: String, // DRAFT, MEASUREMENT, DESIGN, QUOTATION, APPROVED, PRODUCTION, INSTALLATION, COMPLETED, CANCELLED
  val contractCurrency: String = "AFN",
  val createdAt: Long = System.currentTimeMillis(),
  val expectedDate: Long? = null,
  val notes: String = "",
  val totalContractAmount: String = "0.00",
  val totalPaidAmount: String = "0.00",
  val balanceDue: String = "0.00"
)

@Entity(tableName = "project_items")
data class ProjectItemEntity(
  @PrimaryKey val id: String,
  val projectId: String,
  val itemCode: String,
  val description: String,
  val productType: String,
  val materialFamily: String,
  val widthMm: Int,
  val heightMm: Int,
  val quantity: Int,
  val designId: String? = null
)

@Entity(tableName = "designs")
data class DesignEntity(
  @PrimaryKey val id: String,
  val name: String,
  val productType: String,
  val materialFamily: String,
  val profileSystem: String,
  val frameFinish: String,
  val sashFinish: String = "WHITE",
  val outerGlassTint: String = "CLEAR",
  val innerGlassTint: String = "CLEAR",
  val widthMm: Int,
  val heightMm: Int,
  val hasTransom: Boolean,
  val transomHeightMm: Int,
  val panelsJson: String, // serialized JSON of panels
  val isApproved: Boolean = false,
  val version: Int = 1,
  val notes: String = "",
  val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "profiles")
data class ProfileEntity(
  @PrimaryKey val id: String,
  val code: String,
  val name: String,
  val materialFamily: String, // UPVC, ALUMINUM
  val brandSystem: String,    // Wintech, Vistabest, Akpa, ...
  val sectionType: String,    // فریم, لنگه, مولیون, زوار
  val stockLengthMm: Int = 6000,
  val purchasePrice: String = "180.00",
  val currency: String = "AFN",
  val supplierName: String = "",
  val isActive: Boolean = true
)

@Entity(tableName = "glasses")
data class GlassEntity(
  @PrimaryKey val id: String,
  val title: String,
  val glazingType: String,
  val thicknessMm: Int,
  val tint: String,
  val pricePerM2: String = "650.00",
  val currency: String = "AFN",
  val supplierName: String = "",
  val isActive: Boolean = true
)

@Entity(tableName = "accessories")
data class AccessoryEntity(
  @PrimaryKey val id: String,
  val code: String,
  val name: String,
  val category: String, // دستگیره، لولا، قفل، لاستیک، پیچ، اتصالات
  val unit: String,     // عدد، متر، بسته
  val unitPrice: String = "100.00",
  val currency: String = "AFN",
  val isActive: Boolean = true
)

@Entity(tableName = "invoices")
data class InvoiceEntity(
  @PrimaryKey val id: String,
  val projectId: String,
  val invoiceNumber: String,
  val isFinalInvoice: Boolean, // false = Quotation, true = Invoice
  val currency: String = "AFN",
  val subtotal: String,
  val discountAmount: String = "0.00",
  val taxAmount: String = "0.00",
  val grandTotal: String,
  val issueDate: Long = System.currentTimeMillis(),
  val isApproved: Boolean = false,
  val notes: String = ""
)

@Entity(tableName = "payments")
data class PaymentEntity(
  @PrimaryKey val id: String,
  val customerId: String,
  val projectId: String,
  val invoiceId: String? = null,
  val amount: String,
  val currency: String = "AFN",
  val exchangeRateToAfn: String = "1.00",
  val paymentMethod: String, // نقدی، حواله بانکی، چک
  val referenceNumber: String = "",
  val paymentDate: Long = System.currentTimeMillis(),
  val notes: String = ""
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
  @PrimaryKey val id: String,
  val projectId: String? = null,
  val title: String,
  val category: String,
  val amount: String,
  val currency: String = "AFN",
  val expenseDate: Long = System.currentTimeMillis(),
  val notes: String = ""
)

@Entity(tableName = "inventory_movements")
data class InventoryMovementEntity(
  @PrimaryKey val id: String,
  val materialId: String,
  val materialName: String,
  val movementType: String, // PURCHASE_IN (ورود خرید), PROJECT_OUT (مصرف در پروژه), SCRAP_OUT (ضایعات), ADJUSTMENT (تعدیل)
  val quantity: String,
  val unit: String,
  val projectId: String? = null,
  val date: Long = System.currentTimeMillis(),
  val notes: String = ""
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val username: String,
  val action: String,
  val entityType: String,
  val entityId: String,
  val details: String,
  val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class SettingEntity(
  @PrimaryKey val key: String,
  val value: String
)

@Entity(tableName = "material_products")
data class MaterialProductEntity(
  @PrimaryKey val id: String,
  val code: String,
  val name: String,
  val category: String,
  val brandModel: String = "",
  val materialFamily: String = "UPVC",
  val description: String = "",
  val specifications: String = "",
  val unit: String = "عدد",
  val purchasePriceAfn: String = "0.00",
  val purchasePriceUsd: String = "0.00",
  val stockLengthMm: Int = 0,
  val thicknessMm: Int = 0,
  val colorFinish: String = "سفید",
  val compatibility: String = "عمومی",
  val stockQty: String = "0.00",
  val minStockLevel: String = "0.00",
  val supplierName: String = "",
  val isActive: Boolean = true,
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "project_calculation_sessions")
data class ProjectCalculationSessionEntity(
  @PrimaryKey val id: String,
  val sessionCode: String,
  val customerName: String,
  val customerPhone: String,
  val title: String,
  val siteLocation: String,
  val currency: String = "AFN",
  val exchangeRate: String = "72.50",
  val pricingBasis: String = "مجموع اقلام",
  val itemsJson: String,
  val transportCost: String = "0.00",
  val installationCost: String = "0.00",
  val discountAmount: String = "0.00",
  val markupPercent: String = "25.00",
  val totalAmount: String = "0.00",
  val status: String = "DRAFT",
  val notes: String = "",
  val createdAt: Long = System.currentTimeMillis()
)

