package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.core.AppSettings
import com.example.core.SecurityHelper
import com.example.core.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
  entities = [
    UserEntity::class,
    AuditLogEntity::class,
    CustomerEntity::class,
    ProjectEntity::class,
    ProjectItemEntity::class,
    DesignEntity::class,
    ProfileEntity::class,
    GlassEntity::class,
    AccessoryEntity::class,
    InvoiceEntity::class,
    PaymentEntity::class,
    ExpenseEntity::class,
    InventoryMovementEntity::class,
    SettingEntity::class,
    MaterialProductEntity::class,
    ProjectCalculationSessionEntity::class
  ],
  version = 2,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun appDao(): AppDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    /**
     * Migrations are declared explicitly: `fallbackToDestructiveMigration()` was removed because
     * this app is the single offline source of truth for a workshop, so a silent table drop would
     * destroy real business data.
     */
    val MIGRATIONS: Array<Migration> = arrayOf(Migration_1_2)

    fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "window_workshop.db"
        )
          .addMigrations(*MIGRATIONS)
          .addCallback(DatabaseCallback(scope))
          .build()
        INSTANCE = instance
        instance
      }
    }

    private class DatabaseCallback(
      private val scope: CoroutineScope
    ) : Callback() {
      override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        INSTANCE?.let { database ->
          scope.launch(Dispatchers.IO) {
            seedInitialData(database.appDao())
          }
        }
      }
    }

    /**
     * Creates only what a new installation genuinely needs:
     * the first administrator account and the default settings.
     * Catalog items and demo customers/projects are **not** seeded so that a shipped build never
     * contains sample business data. The catalog is filled by the workshop itself.
     */
    suspend fun seedInitialData(dao: AppDao) {
      if (dao.countUsers() == 0) {
        val adminSalt = SecurityHelper.generateSalt()
        dao.insertUser(
          UserEntity(
            id = "user_admin",
            username = "admin",
            fullName = "مدیر ارشد کارگاه",
            passwordHash = SecurityHelper.hashPassword("admin123", adminSalt),
            salt = adminSalt,
            role = UserRole.OWNER_ADMIN.code
          )
        )
        // The bootstrap password must be replaced on first login.
        dao.setSetting(SettingEntity(AppSettings.KEY_FORCE_PASSWORD_CHANGE, "true"))
      }

      val existingSettings = dao.selectAllSettings().associate { it.key to it.value }
      AppSettings.DEFAULTS.forEach { (key, value) ->
        if (!existingSettings.containsKey(key)) {
          dao.setSetting(SettingEntity(key, value))
        }
      }
    }
  }
}

/**
 * v1 -> v2: the catalog/calculation tables were added after the first release candidate.
 * Every CREATE TABLE statement below matches the Room-generated schema for these entities, so
 * existing installs keep their customers, projects, invoices and payments.
 */
object Migration_1_2 : Migration(1, 2) {
  override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL(
      """CREATE TABLE IF NOT EXISTS `material_products` (
        |`id` TEXT NOT NULL,
        |`code` TEXT NOT NULL,
        |`name` TEXT NOT NULL,
        |`category` TEXT NOT NULL,
        |`brandModel` TEXT NOT NULL,
        |`materialFamily` TEXT NOT NULL,
        |`description` TEXT NOT NULL,
        |`specifications` TEXT NOT NULL,
        |`unit` TEXT NOT NULL,
        |`purchasePriceAfn` TEXT NOT NULL,
        |`purchasePriceUsd` TEXT NOT NULL,
        |`stockLengthMm` INTEGER NOT NULL,
        |`thicknessMm` INTEGER NOT NULL,
        |`colorFinish` TEXT NOT NULL,
        |`compatibility` TEXT NOT NULL,
        |`stockQty` TEXT NOT NULL,
        |`minStockLevel` TEXT NOT NULL,
        |`supplierName` TEXT NOT NULL,
        |`isActive` INTEGER NOT NULL,
        |`createdAt` INTEGER NOT NULL,
        |PRIMARY KEY(`id`))""".trimMargin()
    )
    db.execSQL(
      """CREATE TABLE IF NOT EXISTS `project_calculation_sessions` (
        |`id` TEXT NOT NULL,
        |`sessionCode` TEXT NOT NULL,
        |`customerName` TEXT NOT NULL,
        |`customerPhone` TEXT NOT NULL,
        |`title` TEXT NOT NULL,
        |`siteLocation` TEXT NOT NULL,
        |`currency` TEXT NOT NULL,
        |`exchangeRate` TEXT NOT NULL,
        |`pricingBasis` TEXT NOT NULL,
        |`itemsJson` TEXT NOT NULL,
        |`transportCost` TEXT NOT NULL,
        |`installationCost` TEXT NOT NULL,
        |`discountAmount` TEXT NOT NULL,
        |`markupPercent` TEXT NOT NULL,
        |`totalAmount` TEXT NOT NULL,
        |`status` TEXT NOT NULL,
        |`notes` TEXT NOT NULL,
        |`createdAt` INTEGER NOT NULL,
        |PRIMARY KEY(`id`))""".trimMargin()
    )
  }
}
