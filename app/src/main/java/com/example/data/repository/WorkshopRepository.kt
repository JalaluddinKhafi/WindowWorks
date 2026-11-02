package com.example.data.repository

import com.example.core.AppSettings
import com.example.core.CurrencyHelper
import com.example.core.SecurityHelper
import com.example.core.UserRole
import com.example.data.backup.BackupManager
import com.example.data.local.*
import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode

class WorkshopRepository(private val dao: AppDao) {

  // ---------------------------------------------------------------------------------------------
  // Session
  // ---------------------------------------------------------------------------------------------

  /** Null until somebody logs in; every permission check goes through [currentRole]. */
  var currentUserId: String? = null
    private set
  var currentUsername: String = ""
    private set
  var currentFullName: String = ""
    private set
  var currentRole: UserRole = UserRole.READ_ONLY
    private set
  var mustChangePassword: Boolean = false
    private set

  val isSignedIn: Boolean get() = currentUserId != null

  private val _sessionState = MutableStateFlow(0L)

  /** Emits on every login / logout / password / role change so the UI can recompose. */
  val sessionState: StateFlow<Long> = _sessionState.asStateFlow()

  private fun notifySessionChanged() {
    _sessionState.value = _sessionState.value + 1
  }

  suspend fun login(username: String, password: String): String? {
    val user = dao.getUserByUsername(username.trim())
      ?: return "نام کاربری یا رمز عبور اشتباه است."
    if (!user.isActive) return "حساب کاربری غیرفعال شده است."
    if (!SecurityHelper.verifyPassword(password, user.salt, user.passwordHash)) {
      return "نام کاربری یا رمز عبور اشتباه است."
    }

    // Transparently upgrade legacy SHA-256 hashes to PBKDF2 on a successful login.
    if (SecurityHelper.needsRehash(user.passwordHash)) {
      val salt = SecurityHelper.generateSalt()
      dao.updateUser(
        user.copy(salt = salt, passwordHash = SecurityHelper.hashPassword(password, salt))
      )
    }

    applySession(user)
    return null
  }

  suspend fun logout() {
    currentUserId = null
    currentUsername = ""
    currentFullName = ""
    currentRole = UserRole.READ_ONLY
    mustChangePassword = false
    dao.setSetting(SettingEntity(AppSettings.KEY_LAST_USER_ID, ""))
    notifySessionChanged()
  }

  /** Used by the backup restore flow to re-apply the signed-in account afterwards. */
  suspend fun reloadSession() {
    val id = currentUserId ?: return
    val user = userEntityById(id)
    if (user != null && user.isActive) applySession(user) else logout()
  }

  /** Restores the previously signed-in user after an app restart. */
  suspend fun restoreSession() {
    val id = dao.getSetting(AppSettings.KEY_LAST_USER_ID)
    if (id.isNullOrBlank()) {
      logout()
      return
    }
    val user = userEntityById(id)
    if (user != null && user.isActive) applySession(user) else logout()
  }

  private suspend fun userEntityById(id: String): UserEntity? =
    dao.selectAllUsers().find { it.id == id }

  private suspend fun applySession(user: UserEntity) {
    currentUserId = user.id
    currentUsername = user.username
    currentFullName = user.fullName
    currentRole = UserRole.fromCode(user.role)
    // The bootstrap admin must replace the default password; the flag lives in app_settings.
    mustChangePassword = dao.getSetting(AppSettings.KEY_FORCE_PASSWORD_CHANGE) == "true" &&
      user.role == UserRole.OWNER_ADMIN.code
    dao.setSetting(SettingEntity(AppSettings.KEY_LAST_USER_ID, user.id))
    notifySessionChanged()
  }

  /** Changes the password of the signed-in user. Returns an error message or null on success. */
  suspend fun changePassword(oldPassword: String, newPassword: String): String? {
    val userId = currentUserId ?: return "ابتدا وارد حساب خود شوید."
    if (newPassword.length < 6) return "رمز عبور جدید باید حداقل ۶ کاراکتر باشد."
    val user = userEntityById(userId) ?: return "کاربر یافت نشد."
    if (!SecurityHelper.verifyPassword(oldPassword, user.salt, user.passwordHash)) {
      return "رمز عبور فعلی درست نیست."
    }
    val salt = SecurityHelper.generateSalt()
    dao.updateUser(
      user.copy(
        salt = salt,
        passwordHash = SecurityHelper.hashPassword(newPassword, salt),
        isActive = true
      )
    )
    dao.setSetting(SettingEntity(AppSettings.KEY_FORCE_PASSWORD_CHANGE, "false"))
    applySession(user)
    audit("CHANGE_PASSWORD", "User", userId, "تغییر رمز عبور کاربر ${user.username}")
    return null
  }

  /**
   * Emergency password reset for admin account in case user forgets their password.
   * Resets password to [newPassword] (default "admin123") and ensures admin account is active.
   */
  suspend fun resetAdminPassword(newPassword: String = "admin123"): Boolean {
    val allUsers = dao.selectAllUsers()
    val admin = allUsers.find { it.role == UserRole.OWNER_ADMIN.code }
      ?: allUsers.find { it.username.equals("admin", ignoreCase = true) }
      ?: allUsers.firstOrNull()

    if (admin != null) {
      val salt = SecurityHelper.generateSalt()
      dao.updateUser(
        admin.copy(
          salt = salt,
          passwordHash = SecurityHelper.hashPassword(newPassword, salt),
          isActive = true
        )
      )
      dao.setSetting(SettingEntity(AppSettings.KEY_FORCE_PASSWORD_CHANGE, "false"))
      audit("RESET_PASSWORD", "User", admin.id, "بازیابی اضطراری رمز عبور مدیر (${admin.username})")
      return true
    } else {
      val salt = SecurityHelper.generateSalt()
      dao.insertUser(
        UserEntity(
          id = "user_admin",
          username = "admin",
          fullName = "مدیر ارشد کارگاه",
          passwordHash = SecurityHelper.hashPassword(newPassword, salt),
          salt = salt,
          role = UserRole.OWNER_ADMIN.code,
          isActive = true
        )
      )
      dao.setSetting(SettingEntity(AppSettings.KEY_FORCE_PASSWORD_CHANGE, "false"))
      return true
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Users & audit
  // ---------------------------------------------------------------------------------------------

  fun getAllUsers(): Flow<List<UserModel>> = dao.getAllUsersIncludingInactive().map { users ->
    users.map {
      UserModel(
        id = it.id,
        username = it.username,
        fullName = it.fullName,
        role = UserRole.fromCode(it.role),
        isActive = it.isActive,
        createdAt = it.createdAt
      )
    }
  }

  fun getRecentAuditLogs(): Flow<List<AuditLogModel>> = dao.getRecentAuditLogs().map { logs ->
    logs.map {
      AuditLogModel(
        id = it.id,
        username = it.username,
        action = it.action,
        entityType = it.entityType,
        entityId = it.entityId,
        details = it.details,
        timestamp = it.timestamp
      )
    }
  }

  /** Creates a user. [password] is only required for new accounts. */
  suspend fun saveUser(user: UserModel, password: String?): String? {
    if (!currentRole.canManageUsers()) return "شما اجازه مدیریت کاربران را ندارید."
    if (user.username.isBlank()) return "نام کاربری الزامی است."
    val existing = dao.getUserByUsername(user.username.trim())
    if (existing != null && existing.id != user.id) return "این نام کاربری قبلاً ثبت شده است."

    val current = userEntityById(user.id)
    if (current == null) {
      if (password.isNullOrBlank() || password.length < 6) {
        return "برای کاربر جدید رمز عبور حداقل ۶ کاراکتری لازم است."
      }
      val salt = SecurityHelper.generateSalt()
      dao.insertUser(
        UserEntity(
          id = user.id,
          username = user.username.trim(),
          fullName = user.fullName.trim(),
          passwordHash = SecurityHelper.hashPassword(password, salt),
          salt = salt,
          role = user.role.code,
          isActive = user.isActive
        )
      )
      audit("CREATE_USER", "User", user.id, "ایجاد کاربر ${user.username} با نقش ${user.role.titlePersian}")
    } else {
      if (current.id == currentUserId && current.role == UserRole.OWNER_ADMIN.code &&
        user.role != UserRole.OWNER_ADMIN
      ) {
        return "نمی‌توانید نقش حساب کاربری خودتان را تنزل دهید."
      }
      val newSalt = if (password.isNullOrBlank()) current.salt else SecurityHelper.generateSalt()
      val newHash = if (password.isNullOrBlank()) current.passwordHash
      else SecurityHelper.hashPassword(password, newSalt)
      dao.updateUser(
        current.copy(
          username = user.username.trim(),
          fullName = user.fullName.trim(),
          role = user.role.code,
          isActive = user.isActive,
          salt = newSalt,
          passwordHash = newHash
        )
      )
      audit("UPDATE_USER", "User", user.id, "ویرایش کاربر ${user.username}")
    }
    return null
  }

  suspend fun deleteUser(userId: String): String? {
    if (!currentRole.canManageUsers()) return "شما اجازه حذف کاربر را ندارید."
    if (userId == currentUserId) return "حساب کاربری جاری قابل حذف نیست."
    val target = userEntityById(userId) ?: return null
    val admins = dao.selectAllUsers().filter { it.role == UserRole.OWNER_ADMIN.code && it.isActive }
    if (target.role == UserRole.OWNER_ADMIN.code && admins.size <= 1) {
      return "حداقل یک مدیر کارگاه باید باقی بماند."
    }
    dao.deleteUser(userId)
    audit("DELETE_USER", "User", userId, "حذف کاربر ${target.username}")
    return null
  }

  private suspend fun audit(action: String, entityType: String, entityId: String, details: String) {
    dao.insertAudit(
      AuditLogEntity(
        username = currentUsername.ifBlank { "system" },
        action = action,
        entityType = entityType,
        entityId = entityId,
        details = details
      )
    )
  }

  // ---------------------------------------------------------------------------------------------
  // Settings
  // ---------------------------------------------------------------------------------------------

  fun getSettings(): Flow<AppSettings> = dao.getAllSettings().map { list ->
    AppSettings.fromMap(list.associate { it.key to it.value })
  }

  suspend fun saveSettings(settings: AppSettings): String? {
    if (!currentRole.canEditCatalogAndPrices()) return "شما اجازه تغییر تنظیمات کارگاه را ندارید."
    if (settings.exchangeRateUsdToAfn <= BigDecimal.ZERO) {
      return "نرخ برابری دالر باید بزرگتر از صفر باشد."
    }
    AppSettings.toMap(settings).forEach { (key, value) ->
      dao.setSetting(SettingEntity(key, value))
    }
    audit("SAVE_SETTINGS", "Settings", "app", "به‌روزرسانی تنظیمات کارگاه")
    return null
  }

  suspend fun setLanguage(languageCode: String) {
    dao.setSetting(SettingEntity(AppSettings.KEY_APP_LANGUAGE, languageCode))
  }

  // ---------------------------------------------------------------------------------------------
  // Backup & restore
  // ---------------------------------------------------------------------------------------------

  suspend fun exportBackupJson(): String {
    return BackupManager.export(dao)
  }

  suspend fun restoreBackupJson(json: String): RestoreResult {
    if (!currentRole.canExportOrBackup()) {
      return RestoreResult.Failure("فقط مدیر کارگاه اجازه بازیابی اطلاعات را دارد.")
    }
    return try {
      val snapshot = BackupManager.parse(json)
      val summary = BackupManager.restore(dao, snapshot, currentUserId)
      // The signed-in account may have been replaced by the backup contents.
      reloadSession()
      RestoreResult.Success(summary)
    } catch (e: BackupManager.BackupException) {
      RestoreResult.Failure(e.message ?: "فایل پشتیبان معتبر نیست.")
    } catch (e: Exception) {
      RestoreResult.Failure("خطا در بازیابی اطلاعات: ${e.message ?: "نامشخص"}")
    }
  }

  // Customers. Deactivated customers are still returned so history/backups stay consistent.
  fun getAllCustomers(): Flow<List<CustomerModel>> = dao.getAllCustomersIncludingInactive().map { entities ->
    entities.map {
      CustomerModel(
        id = it.id,
        code = it.code,
        fullName = it.fullName,
        phone = it.phone,
        address = it.address,
        notes = it.notes,
        isActive = it.isActive,
        createdAt = it.createdAt
      )
    }
  }

  suspend fun saveCustomer(customer: CustomerModel) {
    if (!currentRole.canManageCustomers()) return
    dao.insertCustomer(
      CustomerEntity(
        id = customer.id,
        code = customer.code,
        fullName = customer.fullName,
        phone = customer.phone,
        address = customer.address,
        notes = customer.notes,
        isActive = customer.isActive,
        createdAt = customer.createdAt,
        updatedAt = System.currentTimeMillis()
      )
    )
    dao.insertAudit(
      AuditLogEntity(
        username = currentUsername,
        action = "SAVE_CUSTOMER",
        entityType = "Customer",
        entityId = customer.id,
        details = "ثبت/ویرایش مشتری ${customer.fullName}"
      )
    )
  }

  // Projects
  fun getAllProjects(): Flow<List<ProjectModel>> = dao.getAllProjects().map { entities ->
    entities.map { mapProjectEntityToModel(it) }
  }

  fun getProjectsByCustomer(customerId: String): Flow<List<ProjectModel>> =
    dao.getProjectsByCustomer(customerId).map { entities ->
      entities.map { mapProjectEntityToModel(it) }
    }

  private fun mapProjectEntityToModel(entity: ProjectEntity): ProjectModel {
    val statusEnum = try {
      ProjectStatus.valueOf(entity.status)
    } catch (_: Exception) {
      ProjectStatus.DRAFT
    }
    return ProjectModel(
      id = entity.id,
      projectNumber = entity.projectNumber,
      customerId = entity.customerId,
      title = entity.title,
      siteAddress = entity.siteAddress,
      status = statusEnum,
      contractCurrency = entity.contractCurrency,
      createdAt = entity.createdAt,
      expectedCompletionDate = entity.expectedDate,
      notes = entity.notes,
      totalContractAmount = CurrencyHelper.parseAmount(entity.totalContractAmount),
      totalPaidAmount = CurrencyHelper.parseAmount(entity.totalPaidAmount),
      balanceDue = CurrencyHelper.parseAmount(entity.balanceDue)
    )
  }

  suspend fun saveProject(project: ProjectModel) {
    if (!currentRole.canManageProjects()) return
    dao.insertProject(
      ProjectEntity(
        id = project.id,
        projectNumber = project.projectNumber,
        customerId = project.customerId,
        title = project.title,
        siteAddress = project.siteAddress,
        status = project.status.name,
        contractCurrency = project.contractCurrency,
        createdAt = project.createdAt,
        expectedDate = project.expectedCompletionDate,
        notes = project.notes,
        totalContractAmount = project.totalContractAmount.toPlainString(),
        totalPaidAmount = project.totalPaidAmount.toPlainString(),
        balanceDue = project.balanceDue.toPlainString()
      )
    )
    dao.insertAudit(
      AuditLogEntity(
        username = currentUsername,
        action = "SAVE_PROJECT",
        entityType = "Project",
        entityId = project.id,
        details = "ذخیره پروژه ${project.title} (${project.projectNumber})"
      )
    )
  }

  suspend fun deleteProject(id: String) {
    if (!currentRole.canManageProjects()) return
    dao.deleteItemsByProject(id)
    dao.deleteProject(id)
    dao.insertAudit(
      AuditLogEntity(
        username = currentUsername,
        action = "DELETE_PROJECT",
        entityType = "Project",
        entityId = id,
        details = "حذف پروژه"
      )
    )
  }

  // Project Items
  fun getProjectItems(projectId: String): Flow<List<ProjectItemModel>> =
    dao.getItemsByProject(projectId).map { entities ->
      entities.map {
        ProjectItemModel(
          id = it.id,
          projectId = it.projectId,
          itemCode = it.itemCode,
          description = it.description,
          productType = try { ProductType.valueOf(it.productType) } catch (_: Exception) { ProductType.WINDOW },
          materialFamily = try { MaterialFamily.valueOf(it.materialFamily) } catch (_: Exception) { MaterialFamily.UPVC },
          widthMm = it.widthMm,
          heightMm = it.heightMm,
          quantity = it.quantity,
          designId = it.designId
        )
      }
    }

  suspend fun saveProjectItem(item: ProjectItemModel) {
    if (!currentRole.canManageProjects()) return
    dao.insertProjectItem(
      ProjectItemEntity(
        id = item.id,
        projectId = item.projectId,
        itemCode = item.itemCode,
        description = item.description,
        productType = item.productType.name,
        materialFamily = item.materialFamily.name,
        widthMm = item.widthMm,
        heightMm = item.heightMm,
        quantity = item.quantity,
        designId = item.designId
      )
    )
  }

  suspend fun deleteProjectItem(itemId: String) {
    dao.deleteProjectItem(itemId)
  }

  // Designs
  suspend fun getDesign(id: String): WindowDesign? {
    val entity = dao.getDesignById(id) ?: return null
    return deserializeDesign(entity)
  }

  suspend fun saveDesign(design: WindowDesign) {
    if (!currentRole.canDesignWindows()) return
    val entity = serializeDesign(design)
    dao.insertDesign(entity)
    dao.insertAudit(
      AuditLogEntity(
        username = currentUsername,
        action = "SAVE_DESIGN",
        entityType = "Design",
        entityId = design.id,
        details = "ذخیره طراحی ${design.name} ابعاد ${design.widthMm}x${design.heightMm}"
      )
    )
  }

  private fun serializeDesign(design: WindowDesign): DesignEntity {
    val arr = JSONArray()
    for (p in design.panels) {
      val obj = JSONObject().apply {
        put("id", p.id)
        put("openingType", p.openingType.name)
        put("openingDirection", p.openingDirection.name)
        put("glazingType", p.glazingType.name)
        put("glassTint", p.glassTint.name)
        put("innerGlassTint", p.innerGlassTint.name)
        put("sashFinish", p.sashFinish?.name ?: "")
        put("widthRatio", p.widthRatio.toDouble())
        put("handleModel", p.handleModel)
        put("hasHandle", p.hasHandle)
        put("handleX", p.handleX?.toDouble())
        put("handleY", p.handleY.toDouble())
        put("lockPoints", p.lockPoints)
      }
      arr.put(obj)
    }
    if (arr.length() > 0) {
      design.layoutRoot?.let { arr.getJSONObject(0).put("layoutRoot", DesignLayoutCodec.encode(it)) }
      arr.getJSONObject(0).put("designOpeningDirection", design.openingDirection.name)
    }

    return DesignEntity(
      id = design.id,
      name = design.name,
      productType = design.productType.name,
      materialFamily = design.materialFamily.name,
      profileSystem = design.profileSystem,
      frameFinish = design.frameFinish.name,
      sashFinish = design.sashFinish.name,
      outerGlassTint = design.outerGlassTint.name,
      innerGlassTint = design.innerGlassTint.name,
      widthMm = design.widthMm,
      heightMm = design.heightMm,
      hasTransom = design.hasTransom,
      transomHeightMm = design.transomHeightMm,
      panelsJson = arr.toString(),
      isApproved = design.isApproved,
      version = design.version,
      notes = design.notes
    )
  }

  private fun deserializeDesign(entity: DesignEntity): WindowDesign {
    val panels = mutableListOf<PanelConfig>()
    var layoutRoot: LayoutNode? = null
    var designDirection = OpeningDirection.INWARD
    try {
      val arr = JSONArray(entity.panelsJson)
      if (arr.length() > 0) {
        arr.getJSONObject(0).optJSONObject("layoutRoot")?.let { layoutRoot = DesignLayoutCodec.decode(it) }
        designDirection = runCatching { OpeningDirection.valueOf(arr.getJSONObject(0).optString("designOpeningDirection", "INWARD")) }.getOrDefault(OpeningDirection.INWARD)
      }
      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        val sashFinStr = obj.optString("sashFinish", "")
        panels.add(
          PanelConfig(
            id = obj.optString("id", "p_$i"),
            openingType = try { OpeningType.valueOf(obj.optString("openingType", "FIXED")) } catch (_: Exception) { OpeningType.FIXED },
            openingDirection = try { OpeningDirection.valueOf(obj.optString("openingDirection", "INWARD")) } catch (_: Exception) { OpeningDirection.INWARD },
            glazingType = try { GlazingType.valueOf(obj.optString("glazingType", "DOUBLE_STANDARD")) } catch (_: Exception) { GlazingType.DOUBLE_STANDARD },
            glassTint = try { GlassTint.valueOf(obj.optString("glassTint", "CLEAR")) } catch (_: Exception) { GlassTint.CLEAR },
            innerGlassTint = try { GlassTint.valueOf(obj.optString("innerGlassTint", "CLEAR")) } catch (_: Exception) { GlassTint.CLEAR },
            sashFinish = if (sashFinStr.isNotBlank()) try { FrameFinish.valueOf(sashFinStr) } catch (_: Exception) { null } else null,
            widthRatio = obj.optDouble("widthRatio", 1.0).toFloat(),
            handleModel = obj.optString("handleModel", "دستگیره روتو استاندارد"),
            hasHandle = obj.optBoolean("hasHandle", true),
            handleX = if (obj.has("handleX") && !obj.isNull("handleX")) obj.optDouble("handleX", 0.5).toFloat().coerceIn(0f, 1f) else null,
            handleY = obj.optDouble("handleY", 0.5).toFloat().coerceIn(0f, 1f),
            lockPoints = obj.optInt("lockPoints", 1).coerceIn(1, 10)
          )
        )
      }
    } catch (_: Exception) {
      panels.clear()
      layoutRoot = null
      panels.add(PanelConfig("p1", OpeningType.FIXED))
    }

    return WindowDesign(
      id = entity.id,
      name = entity.name,
      productType = try { ProductType.valueOf(entity.productType) } catch (_: Exception) { ProductType.WINDOW },
      materialFamily = try { MaterialFamily.valueOf(entity.materialFamily) } catch (_: Exception) { MaterialFamily.UPVC },
      profileSystem = entity.profileSystem,
      frameFinish = try { FrameFinish.valueOf(entity.frameFinish) } catch (_: Exception) { FrameFinish.WHITE },
      sashFinish = try { FrameFinish.valueOf(entity.sashFinish) } catch (_: Exception) { FrameFinish.WHITE },
      outerGlassTint = try { GlassTint.valueOf(entity.outerGlassTint) } catch (_: Exception) { GlassTint.CLEAR },
      innerGlassTint = try { GlassTint.valueOf(entity.innerGlassTint) } catch (_: Exception) { GlassTint.CLEAR },
      widthMm = entity.widthMm,
      heightMm = entity.heightMm,
      hasTransom = entity.hasTransom,
      transomHeightMm = entity.transomHeightMm,
      panels = if (panels.isEmpty()) listOf(PanelConfig("p1")) else panels,
      layoutRoot = layoutRoot,
      openingDirection = designDirection,
      isApproved = entity.isApproved,
      version = entity.version,
      notes = entity.notes
    )
  }

  // Finance
  fun getInvoicesByProject(projectId: String): Flow<List<InvoiceModel>> =
    dao.getInvoicesByProject(projectId).map { entities ->
      entities.map {
        InvoiceModel(
          id = it.id,
          projectId = it.projectId,
          invoiceNumber = it.invoiceNumber,
          isFinalInvoice = it.isFinalInvoice,
          currency = it.currency,
          subtotal = CurrencyHelper.parseAmount(it.subtotal),
          discountAmount = CurrencyHelper.parseAmount(it.discountAmount),
          taxAmount = CurrencyHelper.parseAmount(it.taxAmount),
          grandTotal = CurrencyHelper.parseAmount(it.grandTotal),
          issueDate = it.issueDate,
          isApproved = it.isApproved,
          notes = it.notes
        )
      }
    }

  suspend fun saveInvoice(invoice: InvoiceModel) {
    if (!currentRole.canCreateInvoices()) return
    dao.insertInvoice(
      InvoiceEntity(
        id = invoice.id,
        projectId = invoice.projectId,
        invoiceNumber = invoice.invoiceNumber,
        isFinalInvoice = invoice.isFinalInvoice,
        currency = invoice.currency,
        subtotal = invoice.subtotal.toPlainString(),
        discountAmount = invoice.discountAmount.toPlainString(),
        taxAmount = invoice.taxAmount.toPlainString(),
        grandTotal = invoice.grandTotal.toPlainString(),
        issueDate = invoice.issueDate,
        isApproved = invoice.isApproved,
        notes = invoice.notes
      )
    )
    dao.insertAudit(
      AuditLogEntity(
        username = currentUsername,
        action = if (invoice.isFinalInvoice) "SAVE_INVOICE" else "SAVE_QUOTATION",
        entityType = "Invoice",
        entityId = invoice.id,
        details = "صدور سند شماره ${invoice.invoiceNumber} به مبلغ ${invoice.grandTotal} ${invoice.currency}"
      )
    )
  }

  fun getPaymentsByProject(projectId: String): Flow<List<PaymentModel>> =
    dao.getPaymentsByProject(projectId).map { entities ->
      entities.map {
        PaymentModel(
          id = it.id,
          customerId = it.customerId,
          projectId = it.projectId,
          invoiceId = it.invoiceId,
          amount = CurrencyHelper.parseAmount(it.amount),
          currency = it.currency,
          exchangeRateToAfn = CurrencyHelper.parseAmount(it.exchangeRateToAfn),
          paymentMethod = it.paymentMethod,
          referenceNumber = it.referenceNumber,
          paymentDate = it.paymentDate,
          notes = it.notes
        )
      }
    }

  // ---------------------------------------------------------------------------------------------
  // Catalog: glass & accessories are editable from the catalog screen
  // ---------------------------------------------------------------------------------------------

  /**
   * Recomputes `totalPaidAmount` / `balanceDue` for a project from its payments.
   * Payments in another currency are converted with the rate stored on each payment.
   */
  suspend fun recalculateProjectBalance(projectId: String) {
    val projectEntity = dao.getProjectById(projectId) ?: return
    val contractCurrency = projectEntity.contractCurrency.ifBlank { CurrencyHelper.CURRENCY_AFN }

    var paidInContractCurrency = BigDecimal.ZERO
    dao.selectAllPayments()
      .filter { it.projectId == projectId }
      .forEach { p ->
        val amount = CurrencyHelper.parseAmount(p.amount)
        val rate = CurrencyHelper.parseAmount(p.exchangeRateToAfn)
          .takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ONE
        // payment.currency -> AFN -> contractCurrency
        val inAfn = if (p.currency.equals(CurrencyHelper.CURRENCY_USD, ignoreCase = true)) {
          amount.multiply(rate)
        } else {
          amount
        }
        val inContract = if (contractCurrency.equals(CurrencyHelper.CURRENCY_USD, ignoreCase = true)) {
          if (rate > BigDecimal.ZERO) inAfn.divide(rate, 2, RoundingMode.HALF_UP) else inAfn
        } else {
          inAfn
        }
        paidInContractCurrency = paidInContractCurrency.add(inContract)
      }

    val paid = paidInContractCurrency.setScale(2, RoundingMode.HALF_UP)
    val contract = CurrencyHelper.parseAmount(projectEntity.totalContractAmount)
    val balance = contract.subtract(paid).max(BigDecimal.ZERO)

    dao.updateProject(
      projectEntity.copy(
        totalPaidAmount = paid.toPlainString(),
        balanceDue = balance.toPlainString()
      )
    )
  }

  // ---------------------------------------------------------------------------------------------
  // Expenses
  // ---------------------------------------------------------------------------------------------

  fun getAllExpenses(): Flow<List<ExpenseModel>> = dao.getAllExpenses().map { list ->
    list.map { it.toModel() }
  }

  suspend fun saveExpense(expense: ExpenseModel) {
    if (!currentRole.canRecordPayments()) return
    dao.insertExpense(
      ExpenseEntity(
        id = expense.id,
        projectId = expense.projectId?.ifBlank { null },
        title = expense.title,
        category = expense.category,
        amount = expense.amount.toPlainString(),
        currency = expense.currency,
        expenseDate = expense.expenseDate,
        notes = expense.notes
      )
    )
    audit("SAVE_EXPENSE", "Expense", expense.id, "ثبت هزینه ${expense.title} به مبلغ ${expense.amount} ${expense.currency}")
  }

  suspend fun deletePayment(paymentId: String) {
    if (!currentRole.canRecordPayments()) return
    val payment = dao.getPaymentById(paymentId) ?: return
    dao.deletePayment(paymentId)
    recalculateProjectBalance(payment.projectId)
    audit("DELETE_PAYMENT", "Payment", paymentId, "حذف رسید دریافت وجه")
  }

  suspend fun deleteExpense(expenseId: String) {
    if (!currentRole.canRecordPayments()) return
    dao.deleteExpense(expenseId)
    audit("DELETE_EXPENSE", "Expense", expenseId, "حذف سند هزینه")
  }

  private fun ExpenseEntity.toModel() = ExpenseModel(
    id = id,
    projectId = projectId,
    title = title,
    category = category,
    amount = CurrencyHelper.parseAmount(amount),
    currency = currency,
    expenseDate = expenseDate,
    notes = notes
  )

  // ---------------------------------------------------------------------------------------------
  // Inventory
  // ---------------------------------------------------------------------------------------------

  fun getAllPayments(): Flow<List<PaymentModel>> = dao.getAllPayments().map { entities ->
    entities.map {
      PaymentModel(
        id = it.id,
        customerId = it.customerId,
        projectId = it.projectId,
        invoiceId = it.invoiceId,
        amount = CurrencyHelper.parseAmount(it.amount),
        currency = it.currency,
        exchangeRateToAfn = CurrencyHelper.parseAmount(it.exchangeRateToAfn),
        paymentMethod = it.paymentMethod,
        referenceNumber = it.referenceNumber,
        paymentDate = it.paymentDate,
        notes = it.notes
      )
    }
  }

  fun getAllInvoices(): Flow<List<InvoiceModel>> = dao.getAllInvoices().map { entities ->
    entities.map {
      InvoiceModel(
        id = it.id,
        projectId = it.projectId,
        invoiceNumber = it.invoiceNumber,
        isFinalInvoice = it.isFinalInvoice,
        currency = it.currency,
        subtotal = CurrencyHelper.parseAmount(it.subtotal),
        discountAmount = CurrencyHelper.parseAmount(it.discountAmount),
        taxAmount = CurrencyHelper.parseAmount(it.taxAmount),
        grandTotal = CurrencyHelper.parseAmount(it.grandTotal),
        issueDate = it.issueDate,
        isApproved = it.isApproved,
        notes = it.notes
      )
    }
  }

  fun getInventoryMovements(): Flow<List<InventoryMovementModel>> =
    dao.getAllInventoryMovements().map { list -> list.map { it.toModel() } }

  /**
   * Records an inventory movement and keeps the linked material's stock quantity in sync.
   * A movement without a catalog material (free text) is still stored in the ledger.
   */
  suspend fun recordInventoryMovement(movement: InventoryMovementModel) {
    if (!currentRole.canManageInventory()) return
    if (movement.quantity <= BigDecimal.ZERO) return

    dao.insertInventoryMovement(
      InventoryMovementEntity(
        id = movement.id,
        materialId = movement.materialId,
        materialName = movement.materialName,
        movementType = movement.movementType,
        quantity = movement.quantity.toPlainString(),
        unit = movement.unit,
        projectId = movement.projectId,
        date = movement.date,
        notes = movement.notes
      )
    )

    val product = dao.getMaterialProductById(movement.materialId)
    if (product != null) {
      val currentQty = CurrencyHelper.parseAmount(product.stockQty)
      val delta = if (movement.movementType == InventoryMovementModel.TYPE_PURCHASE_IN) {
        movement.quantity
      } else {
        movement.quantity.negate()
      }
      val newQty = currentQty.add(delta).max(BigDecimal.ZERO)
      dao.updateProductStock(product.id, newQty.toPlainString())
    }

    audit(
      "INVENTORY_MOVEMENT",
      "Inventory",
      movement.id,
      "${InventoryMovementModel.titleFor(movement.movementType)} - ${movement.materialName} (${movement.quantity} ${movement.unit})"
    )
  }

  private fun InventoryMovementEntity.toModel() = InventoryMovementModel(
    id = id,
    materialId = materialId,
    materialName = materialName,
    movementType = movementType,
    quantity = CurrencyHelper.parseAmount(quantity),
    unit = unit,
    projectId = projectId,
    date = date,
    notes = notes
  )

  // Catalog
  fun getAllProfiles(): Flow<List<ProfileEntity>> = dao.getAllProfiles()
  fun getAllGlasses(): Flow<List<GlassEntity>> = dao.getAllGlasses()
  fun getAllAccessories(): Flow<List<AccessoryEntity>> = dao.getAllAccessories()

  // Material Products (اجناس و مواد)
  fun getAllMaterialProducts(): Flow<List<MaterialProductModel>> = dao.getAllMaterialProducts().map { list ->
    list.map { mapProductEntityToModel(it) }
  }

  suspend fun saveMaterialProduct(product: MaterialProductModel) {
    if (!currentRole.canEditCatalogAndPrices()) return
    dao.insertMaterialProduct(
      MaterialProductEntity(
        id = product.id,
        code = product.code,
        name = product.name,
        category = product.category,
        brandModel = product.brandModel,
        materialFamily = product.materialFamily,
        description = product.description,
        specifications = product.specifications,
        unit = product.unit,
        purchasePriceAfn = product.purchasePriceAfn.toPlainString(),
        purchasePriceUsd = product.purchasePriceUsd.toPlainString(),
        stockLengthMm = product.stockLengthMm,
        thicknessMm = product.thicknessMm,
        colorFinish = product.colorFinish,
        compatibility = product.compatibility,
        stockQty = product.stockQty.toPlainString(),
        minStockLevel = product.minStockLevel.toPlainString(),
        supplierName = product.supplierName,
        isActive = product.isActive,
        createdAt = product.createdAt
      )
    )
    dao.insertAudit(
      AuditLogEntity(
        username = currentUsername,
        action = "SAVE_PRODUCT",
        entityType = "MaterialProduct",
        entityId = product.id,
        details = "ثبت/ویرایش محصول ${product.name} (${product.code})"
      )
    )
  }

  suspend fun deleteMaterialProduct(id: String) {
    if (!currentRole.canEditCatalogAndPrices()) return
    dao.deleteMaterialProduct(id)
    audit("DELETE_PRODUCT", "MaterialProduct", id, "حذف کالا از کاتالوگ")
  }

  private fun mapProductEntityToModel(e: MaterialProductEntity): MaterialProductModel {
    return MaterialProductModel(
      id = e.id,
      code = e.code,
      name = e.name,
      category = e.category,
      brandModel = e.brandModel,
      materialFamily = e.materialFamily,
      description = e.description,
      specifications = e.specifications,
      unit = e.unit,
      purchasePriceAfn = CurrencyHelper.parseAmount(e.purchasePriceAfn),
      purchasePriceUsd = CurrencyHelper.parseAmount(e.purchasePriceUsd),
      stockLengthMm = e.stockLengthMm,
      thicknessMm = e.thicknessMm,
      colorFinish = e.colorFinish,
      compatibility = e.compatibility,
      stockQty = CurrencyHelper.parseAmount(e.stockQty),
      minStockLevel = CurrencyHelper.parseAmount(e.minStockLevel),
      supplierName = e.supplierName,
      isActive = e.isActive,
      createdAt = e.createdAt
    )
  }

  // Project Calculation Sessions (محاسبه پروژه)
  fun getAllCalculationSessions(): Flow<List<ProjectCalculationSession>> = dao.getAllCalculationSessions().map { list ->
    list.map { mapSessionEntityToModel(it) }
  }

  suspend fun saveCalculationSession(session: ProjectCalculationSession) {
    if (!currentRole.canCreateInvoices()) return
    val itemsArr = JSONArray()
    for (item in session.items) {
      val obj = JSONObject().apply {
        put("id", item.id)
        put("title", item.title)
        put("productType", item.productType.name)
        put("materialFamily", item.materialFamily.name)
        put("profileSystem", item.profileSystem)
        put("widthMm", item.widthMm)
        put("heightMm", item.heightMm)
        put("quantity", item.quantity)
        put("panelCount", item.panelCount)
        put("openingType", item.openingType.name)
        put("glazingType", item.glazingType.name)
        put("glassTint", item.glassTint.name)
        put("frameFinish", item.frameFinish.name)
        put("unitMaterialCost", item.unitMaterialCost.toPlainString())
        put("unitLaborCost", item.unitLaborCost.toPlainString())
        put("unitPrice", item.unitPrice.toPlainString())
        put("lineTotal", item.lineTotal.toPlainString())
        put("notes", item.notes)
        put("designId", item.designId ?: "")
      }
      itemsArr.put(obj)
    }

    dao.insertCalculationSession(
      ProjectCalculationSessionEntity(
        id = session.id,
        sessionCode = session.sessionCode,
        customerName = session.customerName,
        customerPhone = session.customerPhone,
        title = session.title,
        siteLocation = session.siteLocation,
        currency = session.currency,
        exchangeRate = session.exchangeRateUsdToAfn.toPlainString(),
        pricingBasis = session.pricingBasis,
        itemsJson = itemsArr.toString(),
        transportCost = session.transportCost.toPlainString(),
        installationCost = session.installationCost.toPlainString(),
        discountAmount = session.discountAmount.toPlainString(),
        markupPercent = session.markupPercent.toPlainString(),
        totalAmount = session.totalProjectAmount.toPlainString(),
        status = session.status,
        notes = session.notes,
        createdAt = session.createdAt
      )
    )

    dao.insertAudit(
      AuditLogEntity(
        username = currentUsername,
        action = "SAVE_CALCULATION_SESSION",
        entityType = "ProjectCalculation",
        entityId = session.id,
        details = "محاسبه استعلام پروژه ${session.title} (${session.sessionCode})"
      )
    )
  }

  suspend fun deleteCalculationSession(id: String) {
    dao.deleteCalculationSession(id)
  }

  private fun mapSessionEntityToModel(e: ProjectCalculationSessionEntity): ProjectCalculationSession {
    val items = mutableListOf<ProjectCalculationItem>()
    try {
      val arr = JSONArray(e.itemsJson)
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        items.add(
          ProjectCalculationItem(
            id = o.optString("id", "item_$i"),
            title = o.optString("title", "پنجره"),
            productType = try { ProductType.valueOf(o.optString("productType", "WINDOW")) } catch (_: Exception) { ProductType.WINDOW },
            materialFamily = try { MaterialFamily.valueOf(o.optString("materialFamily", "UPVC")) } catch (_: Exception) { MaterialFamily.UPVC },
            profileSystem = o.optString("profileSystem", "Wintech 6000"),
            widthMm = o.optInt("widthMm", 1400),
            heightMm = o.optInt("heightMm", 1600),
            quantity = o.optInt("quantity", 1),
            panelCount = o.optInt("panelCount", 2),
            openingType = try { OpeningType.valueOf(o.optString("openingType", "FIXED")) } catch (_: Exception) { OpeningType.FIXED },
            glazingType = try { GlazingType.valueOf(o.optString("glazingType", "DOUBLE_STANDARD")) } catch (_: Exception) { GlazingType.DOUBLE_STANDARD },
            glassTint = try { GlassTint.valueOf(o.optString("glassTint", "CLEAR")) } catch (_: Exception) { GlassTint.CLEAR },
            frameFinish = try { FrameFinish.valueOf(o.optString("frameFinish", "WHITE")) } catch (_: Exception) { FrameFinish.WHITE },
            unitMaterialCost = CurrencyHelper.parseAmount(o.optString("unitMaterialCost", "0")),
            unitLaborCost = CurrencyHelper.parseAmount(o.optString("unitLaborCost", "0")),
            unitPrice = CurrencyHelper.parseAmount(o.optString("unitPrice", "0")),
            lineTotal = CurrencyHelper.parseAmount(o.optString("lineTotal", "0")),
            notes = o.optString("notes", ""),
            designId = o.optString("designId").ifBlank { null }
          )
        )
      }
    } catch (_: Exception) {}

    val totalMaterial = items.fold(BigDecimal.ZERO) { acc, it -> acc.add(it.unitMaterialCost.multiply(BigDecimal(it.quantity))) }
    val totalArea = items.fold(BigDecimal.ZERO) { acc, it ->
      val area = BigDecimal(it.widthMm * it.heightMm).divide(BigDecimal(1_000_000), 4, java.math.RoundingMode.HALF_UP)
      acc.add(area.multiply(BigDecimal(it.quantity)))
    }

    return ProjectCalculationSession(
      id = e.id,
      sessionCode = e.sessionCode,
      customerName = e.customerName,
      customerPhone = e.customerPhone,
      title = e.title,
      siteLocation = e.siteLocation,
      currency = e.currency,
      exchangeRateUsdToAfn = CurrencyHelper.parseAmount(e.exchangeRate),
      pricingBasis = e.pricingBasis,
      items = items,
      transportCost = CurrencyHelper.parseAmount(e.transportCost),
      installationCost = CurrencyHelper.parseAmount(e.installationCost),
      discountAmount = CurrencyHelper.parseAmount(e.discountAmount),
      markupPercent = CurrencyHelper.parseAmount(e.markupPercent),
      totalMaterialCost = totalMaterial,
      totalEstimatedCost = CurrencyHelper.parseAmount(e.totalAmount),
      totalProjectAmount = CurrencyHelper.parseAmount(e.totalAmount),
      totalAreaM2 = totalArea,
      status = e.status,
      notes = e.notes,
      createdAt = e.createdAt
    )
  }

  suspend fun savePayment(payment: PaymentModel): String? {
    if (!currentRole.canRecordPayments()) return "شما اجازه ثبت دریافت وجه را ندارید."
    if (payment.amount <= BigDecimal.ZERO) return "مبلغ دریافتی باید بزرگتر از صفر باشد."

    val rate = if (payment.currency.equals(CurrencyHelper.CURRENCY_USD, ignoreCase = true)) {
      payment.exchangeRateToAfn.takeIf { it > BigDecimal.ZERO }
        ?: return "نرخ تبدیل دالر باید معتبر باشد."
    } else {
      BigDecimal.ONE
    }

    dao.insertPayment(
      PaymentEntity(
        id = payment.id,
        customerId = payment.customerId,
        projectId = payment.projectId,
        invoiceId = payment.invoiceId,
        amount = payment.amount.toPlainString(),
        currency = payment.currency,
        exchangeRateToAfn = rate.toPlainString(),
        paymentMethod = payment.paymentMethod,
        referenceNumber = payment.referenceNumber,
        paymentDate = payment.paymentDate,
        notes = payment.notes
      )
    )

    // Recalculate the project balance in the contract currency, converting foreign payments.
    recalculateProjectBalance(payment.projectId)
    audit("RECEIVE_PAYMENT", "Payment", payment.id, "دریافت وجه ${payment.amount} ${payment.currency} بابت پروژه")
    return null
  }

  suspend fun saveProfile(profile: ProfileEntity) {
    if (!currentRole.canEditCatalogAndPrices()) return
    dao.insertProfile(profile)
  }

  suspend fun saveGlass(glass: GlassEntity) {
    if (!currentRole.canEditCatalogAndPrices()) return
    dao.insertGlass(glass)
  }

  suspend fun saveAccessory(accessory: AccessoryEntity) {
    if (!currentRole.canEditCatalogAndPrices()) return
    dao.insertAccessory(accessory)
  }

  /** Customer creation used by the customer list (generates code when empty). */
  suspend fun createCustomerWithNextCode(customer: CustomerModel) {
    val code = customer.code.ifBlank {
      "CST-${(dao.selectAllCustomers().size + 101)}"
    }
    saveCustomer(customer.copy(code = code))
  }
}
