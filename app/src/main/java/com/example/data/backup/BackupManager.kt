package com.example.data.backup

import com.example.data.local.AccessoryEntity
import com.example.data.local.AppDao
import com.example.data.local.CustomerEntity
import com.example.data.local.DesignEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.GlassEntity
import com.example.data.local.InventoryMovementEntity
import com.example.data.local.InvoiceEntity
import com.example.data.local.MaterialProductEntity
import com.example.data.local.PaymentEntity
import com.example.data.local.ProfileEntity
import com.example.data.local.ProjectCalculationSessionEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectItemEntity
import com.example.data.local.SettingEntity
import com.example.data.local.UserEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Complete, lossless JSON export/import of the local database.
 *
 * The format is a plain JSON document:
 * ```
 * { "app": "WindowWorkshop", "schemaVersion": 2, "exportedAt": 1700000000000,
 *   "users": [...], "customers": [...], ... "settings": [...] }
 * ```
 * Reading is defensive: unknown fields are ignored and missing collections are treated as empty,
 * so a backup created by an older build can still be restored.
 */
object BackupManager {
  const val APP_ID = "WindowWorkshop"
  const val SCHEMA_VERSION = 2
  private const val MIN_SUPPORTED_SCHEMA = 1

  private const val KEY_USERS = "users"
  private const val KEY_CUSTOMERS = "customers"
  private const val KEY_PROJECTS = "projects"
  private const val KEY_PROJECT_ITEMS = "projectItems"
  private const val KEY_DESIGNS = "designs"
  private const val KEY_PROFILES = "profiles"
  private const val KEY_GLASSES = "glasses"
  private const val KEY_ACCESSORIES = "accessories"
  private const val KEY_INVOICES = "invoices"
  private const val KEY_PAYMENTS = "payments"
  private const val KEY_EXPENSES = "expenses"
  private const val KEY_INVENTORY = "inventoryMovements"
  private const val KEY_SETTINGS = "settings"
  private const val KEY_MATERIAL_PRODUCTS = "materialProducts"
  private const val KEY_CALCULATION_SESSIONS = "calculationSessions"

  class Snapshot(
    val users: List<UserEntity> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val projectItems: List<ProjectItemEntity> = emptyList(),
    val designs: List<DesignEntity> = emptyList(),
    val profiles: List<ProfileEntity> = emptyList(),
    val glasses: List<GlassEntity> = emptyList(),
    val accessories: List<AccessoryEntity> = emptyList(),
    val invoices: List<InvoiceEntity> = emptyList(),
    val payments: List<PaymentEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val inventoryMovements: List<InventoryMovementEntity> = emptyList(),
    val settings: List<SettingEntity> = emptyList(),
    val materialProducts: List<MaterialProductEntity> = emptyList(),
    val calculationSessions: List<ProjectCalculationSessionEntity> = emptyList()
  )

  class BackupException(message: String) : Exception(message)

  // ---------------------------------------------------------------------------------------------
  // EXPORT
  // ---------------------------------------------------------------------------------------------

  suspend fun export(dao: AppDao): String {
    val root = JSONObject()
    root.put("app", APP_ID)
    root.put("schemaVersion", SCHEMA_VERSION)
    root.put("exportedAt", System.currentTimeMillis())

    root.put(KEY_USERS, JSONArray(dao.selectAllUsers().map { it.toJson() }))
    root.put(KEY_CUSTOMERS, JSONArray(dao.selectAllCustomers().map { it.toJson() }))
    root.put(KEY_PROJECTS, JSONArray(dao.selectAllProjects().map { it.toJson() }))
    root.put(KEY_PROJECT_ITEMS, JSONArray(dao.selectAllProjectItems().map { it.toJson() }))
    root.put(KEY_DESIGNS, JSONArray(dao.selectAllDesigns().map { it.toJson() }))
    root.put(KEY_PROFILES, JSONArray(dao.selectAllProfiles().map { it.toJson() }))
    root.put(KEY_GLASSES, JSONArray(dao.selectAllGlasses().map { it.toJson() }))
    root.put(KEY_ACCESSORIES, JSONArray(dao.selectAllAccessories().map { it.toJson() }))
    root.put(KEY_INVOICES, JSONArray(dao.selectAllInvoices().map { it.toJson() }))
    root.put(KEY_PAYMENTS, JSONArray(dao.selectAllPayments().map { it.toJson() }))
    root.put(KEY_EXPENSES, JSONArray(dao.selectAllExpenses().map { it.toJson() }))
    root.put(KEY_INVENTORY, JSONArray(dao.selectAllInventoryMovements().map { it.toJson() }))
    root.put(KEY_SETTINGS, JSONArray(dao.selectAllSettings().map { it.toJson() }))
    root.put(KEY_MATERIAL_PRODUCTS, JSONArray(dao.selectAllMaterialProducts().map { it.toJson() }))
    root.put(KEY_CALCULATION_SESSIONS, JSONArray(dao.selectAllCalculationSessions().map { it.toJson() }))

    return root.toString(2)
  }

  // ---------------------------------------------------------------------------------------------
  // IMPORT
  // ---------------------------------------------------------------------------------------------

  /** Parses and validates a backup document. Throws [BackupException] when it is not usable. */
  fun parse(json: String): Snapshot {
    val root = try {
      JSONObject(json)
    } catch (_: Exception) {
      throw BackupException("فایل انتخاب‌شده یک فایل JSON معتبر نیست.")
    }

    val app = root.optString("app", "")
    if (app.isNotBlank() && app != APP_ID) {
      throw BackupException("این فایل مربوط به برنامه «$app» است و قابل بازیابی در WindowWorkshop نیست.")
    }
    val schema = root.optInt("schemaVersion", MIN_SUPPORTED_SCHEMA)
    if (schema > SCHEMA_VERSION) {
      throw BackupException("نسخه فایل پشتیبان ($schema) جدیدتر از نسخه این برنامه است.")
    }
    if (schema < MIN_SUPPORTED_SCHEMA) {
      throw BackupException("نسخه فایل پشتیبان پشتیبانی نمی‌شود.")
    }

    val snapshot = Snapshot(
      users = root.array(KEY_USERS) { userFromJson(it) },
      customers = root.array(KEY_CUSTOMERS) { customerFromJson(it) },
      projects = root.array(KEY_PROJECTS) { projectFromJson(it) },
      projectItems = root.array(KEY_PROJECT_ITEMS) { projectItemFromJson(it) },
      designs = root.array(KEY_DESIGNS) { designFromJson(it) },
      profiles = root.array(KEY_PROFILES) { profileFromJson(it) },
      glasses = root.array(KEY_GLASSES) { glassFromJson(it) },
      accessories = root.array(KEY_ACCESSORIES) { accessoryFromJson(it) },
      invoices = root.array(KEY_INVOICES) { invoiceFromJson(it) },
      payments = root.array(KEY_PAYMENTS) { paymentFromJson(it) },
      expenses = root.array(KEY_EXPENSES) { expenseFromJson(it) },
      inventoryMovements = root.array(KEY_INVENTORY) { inventoryMovementFromJson(it) },
      settings = root.array(KEY_SETTINGS) { settingFromJson(it) },
      materialProducts = root.array(KEY_MATERIAL_PRODUCTS) { materialProductFromJson(it) },
      calculationSessions = root.array(KEY_CALCULATION_SESSIONS) { calculationSessionFromJson(it) }
    )

    if (snapshot.users.isEmpty()) {
      throw BackupException("فایل پشتیبان هیچ کاربری ندارد؛ بازیابی آن باعث قفل شدن برنامه می‌شود.")
    }
    if (snapshot.customers.isEmpty() && snapshot.projects.isEmpty() &&
      snapshot.materialProducts.isEmpty()
    ) {
      throw BackupException("فایل پشتیبان خالی است و هیچ داده‌ای برای بازیابی ندارد.")
    }
    return snapshot
  }

  /**
   * Replaces the current contents of the database with [backup].
   * [currentUserId] is kept signed-in when possible, otherwise the first active admin is used.
   * The wipe + import runs inside one Room transaction, so a failure rolls everything back.
   */
  suspend fun restore(dao: AppDao, backup: Snapshot, currentUserId: String?): String {
    val adminToRestore = backup.users.find { it.isActive && it.role == "OWNER_ADMIN" }
      ?: backup.users.find { it.isActive }
      ?: backup.users.first()
    val restoredUser = backup.users.find { it.id == currentUserId } ?: adminToRestore

    dao.replaceAllData {
      backup.users.forEach { dao.insertUser(it.copy(isActive = true)) }
      backup.customers.forEach { dao.insertCustomer(it) }
      backup.projects.forEach { dao.insertProject(it) }
      backup.projectItems.forEach { dao.insertProjectItem(it) }
      backup.designs.forEach { dao.insertDesign(it) }
      backup.profiles.forEach { dao.insertProfile(it) }
      backup.glasses.forEach { dao.insertGlass(it) }
      backup.accessories.forEach { dao.insertAccessory(it) }
      backup.invoices.forEach { dao.insertInvoice(it) }
      backup.payments.forEach { dao.insertPayment(it) }
      backup.expenses.forEach { dao.insertExpense(it) }
      backup.inventoryMovements.forEach { dao.insertInventoryMovement(it) }
      backup.materialProducts.forEach { dao.insertMaterialProduct(it) }
      backup.calculationSessions.forEach { dao.insertCalculationSession(it) }
      backup.settings.forEach { dao.setSetting(it) }
      dao.ensureSettings()

      dao.insertAudit(
        com.example.data.local.AuditLogEntity(
          username = restoredUser.username,
          action = "RESTORE_BACKUP",
          entityType = "Database",
          entityId = "backup",
          details = "بازیابی کامل پایگاه داده از فایل پشتیبان"
        )
      )
    }

    return buildString {
      append("${backup.customers.size} مشتری، ")
      append("${backup.projects.size} پروژه، ")
      append("${backup.materialProducts.size} کالا، ")
      append("${backup.invoices.size} فاکتور، ")
      append("${backup.payments.size} دریافت و ")
      append("${backup.users.size} کاربر بازیابی شد.")
    }
  }

  // ---------------------------------------------------------------------------------------------
  // JSON helpers
  // ---------------------------------------------------------------------------------------------

  private fun <T> JSONObject.array(key: String, parser: (JSONObject) -> T): List<T> =
    this.optJSONArray(key)?.let { arr ->
      (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(parser) }
    } ?: emptyList()

  // --- entity <-> json ------------------------------------------------------------------------

  private fun UserEntity.toJson() = JSONObject().apply {
    put("id", id); put("username", username); put("fullName", fullName)
    put("passwordHash", passwordHash); put("salt", salt); put("role", role)
    put("isActive", isActive); put("createdAt", createdAt)
  }

  private fun CustomerEntity.toJson() = JSONObject().apply {
    put("id", id); put("code", code); put("fullName", fullName); put("phone", phone)
    put("address", address); put("notes", notes); put("isActive", isActive)
    put("createdAt", createdAt); put("updatedAt", updatedAt)
  }

  private fun ProjectEntity.toJson() = JSONObject().apply {
    put("id", id); put("projectNumber", projectNumber); put("customerId", customerId)
    put("title", title); put("siteAddress", siteAddress); put("status", status)
    put("contractCurrency", contractCurrency); put("createdAt", createdAt)
    expectedDate?.let { put("expectedDate", it) }
    put("notes", notes); put("totalContractAmount", totalContractAmount)
    put("totalPaidAmount", totalPaidAmount); put("balanceDue", balanceDue)
  }

  private fun ProjectItemEntity.toJson() = JSONObject().apply {
    put("id", id); put("projectId", projectId); put("itemCode", itemCode)
    put("description", description); put("productType", productType)
    put("materialFamily", materialFamily); put("widthMm", widthMm); put("heightMm", heightMm)
    put("quantity", quantity); designId?.let { put("designId", it) }
  }

  private fun DesignEntity.toJson() = JSONObject().apply {
    put("id", id); put("name", name); put("productType", productType)
    put("materialFamily", materialFamily); put("profileSystem", profileSystem)
    put("frameFinish", frameFinish); put("sashFinish", sashFinish)
    put("outerGlassTint", outerGlassTint); put("innerGlassTint", innerGlassTint)
    put("widthMm", widthMm); put("heightMm", heightMm); put("hasTransom", hasTransom)
    put("transomHeightMm", transomHeightMm); put("panelsJson", panelsJson)
    put("isApproved", isApproved); put("version", version); put("notes", notes)
    put("updatedAt", updatedAt)
  }

  private fun ProfileEntity.toJson() = JSONObject().apply {
    put("id", id); put("code", code); put("name", name); put("materialFamily", materialFamily)
    put("brandSystem", brandSystem); put("sectionType", sectionType)
    put("stockLengthMm", stockLengthMm); put("purchasePrice", purchasePrice)
    put("currency", currency); put("supplierName", supplierName); put("isActive", isActive)
  }

  private fun GlassEntity.toJson() = JSONObject().apply {
    put("id", id); put("title", title); put("glazingType", glazingType)
    put("thicknessMm", thicknessMm); put("tint", tint); put("pricePerM2", pricePerM2)
    put("currency", currency); put("supplierName", supplierName); put("isActive", isActive)
  }

  private fun AccessoryEntity.toJson() = JSONObject().apply {
    put("id", id); put("code", code); put("name", name); put("category", category)
    put("unit", unit); put("unitPrice", unitPrice); put("currency", currency)
    put("isActive", isActive)
  }

  private fun InvoiceEntity.toJson() = JSONObject().apply {
    put("id", id); put("projectId", projectId); put("invoiceNumber", invoiceNumber)
    put("isFinalInvoice", isFinalInvoice); put("currency", currency); put("subtotal", subtotal)
    put("discountAmount", discountAmount); put("taxAmount", taxAmount)
    put("grandTotal", grandTotal); put("issueDate", issueDate)
    put("isApproved", isApproved); put("notes", notes)
  }

  private fun PaymentEntity.toJson() = JSONObject().apply {
    put("id", id); put("customerId", customerId); put("projectId", projectId)
    invoiceId?.let { put("invoiceId", it) }
    put("amount", amount); put("currency", currency)
    put("exchangeRateToAfn", exchangeRateToAfn); put("paymentMethod", paymentMethod)
    put("referenceNumber", referenceNumber); put("paymentDate", paymentDate)
    put("notes", notes)
  }

  private fun ExpenseEntity.toJson() = JSONObject().apply {
    put("id", id); projectId?.let { put("projectId", it) }
    put("title", title); put("category", category); put("amount", amount)
    put("currency", currency); put("expenseDate", expenseDate); put("notes", notes)
  }

  private fun InventoryMovementEntity.toJson() = JSONObject().apply {
    put("id", id); put("materialId", materialId); put("materialName", materialName)
    put("movementType", movementType); put("quantity", quantity); put("unit", unit)
    projectId?.let { put("projectId", it) }
    put("date", date); put("notes", notes)
  }

  private fun SettingEntity.toJson() = JSONObject().apply {
    put("key", key); put("value", value)
  }

  private fun MaterialProductEntity.toJson() = JSONObject().apply {
    put("id", id); put("code", code); put("name", name); put("category", category)
    put("brandModel", brandModel); put("materialFamily", materialFamily)
    put("description", description); put("specifications", specifications)
    put("unit", unit); put("purchasePriceAfn", purchasePriceAfn)
    put("purchasePriceUsd", purchasePriceUsd); put("stockLengthMm", stockLengthMm)
    put("thicknessMm", thicknessMm); put("colorFinish", colorFinish)
    put("compatibility", compatibility); put("stockQty", stockQty)
    put("minStockLevel", minStockLevel); put("supplierName", supplierName)
    put("isActive", isActive); put("createdAt", createdAt)
  }

  private fun ProjectCalculationSessionEntity.toJson() = JSONObject().apply {
    put("id", id); put("sessionCode", sessionCode); put("customerName", customerName)
    put("customerPhone", customerPhone); put("title", title); put("siteLocation", siteLocation)
    put("currency", currency); put("exchangeRate", exchangeRate)
    put("pricingBasis", pricingBasis); put("itemsJson", itemsJson)
    put("transportCost", transportCost); put("installationCost", installationCost)
    put("discountAmount", discountAmount); put("markupPercent", markupPercent)
    put("totalAmount", totalAmount); put("status", status); put("notes", notes)
    put("createdAt", createdAt)
  }

  // --- json -> entity (defaults make older backups forward compatible) -------------------------

  private fun JSONObject.str(key: String, fallback: String = ""): String =
    if (has(key) && !isNull(key)) optString(key, fallback) else fallback

  private fun JSONObject.num(key: String, fallback: Long = 0L): Long =
    if (has(key) && !isNull(key)) optLong(key, fallback) else fallback

  private fun JSONObject.bool(key: String, fallback: Boolean = false): Boolean =
    if (has(key) && !isNull(key)) optBoolean(key, fallback) else fallback

  private fun userFromJson(o: JSONObject) = UserEntity(
    id = o.str("id"), username = o.str("username"), fullName = o.str("fullName"),
    passwordHash = o.str("passwordHash"), salt = o.str("salt"),
    role = o.str("role", "READ_ONLY"), isActive = o.bool("isActive", true),
    createdAt = o.num("createdAt", System.currentTimeMillis())
  )

  private fun customerFromJson(o: JSONObject) = CustomerEntity(
    id = o.str("id"), code = o.str("code"), fullName = o.str("fullName"), phone = o.str("phone"),
    address = o.str("address"), notes = o.str("notes"), isActive = o.bool("isActive", true),
    createdAt = o.num("createdAt", System.currentTimeMillis()),
    updatedAt = o.num("updatedAt", System.currentTimeMillis())
  )

  private fun projectFromJson(o: JSONObject) = ProjectEntity(
    id = o.str("id"), projectNumber = o.str("projectNumber"), customerId = o.str("customerId"),
    title = o.str("title"), siteAddress = o.str("siteAddress"), status = o.str("status", "DRAFT"),
    contractCurrency = o.str("contractCurrency", "AFN"),
    createdAt = o.num("createdAt", System.currentTimeMillis()),
    expectedDate = if (o.has("expectedDate") && !o.isNull("expectedDate")) o.optLong("expectedDate") else null,
    notes = o.str("notes"), totalContractAmount = o.str("totalContractAmount", "0.00"),
    totalPaidAmount = o.str("totalPaidAmount", "0.00"), balanceDue = o.str("balanceDue", "0.00")
  )

  private fun projectItemFromJson(o: JSONObject) = ProjectItemEntity(
    id = o.str("id"), projectId = o.str("projectId"), itemCode = o.str("itemCode"),
    description = o.str("description"), productType = o.str("productType", "WINDOW"),
    materialFamily = o.str("materialFamily", "UPVC"), widthMm = o.num("widthMm").toInt(),
    heightMm = o.num("heightMm").toInt(), quantity = o.num("quantity", 1).toInt(),
    designId = o.str("designId").ifBlank { null }
  )

  private fun designFromJson(o: JSONObject) = DesignEntity(
    id = o.str("id"), name = o.str("name"), productType = o.str("productType", "WINDOW"),
    materialFamily = o.str("materialFamily", "UPVC"), profileSystem = o.str("profileSystem"),
    frameFinish = o.str("frameFinish", "WHITE"), sashFinish = o.str("sashFinish", "WHITE"),
    outerGlassTint = o.str("outerGlassTint", "CLEAR"), innerGlassTint = o.str("innerGlassTint", "CLEAR"),
    widthMm = o.num("widthMm").toInt(), heightMm = o.num("heightMm").toInt(),
    hasTransom = o.bool("hasTransom"), transomHeightMm = o.num("transomHeightMm").toInt(),
    panelsJson = o.str("panelsJson", "[]"), isApproved = o.bool("isApproved"),
    version = o.num("version", 1).toInt(), notes = o.str("notes"),
    updatedAt = o.num("updatedAt", System.currentTimeMillis())
  )

  private fun profileFromJson(o: JSONObject) = ProfileEntity(
    id = o.str("id"), code = o.str("code"), name = o.str("name"),
    materialFamily = o.str("materialFamily", "UPVC"), brandSystem = o.str("brandSystem"),
    sectionType = o.str("sectionType", "فریم"), stockLengthMm = o.num("stockLengthMm", 6000).toInt(),
    purchasePrice = o.str("purchasePrice", "0.00"), currency = o.str("currency", "AFN"),
    supplierName = o.str("supplierName"), isActive = o.bool("isActive", true)
  )

  private fun glassFromJson(o: JSONObject) = GlassEntity(
    id = o.str("id"), title = o.str("title"), glazingType = o.str("glazingType", "دوجداره"),
    thicknessMm = o.num("thicknessMm", 20).toInt(), tint = o.str("tint"),
    pricePerM2 = o.str("pricePerM2", "0.00"), currency = o.str("currency", "AFN"),
    supplierName = o.str("supplierName"), isActive = o.bool("isActive", true)
  )

  private fun accessoryFromJson(o: JSONObject) = AccessoryEntity(
    id = o.str("id"), code = o.str("code"), name = o.str("name"),
    category = o.str("category"), unit = o.str("unit", "عدد"),
    unitPrice = o.str("unitPrice", "0.00"), currency = o.str("currency", "AFN"),
    isActive = o.bool("isActive", true)
  )

  private fun invoiceFromJson(o: JSONObject) = InvoiceEntity(
    id = o.str("id"), projectId = o.str("projectId"), invoiceNumber = o.str("invoiceNumber"),
    isFinalInvoice = o.bool("isFinalInvoice"), currency = o.str("currency", "AFN"),
    subtotal = o.str("subtotal", "0.00"), discountAmount = o.str("discountAmount", "0.00"),
    taxAmount = o.str("taxAmount", "0.00"), grandTotal = o.str("grandTotal", "0.00"),
    issueDate = o.num("issueDate", System.currentTimeMillis()),
    isApproved = o.bool("isApproved"), notes = o.str("notes")
  )

  private fun paymentFromJson(o: JSONObject) = PaymentEntity(
    id = o.str("id"), customerId = o.str("customerId"), projectId = o.str("projectId"),
    invoiceId = o.str("invoiceId").ifBlank { null }, amount = o.str("amount", "0.00"),
    currency = o.str("currency", "AFN"), exchangeRateToAfn = o.str("exchangeRateToAfn", "1.00"),
    paymentMethod = o.str("paymentMethod", "نقدی"),
    referenceNumber = o.str("referenceNumber"),
    paymentDate = o.num("paymentDate", System.currentTimeMillis()), notes = o.str("notes")
  )

  private fun expenseFromJson(o: JSONObject) = ExpenseEntity(
    id = o.str("id"), projectId = o.str("projectId").ifBlank { null }, title = o.str("title"),
    category = o.str("category"), amount = o.str("amount", "0.00"),
    currency = o.str("currency", "AFN"),
    expenseDate = o.num("expenseDate", System.currentTimeMillis()), notes = o.str("notes")
  )

  private fun inventoryMovementFromJson(o: JSONObject) = InventoryMovementEntity(
    id = o.str("id"), materialId = o.str("materialId"), materialName = o.str("materialName"),
    movementType = o.str("movementType", "PURCHASE_IN"), quantity = o.str("quantity", "0"),
    unit = o.str("unit", "عدد"), projectId = o.str("projectId").ifBlank { null },
    date = o.num("date", System.currentTimeMillis()), notes = o.str("notes")
  )

  private fun settingFromJson(o: JSONObject) =
    SettingEntity(key = o.str("key"), value = o.str("value"))

  private fun materialProductFromJson(o: JSONObject) = MaterialProductEntity(
    id = o.str("id"), code = o.str("code"), name = o.str("name"), category = o.str("category"),
    brandModel = o.str("brandModel"), materialFamily = o.str("materialFamily", "UPVC"),
    description = o.str("description"), specifications = o.str("specifications"),
    unit = o.str("unit", "عدد"), purchasePriceAfn = o.str("purchasePriceAfn", "0.00"),
    purchasePriceUsd = o.str("purchasePriceUsd", "0.00"),
    stockLengthMm = o.num("stockLengthMm").toInt(), thicknessMm = o.num("thicknessMm").toInt(),
    colorFinish = o.str("colorFinish", "سفید"), compatibility = o.str("compatibility", "عمومی"),
    stockQty = o.str("stockQty", "0.00"), minStockLevel = o.str("minStockLevel", "0.00"),
    supplierName = o.str("supplierName"), isActive = o.bool("isActive", true),
    createdAt = o.num("createdAt", System.currentTimeMillis())
  )

  private fun calculationSessionFromJson(o: JSONObject) =
    ProjectCalculationSessionEntity(
      id = o.str("id"), sessionCode = o.str("sessionCode"),
      customerName = o.str("customerName"), customerPhone = o.str("customerPhone"),
      title = o.str("title"), siteLocation = o.str("siteLocation"),
      currency = o.str("currency", "AFN"), exchangeRate = o.str("exchangeRate", "72.50"),
      pricingBasis = o.str("pricingBasis"), itemsJson = o.str("itemsJson", "[]"),
      transportCost = o.str("transportCost", "0.00"),
      installationCost = o.str("installationCost", "0.00"),
      discountAmount = o.str("discountAmount", "0.00"),
      markupPercent = o.str("markupPercent", "25.00"), totalAmount = o.str("totalAmount", "0.00"),
      status = o.str("status", "DRAFT"), notes = o.str("notes"),
      createdAt = o.num("createdAt", System.currentTimeMillis())
    )
}
