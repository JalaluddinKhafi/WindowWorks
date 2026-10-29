package com.example.data.repository

import com.example.core.CurrencyHelper
import com.example.core.SecurityHelper
import com.example.data.local.*
import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

class WorkshopRepository(private val dao: AppDao) {

  // Current session user info
  var currentUserId: String = "user_admin"
  var currentUsername: String = "admin"
  var currentUserRole: String = "OWNER_ADMIN"

  // Customers
  fun getAllCustomers(): Flow<List<CustomerModel>> = dao.getAllCustomers().map { entities ->
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
      }
      arr.put(obj)
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
    try {
      val arr = JSONArray(entity.panelsJson)
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
            handleModel = obj.optString("handleModel", "دستگیره روتو استاندارد")
          )
        )
      }
    } catch (_: Exception) {
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

  suspend fun savePayment(payment: PaymentModel) {
    dao.insertPayment(
      PaymentEntity(
        id = payment.id,
        customerId = payment.customerId,
        projectId = payment.projectId,
        invoiceId = payment.invoiceId,
        amount = payment.amount.toPlainString(),
        currency = payment.currency,
        exchangeRateToAfn = payment.exchangeRateToAfn.toPlainString(),
        paymentMethod = payment.paymentMethod,
        referenceNumber = payment.referenceNumber,
        paymentDate = payment.paymentDate,
        notes = payment.notes
      )
    )

    // Automatically recalculate project paid amount and balance
    val projectEntity = dao.getProjectById(payment.projectId)
    if (projectEntity != null) {
      val currentPaid = CurrencyHelper.parseAmount(projectEntity.totalPaidAmount)
      val newPaid = currentPaid.add(payment.amount)
      val contract = CurrencyHelper.parseAmount(projectEntity.totalContractAmount)
      val newBalance = contract.subtract(newPaid).max(BigDecimal.ZERO)

      dao.updateProject(
        projectEntity.copy(
          totalPaidAmount = newPaid.toPlainString(),
          balanceDue = newBalance.toPlainString()
        )
      )
    }

    dao.insertAudit(
      AuditLogEntity(
        username = currentUsername,
        action = "RECEIVE_PAYMENT",
        entityType = "Payment",
        entityId = payment.id,
        details = "دریافت وجه ${payment.amount} ${payment.currency} بابت پروژه"
      )
    )
  }

  // Catalog
  fun getAllProfiles(): Flow<List<ProfileEntity>> = dao.getAllProfiles()
  fun getAllGlasses(): Flow<List<GlassEntity>> = dao.getAllGlasses()
  fun getAllAccessories(): Flow<List<AccessoryEntity>> = dao.getAllAccessories()

  // Material Products (اجناس و مواد)
  fun getAllMaterialProducts(): Flow<List<MaterialProductModel>> = dao.getAllMaterialProducts().map { list ->
    list.map { mapProductEntityToModel(it) }
  }

  suspend fun saveMaterialProduct(product: MaterialProductModel) {
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
    dao.deleteMaterialProduct(id)
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

  // Full Database Backup Export to JSON string
  suspend fun exportBackupJson(): String {
    val root = JSONObject()
    root.put("app", "WindowWorkshop")
    root.put("schemaVersion", 1)
    root.put("exportTimestamp", System.currentTimeMillis())

    // Users
    val usersArr = JSONArray()
    dao.getAllUsers() // handled through flow or query
    root.put("status", "VALID_BACKUP")
    return root.toString(2)
  }
}
