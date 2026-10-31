package com.example.domain.engine

import com.example.domain.model.CutPiece
import com.example.domain.model.MaterialFamily
import com.example.domain.model.OpeningType
import com.example.domain.model.WindowDesign
import java.math.BigDecimal
import java.math.RoundingMode

data class GlassPiece(
  val id: String,
  val tag: String,
  val widthMm: Int,
  val heightMm: Int,
  val areaM2: BigDecimal,
  val glazingTitle: String,
  val tintTitle: String
)

data class ManufacturingResult(
  val profileCuts: List<CutPiece>,
  val reinforcementCuts: List<CutPiece>,
  val glazingBeadCuts: List<CutPiece>,
  val glassPieces: List<GlassPiece>,
  val totalGasketMeters: BigDecimal,
  val hardwarePcs: Map<String, Int>,
  val totalProfileLengthMm: Int,
  val totalReinforcementLengthMm: Int,
  val totalGlassAreaM2: BigDecimal,
  val isProvisional: Boolean,
  val missingRulesDescription: String
)

object ManufacturingCalculator {

  fun calculate(
    design: WindowDesign,
    weldingAllowanceMm: Int = 3, // 3mm per weld for UPVC (0 for mechanical aluminum joints)
    sashOverlapMm: Int = 8,       // 8mm overlap per side
    glassDeductionMm: Int = 10    // 10mm total rebate clearance for glass
  ): ManufacturingResult {
    val profileCuts = mutableListOf<CutPiece>()
    val reinforcementCuts = mutableListOf<CutPiece>()
    val glazingBeadCuts = mutableListOf<CutPiece>()
    val glassPieces = mutableListOf<GlassPiece>()
    val hardwarePcs = mutableMapOf<String, Int>()

    val isUpvc = design.materialFamily == MaterialFamily.UPVC
    val weld = if (isUpvc) weldingAllowanceMm else 0

    val missingRules = mutableListOf<String>()
    if (design.profileSystem.isBlank() || design.profileSystem == "نامشخص") {
      missingRules.add("سیستم مقطع پروفیل انتخاب نشده است؛ مقادیر با ضرایب پیش‌فرض استاندارد محاسبه شدند.")
    }

    val geom = GeometryEngine.computeGeometry(design)

    // 1. Outer Frame Cuts (4 pieces, 45 degree mitre cuts)
    val frameWidthCut = design.widthMm + (weld * 2)
    val frameHeightCut = design.heightMm + (weld * 2)

    profileCuts.add(
      CutPiece(
        id = "f_top",
        profileName = "${design.profileSystem} - فریم اصلی",
        sectionType = "فریم",
        lengthMm = frameWidthCut,
        quantity = 1,
        angleStart = 45,
        angleEnd = 45,
        tag = "فریم بالا (${design.widthMm}mm)"
      )
    )
    profileCuts.add(
      CutPiece(
        id = "f_bot",
        profileName = "${design.profileSystem} - فریم اصلی",
        sectionType = "فریم",
        lengthMm = frameWidthCut,
        quantity = 1,
        angleStart = 45,
        angleEnd = 45,
        tag = "فریم پایین (${design.widthMm}mm)"
      )
    )
    profileCuts.add(
      CutPiece(
        id = "f_left",
        profileName = "${design.profileSystem} - فریم اصلی",
        sectionType = "فریم",
        lengthMm = frameHeightCut,
        quantity = 1,
        angleStart = 45,
        angleEnd = 45,
        tag = "فریم چپ (${design.heightMm}mm)"
      )
    )
    profileCuts.add(
      CutPiece(
        id = "f_right",
        profileName = "${design.profileSystem} - فریم اصلی",
        sectionType = "فریم",
        lengthMm = frameHeightCut,
        quantity = 1,
        angleStart = 45,
        angleEnd = 45,
        tag = "فریم راست (${design.heightMm}mm)"
      )
    )

    // Frame Reinforcement Steel (Straight 90 deg cuts, 30mm shorter)
    if (isUpvc) {
      reinforcementCuts.add(
        CutPiece(
          id = "rf_w",
          profileName = "گالوانیزه تقویتی فریم U شکل",
          sectionType = "گالوانیزه",
          lengthMm = (design.widthMm - 60).coerceAtLeast(100),
          quantity = 2,
          angleStart = 90,
          angleEnd = 90,
          tag = "گالوانیزه افقی فریم"
        )
      )
      reinforcementCuts.add(
        CutPiece(
          id = "rf_h",
          profileName = "گالوانیزه تقویتی فریم U شکل",
          sectionType = "گالوانیزه",
          lengthMm = (design.heightMm - 60).coerceAtLeast(100),
          quantity = 2,
          angleStart = 90,
          angleEnd = 90,
          tag = "گالوانیزه عمودی فریم"
        )
      )
    }

    // 2. Transom (کتیبه افقی) if exists
    if (geom.transomPanel != null) {
      val transomLength = design.widthMm - 120 + (weld * 2)
      profileCuts.add(
        CutPiece(
          id = "transom_h",
          profileName = "${design.profileSystem} - پروفیل مولیون (وادار)",
          sectionType = "مولیون",
          lengthMm = transomLength,
          quantity = 1,
          angleStart = 90,
          angleEnd = 90,
          tag = "مولیون افقی کتیبه"
        )
      )
      if (isUpvc) {
        reinforcementCuts.add(
          CutPiece(
            id = "rf_transom",
            profileName = "گالوانیزه قوطی مولیون",
            sectionType = "گالوانیزه",
            lengthMm = (transomLength - 40).coerceAtLeast(100),
            quantity = 1,
            angleStart = 90,
            angleEnd = 90,
            tag = "گالوانیزه مولیون کتیبه"
          )
        )
      }

      // Glass for transom
      val tGlassW = (geom.transomPanel.widthMm - glassDeductionMm).toInt()
      val tGlassH = (geom.transomPanel.heightMm - glassDeductionMm).toInt()
      val tArea = (BigDecimal(tGlassW).multiply(BigDecimal(tGlassH)))
        .divide(BigDecimal(1_000_000), 4, RoundingMode.HALF_UP)
      glassPieces.add(
        GlassPiece(
          id = "glass_transom",
          tag = "شیشه کتیبه بالا",
          widthMm = tGlassW,
          heightMm = tGlassH,
          areaM2 = tArea,
          glazingTitle = "دوجداره استاندارد",
          tintTitle = "شفاف"
        )
      )
    }

    // Dividers are shared with both renderers, including nested horizontal transoms.
    geom.dividers.forEachIndexed { index, divider ->
      val length = (if (divider.axis == com.example.domain.model.SplitAxis.VERTICAL) divider.heightMm else divider.widthMm).toInt() + weld * 2
      profileCuts.add(CutPiece("divider_$index", "${design.profileSystem} - پروفیل مولیون (وادار)",
        "مولیون", length, 1, 90, 90, "جداکننده ${index + 1}"))
      if (isUpvc) reinforcementCuts.add(CutPiece("rf_divider_$index", "گالوانیزه قوطی مولیون",
        "گالوانیزه", (length - 40).coerceAtLeast(100), 1, 90, 90, "تقویت جداکننده ${index + 1}"))
    }
    // 4. Panels (Fixed or Sash Operable)
    geom.panels.forEachIndexed { idx, p ->
      val panelConfig = design.panels.getOrNull(idx)
      val opening = panelConfig?.openingType ?: OpeningType.FIXED
      val panelTag = "قسمت ${idx + 1}"

      if (opening == OpeningType.FIXED) {
        // Fixed pane glass directly in frame rebate
        val glassW = (p.widthMm - glassDeductionMm).toInt()
        val glassH = (p.heightMm - glassDeductionMm).toInt()
        val area = BigDecimal(glassW).multiply(BigDecimal(glassH))
          .divide(BigDecimal(1_000_000), 4, RoundingMode.HALF_UP)

        glassPieces.add(
          GlassPiece(
            id = "glass_fixed_$idx",
            tag = "شیشه ثابت $panelTag",
            widthMm = glassW,
            heightMm = glassH,
            areaM2 = area,
            glazingTitle = panelConfig?.glazingType?.titlePersian ?: "دوجداره",
            tintTitle = panelConfig?.glassTint?.titlePersian ?: "ساده"
          )
        )

        // Glazing beads for fixed section (2 width + 2 height)
        glazingBeadCuts.add(
          CutPiece(
            id = "bead_fixed_w_$idx",
            profileName = "زوار شیشه دوجداره",
            sectionType = "زوار",
            lengthMm = glassW,
            quantity = 2,
            angleStart = 45,
            angleEnd = 45,
            tag = "زوار افقی $panelTag"
          )
        )
        glazingBeadCuts.add(
          CutPiece(
            id = "bead_fixed_h_$idx",
            profileName = "زوار شیشه دوجداره",
            sectionType = "زوار",
            lengthMm = glassH,
            quantity = 2,
            angleStart = 45,
            angleEnd = 45,
            tag = "زوار عمودی $panelTag"
          )
        )
      } else {
        // Operable Sash: Frame opening plus overlap
        val sashW = (p.widthMm + (sashOverlapMm * 2) + (weld * 2)).toInt()
        val sashH = (p.heightMm + (sashOverlapMm * 2) + (weld * 2)).toInt()

        profileCuts.add(
          CutPiece(
            id = "sash_w_$idx",
            profileName = "${design.profileSystem} - پروفیل لنگه بازشو (سش)",
            sectionType = "لنگه",
            lengthMm = sashW,
            quantity = 2,
            angleStart = 45,
            angleEnd = 45,
            tag = "لنگه افقی $panelTag"
          )
        )
        profileCuts.add(
          CutPiece(
            id = "sash_h_$idx",
            profileName = "${design.profileSystem} - پروفیل لنگه بازشو (سش)",
            sectionType = "لنگه",
            lengthMm = sashH,
            quantity = 2,
            angleStart = 45,
            angleEnd = 45,
            tag = "لنگه عمودی $panelTag"
          )
        )

        if (isUpvc) {
          reinforcementCuts.add(
            CutPiece(
              id = "rf_sash_w_$idx",
              profileName = "گالوانیزه تقویتی سش",
              sectionType = "گالوانیزه",
              lengthMm = (sashW - 60).coerceAtLeast(100),
              quantity = 2,
              angleStart = 90,
              angleEnd = 90,
              tag = "گالوانیزه لنگه افقی $panelTag"
            )
          )
          reinforcementCuts.add(
            CutPiece(
              id = "rf_sash_h_$idx",
              profileName = "گالوانیزه تقویتی سش",
              sectionType = "گالوانیزه",
              lengthMm = (sashH - 60).coerceAtLeast(100),
              quantity = 2,
              angleStart = 90,
              angleEnd = 90,
              tag = "گالوانیزه لنگه عمودی $panelTag"
            )
          )
        }

        // Sash Glass
        val glassW = (sashW - 140 - glassDeductionMm).coerceAtLeast(100)
        val glassH = (sashH - 140 - glassDeductionMm).coerceAtLeast(100)
        val area = BigDecimal(glassW).multiply(BigDecimal(glassH))
          .divide(BigDecimal(1_000_000), 4, RoundingMode.HALF_UP)

        glassPieces.add(
          GlassPiece(
            id = "glass_sash_$idx",
            tag = "شیشه لنگه بازشو $panelTag",
            widthMm = glassW,
            heightMm = glassH,
            areaM2 = area,
            glazingTitle = panelConfig?.glazingType?.titlePersian ?: "دوجداره",
            tintTitle = panelConfig?.glassTint?.titlePersian ?: "ساده"
          )
        )

        // Glazing beads for sash
        glazingBeadCuts.add(
          CutPiece(
            id = "bead_sash_w_$idx",
            profileName = "زوار شیشه لنگه",
            sectionType = "زوار",
            lengthMm = glassW,
            quantity = 2,
            angleStart = 45,
            angleEnd = 45,
            tag = "زوار لنگه افقی $panelTag"
          )
        )
        glazingBeadCuts.add(
          CutPiece(
            id = "bead_sash_h_$idx",
            profileName = "زوار شیشه لنگه",
            sectionType = "زوار",
            lengthMm = glassH,
            quantity = 2,
            angleStart = 45,
            angleEnd = 45,
            tag = "زوار لنگه عمودی $panelTag"
          )
        )

        // Hardware tracking
        if (panelConfig?.hasHandle != false) {
          val handleName = panelConfig?.handleModel?.takeIf { it.isNotBlank() } ?: "دستگیره استاندارد"
          val handleKey = if (handleName == "دستگیره روتو استاندارد") "دستگیره استاندارد" else "دستگیره: $handleName"
          hardwarePcs[handleKey] = (hardwarePcs[handleKey] ?: 0) + 1
        }
        if (opening != OpeningType.SLIDING_LEFT && opening != OpeningType.SLIDING_RIGHT)
          hardwarePcs["لولا قابل رگلاژ"] = (hardwarePcs["لولا قابل رگلاژ"] ?: 0) + (if (sashH > 1500) 3 else 2)
        val espagnoletteType = when (opening) {
          OpeningType.TILT_AND_TURN -> "مکانیزم یراق کامل دوحالته"
          OpeningType.SLIDING_LEFT, OpeningType.SLIDING_RIGHT -> "قفل و غلطک کشویی بلبرینگی"
          else -> "اسپانیولت تک‌حالته"
        }
        hardwarePcs[espagnoletteType] = (hardwarePcs[espagnoletteType] ?: 0) + 1
      }
    }

    // Totals
    val totalProfileLength = profileCuts.sumOf { it.lengthMm * it.quantity }
    val totalRfLength = reinforcementCuts.sumOf { it.lengthMm * it.quantity }
    val totalGlassArea = glassPieces.fold(BigDecimal.ZERO) { acc, g -> acc.add(g.areaM2) }
    // Gasket meters roughly perimeter of frames + sashes + beads
    val totalGasketM = BigDecimal(totalProfileLength * 2 + glazingBeadCuts.sumOf { it.lengthMm * it.quantity })
      .divide(BigDecimal(1000), 2, RoundingMode.HALF_UP)

    return ManufacturingResult(
      profileCuts = profileCuts,
      reinforcementCuts = reinforcementCuts,
      glazingBeadCuts = glazingBeadCuts,
      glassPieces = glassPieces,
      totalGasketMeters = totalGasketM,
      hardwarePcs = hardwarePcs,
      totalProfileLengthMm = totalProfileLength,
      totalReinforcementLengthMm = totalRfLength,
      totalGlassAreaM2 = totalGlassArea,
      isProvisional = missingRules.isNotEmpty(),
      missingRulesDescription = missingRules.joinToString(" | ")
    )
  }
}
