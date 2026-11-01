package com.example

import com.example.domain.engine.*
import com.example.domain.model.*
import com.example.ui.designer.DesignHistory
import org.junit.Assert.*
import org.junit.Test

class DesignEditorTest {
  private fun single() = WindowDesign("test", "طرح آزمایشی", widthMm = 2400, heightMm = 2200,
    panels = listOf(PanelConfig("p1", OpeningType.CASEMENT_LEFT)))

  @Test fun nestedSplitsTileTheOpeningWithoutOverlapsAndProduceRealCuts() {
    val vertical = DesignEditor.split(single(), 0, SplitAxis.VERTICAL, 0.4f)
    val nested = DesignEditor.split(vertical, 1, SplitAxis.HORIZONTAL, 0.3f)
    val g = GeometryEngine.computeGeometry(nested)
    assertEquals(3, g.panels.size)
    assertEquals(2, g.dividers.size)
    assertTrue(g.validationErrors.isEmpty())
    val area = g.panels.sumOf { (it.widthMm * it.heightMm).toDouble() } + g.dividers.sumOf { (it.widthMm * it.heightMm).toDouble() }
    assertEquals((2400.0 - 120) * (2200 - 120), area, 1.0)
    for (a in g.panels) for (b in g.panels) if (a.panelIndex != b.panelIndex) {
      val intersectionW = minOf(a.xMm + a.widthMm, b.xMm + b.widthMm) - maxOf(a.xMm, b.xMm)
      val intersectionH = minOf(a.yMm + a.heightMm, b.yMm + b.heightMm) - maxOf(a.yMm, b.yMm)
      assertTrue(intersectionW <= 0 || intersectionH <= 0)
    }
    val m = ManufacturingCalculator.calculate(nested)
    val cuts = m.profileCuts.filter { it.id.startsWith("divider_") }
    assertEquals(2, cuts.size)
    assertEquals(g.dividers[1].widthMm.toInt() + 6, cuts[1].lengthMm)
    assertEquals(3, m.glassPieces.size)
    assertTrue(m.glassPieces.all { it.widthMm > 0 && it.heightMm > 0 })
  }

  @Test fun removalExpandsTheSiblingAndNeverRemovesLastPanel() {
    val split = DesignEditor.split(DesignEditor.split(single(), 0, SplitAxis.VERTICAL), 1, SplitAxis.HORIZONTAL)
    val removed = DesignEditor.remove(split, 0)
    assertEquals(2, removed.panels.size)
    assertEquals(1, GeometryEngine.computeGeometry(removed).dividers.size)
    assertEquals(2280f, GeometryEngine.computeGeometry(removed).panels.first().widthMm, 0.01f)
    val last = DesignEditor.remove(removed, 0)
    assertEquals(last, DesignEditor.remove(last, 0))
  }

  @Test fun movingDividerKeepsSharedGeometryAndRejectsTinyOpenings() {
    val split = DesignEditor.split(single(), 0, SplitAxis.VERTICAL)
    val resized = DesignEditor.resizeParent(split, 0, 0.7f)
    assertEquals((2280f - 75) * 0.7f, GeometryEngine.computeGeometry(resized).panels.first().widthMm, 0.01f)
    val available = DesignEditor.parentAvailableMm(split, 0)!!
    val precise = DesignEditor.resizeParent(split, 0, 180f / available)
    assertEquals(180f, GeometryEngine.computeGeometry(precise).panels.first().widthMm, 0.01f)
    val tiny = single().copy(widthMm = 400, heightMm = 400)
    assertEquals(tiny, DesignEditor.split(tiny, 0, SplitAxis.VERTICAL))
  }

  @Test fun legacyUnequalPanelsKeepTheirOriginalWidthsWhenConvertingToTree() {
    val legacy = single().copy(panels = listOf(PanelConfig("a", widthRatio = 1f), PanelConfig("b", widthRatio = 2f), PanelConfig("c", widthRatio = 3f)))
    val before = GeometryEngine.computeGeometry(legacy)
    val after = GeometryEngine.computeGeometry(legacy.copy(layoutRoot = DesignEditor.root(legacy)))
    before.panels.zip(after.panels).forEach { (a, b) ->
      assertEquals(a.xMm, b.xMm, 0.01f)
      assertEquals(a.widthMm, b.widthMm, 0.01f)
    }
  }

  @Test fun disablingHandleChangesBillOfMaterialsAndSlidingDoesNotAddHinges() {
    val noHandle = single().copy(panels = listOf(PanelConfig("p1", OpeningType.SLIDING_RIGHT, hasHandle = false)))
    val m = ManufacturingCalculator.calculate(noHandle)
    assertFalse(m.hardwarePcs.keys.any { it.contains("دستگیره") })
    assertFalse(m.hardwarePcs.containsKey("لولا قابل رگلاژ"))
    assertTrue(m.hardwarePcs.containsKey("قفل و غلطک کشویی بلبرینگی"))
  }

  @Test fun undoRedoRestoresTheWholeLayoutAndNewEditClearsRedo() {
    val initial = single()
    val history = DesignHistory(initial)
    val split = DesignEditor.split(initial, 0, SplitAxis.HORIZONTAL)
    history.commit(split); history.undo()
    assertEquals(initial, history.current)
    history.redo(); assertEquals(split, history.current)
    history.undo(); history.commit(initial.copy(name = "تغییر"))
    assertFalse(history.canRedo)
  }

  @Test fun svgEscapesNamesAndExportsAllNestedPanelsAndHandles() {
    val design = DesignEditor.split(single().copy(name = "<test & name>"), 0, SplitAxis.HORIZONTAL)
    val svg = DesignSvgExporter.export(design)
    assertTrue(svg.contains("&lt;test &amp; name&gt;"))
    assertTrue(svg.contains("1:")); assertTrue(svg.contains("2:"))
    assertTrue(svg.contains("viewBox="))
  }
}
