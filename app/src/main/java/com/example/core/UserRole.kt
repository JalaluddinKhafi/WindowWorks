package com.example.core

enum class UserRole(val code: String, val titlePersian: String, val descriptionPersian: String) {
  OWNER_ADMIN(
    "OWNER_ADMIN",
    "مدیر کارگاه (دسترسی کامل)",
    "مدیریت کامل پروژه‌ها، تنظیمات کارگاه، قیمت‌ها، حذف، حسابداری و کاربران"
  ),
  SALES(
    "SALES",
    "مسئول فروش و بازاریابی",
    "ثبت و پیگیری مشتریان، صدور پیش‌فاکتور، ثبت قرارداد و مشاهده کاتالوگ"
  ),
  DESIGNER(
    "DESIGNER",
    "مهندس طراح فنی",
    "طراحی پارامتریک در و پنجره، تعیین مقاطع، ابعاد و تهیه نقشه برش و BOM"
  ),
  ACCOUNTANT(
    "ACCOUNTANT",
    "حسابدار مالی",
    "ثبت فاکتورهای نهایی، اقساط، رسیدها، هزینه‌ها، گزارشات سود و حساب‌ها"
  ),
  OPERATOR(
    "OPERATOR",
    "اپراتور خط تولید و مونتاژ",
    "مشاهده نقشه‌های برش، تایید مراحل تولید و ثبت مصرف یا ضایعات متریال"
  ),
  READ_ONLY(
    "READ_ONLY",
    "کاربر مشاهده‌گر",
    "صرفاً مشاهده اطلاعات بدون امکان تغییر، ذخیره یا حذف"
  );

  companion object {
    fun fromCode(code: String): UserRole {
      return entries.find { it.code.equals(code, ignoreCase = true) } ?: READ_ONLY
    }
  }

  fun canManageUsers(): Boolean = this == OWNER_ADMIN
  fun canEditCatalogAndPrices(): Boolean = this == OWNER_ADMIN || this == ACCOUNTANT
  fun canDesignWindows(): Boolean = this == OWNER_ADMIN || this == DESIGNER || this == SALES
  fun canCreateInvoices(): Boolean = this == OWNER_ADMIN || this == SALES || this == ACCOUNTANT
  fun canApproveInvoices(): Boolean = this == OWNER_ADMIN || this == ACCOUNTANT
  fun canRecordPayments(): Boolean = this == OWNER_ADMIN || this == ACCOUNTANT
  fun canUpdateProduction(): Boolean = this == OWNER_ADMIN || this == OPERATOR || this == DESIGNER
  fun canManageInventory(): Boolean = this == OWNER_ADMIN || this == OPERATOR || this == ACCOUNTANT
  fun canExportOrBackup(): Boolean = this == OWNER_ADMIN
}
