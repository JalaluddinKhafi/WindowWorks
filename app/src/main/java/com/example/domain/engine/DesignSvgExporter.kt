package com.example.domain.engine

import com.example.domain.model.*
import java.util.Locale

/** Millimetre-based vector elevation, readable by browsers and vector/CAD import tools. */
object DesignSvgExporter {
  fun export(design: WindowDesign): String {
    val geometry = GeometryEngine.computeGeometry(design)
    fun n(v: Float) = String.format(Locale.US, "%.2f", v)
    fun color(v: Long) = "#%06X".format(Locale.US, v and 0xFFFFFF)
    fun escape(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    return buildString {
      append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"${design.widthMm}mm\" height=\"${design.heightMm}mm\" viewBox=\"-100 -100 ${design.widthMm + 300} ${design.heightMm + 300}\">")
      append("<title>${escape(design.name)}</title><g stroke=\"#475569\" stroke-width=\"3\">")
      fun rect(x: Float, y: Float, w: Float, h: Float, fill: String) {
        append("<rect x=\"${n(x)}\" y=\"${n(y)}\" width=\"${n(w)}\" height=\"${n(h)}\" fill=\"$fill\"/>")
      }
      fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
        append("<line x1=\"${n(x1)}\" y1=\"${n(y1)}\" x2=\"${n(x2)}\" y2=\"${n(y2)}\"/>")
      }
      rect(0f, 0f, design.widthMm.toFloat(), design.heightMm.toFloat(), color(design.frameFinish.colorHex))
      geometry.transomPanel?.let { p -> rect(p.xMm, p.yMm, p.widthMm, p.heightMm, color(design.outerGlassTint.colorHex)) }
      geometry.panels.forEach { p ->
        val panel = design.panels[p.panelIndex]
        val face = if (p.isSash) minOf(65f, p.widthMm / 4, p.heightMm / 4) else 0f
        rect(p.xMm, p.yMm, p.widthMm, p.heightMm, color((panel.sashFinish ?: design.sashFinish).colorHex))
        rect(p.xMm + face, p.yMm + face, p.widthMm - 2 * face, p.heightMm - 2 * face, color(panel.glassTint.colorHex))
        val x = p.xMm; val y = p.yMm; val w = p.widthMm; val h = p.heightMm
        when (panel.openingType) {
          OpeningType.CASEMENT_LEFT -> { line(x, y, x + w, y + h / 2); line(x + w, y + h / 2, x, y + h) }
          OpeningType.CASEMENT_RIGHT, OpeningType.TILT_AND_TURN -> { line(x + w, y, x, y + h / 2); line(x, y + h / 2, x + w, y + h) }
          OpeningType.TOP_HUNG -> { line(x, y, x + w / 2, y + h); line(x + w / 2, y + h, x + w, y) }
          OpeningType.SLIDING_LEFT, OpeningType.SLIDING_RIGHT -> {
            val sign = if (panel.openingType == OpeningType.SLIDING_LEFT) -1 else 1
            val end = x + w * (if (sign < 0) 0.25f else 0.75f)
            line(x + w - (end - x), y + h / 2, end, y + h / 2)
            line(end - sign * 30, y + h / 2 - 30, end, y + h / 2)
            line(end - sign * 30, y + h / 2 + 30, end, y + h / 2)
          }
          else -> Unit
        }
        if (p.isSash && panel.hasHandle) {
          val defaultX = when (panel.openingType) { OpeningType.CASEMENT_LEFT -> 1 - face / (2 * w); OpeningType.TOP_HUNG -> 0.5f; else -> face / (2 * w) }
          val hx = x + w * (panel.handleX ?: defaultX); val hy = y + h * panel.handleY
          rect(hx - 6, hy - 6, 12f, 65f, "#0f172a")
        }
        append("<text x=\"${n(x + w / 2)}\" y=\"${n(y + h - 20)}\" text-anchor=\"middle\" font-size=\"30\" stroke=\"none\">${p.panelIndex + 1}: ${w.toInt()} × ${h.toInt()} mm</text>")
      }
      geometry.dividers.forEach { rect(it.xMm, it.yMm, it.widthMm, it.heightMm, color(design.frameFinish.colorHex)) }
      append("</g><text x=\"${design.widthMm / 2}\" y=\"${design.heightMm + 100}\" font-size=\"45\" text-anchor=\"middle\">${design.widthMm} × ${design.heightMm} mm</text></svg>")
    }
  }
}
