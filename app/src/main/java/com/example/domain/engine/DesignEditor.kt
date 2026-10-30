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

  fun split(design: WindowDesign, index: Int, axis: SplitAxis, fraction: Float = 0.5f): WindowDesign {
    if (design.panels.size >= 48) return design
    val panel = design.panels.getOrNull(index) ?: return design
    val newPanel = panel.copy(id = UUID.randomUUID().toString(), openingType = OpeningType.FIXED)
    fun visit(node: LayoutNode): LayoutNode = if (node.panelId == panel.id) {
      LayoutNode(axis = axis, fraction = fraction.coerceIn(0.1f, 0.9f),
        first = LayoutNode(panelId = panel.id), second = LayoutNode(panelId = newPanel.id))
    } else node.copy(first = node.first?.let(::visit), second = node.second?.let(::visit))
    val candidate = design.copy(layoutRoot = visit(root(design)), panels = design.panels + newPanel)
    return if (GeometryEngine.computeGeometry(candidate).panels.any { it.widthMm < 160 || it.heightMm < 160 }) design else candidate
  }

  /** Removing an opening expands its sibling into the parent opening. */
  fun remove(design: WindowDesign, index: Int): WindowDesign {
    if (design.panels.size <= 1) return design
    val id = design.panels.getOrNull(index)?.id ?: return design
    fun visit(node: LayoutNode): LayoutNode? {
      if (node.panelId == id) return null
      if (node.panelId != null) return node
      val a = node.first?.let(::visit)
      val b = node.second?.let(::visit)
      return if (a == null) b else if (b == null) a else node.copy(first = a, second = b)
    }
    return design.copy(layoutRoot = visit(root(design)), panels = design.panels.filterNot { it.id == id })
  }

  fun parent(design: WindowDesign, index: Int): LayoutNode? {
    val id = design.panels.getOrNull(index)?.id ?: return null
    fun find(n: LayoutNode): LayoutNode? = when {
      n.first?.panelId == id || n.second?.panelId == id -> n
      else -> n.first?.let(::find) ?: n.second?.let(::find)
    }
    return find(root(design))
  }

  fun resizeParent(design: WindowDesign, index: Int, fraction: Float): WindowDesign {
    val target = parent(design, index) ?: return design
    fun visit(n: LayoutNode): LayoutNode = if (n === target) n.copy(fraction = fraction.coerceIn(0.01f, 0.99f))
      else n.copy(first = n.first?.let(::visit), second = n.second?.let(::visit))
    // Use the same tree instance for reference-based replacement in legacy layouts.
    val explicit = if (design.layoutRoot == null) design.copy(layoutRoot = root(design)) else design
    if (design.layoutRoot == null) return resizeParent(explicit, index, fraction)
    val candidate = explicit.copy(layoutRoot = visit(explicit.layoutRoot!!))
    return if (GeometryEngine.computeGeometry(candidate).panels.any { it.widthMm < 160 || it.heightMm < 160 }) design else candidate
  }

  /** Available size on both sides of the selected panel's parent divider, excluding its face. */
  fun parentAvailableMm(design: WindowDesign, index: Int): Float? {
    val parent = parent(design, index) ?: return null
    fun ids(n: LayoutNode): List<String> = n.panelId?.let { listOf(it) }
      ?: (n.first?.let(::ids).orEmpty() + n.second?.let(::ids).orEmpty())
    val descendants = ids(parent).toSet()
    val bounds = GeometryEngine.computeGeometry(design).panels.filter { design.panels[it.panelIndex].id in descendants }
    if (bounds.isEmpty()) return null
    return (if (parent.axis == SplitAxis.VERTICAL)
      bounds.maxOf { it.xMm + it.widthMm } - bounds.minOf { it.xMm }
    else bounds.maxOf { it.yMm + it.heightMm } - bounds.minOf { it.yMm }) - 75f
  }
}
