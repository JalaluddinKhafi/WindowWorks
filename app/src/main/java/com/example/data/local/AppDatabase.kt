package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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

    fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "window_workshop.db"
        )
          .addCallback(DatabaseCallback(scope))
          .fallbackToDestructiveMigration()
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
            populateInitialData(database.appDao())
          }
        }
      }

      suspend fun populateInitialData(dao: AppDao) {
        // 1. Initial Admin User
        val adminSalt = SecurityHelper.generateSalt()
        val adminHash = SecurityHelper.hashPassword("admin123", adminSalt)
        dao.insertUser(
          UserEntity(
            id = "user_admin",
            username = "admin",
            fullName = "مدیر ارشد کارگاه",
            passwordHash = adminHash,
            salt = adminSalt,
            role = UserRole.OWNER_ADMIN.code
          )
        )

        // 2. Initial Settings
        dao.setSetting(SettingEntity("workshop_name", "کارگاه تخصصی در و پنجره دوجداره"))
        dao.setSetting(SettingEntity("base_currency", "AFN"))
        dao.setSetting(SettingEntity("exchange_rate_usd_to_afn", "72.50"))
        dao.setSetting(SettingEntity("default_markup_percent", "25.00"))
        dao.setSetting(SettingEntity("saw_kerf_mm", "4"))
        dao.setSetting(SettingEntity("end_trim_mm", "20"))

        // 3. Profiles Catalog
        val profiles = listOf(
          ProfileEntity("p1", "PRF-W6-F", "پروفیل فریم ۴ کانال وینتک (Wintech)", "UPVC", "Wintech 6000", "فریم", 6000, "190.00", "AFN"),
          ProfileEntity("p2", "PRF-W6-S", "پروفیل لنگه بازشو وینتک (Wintech Sash)", "UPVC", "Wintech 6000", "لنگه", 6000, "210.00", "AFN"),
          ProfileEntity("p3", "PRF-W6-M", "پروفیل مولیون (وادار میانی) وینتک", "UPVC", "Wintech 6000", "مولیون", 6000, "205.00", "AFN"),
          ProfileEntity("p4", "PRF-W6-B", "زوار دوجداره وینتک", "UPVC", "Wintech 6000", "زوار", 6000, "75.00", "AFN"),
          ProfileEntity("p5", "PRF-VB-F", "پروفیل فریم ویستابست (Vistabest)", "UPVC", "Vistabest 60", "فریم", 6000, "230.00", "AFN"),
          ProfileEntity("p6", "PRF-VB-S", "پروفیل لنگه ویستابست (Vistabest)", "UPVC", "Vistabest 60", "لنگه", 6000, "245.00", "AFN"),
          ProfileEntity("p7", "PRF-AK-F", "پروفیل آلومینیوم ترمال‌بریک آکپا", "ALUMINUM", "Akpa Thermal", "فریم", 6000, "380.00", "AFN")
        )
        profiles.forEach { dao.insertProfile(it) }

        // 4. Glasses Catalog
        val glasses = listOf(
          GlassEntity("g1", "شیشه دوجداره ساده شفاف (۴+۱۲+۴)", "دوجداره", 20, "ساده شفاف", "680.00", "AFN"),
          GlassEntity("g2", "شیشه دوجداره برنز دودی (۴+۱۲+۴)", "دوجداره", 20, "برنز", "760.00", "AFN"),
          GlassEntity("g3", "شیشه دوجداره رفلکس آبی (۶+۱۲+۴)", "دوجداره", 22, "آبی رفلکس", "820.00", "AFN"),
          GlassEntity("g4", "شیشه تک‌جداره ۶ میلی‌متر ساده", "تک‌جداره", 6, "ساده شفاف", "420.00", "AFN")
        )
        glasses.forEach { dao.insertGlass(it) }

        // 5. Accessories Catalog
        val accessories = listOf(
          AccessoryEntity("a1", "ACC-HND-01", "دستگیره آلومینیومی پنجره روتو", "دستگیره", "عدد", "220.00", "AFN"),
          AccessoryEntity("a2", "ACC-HNG-01", "لولا قابل رگلاژ سه بعدی ۱۰۰mm", "لولا", "عدد", "95.00", "AFN"),
          AccessoryEntity("a3", "ACC-ESP-01", "اسپانیولت تک‌حالته فورنکس", "قفل", "عدد", "340.00", "AFN"),
          AccessoryEntity("a4", "ACC-ESP-02", "مکانیزم کامل یراق دوحالته", "قفل", "دست", "1250.00", "AFN"),
          AccessoryEntity("a5", "ACC-EPD-01", "لاستیک درزبندی EPDM اصل", "لاستیک", "متر", "16.00", "AFN"),
          AccessoryEntity("a6", "ACC-SCR-01", "پیچ سرمته گالوانیزه ۴×۲۵", "پیچ", "بسته", "180.00", "AFN")
        )
        accessories.forEach { dao.insertAccessory(it) }

        // 6. Comprehensive Material & Product Catalog (اجناس و مواد)
        val materialProducts = listOf(
          // PVC Profiles 6000 & 7000 Series
          MaterialProductEntity("mp1", "PRF-W60-F", "پروفیل فریم سری ۶۰۰۰ وینتک ۴ کانال", "پروفیل UPVC", "Wintech 6000", "UPVC", "فریم اصلی ۴ کانال عرض ۶۰ میلی‌متر", "عرض ۶۰mm، ۴ کانال عایق", "شاخه", "1140.00", "15.70", 6000, 60, "سفید", "سری ۶۰۰۰ وینتک", "50", "10", "تامین‌کننده مرکزی"),
          MaterialProductEntity("mp2", "PRF-W60-S", "پروفیل لنگه پنجره سری ۶۰۰۰ وینتک", "پروفیل UPVC", "Wintech 6000", "UPVC", "سش بازشو پنجره تک‌حالته و دوحالته", "عرض ۶۰mm، ۳ کانال", "شاخه", "1260.00", "17.38", 6000, 60, "سفید", "سری ۶۰۰۰ وینتک", "40", "10", "تامین‌کننده مرکزی"),
          MaterialProductEntity("mp3", "PRF-W60-M", "پروفیل مولیون (وادار میانی) وینتک ۶۰۰۰", "پروفیل UPVC", "Wintech 6000", "UPVC", "تقسیم‌کننده عمودی و افقی (کتیبه)", "عرض ۶۰mm، مولیون T شکل", "شاخه", "1230.00", "16.96", 6000, 60, "سفید", "سری ۶۰۰۰ وینتک", "35", "8", "تامین‌کننده مرکزی"),
          MaterialProductEntity("mp4", "PRF-W60-B", "زوار دوجداره استاندارد ۲۰mm وینتک", "پروفیل UPVC", "Wintech 6000", "UPVC", "نگه‌دارنده شیشه دوجداره ۲۰ میلی‌متر", "پاخ ۴۵ درجه، با لاستیک", "شاخه", "450.00", "6.20", 6000, 20, "سفید", "سری ۶۰۰۰ وینتک", "60", "15", "تامین‌کننده مرکزی"),
          MaterialProductEntity("mp5", "PRF-W70-F", "پروفیل فریم سری ۷۰۰۰ وینتک ۵ کانال", "پروفیل UPVC", "Wintech 7000", "UPVC", "فریم فوق عایق سری ۷۰۰۰ عرض ۷۰mm", "عرض ۷۰mm، ۵ کانال ویژه مناطق سردسیر", "شاخه", "1480.00", "20.40", 6000, 70, "سفید", "سری ۷۰۰۰ وینتک", "30", "5", "تامین‌کننده مرکزی"),
          MaterialProductEntity("mp6", "PRF-W70-S", "پروفیل لنگه درب سنگین سری ۷۰۰۰ وینتک", "پروفیل UPVC", "Wintech 7000", "UPVC", "سش درب بازشوی سنگین با تقویت گالوانیزه قوطی", "ارتفاع لنگه ۹۸mm جهت درب بالکن و ورودی", "شاخه", "1680.00", "23.17", 6000, 70, "سفید", "سری ۷۰۰۰ وینتک", "20", "5", "تامین‌کننده مرکزی"),
          // Aluminum Profiles
          MaterialProductEntity("mp7", "PRF-AK-TF", "پروفیل فریم ترمال‌بریک آکپا سری ۶۰", "پروفیل آلومینیوم", "Akpa Thermal", "ALUMINUM", "فریم دوجداره آلومینیوم با تیغه پلی‌آمید", "عرض ۶۰mm، تیغه پلی‌آمید ۲۴mm", "شاخه", "2280.00", "31.45", 6000, 60, "نقره‌ای آنودایز", "آکپا ترمال", "25", "5", "صنایع آلومینیوم آکپا"),
          MaterialProductEntity("mp8", "PRF-AK-TS", "پروفیل لنگه ترمال‌بریک آکپا سری ۶۰", "پروفیل آلومینیوم", "Akpa Thermal", "ALUMINUM", "لنگه پنجره بازشوی لولایی ترمال‌بریک", "با گوشه فیکس‌شو دایکاست", "شاخه", "2450.00", "33.79", 6000, 60, "نقره‌ای آنودایز", "آکپا ترمال", "25", "5", "صنایع آلومینیوم آکپا"),
          // Glasses
          MaterialProductEntity("mp9", "GLS-DB-CLR", "شیشه دوجداره فلوت شفاف استاندارد (۴+۱۲+۴)", "شیشه", "کاوه فلوت", "GENERAL", "دوجداره همراه با تزریق گاز آرگون و چسب پلی‌سولفاید", "۴mm ساده + اسپیسر ۱۲mm + ۴mm ساده", "مترمربع", "680.00", "9.38", 0, 20, "شفاف", "استاندارد دوجداره", "200", "30", "جام شیشه کابل"),
          MaterialProductEntity("mp10", "GLS-DB-BRZ", "شیشه دوجداره برنز دودی (۴+۱۲+۴)", "شیشه", "کاوه فلوت", "GENERAL", "لایه بیرونی برنز دودی، لایه داخلی شفاف با گاز آرگون", "۴mm برنز + اسپیسر ۱۲mm + ۴mm ساده", "مترمربع", "760.00", "10.48", 0, 20, "برنز دودی", "استاندارد دوجداره", "150", "20", "جام شیشه کابل"),
          MaterialProductEntity("mp11", "GLS-DB-LOWE", "شیشه دوجداره سوپر سیلور کم‌گسیل Low-E", "شیشه", "کاوه فلوت", "GENERAL", "فیلتر حرارتی تابستان و زمستان با گاز آرگون", "۴mm Low-E + اسپیسر ۱۲mm + ۴mm فلوت", "مترمربع", "890.00", "12.27", 0, 20, "Low-E سبزآبی", "سوپر سیلور", "100", "15", "جام شیشه کابل"),
          // Hardware & Accessories
          MaterialProductEntity("mp12", "HDW-HND-RT", "دستگیره آلومینیومی پنجره روتو آلمان (Roto)", "دستگیره", "Roto Line", "GENERAL", "دستگیره تک‌جهته با ۸ جهت چرخش و پین فلزی", "آلیاژ آلومینیوم فشرده با روکش پودری الکترواستاتیک", "عدد", "220.00", "3.03", 0, 0, "سفید", "انواع لنگه پنجره", "120", "25", "یراق‌آلات برتر"),
          MaterialProductEntity("mp13", "HDW-HNG-3D", "لولا قابل رگلاژ سه بعدی ۱۰۰mm روتو", "لولا", "Roto Solid", "GENERAL", "لولا سنگین با تحمل بار ۱۲۰ کیلوگرم و تنظیم ۳ جهته", "رگلاژ ارتفاع، عمق و فاصله لنگه", "عدد", "95.00", "1.31", 0, 0, "سفید", "UPVC و آلومینیوم", "300", "50", "یراق‌آلات برتر"),
          MaterialProductEntity("mp14", "HDW-ESP-TT", "ست کامل مکانیزم یراق دوحالته فورنکس (Fornax)", "قفل و یراق", "Fornax Tilt&Turn", "GENERAL", "اسپانیولت عمودی، بازویی قیچی، لولای پایینی و گوشه‌گردان", "سیستم امنیتی ضدسرقت با زبانه قارچی", "دست", "1280.00", "17.65", 0, 0, "سیلور", "لنگه‌های تا ابعاد ۱۶۰۰×۱۶۰۰", "45", "10", "یراق‌آلات برتر"),
          MaterialProductEntity("mp15", "HDW-ROL-SL", "غلطک دوبل بلبرینگی کشویی استیل", "غلطک", "Vorne Sliding", "GENERAL", "غلطک با رولیک برنجی ضدسایش و استیل ۳۰۴", "تحمل وزن تا ۱۴۰ کیلوگرم در هر لنگه", "عدد", "165.00", "2.28", 0, 0, "سیلور", "پنجره و درب کشویی", "80", "20", "یراق‌آلات برتر"),
          // Reinforcement & Gaskets
          MaterialProductEntity("mp16", "STL-GALV-U", "گالوانیزه تقویتی U شکل ضخامت ۱.۲۵mm", "گالوانیزه", "فولاد مبارکه", "GENERAL", "ورق گالوانیزه ضدزنگ جهت تقویت خمش فریم و سش", "ضخامت ۱.۲۵mm، آبکاری روی ۱۲۰ گرم", "متر", "75.00", "1.03", 6000, 1, "طوسی فلزی", "پروفیل ۶۰۰۰ و ۷۰۰۰", "400", "80", "آهن‌آلات پایتخت"),
          MaterialProductEntity("mp17", "GSK-EPDM-01", "لاستیک درزبندی آب‌بندی و هوابندی اصل EPDM", "لاستیک EPDM", "Secon EPDM", "GENERAL", "لاستیک مقاوم در برابر اشعه خورشید UV و گرما و سرما", "انعطاف‌پذیری دائم از -۴۰ تا +۱۲۰ درجه سانتی‌گراد", "متر", "16.00", "0.22", 0, 0, "مشکی مات", "شیار پروفیل‌های استاندارد", "1500", "200", "تامین لاستیک صنعتی"),
          MaterialProductEntity("mp18", "CNS-SLCN-MS", "چسب ماستیک سیلیکون نوترال خنثی آنتی‌یووی", "چسب و درزگیر", "SomaFix Neutral", "GENERAL", "چسب درزگیری نهایی نصب پنجره بدون خورندگی و ترک", "حجم ۳۱۰ میلی‌لیتر مقاوم در برابر باران و سرما", "عدد", "180.00", "2.48", 0, 0, "سفید", "نصب و درزگیری", "150", "30", "ابزار و یراق کابل"),
          // Services
          MaterialProductEntity("mp19", "SRV-LBR-M2", "خدمات برش، جوش و مونتاژ کارگاهی (هر مترمربع)", "خدمات کارگاهی", "کارگاه WindowWorkshop", "GENERAL", "اجرت ساخت کامل فریم، سش، نصب یراق و رگلاژ کارخانه‌ای", "مبتنی بر استانداردهای مهندسی", "مترمربع", "320.00", "4.41", 0, 0, "سرویس", "عمومی", "999", "0", "واحد فنی کارگاه"),
          MaterialProductEntity("mp20", "SRV-INST-M2", "خدمات نصب و رگلاژ در محل پروژه (هر مترمربع)", "خدمات کارگاهی", "تیم نصب", "GENERAL", "شامل تراز لیزری، پیچ بولت بتن، فوم تزریقی و سیلیکون‌کشی", "ضمانت ۵ ساله آب‌بندی و هوابندی", "مترمربع", "220.00", "3.03", 0, 0, "سرویس", "عمومی", "999", "0", "واحد نصب کارگاه")
        )
        materialProducts.forEach { dao.insertMaterialProduct(it) }

        // 6. Sample Initial Customer & Project
        val customerId = "cust_sample_1"
        dao.insertCustomer(
          CustomerEntity(
            id = customerId,
            code = "CST-101",
            fullName = "حاج احمد رحیمی",
            phone = "0799123456",
            address = "کابل، شهر نو، کوچه قصابی",
            notes = "پروژه مسکونی ۴ طبقه"
          )
        )

        val projectId = "proj_sample_1"
        dao.insertProject(
          ProjectEntity(
            id = projectId,
            projectNumber = "PRJ-2026-001",
            customerId = customerId,
            title = "پروژه ساختمان مسکونی رحیمی",
            siteAddress = "کابل، شهر نو، پلاک ۲۴",
            status = "APPROVED",
            contractCurrency = "AFN",
            notes = "شامل ۱۲ لنگه پنجره دوجداره و ۲ درب بالکنی",
            totalContractAmount = "145000.00",
            totalPaidAmount = "50000.00",
            balanceDue = "95000.00"
          )
        )

        // Initial Payment
        dao.insertPayment(
          PaymentEntity(
            id = "pay_sample_1",
            customerId = customerId,
            projectId = projectId,
            amount = "50000.00",
            currency = "AFN",
            exchangeRateToAfn = "1.00",
            paymentMethod = "حواله بانکی",
            referenceNumber = "BNK-88319",
            notes = "پیش‌پرداخت اولیه قرارداد"
          )
        )
      }
    }
  }
}
