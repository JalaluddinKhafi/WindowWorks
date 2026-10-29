package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

  // USERS
  @Query("SELECT * FROM users WHERE isActive = 1")
  fun getAllUsers(): Flow<List<UserEntity>>

  @Query("SELECT * FROM users ORDER BY role, username")
  fun getAllUsersIncludingInactive(): Flow<List<UserEntity>>

  @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
  suspend fun getUserByUsername(username: String): UserEntity?

  @Query("SELECT COUNT(*) FROM users")
  suspend fun countUsers(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertUser(user: UserEntity)

  @Update
  suspend fun updateUser(user: UserEntity)

  @Query("DELETE FROM users WHERE id = :id")
  suspend fun deleteUser(id: String)

  /** Returns every row of the given table as JSON, used by the backup exporter. */
  @Query("SELECT * FROM users")
  suspend fun selectAllUsers(): List<UserEntity>

  @Query("SELECT * FROM customers")
  suspend fun selectAllCustomers(): List<CustomerEntity>

  @Query("SELECT * FROM projects")
  suspend fun selectAllProjects(): List<ProjectEntity>

  @Query("SELECT * FROM project_items")
  suspend fun selectAllProjectItems(): List<ProjectItemEntity>

  @Query("SELECT * FROM designs")
  suspend fun selectAllDesigns(): List<DesignEntity>

  @Query("SELECT * FROM profiles")
  suspend fun selectAllProfiles(): List<ProfileEntity>

  @Query("SELECT * FROM glasses")
  suspend fun selectAllGlasses(): List<GlassEntity>

  @Query("SELECT * FROM accessories")
  suspend fun selectAllAccessories(): List<AccessoryEntity>

  @Query("SELECT * FROM invoices")
  suspend fun selectAllInvoices(): List<InvoiceEntity>

  @Query("SELECT * FROM payments")
  suspend fun selectAllPayments(): List<PaymentEntity>

  @Query("SELECT * FROM expenses")
  suspend fun selectAllExpenses(): List<ExpenseEntity>

  @Query("SELECT * FROM inventory_movements")
  suspend fun selectAllInventoryMovements(): List<InventoryMovementEntity>

  @Query("SELECT * FROM app_settings")
  suspend fun selectAllSettings(): List<SettingEntity>

  @Query("SELECT * FROM material_products")
  suspend fun selectAllMaterialProducts(): List<MaterialProductEntity>

  @Query("SELECT * FROM project_calculation_sessions")
  suspend fun selectAllCalculationSessions(): List<ProjectCalculationSessionEntity>

  // AUDIT
  @Insert
  suspend fun insertAudit(log: AuditLogEntity)

  @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 100")
  fun getRecentAuditLogs(): Flow<List<AuditLogEntity>>

  // CUSTOMERS
  @Query("SELECT * FROM customers WHERE isActive = 1 ORDER BY createdAt DESC")
  fun getAllCustomers(): Flow<List<CustomerEntity>>

  @Query("SELECT * FROM customers ORDER BY fullName")
  fun getAllCustomersIncludingInactive(): Flow<List<CustomerEntity>>

  @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
  suspend fun getCustomerById(id: String): CustomerEntity?

  @Query("SELECT * FROM customers WHERE fullName LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%'")
  fun searchCustomers(query: String): Flow<List<CustomerEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCustomer(customer: CustomerEntity)

  @Update
  suspend fun updateCustomer(customer: CustomerEntity)

  // PROJECTS
  @Query("SELECT * FROM projects ORDER BY createdAt DESC")
  fun getAllProjects(): Flow<List<ProjectEntity>>

  @Query("SELECT * FROM projects WHERE customerId = :customerId ORDER BY createdAt DESC")
  fun getProjectsByCustomer(customerId: String): Flow<List<ProjectEntity>>

  @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
  suspend fun getProjectById(id: String): ProjectEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertProject(project: ProjectEntity)

  @Update
  suspend fun updateProject(project: ProjectEntity)

  @Query("DELETE FROM projects WHERE id = :id")
  suspend fun deleteProject(id: String)

  @Query("DELETE FROM project_items WHERE projectId = :projectId")
  suspend fun deleteItemsByProject(projectId: String)

  // PROJECT ITEMS
  @Query("SELECT * FROM project_items WHERE projectId = :projectId")
  fun getItemsByProject(projectId: String): Flow<List<ProjectItemEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertProjectItem(item: ProjectItemEntity)

  @Query("DELETE FROM project_items WHERE id = :itemId")
  suspend fun deleteProjectItem(itemId: String)

  // DESIGNS
  @Query("SELECT * FROM designs WHERE id = :id LIMIT 1")
  suspend fun getDesignById(id: String): DesignEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDesign(design: DesignEntity)

  @Update
  suspend fun updateDesign(design: DesignEntity)

  @Query("DELETE FROM designs WHERE id = :id")
  suspend fun deleteDesign(id: String)

  // CATALOG: PROFILES
  @Query("SELECT * FROM profiles WHERE isActive = 1 ORDER BY brandSystem, name")
  fun getAllProfiles(): Flow<List<ProfileEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertProfile(profile: ProfileEntity)

  // CATALOG: GLASSES
  @Query("SELECT * FROM glasses WHERE isActive = 1 ORDER BY title")
  fun getAllGlasses(): Flow<List<GlassEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertGlass(glass: GlassEntity)

  // CATALOG: ACCESSORIES
  @Query("SELECT * FROM accessories WHERE isActive = 1 ORDER BY category, name")
  fun getAllAccessories(): Flow<List<AccessoryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAccessory(accessory: AccessoryEntity)

  // FINANCE: INVOICES
  @Query("SELECT * FROM invoices WHERE projectId = :projectId ORDER BY issueDate DESC")
  fun getInvoicesByProject(projectId: String): Flow<List<InvoiceEntity>>

  @Query("SELECT * FROM invoices ORDER BY issueDate DESC")
  fun getAllInvoices(): Flow<List<InvoiceEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertInvoice(invoice: InvoiceEntity)

  // FINANCE: PAYMENTS
  @Query("SELECT * FROM payments WHERE projectId = :projectId ORDER BY paymentDate DESC")
  fun getPaymentsByProject(projectId: String): Flow<List<PaymentEntity>>

  @Query("SELECT * FROM payments WHERE customerId = :customerId ORDER BY paymentDate DESC")
  fun getPaymentsByCustomer(customerId: String): Flow<List<PaymentEntity>>

  @Query("SELECT * FROM payments ORDER BY paymentDate DESC")
  fun getAllPayments(): Flow<List<PaymentEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPayment(payment: PaymentEntity)

  // FINANCE: EXPENSES
  @Query("SELECT * FROM expenses WHERE projectId = :projectId ORDER BY expenseDate DESC")
  fun getExpensesByProject(projectId: String): Flow<List<ExpenseEntity>>

  @Query("SELECT * FROM expenses ORDER BY expenseDate DESC")
  fun getAllExpenses(): Flow<List<ExpenseEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertExpense(expense: ExpenseEntity)

  // INVENTORY
  @Query("SELECT * FROM inventory_movements ORDER BY date DESC")
  fun getAllInventoryMovements(): Flow<List<InventoryMovementEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertInventoryMovement(movement: InventoryMovementEntity)

  // SETTINGS
  @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
  suspend fun getSetting(key: String): String?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun setSetting(setting: SettingEntity)

  @Query("SELECT * FROM app_settings")
  fun getAllSettings(): Flow<List<SettingEntity>>

  // MATERIAL PRODUCTS (اجناس و مواد)
  @Query("SELECT * FROM material_products WHERE isActive = 1 ORDER BY category, name")
  fun getAllMaterialProducts(): Flow<List<MaterialProductEntity>>

  @Query("SELECT * FROM material_products WHERE isActive = 1 AND category = :category ORDER BY name")
  fun getMaterialProductsByCategory(category: String): Flow<List<MaterialProductEntity>>

  @Query("SELECT * FROM material_products WHERE id = :id LIMIT 1")
  suspend fun getMaterialProductById(id: String): MaterialProductEntity?

  @Query("SELECT * FROM material_products WHERE name LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
  fun searchMaterialProducts(query: String): Flow<List<MaterialProductEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMaterialProduct(product: MaterialProductEntity)

  @Update
  suspend fun updateMaterialProduct(product: MaterialProductEntity)

  @Query("DELETE FROM material_products WHERE id = :id")
  suspend fun deleteMaterialProduct(id: String)

  @Query("UPDATE material_products SET stockQty = :newQty WHERE id = :id")
  suspend fun updateProductStock(id: String, newQty: String)

  // PROJECT CALCULATION SESSIONS (محاسبه پروژه)
  @Query("SELECT * FROM project_calculation_sessions ORDER BY createdAt DESC")
  fun getAllCalculationSessions(): Flow<List<ProjectCalculationSessionEntity>>

  @Query("SELECT * FROM project_calculation_sessions WHERE id = :id LIMIT 1")
  suspend fun getCalculationSessionById(id: String): ProjectCalculationSessionEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCalculationSession(session: ProjectCalculationSessionEntity)

  @Update
  suspend fun updateCalculationSession(session: ProjectCalculationSessionEntity)

  @Query("DELETE FROM project_calculation_sessions WHERE id = :id")
  suspend fun deleteCalculationSession(id: String)



  /** Empties every business table (used before restoring a backup). */
  @Query("DELETE FROM users") suspend fun clearUsers()
  @Query("DELETE FROM customers") suspend fun clearCustomers()
  @Query("DELETE FROM projects") suspend fun clearProjects()
  @Query("DELETE FROM project_items") suspend fun clearProjectItems()
  @Query("DELETE FROM designs") suspend fun clearDesigns()
  @Query("DELETE FROM profiles") suspend fun clearProfiles()
  @Query("DELETE FROM glasses") suspend fun clearGlasses()
  @Query("DELETE FROM accessories") suspend fun clearAccessories()
  @Query("DELETE FROM invoices") suspend fun clearInvoices()
  @Query("DELETE FROM payments") suspend fun clearPayments()
  @Query("DELETE FROM expenses") suspend fun clearExpenses()
  @Query("DELETE FROM inventory_movements") suspend fun clearInventoryMovements()
  @Query("DELETE FROM material_products") suspend fun clearMaterialProducts()
  @Query("DELETE FROM project_calculation_sessions") suspend fun clearCalculationSessions()
  @Query("DELETE FROM app_settings") suspend fun clearSettings()
  @Query("DELETE FROM audit_logs") suspend fun clearAuditLogs()



  @Transaction
  suspend fun resetAllData() {
    // Child tables first so foreign keys (if enabled later) never block the wipe.
    clearProjectItems()
    clearInvoices()
    clearPayments()
    clearExpenses()
    clearInventoryMovements()
    clearDesigns()
    clearProjects()
    clearCustomers()
    clearCalculationSessions()
    clearMaterialProducts()
    clearProfiles()
    clearGlasses()
    clearAccessories()
    clearSettings()
    clearUsers()
    clearAuditLogs()
  }

  /**
   * Runs a full wipe + re-import in one transaction so a failed restore can never leave the
   * database half-emptied (either the backup is applied completely, or nothing changes).
   */
  @Transaction
  suspend fun replaceAllData(block: suspend () -> Unit) {
    resetAllData()
    block()
  }

  /** Inserts any missing default setting without touching existing values. */
  suspend fun ensureSettings() {
    val existing = selectAllSettings().map { it.key }.toSet()
    com.example.core.AppSettings.DEFAULTS.forEach { (key, value) ->
      if (key !in existing) setSetting(SettingEntity(key, value))
    }
  }

  @Query("DELETE FROM expenses WHERE id = :id")
  suspend fun deleteExpense(id: String)

  @Query("DELETE FROM payments WHERE id = :id")
  suspend fun deletePayment(id: String)



  @Query("SELECT * FROM payments WHERE id = :id LIMIT 1")
  suspend fun getPaymentById(id: String): PaymentEntity?


}
