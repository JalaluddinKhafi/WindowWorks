package com.example.domain.model

import java.math.BigDecimal

enum class ProductType(val code: String, val titlePersian: String) {
  WINDOW("WINDOW", "پنجره"),
  DOOR("DOOR", "درب")
}

enum class MaterialFamily(val code: String, val titlePersian: String) {
  UPVC("UPVC", "یو پی وی سی (UPVC)"),
  ALUMINUM("ALUMINUM", "آلومینیوم ترمال‌بریک و نرمال")
}

enum class WindowShape(val code: String, val titlePersian: String, val titleEnglish: String) {
  RECTANGLE("RECTANGLE", "مستطیل استاندارد", "Standard Rectangle"),
  ARCHED("ARCHED", "هلالی (قوس‌دار)", "Arched / Round Top"),
  TRIANGLE("TRIANGLE", "مثلثی (شیروانی)", "Triangular / Gable"),
  TRAPEZOIDAL("TRAPEZOIDAL", "ذوزنقه‌ای", "Trapezoidal / Rake")
}

enum class OpeningType(val code: String, val titlePersian: String) {
  FIXED("FIXED", "ثابت (بدون بازشو)"),
  CASEMENT_LEFT("CASEMENT_LEFT", "تک‌حالته چپ‌بازشو (لولایی)"),
  CASEMENT_RIGHT("CASEMENT_RIGHT", "تک‌حالته راست‌بازشو (لولایی)"),
  DOUBLE_CASEMENT("DOUBLE_CASEMENT", "فرانسوی دولنگه بازشو"),
  TILT("TILT", "تک‌حالته شیب‌دار (کلنگی)"),
  TILT_AND_TURN("TILT_AND_TURN", "دوحالته (بازشو و شیب‌دار)"),
  SLIDING_LEFT("SLIDING_LEFT", "کشویی به چپ"),
  SLIDING_RIGHT("SLIDING_RIGHT", "کشویی به راست"),
  TOP_HUNG("TOP_HUNG", "کلنگی / سایه‌بانی (بازشو از بالا)")
}

enum class OpeningDirection(val code: String, val titlePersian: String) {
  INWARD("INWARD", "بازشو به داخل (Inward)"),
  OUTWARD("OUTWARD", "بازشو به خارج (Outward)")
}

enum class GlazingType(val titlePersian: String, val thicknessMm: Int) {
  SINGLE("تک‌جداره (۴ میلی‌متر)", 4),
  DOUBLE_STANDARD("دوجداره استاندارد (۴+۱۲+۴)", 20),
  DOUBLE_THICK("دوجداره ضخیم (۶+۱۶+۶)", 28),
  TRIPLE("سه‌جداره عایق ویژه", 36)
}

enum class GlassTint(val id: String, val titlePersian: String, val colorHex: Long) {
  CLEAR("CLEAR", "ساده شفاف (Clear)", 0xFF38BDF8),
  BRONZE("BRONZE", "برنز دودی (Bronze)", 0xFFB45309),
  REFLECTIVE_BLUE("REFLECTIVE_BLUE", "آبی رفلکس (Reflective)", 0xFF0284C7),
  FROSTED("FROSTED", "مشجر / برفی (Frosted)", 0xFFE2E8F0),
  LOW_E("LOW_E", "کم‌گسیل سوپرسیلور (Low-E)", 0xFF10B981)
}

enum class FrameFinish(val id: String, val titlePersian: String, val colorHex: Long) {
  WHITE("WHITE", "سفید کارخانه‌ای (RAL 9016)", 0xFFF8FAFC),
  ANTHRACITE("ANTHRACITE", "طوسی آنتراسیت (RAL 7016)", 0xFF334155),
  BROWN_WOOD("BROWN_WOOD", "طرح چوب گردویی (Walnut)", 0xFF5D4037),
  GOLDEN_OAK("GOLDEN_OAK", "بلوط طلایی (Golden Oak)", 0xFF8D6E63),
  ANODIZED_SILVER("ANODIZED_SILVER", "نقره‌ای آنودایز", 0xFFB0BEC5),
  BLACK_MATT("BLACK_MATT", "مشکی مات کربنی", 0xFF1E293B)
}

enum class HandleColor(val id: String, val titlePersian: String, val colorHex: Long) {
  SILVER("SILVER", "نقره‌ای مات (Silver)", 0xFFCBD5E1),
  BLACK_MATT("BLACK_MATT", "مشکی مات کربنی", 0xFF1E293B),
  GOLD("GOLD", "طلایی لوکس (Gold)", 0xFFEAB308),
  BRASS("BRASS", "برنجی کهنه‌نما (Brass)", 0xFFB45309),
  WHITE("WHITE", "سفید کلاسیک", 0xFFF8FAFC)
}

enum class CoatingType(val id: String, val titlePersian: String) {
  MATTE("MATTE", "مات پودری الکتراستاتیک"),
  WOODGRAIN("WOODGRAIN", "روکش طرح چوب لامینیت"),
  GLOSS("GLOSS", "براق هایگلاس"),
  ANODIZED("ANODIZED", "آنودایز متالیک")
}

data class PanelConfig(
  val id: String,
  val openingType: OpeningType = OpeningType.FIXED,
  val openingDirection: OpeningDirection = OpeningDirection.INWARD,
  val glazingType: GlazingType = GlazingType.DOUBLE_STANDARD,
  val glassTint: GlassTint = GlassTint.CLEAR, // Outer pane tint
  val innerGlassTint: GlassTint = GlassTint.CLEAR, // Inner pane tint
  val sashFinish: FrameFinish? = null, // Null means inherit frameFinish
  val widthRatio: Float = 1.0f,
  val customWidthMm: Int? = null,
  val customHeightMm: Int? = null,
  val handleModel: String = "دستگیره روتو استاندارد",
  val lockPoints: Int = 1,
  val hasHandle: Boolean = true,
  val handleX: Float? = null,
  val handleY: Float = 0.5f
)

enum class SplitAxis { VERTICAL, HORIZONTAL }

/** A leaf references a panel; a branch divides its opening, reserving a mullion. */
data class LayoutNode(
  val panelId: String? = null,
  val axis: SplitAxis = SplitAxis.VERTICAL,
  val fraction: Float = 0.5f,
  val first: LayoutNode? = null,
  val second: LayoutNode? = null
)

data class WindowDesign(
  val id: String,
  val name: String,
  val productType: ProductType = ProductType.WINDOW,
  val materialFamily: MaterialFamily = MaterialFamily.UPVC,
  val windowShape: WindowShape = WindowShape.RECTANGLE,
  val archHeightMm: Int = 300,
  val profileSystem: String = "Wintech 6000",
  val frameFinish: FrameFinish = FrameFinish.WHITE,
  val sashFinish: FrameFinish = FrameFinish.WHITE, // Distinct sash color allowed!
  val outerGlassTint: GlassTint = GlassTint.CLEAR,
  val innerGlassTint: GlassTint = GlassTint.CLEAR,
  val openingDirection: OpeningDirection = OpeningDirection.INWARD,
  val widthMm: Int = 1400,
  val heightMm: Int = 1600,
  val hasTransom: Boolean = false,
  val transomHeightMm: Int = 400,
  val hasMullionDivision: Boolean = true,
  val panels: List<PanelConfig> = listOf(
    PanelConfig(id = "p1", openingType = OpeningType.FIXED),
    PanelConfig(id = "p2", openingType = OpeningType.CASEMENT_LEFT)
  ),
  val isApproved: Boolean = false,
  val version: Int = 1,
  val notes: String = "",
  val layoutRoot: LayoutNode? = null,
  val handleColor: HandleColor = HandleColor.SILVER,
  val coatingType: CoatingType = CoatingType.MATTE,
  val hasGrids: Boolean = false,
  val gridRows: Int = 2,
  val gridCols: Int = 2,
  val hasLowThreshold: Boolean = false,
  val customColorHex: Long? = null
)

data class CutPiece(
  val id: String,
  val profileName: String,
  val sectionType: String, // فریم، لنگه، مولیون، زوار، گالوانیزه
  val lengthMm: Int,
  val quantity: Int,
  val angleStart: Int, // 45 or 90
  val angleEnd: Int,   // 45 or 90
  val tag: String
)

data class StockBarPlan(
  val barIndex: Int,
  val profileName: String,
  val stockLengthMm: Int = 6000,
  val cuts: List<CutPiece>,
  val usedLengthMm: Int,
  val wasteLengthMm: Int,
  val wastePercent: Float,
  val isReusableOffcut: Boolean
)

data class CuttingOptimizationResult(
  val profileName: String,
  val stockLengthMm: Int,
  val sawKerfMm: Int,
  val endTrimMm: Int,
  val stockBars: List<StockBarPlan>,
  val totalPiecesCount: Int,
  val totalRequiredLengthMm: Int,
  val totalStockUsedLengthMm: Int,
  val totalWasteLengthMm: Int,
  val overallWastePercent: Float
)

data class BOMItem(
  val id: String,
  val category: String, // پروفیل، شیشه، یراق، گالوانیزه، لاستیک، اتصالات، خدمات
  val code: String,
  val name: String,
  val quantity: BigDecimal,
  val unit: String,
  val unitPrice: BigDecimal,
  val lineTotal: BigDecimal,
  val currency: String = "AFN"
)

data class CostEstimate(
  val id: String,
  val projectId: String,
  val designId: String,
  val profileCost: BigDecimal,
  val glassCost: BigDecimal,
  val hardwareCost: BigDecimal,
  val reinforcementCost: BigDecimal,
  val gasketCost: BigDecimal,
  val consumablesCost: BigDecimal,
  val laborCost: BigDecimal,
  val transportCost: BigDecimal,
  val installationCost: BigDecimal,
  val totalBaseCost: BigDecimal,
  val markupPercent: BigDecimal,
  val profitMarginPercent: BigDecimal,
  val quotationPrice: BigDecimal,
  val pricePerSquareMeter: BigDecimal = BigDecimal.ZERO,
  val pricePerLinearMeter: BigDecimal = BigDecimal.ZERO,
  val currency: String = "AFN",
  val isProvisional: Boolean = false,
  val missingRulesNote: String = "",
  val bomItems: List<BOMItem> = emptyList()
)

// Material and Product Catalog Model for «اجناس و مواد»
data class MaterialProductModel(
  val id: String,
  val code: String,
  val name: String,
  val category: String, // پروفیل UPVC، پروفیل آلومینیوم، شیشه، دستگیره، لولا، قفل، غلطک، لاستیک EPDM، گالوانیزه، پیچ و اتصالات، زوار، چسب و درزگیر، توری، خدمات
  val brandModel: String = "",
  val materialFamily: String = "UPVC", // UPVC, ALUMINUM, GENERAL
  val description: String = "",
  val specifications: String = "",
  val unit: String = "عدد", // شاخه، متر، مترمربع، عدد، دست، بسته
  val purchasePriceAfn: BigDecimal = BigDecimal.ZERO,
  val purchasePriceUsd: BigDecimal = BigDecimal.ZERO,
  val stockLengthMm: Int = 0,
  val thicknessMm: Int = 0,
  val colorFinish: String = "سفید",
  val compatibility: String = "عمومی",
  val stockQty: BigDecimal = BigDecimal.ZERO,
  val minStockLevel: BigDecimal = BigDecimal.ZERO,
  val supplierName: String = "",
  val isActive: Boolean = true,
  val createdAt: Long = System.currentTimeMillis()
)

// Project Calculator Models for «محاسبه پروژه»
data class ProjectCalculationItem(
  val id: String,
  val title: String, // e.g. پنجره دوحالته سالن
  val productType: ProductType = ProductType.WINDOW,
  val materialFamily: MaterialFamily = MaterialFamily.UPVC,
  val profileSystem: String = "Wintech 6000",
  val widthMm: Int = 1400,
  val heightMm: Int = 1600,
  val quantity: Int = 1,
  val panelCount: Int = 2,
  val openingType: OpeningType = OpeningType.CASEMENT_LEFT,
  val glazingType: GlazingType = GlazingType.DOUBLE_STANDARD,
  val glassTint: GlassTint = GlassTint.CLEAR,
  val frameFinish: FrameFinish = FrameFinish.WHITE,
  val unitMaterialCost: BigDecimal = BigDecimal.ZERO,
  val unitLaborCost: BigDecimal = BigDecimal.ZERO,
  val unitPrice: BigDecimal = BigDecimal.ZERO,
  val lineTotal: BigDecimal = BigDecimal.ZERO,
  val notes: String = "",
  val designId: String? = null
)

data class ProjectCalculationSession(
  val id: String,
  val sessionCode: String,
  val customerName: String = "",
  val customerPhone: String = "",
  val title: String = "برآورد استعلام قیمت کارگاه",
  val siteLocation: String = "",
  val currency: String = "AFN",
  val exchangeRateUsdToAfn: BigDecimal = BigDecimal("72.50"),
  val pricingBasis: String = "مجموع اقلام", // مجموع اقلام، بر اساس مترمربع (m²)، بر اساس متر طول
  val items: List<ProjectCalculationItem> = emptyList(),
  val transportCost: BigDecimal = BigDecimal.ZERO,
  val installationCost: BigDecimal = BigDecimal.ZERO,
  val discountAmount: BigDecimal = BigDecimal.ZERO,
  val markupPercent: BigDecimal = BigDecimal("25.00"),
  val totalMaterialCost: BigDecimal = BigDecimal.ZERO,
  val totalEstimatedCost: BigDecimal = BigDecimal.ZERO,
  val suggestedSellingPrice: BigDecimal = BigDecimal.ZERO,
  val totalProjectAmount: BigDecimal = BigDecimal.ZERO,
  val totalAreaM2: BigDecimal = BigDecimal.ZERO,
  val status: String = "DRAFT", // DRAFT, QUOTED, CONVERTED_TO_PROJECT
  val notes: String = "",
  val createdAt: Long = System.currentTimeMillis()
)
