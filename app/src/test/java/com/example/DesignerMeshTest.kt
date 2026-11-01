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
}
