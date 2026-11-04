package com.example

import com.example.domain.engine.DesignEditor
import com.example.domain.engine.GeometryEngine
import com.example.domain.engine.ManufacturingCalculator
import com.example.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Material family (PVC vs aluminum) must drive profile faces, rules and openings. */
class MaterialFamilyDesignTest {

  private fun design(family: MaterialFamily) = WindowDesign(
    id = "mat_${family.name}",
    name = "Material test",
    materialFamily = family,
    profileSystem = MaterialCatalog.defaultFor(family).code,
    profileFaceWidthMm = MaterialCatalog.defaultFor(family).faceWidthMm,
    hasThermalBreak = family != MaterialFamily.ALUMINUM,
    widthMm = 1800,
    heightMm = 1500,
    panels = listOf(PanelConfig("p1", OpeningType.FIXED), PanelConfig("p2", OpeningType.CASEMENT_LEFT))
  )

  @Test
  fun everyFamilyHasSystems() {
    MaterialFamily.entries.forEach { family ->
      assertTrue("no profile system for $family", MaterialCatalog.forFamily(family).isNotEmpty())
      assertEquals(family, MaterialCatalog.defaultFor(family).family)
    }
  }

  @Test
  fun profileFaceWidthFollowsSelectedSystem() {
    val upvcGeom = GeometryEngine.computeGeometry(design(MaterialFamily.UPVC))
    val alumGeom = GeometryEngine.computeGeometry(design(MaterialFamily.ALUMINUM))

    assertEquals(60f, upvcGeom.frameProfileFaceMm, 0.01f)
    assertEquals(50f, alumGeom.frameProfileFaceMm, 0.01f)
    // A different frame face pushes the first pane to a different inset.
    assertNotEquals(upvcGeom.panels.first().xMm, alumGeom.panels.first().xMm, 0.01f)
  }

  @Test
  fun aluminumHasNoGalvanizedReinforcement() {
    val upvc = ManufacturingCalculator.calculate(design(MaterialFamily.UPVC))
    val aluminum = ManufacturingCalculator.calculate(design(MaterialFamily.ALUMINUM))
    assertTrue(upvc.reinforcementCuts.isNotEmpty())
    assertTrue(aluminum.reinforcementCuts.isEmpty())
  }

  @Test
  fun aluminumUsesMitreCorrectionInsteadOfWeld() {
    val upvc = ManufacturingCalculator.calculate(design(MaterialFamily.UPVC))
    val aluminum = ManufacturingCalculator.calculate(design(MaterialFamily.ALUMINUM))
    val upvcFrame = upvc.profileCuts.first { it.id == "f_top" }
    val alumFrame = aluminum.profileCuts.first { it.id == "f_top" }
    // PVC: width + 2 * 3 mm weld. Aluminum: width + 4 mm mitre correction.
    assertEquals(1806, upvcFrame.lengthMm)
    assertEquals(1804, alumFrame.lengthMm)
  }

  @Test
  fun mismatchedSystemProducesWarning() {
    val wrong = design(MaterialFamily.UPVC).copy(profileSystem = "Akpa Thermal")
    val warnings = GeometryEngine.computeGeometry(wrong).validationWarnings
    assertTrue(warnings.any { it.contains("Akpa") })
  }

  @Test
  fun templateKeepsMaterialFamily() {
    val aluminum = design(MaterialFamily.ALUMINUM)
    val template = com.example.domain.engine.CadTemplates.templates.first()
    val applied = com.example.domain.engine.CadTemplates.applyTemplate(aluminum, template)
    assertEquals(MaterialFamily.ALUMINUM, applied.materialFamily)
    assertEquals(aluminum.profileFaceWidthMm, applied.profileFaceWidthMm)
  }

  @Test
  fun switchingFamilyKeepsPanelsValid() {
    val start = design(MaterialFamily.UPVC)
    val split = DesignEditor.split(start, 0, SplitAxis.VERTICAL)
    assertEquals(3, split.panels.size)
    assertTrue(GeometryEngine.computeGeometry(split).validationErrors.isEmpty())
  }
}
