package com.example.domain.engine

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
  val sashProfileFaceMm: Float = 75f,
  val panels: List<PanelBounds>,
  val transomPanel: PanelBounds? = null,
  val validationErrors: List<String> = emptyList(),
  val validationWarnings: List<String> = emptyList(),
  val dividers: List<DividerBounds> = emptyList()
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

  fun computeGeometry(design: WindowDesign): DesignGeometry {
    val errors = mutableListOf<String>()
    val warnings = mutableListOf<String>()

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

    val frameFace = 60f
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

    var currentX = frameFace
    design.panels.forEachIndexed { index, panel ->
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

    return DesignGeometry(
      outerFrameWidthMm = design.widthMm.toFloat(),
      outerFrameHeightMm = design.heightMm.toFloat(),
      frameProfileFaceMm = frameFace,
      mullionProfileFaceMm = mullionFace,
      panels = panelBoundsList,
      transomPanel = transomBounds,
      validationErrors = errors,
      validationWarnings = warnings
    )
  }
}
