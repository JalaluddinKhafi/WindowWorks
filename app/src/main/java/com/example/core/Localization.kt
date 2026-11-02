package com.example.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.example.domain.model.MaterialFamily
import com.example.domain.model.OpeningDirection
import com.example.domain.model.OpeningType
import com.example.domain.model.ProductType

/**
 * Supported Languages for WindowWorks Advanced
 * - English (LTR)
 * - Dari / Persian (RTL)
 * - Pashto (RTL)
 */
enum class AppLanguage(
  val code: String,
  val titleEnglish: String,
  val titleNative: String,
  val isRtl: Boolean
) {
  DARI("fa", "Dari / Persian", "دری (فارسی)", true),
  PASHTO("ps", "Pashto", "پښتو", true),
  ENGLISH("en", "English", "English", false);

  companion object {
    fun fromCode(code: String?): AppLanguage {
      return when (code?.lowercase()) {
        "en" -> ENGLISH
        "ps" -> PASHTO
        "fa", "prs", "dari" -> DARI
        else -> DARI
      }
    }
  }
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.DARI }

/**
 * Centralized string dictionary for complete trilingual localization
 */
object Localization {

  fun formatDigits(number: Number, lang: AppLanguage): String {
    val str = number.toString()
    return if (lang.isRtl) {
      CurrencyHelper.toPersianDigits(str)
    } else {
      str
    }
  }

  fun formatDimensionMm(mm: Number, lang: AppLanguage): String {
    val digits = formatDigits(mm, lang)
    return when (lang) {
      AppLanguage.ENGLISH -> "$digits mm"
      AppLanguage.DARI -> "$digits میلی‌متر"
      AppLanguage.PASHTO -> "$digits ملي متر"
    }
  }

  fun translateProductType(type: ProductType, lang: AppLanguage): String = when (type) {
    ProductType.WINDOW -> when (lang) {
      AppLanguage.ENGLISH -> "Window"
      AppLanguage.DARI -> "پنجره"
      AppLanguage.PASHTO -> "کړکۍ"
    }
    ProductType.DOOR -> when (lang) {
      AppLanguage.ENGLISH -> "Door"
      AppLanguage.DARI -> "درب"
      AppLanguage.PASHTO -> "دروازه"
    }
  }

  fun translateMaterialFamily(family: MaterialFamily, lang: AppLanguage): String = when (family) {
    MaterialFamily.UPVC -> when (lang) {
      AppLanguage.ENGLISH -> "UPVC Profile System"
      AppLanguage.DARI -> "یو پی وی سی (UPVC)"
      AppLanguage.PASHTO -> "یو پي وي سي (UPVC)"
    }
    MaterialFamily.ALUMINUM -> when (lang) {
      AppLanguage.ENGLISH -> "Thermal-Break Aluminum"
      AppLanguage.DARI -> "آلومینیوم ترمال‌بریک و نرمال"
      AppLanguage.PASHTO -> "ترمل بریک او نورمال المونیم"
    }
  }

  fun translateOpeningType(type: OpeningType, lang: AppLanguage): String = when (type) {
    OpeningType.FIXED -> when (lang) {
      AppLanguage.ENGLISH -> "Fixed (Non-opening)"
      AppLanguage.DARI -> "ثابت (بدون بازشو)"
      AppLanguage.PASHTO -> "ثابت (بې له خلاصېدو)"
    }
    OpeningType.CASEMENT_LEFT -> when (lang) {
      AppLanguage.ENGLISH -> "Casement Left (Hinged)"
      AppLanguage.DARI -> "تک‌حالته چپ‌بازشو (لولایی)"
      AppLanguage.PASHTO -> "یو اړخیزه چپ پرانیستل (قبضه لرونکی)"
    }
    OpeningType.CASEMENT_RIGHT -> when (lang) {
      AppLanguage.ENGLISH -> "Casement Right (Hinged)"
      AppLanguage.DARI -> "تک‌حالته راست‌بازشو (لولایی)"
      AppLanguage.PASHTO -> "یو اړخیزه ښي پرانیستل (قبضه لرونکی)"
    }
    OpeningType.TILT_AND_TURN -> when (lang) {
      AppLanguage.ENGLISH -> "Tilt & Turn (Dual-Action)"
      AppLanguage.DARI -> "دوحالته (لولایی و شیب‌دار)"
      AppLanguage.PASHTO -> "دوه اړخیزه (خلاصېدونکی او کاږه)"
    }
    OpeningType.SLIDING_LEFT -> when (lang) {
      AppLanguage.ENGLISH -> "Sliding Left"
      AppLanguage.DARI -> "کشویی به چپ"
      AppLanguage.PASHTO -> "کیڼ لوري ته ښویېدونکی"
    }
    OpeningType.SLIDING_RIGHT -> when (lang) {
      AppLanguage.ENGLISH -> "Sliding Right"
      AppLanguage.DARI -> "کشویی به راست"
      AppLanguage.PASHTO -> "ښي لوري ته ښویېدونکی"
    }
    OpeningType.TOP_HUNG -> when (lang) {
      AppLanguage.ENGLISH -> "Top Hung / Awning"
      AppLanguage.DARI -> "کلنگی / سایه‌بانی (بازشو از بالا)"
      AppLanguage.PASHTO -> "پورتنی راځوړند / سیوری لرونکی"
    }
    OpeningType.DOUBLE_CASEMENT -> when (lang) {
      AppLanguage.ENGLISH -> "Double Casement (French Window)"
      AppLanguage.DARI -> "فرانسوی دولنگه بازشو"
      AppLanguage.PASHTO -> "فرانسوي دوه لاسي خلاصېدونکی"
    }
    OpeningType.TILT -> when (lang) {
      AppLanguage.ENGLISH -> "Tilt / Bottom Hung (Hopper)"
      AppLanguage.DARI -> "تک‌حالته شیب‌دار (کلنگی)"
      AppLanguage.PASHTO -> "یو اړخیزه کاږه (کلنګي)"
    }
  }

  fun translateOpeningDirection(dir: OpeningDirection, lang: AppLanguage): String = when (dir) {
    OpeningDirection.INWARD -> when (lang) {
      AppLanguage.ENGLISH -> "Inward Opening"
      AppLanguage.DARI -> "بازشو به داخل"
      AppLanguage.PASHTO -> "دننه لور ته خلاصېدل"
    }
    OpeningDirection.OUTWARD -> when (lang) {
      AppLanguage.ENGLISH -> "Outward Opening"
      AppLanguage.DARI -> "بازشو به خارج"
      AppLanguage.PASHTO -> "بهر لور ته خلاصېدل"
    }
  }

  fun get(key: String, lang: AppLanguage): String = strings[key]?.let { translations ->
    when (lang) {
      AppLanguage.ENGLISH -> translations.en
      AppLanguage.DARI -> translations.fa
      AppLanguage.PASHTO -> translations.ps
    }
  } ?: key

  private data class Translation(val en: String, val fa: String, val ps: String)

  private val strings = mapOf(
    // Common Actions
    "app_title" to Translation("WindowWorks Advanced", "ویندو ورکس پیشرفته", "پرمختللی ویندو ورکس"),
    "save" to Translation("Save", "ذخیره", "ساتل"),
    "cancel" to Translation("Cancel", "انصراف", "لغوه"),
    "delete" to Translation("Delete", "حذف", "ړنګول"),
    "edit" to Translation("Edit", "ویرایش", "سمول"),
    "add" to Translation("Add", "افزودن", "اضافه کول"),
    "back" to Translation("Back", "بازگشت", "شاته"),
    "confirm" to Translation("Confirm", "تأیید", "تایید"),
    "apply" to Translation("Apply", "اعمال", "پلي کول"),
    "close" to Translation("Close", "بستن", "تړل"),
    "undo" to Translation("Undo", "بازگردانی قبلی", "شاته تګ"),
    "redo" to Translation("Redo", "تکرار مجدد", "بیا تکرار"),
    "reset_zoom" to Translation("Reset View", "اندازه پیش‌فرض", "اصلي لید"),
    "zoom_in" to Translation("Zoom In", "بزرگ‌نمایی", "لویول"),
    "zoom_out" to Translation("Zoom Out", "کوچک‌نمایی", "کوچنی کول"),
    "export_svg" to Translation("Export CAD (SVG)", "خروجی نقشه SVG", "د SVG نقشه صادروئ"),
    "templates" to Translation("Templates", "طرح‌های آماده", "چمتو شوي ډیزاینونه"),
    "choose_template" to Translation("Choose Design Template", "انتخاب قالب استاندارد", "د معیاري ډیزاین ټاکل"),
    "cat_all" to Translation("All", "همه", "ټول"),
    "cat_casement" to Translation("Casement", "لولایی / لنگه‌دار", "قبضه لرونکی"),
    "cat_sliding" to Translation("Sliding", "کشویی", "ښویېدونکی"),
    "cat_fixed" to Translation("Fixed", "ثابت", "ثابت"),
    "cat_door" to Translation("Doors", "درب‌ها", "دروازې"),

    // CAD Template Names
    "tmpl_casement_1_sash" to Translation("Single Casement Window", "پنجره تک‌لنگه بازشو لولایی", "یو اړخیزه خلاصېدونکې کړکۍ"),
    "tmpl_fixed_1" to Translation("Fixed Picture Window", "پنجره تک‌لنگه ثابت فیکس", "ثابته کړکۍ"),
    "tmpl_casement_2_fixed_sash" to Translation("2-Panel (Fixed + Casement)", "پنجره دولنگه (ثابت + لنگه بازشو)", "دوه اړخیزه (ثابته + خلاصېدونکې)"),
    "tmpl_casement_2_double" to Translation("French Double Casement (2 Sashes)", "پنجره دولنگه تمام‌بازشو فرانسوی", "دوه اړخیزه فرانسوي کړکۍ"),
    "tmpl_casement_3_fixed_sash_fixed" to Translation("3-Panel Villa (Fixed-Sash-Fixed)", "پنجره سه‌لنگه ویلایی (ثابت + بازشو + ثابت)", "درې اړخیزه ویلایی کړکۍ"),
    "tmpl_sliding_2_leaf" to Translation("2-Track Sliding (2 Leaves)", "پنجره کشویی جفت‌ریل دولنگه", "دوه اړخیزه کشويي کړکۍ"),
    "tmpl_sliding_3_leaf" to Translation("2-Track Sliding (3 Leaves)", "پنجره کشویی سه‌لنگه", "درې اړخیزه کشويي کړکۍ"),
    "tmpl_sliding_4_leaf" to Translation("4-Leaf Sliding (Center Opening)", "پنجره کشویی چهارلنگه بازشو از وسط", "څلور اړخیزه کشويي کړکۍ"),
    "tmpl_tilt_turn_1" to Translation("Single Tilt & Turn Window", "پنجره تک‌لنگه دوحالته", "یو اړخیزه دوه حالته کړکۍ"),
    "tmpl_top_hung_1" to Translation("Top-Hung Awning Window", "پنجره کلنگی / سرویس", "پورتنۍ راځوړنده کړکۍ"),
    "tmpl_door_single_transom" to Translation("Single Door with Top Transom", "درب تک‌لنگه با کتیبه بالایی", "یو اړخیزه دروازه د کتیبې سره"),
    "tmpl_door_double_transom" to Translation("French Double Door with Transom", "درب دولنگه فرانسوی با کتیبه", "دوه اړخیزه فرانسوي دروازه د کتیبې سره"),

    // Navigation
    "nav_dashboard" to Translation("Dashboard", "داشبورد", "ډشبورډ"),
    "nav_projects" to Translation("Projects", "پروژه‌ها", "پروژې"),
    "nav_designer" to Translation("2D/3D CAD", "طراحی ۲D/3D", "۲D/3D ډیزاین"),
    "nav_calculator" to Translation("Project Calculator", "محاسبه پروژه", "د پروژې محاسبه"),
    "nav_finance" to Translation("Finance & Invoices", "حسابداری", "مالي او محاسبات"),
    "nav_customers" to Translation("Customers", "مشتریان", "پېرودونکي"),
    "nav_materials" to Translation("Materials & Products", "اجناس و مواد", "توکي او توکي"),
    "nav_settings" to Translation("Settings", "تنظیمات", "تنظیمات"),
    "nav_cutting_plan" to Translation("Cutting Plan", "نقشه برش", "د پرېکولو نقشه"),
    "nav_quotation" to Translation("Quotation", "پیش‌فاکتور", "اټکلي بل"),
    "nav_workshop_drawing" to Translation("Workshop Drawing", "نقشه کارگاهی", "د فابریکې نقشه"),
    "nav_inventory" to Translation("Inventory", "انبارداری", "ګودام"),

    // CAD Dimensions & Controls
    "width" to Translation("Width", "عرض", "پلنوالی"),
    "height" to Translation("Height", "ارتفاع", "لوړوالی"),
    "transom" to Translation("Transom (Top Light)", "کتیبه بالایی", "پورتنۍ کتیبه"),
    "transom_height" to Translation("Transom Height", "ارتفاع کتیبه", "د کتیبې لوړوالی"),
    "has_transom" to Translation("Enable Transom", "کتیبه دار", "کتیبه ولري"),
    "panel" to Translation("Panel", "لنگه / بخش", "برخه"),
    "panels" to Translation("Panels", "لنگه‌ها", "برخې"),
    "mullion" to Translation("Mullion", "مولیون (تقسیم‌کننده)", "منځنی تیر"),
    "sash" to Translation("Sash", "لنگه بازشو", "پرانیستل کېدونکې لنگه"),
    "frame_profile" to Translation("Frame Profile", "پروفیل فریم", "د فریم پروفایل"),
    "profile_system" to Translation("Profile System", "سیستم پروفیل", "د پروفایل سیسټم"),
    "frame_finish" to Translation("Frame Finish / Color", "رنگ و روکش فریم", "د فریم رنګ"),
    "sash_finish" to Translation("Sash Finish / Color", "رنگ و روکش لنگه", "د لنگې رنګ"),
    "glazing_type" to Translation("Glazing Type", "نوعیت شیشه", "د شیشې ډول"),
    "outer_glass_tint" to Translation("Outer Glass Tint", "رنگ شیشه بیرونی", "بهرنۍ شیشې رنګ"),
    "inner_glass_tint" to Translation("Inner Glass Tint", "رنگ شیشه داخلی", "دننۍ شیشې رنګ"),
    "hardware_handle" to Translation("Handle & Hardware", "دستگیره و یراق‌آلات", "لاستی او یراق"),
    "snap_to_grid" to Translation("Snap to Grid", "چسبیدن به شبکه (اسنپ)", "شبکې ته نښلول"),
    "snap_off" to Translation("Off (1 mm)", "خاموش (۱ میلی‌متر)", "بند (۱ ملي متر)"),
    "snap_10mm" to Translation("10 mm Precision", "دقت ۱۰ میلی‌متر", "۱۰ ملي متر دقت"),
    "snap_50mm" to Translation("50 mm Step", "گام ۵۰ میلی‌متر", "۵۰ ملي متر ګام"),
    "split_vertical" to Translation("Split Vertical", "تقسیم عمودی (مولیون)", "عمودي وېش"),
    "split_horizontal" to Translation("Split Horizontal", "تقسیم افقی (ترانسوم)", "افقي وېش"),
    "remove_opening" to Translation("Remove Opening", "حذف این قسمت", "دا برخه ړنګول"),
    "center_mullion" to Translation("Center Mullion", "تنظیم در مرکز", "په منځ کې برابرول"),

    // Tap & CAD Instructions
    "tap_to_select" to Translation("Tap any panel or dimension to select and edit", "برای ویرایش روی هر لنگه یا اندازه ضربه بزنید", "د سمولو لپاره په هره برخه یا اندازه کلیک وکړئ"),
    "tap_dim_to_edit" to Translation("Direct Dimension Edit", "ویرایش مستقیم ابعاد", "مستقیم ابعاد سمول"),
    "enter_mm" to Translation("Dimension in millimeters (mm)", "ابعاد به میلی‌متر (mm)", "اندازه په ملي متر (mm)"),
    "invalid_dimension" to Translation("Invalid dimension value", "مقدار اندازه نامعتبر است", "د اندازې ارزښت ناسم دی"),

    // Language Selection
    "language" to Translation("Language", "زبان", "ژبه"),
    "language_changed" to Translation("Language updated", "زبان تغییر کرد", "ژبه بدله شوه"),

    // Metrics & Calculations
    "total_area" to Translation("Total Area", "مساحت کل", "ټوله مساحت"),
    "total_perimeter" to Translation("Total Perimeter", "محیط کل", "ټول محیط"),
    "quotation_price" to Translation("Quotation Price", "قیمت پیش‌فاکتور", "اټکلي بیه"),
    "cost_per_m2" to Translation("Price per m²", "قیمت بر متر مربع", "بیه په هر متر مربع"),
    "cost_per_lm" to Translation("Price per linear meter", "قیمت بر متر طول", "بیه په هر خطي متر"),
    "cutting_instructions" to Translation("Cutting Plan", "نقشه برش کارگاه", "د پرېکولو نقشه"),
    "assembly_instructions" to Translation("Manufacturing Instructions", "دستور کار ساخت کارگاهی", "د جوړولو لارښود"),
    "validation_errors" to Translation("Technical Validation Errors", "خطاهای انطباق فنی استاندارد", "تخنیکي تېروتنې"),
    "validation_warnings" to Translation("Manufacturing Warnings", "هشدارهای فنی و کارگاهی", "تخنیکي خبرداری")
  )
}

/**
 * Convenient composable string lookup
 */
@Composable
fun tr(key: String): String {
  val lang = LocalAppLanguage.current
  return Localization.get(key, lang)
}
