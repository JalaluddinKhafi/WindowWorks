package com.example

import com.example.core.AppLanguage
import com.example.core.Localization
import com.example.domain.engine.CadTemplates
import com.example.domain.engine.DesignEditor
import com.example.domain.engine.GeometryEngine
import com.example.domain.model.FrameFinish
import com.example.domain.model.WindowDesign
import org.junit.Assert.*
import org.junit.Test

class CadTemplatesAndLocalizationTest {

  @Test
  fun testAllTemplatesGenerateValidGeometry() {
    val templates = CadTemplates.templates
    assertTrue("Should have at least 10 CAD templates", templates.size >= 10)

    templates.forEach { tmpl ->
      val design = tmpl.createDesign(
        "test_${tmpl.id}",
        "Test ${tmpl.nameKey}",
        tmpl.defaultWidthMm,
        tmpl.defaultHeightMm,
        "Wintech 6000",
        FrameFinish.WHITE,
        FrameFinish.WHITE
      )

      assertNotNull("Design must not be null for ${tmpl.id}", design)
      assertEquals(tmpl.defaultWidthMm, design.widthMm)
      assertEquals(tmpl.defaultHeightMm, design.heightMm)
      assertEquals(tmpl.panelCount, design.panels.size)

      val geom = GeometryEngine.computeGeometry(design)
      assertTrue("Errors for template ${tmpl.id}: ${geom.validationErrors}", geom.validationErrors.isEmpty())
      assertEquals(tmpl.panelCount, geom.panels.size)
    }
  }

  @Test
  fun testApplyTemplatePreservesIdentity() {
    val current = WindowDesign(
      id = "custom_id_123",
      name = "Custom Bedroom Window",
      notes = "Special safety glass required",
      profileSystem = "Alupen Thermal 65"
    )

    val slidingTmpl = CadTemplates.findById("sliding_2_track_2_leaf")
    assertNotNull(slidingTmpl)

    val updated = CadTemplates.applyTemplate(current, slidingTmpl!!)
    assertEquals("custom_id_123", updated.id)
    assertEquals("Custom Bedroom Window", updated.name)
    assertEquals("Special safety glass required", updated.notes)
    assertEquals("Alupen Thermal 65", updated.profileSystem)
    assertEquals(2, updated.panels.size)
  }

  @Test
  fun testSetPanelWidthMm() {
    val tmpl = CadTemplates.findById("casement_2_fixed_sash")!!
    val design = tmpl.createDesign(
      "test_design",
      "Test",
      1600,
      1500,
      "Wintech 6000",
      FrameFinish.WHITE,
      FrameFinish.WHITE
    )

    val modified = DesignEditor.setPanelWidthMm(design, 0, 600)
    val geom = GeometryEngine.computeGeometry(modified)
    assertTrue(geom.validationErrors.isEmpty())

    val firstPanelWidth = geom.panels[0].widthMm
    // Allow slight float tolerance around 600mm
    assertEquals(600f, firstPanelWidth, 2f)
  }

  @Test
  fun testSnappingFunctions() {
    // Snap to 10mm grid
    val snapped1 = DesignEditor.snapFraction(0.493f, 1000f, 10)
    // 0.493 * 1000 = 493 -> round to 490 -> fraction = 0.49f
    assertEquals(0.49f, snapped1, 0.001f)

    // Snap to 50mm grid
    val snapped2 = DesignEditor.snapFraction(0.493f, 1000f, 50)
    // 493 -> round to 500 -> fraction = 0.5f
    assertEquals(0.50f, snapped2, 0.001f)

    // Snap to equal split (center 0.5f)
    val centerSnap = DesignEditor.snapToEqualSplit(0.512f, 2, 0.04f)
    assertEquals(0.5f, centerSnap, 0.0001f)

    // Far from center -> remains unchanged
    val noSnap = DesignEditor.snapToEqualSplit(0.65f, 2, 0.04f)
    assertEquals(0.65f, noSnap, 0.0001f)
  }

  @Test
  fun testLocalizationTrilingualSupport() {
    // Digits
    assertEquals("1400", Localization.formatDigits(1400, AppLanguage.ENGLISH))
    assertEquals("۱۴۰۰", Localization.formatDigits(1400, AppLanguage.DARI))
    assertEquals("۱۴۰۰", Localization.formatDigits(1400, AppLanguage.PASHTO))

    // Dimensions
    assertEquals("1400 mm", Localization.formatDimensionMm(1400, AppLanguage.ENGLISH))
    assertEquals("۱۴۰۰ میلی‌متر", Localization.formatDimensionMm(1400, AppLanguage.DARI))
    assertEquals("۱۴۰۰ ملي متر", Localization.formatDimensionMm(1400, AppLanguage.PASHTO))

    // Common keys
    assertEquals("Templates", Localization.get("templates", AppLanguage.ENGLISH))
    assertEquals("طرح‌های آماده", Localization.get("templates", AppLanguage.DARI))
    assertEquals("چمتو شوي ډیزاینونه", Localization.get("templates", AppLanguage.PASHTO))

    // RTL verification
    assertFalse(AppLanguage.ENGLISH.isRtl)
    assertTrue(AppLanguage.DARI.isRtl)
    assertTrue(AppLanguage.PASHTO.isRtl)
  }
}
