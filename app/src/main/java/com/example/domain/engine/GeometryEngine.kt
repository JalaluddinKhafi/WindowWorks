package com.example.domain.engine

import com.example.domain.model.MaterialCatalog
import com.example.domain.model.MaterialFamily
import com.example.domain.model.WindowDesign
import com.example.domain.model.LayoutNode
import com.example.domain.model.SplitAxis

data class PanelBounds(
  val panelIndex: Int,
  val xMm: Float,
  val yMm: Float,
  val widthMm: Float,
  val heightMm: Float,
  val isSash: Boolean
)

data class DesignGeometry(
  val outerFrameWidthMm: Float,
  val outerFrameHeightMm: Float,
  val frameProfileFaceMm: Float = 60f,
  val mullionProfileFaceMm: Float = 75f,
  val sashProfileFaceMm: Float = 72f,
  val panels: List<PanelBounds>,
  val transomPanel: PanelBounds? = null,
  val validationErrors: List<String> = emptyList(),
  val validationWarnings: List<String> = emptyList(),
  val dividers: List<DividerBounds> = emptyList(),
  val shape: com.example.domain.model.WindowShape = com.example.domain.model.WindowShape.RECTANGLE,
  val archHeightMm: Float = 0f
)

data class DividerBounds(val xMm: Float, val yMm: Float, val widthMm: Float, val heightMm: Float,
  val axis: SplitAxis)

object GeometryEngine {
  private const val MIN_WINDOW_WIDTH = 400
  private const val MAX_WINDOW_WIDTH = 4000
  private const val MIN_WINDOW_HEIGHT = 400
  private const val MAX_WINDOW_HEIGHT = 3200
  private const val MIN_SASH_WIDTH = 380
  private const val MIN_SASH_HEIGHT = 380
  /** Kept for callers that rely on the PVC default face width. */
  const val DEFAULT_FRAME_FACE_MM = 60f

  /** Face width of the selected profile system; falls back to the workshop default. */
  fun frameFaceMm(design: WindowDesign): Float =
    design.profileFaceWidthMm.coerceIn(30, 120).toFloat().takeIf { it > 0f } ?: DEFAULT_FRAME_FACE_MM

  fun computeGeometry(design: WindowDesign): DesignGeometry {
    val errors = mutableListOf<String>()
    val warnings = mutableListOf<String>()

    val system = com.example.domain.model.MaterialCatalog.find(design.profileSystem)
    val materialWarning = if (system == null) null else if (system.family != design.materialFamily) {
      "سیستم پروفیل «${system.code}» متعلق به ${MaterialCatalog.shortFamilyTitle(system.family)} است ولی جنس انتخابی طرح ${MaterialCatalog.shortFamilyTitle(design.materialFamily)} می‌باشد."
    } else null
    materialWarning?.let(warnings::add)
    if (design.materialFamily == MaterialFamily.ALUMINUM && !design.hasThermalBreak) {
      warnings.add("پروفیل آلومینیومی بدون پل حرارتی است؛ برای اقلیم سرد ضریب انتقال حرارت بالاست.")
    }

    if (design.widthMm < MIN_WINDOW_WIDTH) {
      errors.add("عرض پنجره (${design.widthMm} میلی‌متر) کمتر از حداقل مجاز ($MIN_WINDOW_WIDTH میلی‌متر) است.")
    }
    if (design.widthMm > MAX_WINDOW_WIDTH) {
      warnings.add("عرض بیش از $MAX_WINDOW_WIDTH میلی‌متر نیاز به قوطی تقویت و اتصال چندتکه دارد.")
    }
    if (design.heightMm < MIN_WINDOW_HEIGHT) {
      errors.add("ارتفاع پنجره (${design.heightMm} میلی‌متر) کمتر از حداقل مجاز ($MIN_WINDOW_HEIGHT میلی‌متر) است.")
    }
    if (design.heightMm > MAX_WINDOW_HEIGHT) {
      warnings.add("ارتفاع بیش از $MAX_WINDOW_HEIGHT میلی‌متر ممکن است در اثر بار باد خمش ایجاد کند.")
    }

    val frameFace = frameFaceMm(design)
    val mullionFace = 75f
    val panelCount = design.panels.size.coerceAtLeast(1)

    // Check transom
    val hasTransom = design.hasTransom && design.transomHeightMm in 200..(design.heightMm - 300)
    val transomHeight = if (hasTransom) design.transomHeightMm.toFloat() else 0f
    val mainHeight = design.heightMm - transomHeight

    val transomBounds = if (hasTransom) {
      PanelBounds(
        panelIndex = -1,
        xMm = frameFace,
        yMm = frameFace,
        widthMm = design.widthMm - (frameFace * 2),
        heightMm = transomHeight - (frameFace + (mullionFace / 2)),
        isSash = false
      )
    } else null

    val startY = if (hasTransom) transomHeight + (mullionFace / 2) else frameFace
    val availableHeight = (design.heightMm - (if (hasTransom) transomHeight + (mullionFace / 2) else frameFace)) - frameFace

    // Total width inside frames minus mullions
    val totalMullionsWidth = (panelCount - 1) * mullionFace
    val availableWidth = (design.widthMm - (frameFace * 2)) - totalMullionsWidth

    val totalRatio = design.panels.sumOf { it.widthRatio.toDouble().coerceAtLeast(0.01) }.toFloat().coerceAtLeast(0.01f)
    val panelBoundsList = mutableListOf<PanelBounds>()
    val dividers = mutableListOf<DividerBounds>()

    var currentX = frameFace
    if (design.layoutRoot == null) design.panels.forEachIndexed { index, panel ->
      val panelWidth = ((panel.widthRatio.coerceAtLeast(0.01f) / totalRatio) * availableWidth).coerceAtLeast(1f)
      val isSash = panel.openingType != com.example.domain.model.OpeningType.FIXED

      if (isSash && panelWidth < MIN_SASH_WIDTH) {
        warnings.add("عرض لنگه شماره ${index + 1} (${panelWidth.toInt()} میلی‌متر) ممکن است مانع عملکرد روان یراق گردد.")
      }

      panelBoundsList.add(
        PanelBounds(
          panelIndex = index,
          xMm = currentX,
          yMm = startY,
          widthMm = panelWidth,
          heightMm = availableHeight,
          isSash = isSash
        )
      )

      currentX += panelWidth + mullionFace
    }

    if (design.layoutRoot != null) {
      panelBoundsList.clear()
      fun layout(node: LayoutNode, x: Float, y: Float, w: Float, h: Float) {
        if (node.panelId != null) {
          val index = design.panels.indexOfFirst { it.id == node.panelId }
          if (index < 0) { errors.add("ارجاع نامعتبر در تقسیم‌بندی طرح"); return }
          panelBoundsList.add(PanelBounds(index, x, y, w.coerceAtLeast(1f), h.coerceAtLeast(1f),
            design.panels[index].openingType != com.example.domain.model.OpeningType.FIXED))
          return
        }
        val a = node.first
        val b = node.second
        if (a == null || b == null) { errors.add("تقسیم‌بندی ناقص است"); return }
        val vertical = node.axis == SplitAxis.VERTICAL
        val extent = (if (vertical) w else h) - mullionFace
        val firstSize = extent * node.fraction.coerceIn(0.01f, 0.99f)
        if (extent <= 0 || firstSize < 160 || extent - firstSize < 160) errors.add("بخش‌های طرح برای پروفیل و شیشه بیش از حد کوچک‌اند.")
        if (vertical) {
          dividers.add(DividerBounds(x + firstSize, y, mullionFace, h, node.axis))
          layout(a, x, y, firstSize, h)
          layout(b, x + firstSize + mullionFace, y, extent - firstSize, h)
        } else {
          dividers.add(DividerBounds(x, y + firstSize, w, mullionFace, node.axis))
          layout(a, x, y, w, firstSize)
          layout(b, x, y + firstSize + mullionFace, w, extent - firstSize)
        }
      }
      layout(design.layoutRoot, frameFace, startY, (design.widthMm - frameFace * 2).coerceAtLeast(1f), availableHeight.coerceAtLeast(1f))
      panelBoundsList.sortBy { it.panelIndex }
      if (panelBoundsList.size != design.panels.size) errors.add("همهٔ لنگه‌ها در تقسیم‌بندی موجود نیستند.")
    } else {
      panelBoundsList.dropLast(1).forEach { p ->
        dividers.add(DividerBounds(p.xMm + p.widthMm, p.yMm, mullionFace, p.heightMm, SplitAxis.VERTICAL))
      }
    }
    if (design.hasTransom && !hasTransom) errors.add("ارتفاع کتیبه باید بین ۲۰۰ و ${design.heightMm - 300} میلی‌متر باشد.")
    panelBoundsList.forEach { p ->
      if (p.widthMm < 160 || p.heightMm < 160) errors.add("ابعاد قسمت ${p.panelIndex + 1} کافی نیست.")
      if (p.isSash && (p.widthMm < MIN_SASH_WIDTH || p.heightMm < MIN_SASH_HEIGHT))
        warnings.add("ابعاد بازشوی قسمت ${p.panelIndex + 1} نیاز به بررسی یراق دارد.")
    }

    return DesignGeometry(
      outerFrameWidthMm = design.widthMm.toFloat(),
      outerFrameHeightMm = design.heightMm.toFloat(),
      frameProfileFaceMm = frameFace,
      mullionProfileFaceMm = mullionFace,
      sashProfileFaceMm = system?.sashFaceMm?.toFloat() ?: 72f,
      panels = panelBoundsList,
      transomPanel = transomBounds,
      validationErrors = errors,
      validationWarnings = warnings.distinct(),
      dividers = dividers,
      shape = design.windowShape,
      archHeightMm = if (design.windowShape != com.example.domain.model.WindowShape.RECTANGLE) design.archHeightMm.toFloat() else 0f
    )
  }
}
