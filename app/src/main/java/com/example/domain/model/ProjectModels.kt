package com.example.domain.model

import com.example.core.UserRole
import java.math.BigDecimal

enum class ProjectStatus(val code: String, val titlePersian: String) {
  DRAFT("DRAFT", "پیش‌نویس اولیه"),
  MEASUREMENT("MEASUREMENT", "اندازه‌گیری محل پروژه"),
  DESIGN("DESIGN", "طراحی و مهندسی"),
  QUOTATION("QUOTATION", "صدور پیش‌فاکتور"),
  APPROVED("APPROVED", "تایید کارفرما و عقد قرارداد"),
  PRODUCTION("PRODUCTION", "در حال ساخت و تولید"),
  INSTALLATION("INSTALLATION", "در حال نصب در محل"),
  COMPLETED("COMPLETED", "تکمیل و تسویه نهایی"),
  CANCELLED("CANCELLED", "لغو شده")
}

data class CustomerModel(
  val id: String,
  val code: String,
  val fullName: String,
  val phone: String,
  val address: String = "",
  val notes: String = "",
  val isActive: Boolean = true,
  val createdAt: Long = System.currentTimeMillis()
)

data class ProjectItemModel(
  val id: String,
  val projectId: String,
  val itemCode: String,
  val description: String,
  val productType: ProductType,
  val materialFamily: MaterialFamily,
  val widthMm: Int,
  val heightMm: Int,
  val quantity: Int,
  val designId: String? = null
)

data class ProjectModel(
  val id: String,
  val projectNumber: String,
  val customerId: String,
  val customerName: String = "",
  val title: String,
  val siteAddress: String = "",
  val status: ProjectStatus = ProjectStatus.DRAFT,
  val contractCurrency: String = "AFN",
  val createdAt: Long = System.currentTimeMillis(),
  val expectedCompletionDate: Long? = null,
  val notes: String = "",
  val totalContractAmount: BigDecimal = BigDecimal.ZERO,
  val totalPaidAmount: BigDecimal = BigDecimal.ZERO,
  val balanceDue: BigDecimal = BigDecimal.ZERO
)

data class InvoiceModel(
  val id: String,
  val projectId: String,
  val invoiceNumber: String,
  val isFinalInvoice: Boolean, // false = Quotation, true = Final Invoice
  val currency: String = "AFN",
  val subtotal: BigDecimal,
  val discountAmount: BigDecimal = BigDecimal.ZERO,
  val taxAmount: BigDecimal = BigDecimal.ZERO,
  val grandTotal: BigDecimal,
  val issueDate: Long = System.currentTimeMillis(),
  val isApproved: Boolean = false,
  val notes: String = ""
)

data class PaymentModel(
  val id: String,
  val customerId: String,
  val projectId: String,
  val invoiceId: String? = null,
  val amount: BigDecimal,
  val currency: String = "AFN",
  val exchangeRateToAfn: BigDecimal = BigDecimal.ONE,
  val paymentMethod: String = "نقدی (Cash)",
  val referenceNumber: String = "",
  val paymentDate: Long = System.currentTimeMillis(),
  val notes: String = ""
)

data class ExpenseModel(
  val id: String,
  val projectId: String? = null,
  val title: String,
  val category: String, // مواد اولیه، دستمزد کارگری، کرایه حمل، اجاره کارگاه، استهلاک
  val amount: BigDecimal,
  val currency: String = "AFN",
  val expenseDate: Long = System.currentTimeMillis(),
  val notes: String = ""
)

data class InventoryMovementModel(
  val id: String,
  val materialId: String,
  val materialName: String,
  val movementType: String, // PURCHASE_IN, PROJECT_OUT, SCRAP_OUT, ADJUSTMENT
  val quantity: BigDecimal,
  val unit: String,
  val projectId: String? = null,
  val date: Long = System.currentTimeMillis(),
  val notes: String = ""
) {
  val isInbound: Boolean
    get() = movementType == TYPE_PURCHASE_IN || movementType == TYPE_ADJUSTMENT

  val signedQuantity: BigDecimal
    get() = if (isInbound) quantity else quantity.negate()

  companion object {
    const val TYPE_PURCHASE_IN = "PURCHASE_IN"
    const val TYPE_PROJECT_OUT = "PROJECT_OUT"
    const val TYPE_SCRAP_OUT = "SCRAP_OUT"
    const val TYPE_ADJUSTMENT = "ADJUSTMENT"

    fun titleFor(type: String): String = when (type) {
      TYPE_PURCHASE_IN -> "ورود خرید از تامین‌کننده"
      TYPE_PROJECT_OUT -> "مصرف در پروژه"
      TYPE_SCRAP_OUT -> "ضایعات کارگاهی"
      else -> "تعدیل انبار"
    }
  }
}

data class UserModel(
  val id: String,
  val username: String,
  val fullName: String,
  val role: UserRole,
  val isActive: Boolean = true,
  val createdAt: Long = System.currentTimeMillis()
)

data class AuditLogModel(
  val id: Long,
  val username: String,
  val action: String,
  val entityType: String,
  val entityId: String,
  val details: String,
  val timestamp: Long
)

/** Result of a backup restore attempt. */
sealed interface RestoreResult {
  data class Success(val summary: String) : RestoreResult
  data class Failure(val message: String) : RestoreResult
}
