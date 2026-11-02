package com.example

import com.example.domain.engine.DesignEditor
import com.example.domain.engine.GeometryEngine
import com.example.domain.model.*
import com.example.ui.designer.DesignHistory
import com.example.ui.designer.buildWindow3DMesh
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class DesignModuleCadTests {

  // Test A: Single fixed window 1200x1000 mm creates 1 glass panel with correct frame borders and dimensions
  @Test
  fun testA_singleFixedWindow1200x1000() {
    val design = WindowDesign(
      id = "test-a",
      name = "Single Fixed",
      widthMm = 1200,
      heightMm = 1000,
      panels = listOf(PanelConfig(id = "p1", openingType = OpeningType.FIXED))
    )
    val geom = GeometryEngine.computeGeometry(design)
    assertEquals(1, geom.panels.size)
    assertEquals(0, geom.dividers.size)
    assertNull(geom.transomPanel)
    assertTrue(geom.validationErrors.isEmpty())

    val panel = geom.panels[0]
    assertFalse(panel.isSash)
    // Frame face is 60mm on each side
    assertEquals(60f, panel.xMm, 0.01f)
    assertEquals(60f, panel.yMm, 0.01f)
    assertEquals(1200f - 120f, panel.widthMm, 0.01f)
    assertEquals(1000f - 120f, panel.heightMm, 0.01f)
  }

  // Test B: 1800x1400 mm window with 2 vertical mullions creates 3 equal panels
  @Test
  fun testB_threePanelWindowWithMullions() {
    val initial = WindowDesign(
      id = "test-b",
      name = "Three Panel",
      widthMm = 1800,
      heightMm = 1400,
      panels = listOf(PanelConfig("p1"))
    )
    val threePanels = DesignEditor.equalizeDivisions(initial, 3, SplitAxis.VERTICAL)
    val geom = GeometryEngine.computeGeometry(threePanels)

    assertEquals(3, geom.panels.size)
    assertEquals(2, geom.dividers.size)
    assertTrue(geom.validationErrors.isEmpty())

    // All 3 panels should have equal width
    val w0 = geom.panels[0].widthMm
    val w1 = geom.panels[1].widthMm
    val w2 = geom.panels[2].widthMm
    assertEquals(w0, w1, 0.01f)
    assertEquals(w1, w2, 0.01f)

    // Total width inside frame = 1800 - 120 = 1680. Two mullions = 2 * 75 = 150.
    // Glass width total = 1680 - 150 = 1530 -> exactly 510mm each.
    assertEquals(510f, w0, 0.01f)
  }

  // Test C: Opening symbols and OpeningType configurations
  @Test
  fun testC_openingTypesConfiguration() {
    val types = OpeningType.entries
    assertTrue(types.contains(OpeningType.DOUBLE_CASEMENT))
    assertTrue(types.contains(OpeningType.TILT))
    assertTrue(types.contains(OpeningType.CASEMENT_LEFT))
    assertTrue(types.contains(OpeningType.CASEMENT_RIGHT))
    assertTrue(types.contains(OpeningType.TILT_AND_TURN))
    assertTrue(types.contains(OpeningType.SLIDING_LEFT))
    assertTrue(types.contains(OpeningType.SLIDING_RIGHT))
    assertTrue(types.contains(OpeningType.TOP_HUNG))
    assertTrue(types.contains(OpeningType.FIXED))

    val d = WindowDesign(
      id = "french-door",
      name = "French Door",
      widthMm = 1600,
      heightMm = 2100,
      panels = listOf(PanelConfig("french", openingType = OpeningType.DOUBLE_CASEMENT))
    )
    val geom = GeometryEngine.computeGeometry(d)
    assertTrue(geom.panels[0].isSash)
  }

  // Test D: Dimension edits parametrically update geometry and bounds
  @Test
  fun testD_dimensionEditsUpdateGeometry() {
    var design = WindowDesign(
      id = "test-d",
      name = "Dim Edit",
      widthMm = 1400,
      heightMm = 1200,
      hasTransom = false,
      panels = listOf(PanelConfig("p1"))
    )

    // Edit width and height
    design = design.copy(widthMm = 1600, heightMm = 1500)
    var geom = GeometryEngine.computeGeometry(design)
    assertEquals(1600f - 120f, geom.panels[0].widthMm, 0.01f)
    assertEquals(1500f - 120f, geom.panels[0].heightMm, 0.01f)

    // Add and edit transom
    design = DesignEditor.setTransomHeightMm(design, 450)
    geom = GeometryEngine.computeGeometry(design)
    assertNotNull(geom.transomPanel)
    assertEquals(1600f - 120f, geom.transomPanel!!.widthMm, 0.01f)
    assertTrue(geom.transomPanel!!.heightMm > 200f)
  }

  // Test E: Equal divisions (2, 3, 4 panels)
  @Test
  fun testE_equalDivisions() {
    val base = WindowDesign("test-e", "Equal Divisions", widthMm = 2400, heightMm = 1500)

    val twoSplit = DesignEditor.equalizeDivisions(base, 2, SplitAxis.VERTICAL)
    val g2 = GeometryEngine.computeGeometry(twoSplit)
    assertEquals(2, g2.panels.size)
    assertEquals(g2.panels[0].widthMm, g2.panels[1].widthMm, 0.01f)

    val fourSplit = DesignEditor.equalizeDivisions(base, 4, SplitAxis.VERTICAL)
    val g4 = GeometryEngine.computeGeometry(fourSplit)
    assertEquals(4, g4.panels.size)
    assertEquals(3, g4.dividers.size)
    assertEquals(g4.panels[0].widthMm, g4.panels[3].widthMm, 0.01f)
  }

  // Test F: 3D mesh generation creates valid non-empty polygons
  @Test
  fun testF_meshGenerationCreatesValidFaces() {
    val design = WindowDesign(
      id = "test-f",
      name = "3D Mesh Window",
      widthMm = 1500,
      heightMm = 1200,
      hasTransom = true,
      transomHeightMm = 350,
      panels = listOf(
        PanelConfig("p1", openingType = OpeningType.CASEMENT_LEFT),
        PanelConfig("p2", openingType = OpeningType.FIXED)
      )
    )
    val geom = GeometryEngine.computeGeometry(design)
    val mesh = buildWindow3DMesh(design, geom, frameDepthMm = 70f, sashAngleDeg = 30f)

    assertTrue(mesh.isNotEmpty())
    assertTrue(mesh.count { it.isGlass } >= 2) // Transom glass + panel glass
    assertTrue(mesh.flatMap { it.vertices }.all { it.x.isFinite() && it.y.isFinite() && it.z.isFinite() })
  }

  // Test G: Sash opening kinematics
  @Test
  fun testG_sashKinematics() {
    val dCasement = WindowDesign(
      "test-g", "Casement", widthMm = 1000, heightMm = 1200,
      panels = listOf(PanelConfig("p1", openingType = OpeningType.CASEMENT_LEFT, hasHandle = false))
    )
    val gC = GeometryEngine.computeGeometry(dCasement)
    val closedCasement = buildWindow3DMesh(dCasement, gC, 70f, 0f).single { it.isGlass }.vertices
    val openCasement = buildWindow3DMesh(dCasement, gC, 70f, 45f).single { it.isGlass }.vertices

    // Opening rotates the glass pane in 3D: Z coordinates change from flat (0f)
    assertTrue(closedCasement.all { Math.abs(it.z) < 0.01f })
    assertNotEquals(closedCasement.map { it.z }, openCasement.map { it.z })
    assertTrue(openCasement.any { Math.abs(it.z) > 10f })

    // Sliding sash translation
    val dSliding = WindowDesign(
      "test-g-slide", "Sliding", widthMm = 1000, heightMm = 1200,
      panels = listOf(PanelConfig("p1", openingType = OpeningType.SLIDING_LEFT, hasHandle = false))
    )
    val gS = GeometryEngine.computeGeometry(dSliding)
    val closedSlide = buildWindow3DMesh(dSliding, gS, 70f, 0f).single { it.isGlass }.vertices
    val openSlide = buildWindow3DMesh(dSliding, gS, 70f, 90f).single { it.isGlass }.vertices

    val dx = openSlide[0].x - closedSlide[0].x
    assertTrue(dx < 0f) // Slid left
  }

  // Test H: Window shapes (Rectangle, Arched, Triangle, Trapezoidal)
  @Test
  fun testH_windowShapesSupport() {
    val base = WindowDesign("test-h", "Shapes", widthMm = 1200, heightMm = 1400)

    val rect = DesignEditor.setShape(base, WindowShape.RECTANGLE)
    assertEquals(WindowShape.RECTANGLE, rect.windowShape)
    assertEquals(0f, GeometryEngine.computeGeometry(rect).archHeightMm, 0.01f)

    val arched = DesignEditor.setShape(base, WindowShape.ARCHED, 350)
    assertEquals(WindowShape.ARCHED, arched.windowShape)
    assertEquals(350, arched.archHeightMm)
    assertEquals(350f, GeometryEngine.computeGeometry(arched).archHeightMm, 0.01f)

    val triangle = DesignEditor.setShape(base, WindowShape.TRIANGLE, 400)
    assertEquals(WindowShape.TRIANGLE, triangle.windowShape)

    val trapezoid = DesignEditor.setShape(base, WindowShape.TRAPEZOIDAL, 250)
    assertEquals(WindowShape.TRAPEZOIDAL, trapezoid.windowShape)
  }

  // Test I: Backward compatibility with JSON Layout serialization
  @Test
  fun testI_backwardCompatibility() {
    val leafNode = LayoutNode(panelId = "p1")
    val encodedLeaf = com.example.data.repository.DesignLayoutCodec.encode(leafNode)
    val decodedLeaf = com.example.data.repository.DesignLayoutCodec.decode(encodedLeaf)
    assertEquals("p1", decodedLeaf.panelId)

    val splitNode = LayoutNode(
      axis = SplitAxis.VERTICAL,
      fraction = 0.5f,
      first = LayoutNode(panelId = "p1"),
      second = LayoutNode(panelId = "p2")
    )
    val encSplit = com.example.data.repository.DesignLayoutCodec.encode(splitNode)
    val decSplit = com.example.data.repository.DesignLayoutCodec.decode(encSplit)
    assertEquals(SplitAxis.VERTICAL, decSplit.axis)
    assertEquals(0.5f, decSplit.fraction, 0.001f)
    assertEquals("p1", decSplit.first?.panelId)
    assertEquals("p2", decSplit.second?.panelId)
  }

  // Test J: Undo / Redo history stack
  @Test
  fun testJ_undoRedoHistory() {
    val initial = WindowDesign("test-j", "History Test", widthMm = 1200, heightMm = 1000)
    val history = DesignHistory(initial)

    assertFalse(history.canUndo)
    assertFalse(history.canRedo)

    // Edit 1: width to 1400
    history.commit(initial.copy(widthMm = 1400))
    assertTrue(history.canUndo)
    assertEquals(1400, history.current.widthMm)

    // Edit 2: width to 1600
    history.commit(history.current.copy(widthMm = 1600))
    assertEquals(1600, history.current.widthMm)

    // Undo 1 step -> 1400
    history.undo()
    assertEquals(1400, history.current.widthMm)
    assertTrue(history.canRedo)

    // Undo 2nd step -> 1200
    history.undo()
    assertEquals(1200, history.current.widthMm)
    assertFalse(history.canUndo)

    // Redo 1 step -> 1400
    history.redo()
    assertEquals(1400, history.current.widthMm)
    assertTrue(history.canUndo)
  }
}
