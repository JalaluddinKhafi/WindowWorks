package com.example

import com.example.domain.engine.ManufacturingCalculator
import com.example.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class ManufacturingCalculatorTest {

  @Test
  fun testManufacturingCalculatesAccurateCuts() {
    val design = WindowDesign(
      id = "w_mfg_test",
      name = "پنجره دوجداره دولنگه",
      productType = ProductType.WINDOW,
      materialFamily = MaterialFamily.UPVC,
      profileSystem = "Wintech 6000",
      widthMm = 1400,
      heightMm = 1600,
      hasTransom = false,
      panels = listOf(
        PanelConfig("p1", OpeningType.FIXED),
        PanelConfig("p2", OpeningType.CASEMENT_LEFT)
      )
    )

    val result = ManufacturingCalculator.calculate(
      design = design,
      weldingAllowanceMm = 3
    )

    // Check Outer Frame: 2 pieces of 1400 + 6 = 1406mm, 2 pieces of 1600 + 6 = 1606mm
    val frameTop = result.profileCuts.find { it.id == "f_top" }
    assertNotNull(frameTop)
    assertEquals(1406, frameTop!!.lengthMm)
    assertEquals(45, frameTop.angleStart)
    assertEquals(45, frameTop.angleEnd)

    // Check operable sash cuts exist for panel 2
    val sashW = result.profileCuts.find { it.id == "sash_w_1" }
    assertNotNull(sashW)

    // Check Glass pieces generated
    assertEquals(2, result.glassPieces.size)
    assertTrue(result.totalGlassAreaM2 > BigDecimal.ZERO)

    // Check Hardware
    assertTrue(result.hardwarePcs.containsKey("دستگیره استاندارد"))
    assertEquals(1, result.hardwarePcs["دستگیره استاندارد"])
    assertTrue(result.hardwarePcs.containsKey("لولا قابل رگلاژ"))
  }
}
