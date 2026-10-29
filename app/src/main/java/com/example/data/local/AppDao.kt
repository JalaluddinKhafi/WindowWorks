package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

  // USERS
  @Query("SELECT * FROM users WHERE isActive = 1")
  fun getAllUsers(): Flow<List<UserEntity>>

  @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
  suspend fun getUserByUsername(username: String): UserEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertUser(user: UserEntity)

  @Update
  suspend fun updateUser(user: UserEntity)

  // AUDIT
  @Insert
  suspend fun insertAudit(log: AuditLogEntity)

  @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 100")
  fun getRecentAuditLogs(): Flow<List<AuditLogEntity>>

  // CUSTOMERS
  @Query("SELECT * FROM customers WHERE isActive = 1 ORDER BY createdAt DESC")
  fun getAllCustomers(): Flow<List<CustomerEntity>>

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
}
