package com.example

import com.example.domain.engine.GeometryEngine
import com.example.domain.model.*
import com.example.ui.designer.buildWindow3DMesh
import org.junit.Assert.*
import org.junit.Test

class DesignerMeshTest {
  private fun design(opening: OpeningType) = WindowDesign("test", "mesh", widthMm = 1600, heightMm = 2000,
    panels = listOf(PanelConfig("panel", opening, hasHandle = false)))

  @Test fun operableSashUsesFourProfileBarsAndLeavesTheGlassUnobstructed() {
    val d = design(OpeningType.CASEMENT_LEFT)
    val mesh = buildWindow3DMesh(d, GeometryEngine.computeGeometry(d), 70f, 0f)
    assertEquals(49, mesh.size) // 4 outer bars + 4 sash bars (6 faces each), one glass plane.
    assertEquals(1, mesh.count { it.isGlass })
    assertTrue(mesh.flatMap { it.vertices }.all { it.x.isFinite() && it.y.isFinite() && it.z.isFinite() })
  }

  @Test fun slidingTranslatesGlassWithoutRotatingIt() {
    val d = design(OpeningType.SLIDING_LEFT)
    val g = GeometryEngine.computeGeometry(d)
    val closed = buildWindow3DMesh(d, g, 70f, 0f).single { it.isGlass }.vertices
    val open = buildWindow3DMesh(d, g, 70f, 90f).single { it.isGlass }.vertices
    closed.zip(open).forEach { (a, b) ->
      assertEquals(a.x - g.panels.single().widthMm * 0.8f, b.x, 0.01f)
      assertEquals(a.y, b.y, 0.01f)
      assertEquals(a.z, b.z, 0.01f)
    }
  }
}
