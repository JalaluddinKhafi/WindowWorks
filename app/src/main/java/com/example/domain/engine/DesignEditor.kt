package com.example.domain.engine

import com.example.domain.model.*
import java.util.UUID

/** Pure editing commands shared by the canvas, property inspector and history. */
object DesignEditor {
  fun root(design: WindowDesign): LayoutNode = design.layoutRoot ?: run {
    val widths = GeometryEngine.computeGeometry(design).panels.map { it.widthMm }
    legacyRoot(design.panels, widths)
  }

  private fun legacyRoot(panels: List<PanelConfig>, widths: List<Float>): LayoutNode {
    if (panels.size <= 1) return LayoutNode(panelId = panels.firstOrNull()?.id)
    val extentAfterFirstDivider = widths.sum() + 75f * (panels.size - 2)
    return LayoutNode(axis = SplitAxis.VERTICAL,
      fraction = widths.first() / extentAfterFirstDivider,
      first = LayoutNode(panelId = panels.first().id), second = legacyRoot(panels.drop(1), widths.drop(1)))
  }
}
