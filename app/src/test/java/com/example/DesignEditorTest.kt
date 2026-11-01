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
}
