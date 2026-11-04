package com.example.ui.designer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.core.AppLanguage
import com.example.core.LocalAppLanguage
import com.example.core.Localization
import com.example.domain.engine.GeometryEngine
import com.example.domain.model.OpeningDirection
import com.example.domain.model.OpeningType
import com.example.domain.model.WindowDesign
import com.example.domain.model.WindowShape

enum class CadToolType(val titleFa: String) {
  SELECT("انتخاب"),
  LINE("تقسیم عمودی"),
  RECTANGLE("مستطیل"),
  CIRCLE("دایره"),
  POLYGON("چندضلعی"),
  TEXT("متن"),
  DIMENSION("اندازه"),
  GRID("شبکه"),
  HANDLE("دستگیره"),
  PAN("جابجایی")
}

/** Optional technical layer drawn on the canvas. */
enum class CanvasViewMode(val titleFa: String) {
  BODY("جسم"),
  REPRESENTATION("نماى فني"),
  GLASS_HEAT("شيشه و عايق")
}

@Composable
fun Elevation2DCanvas(
  design: WindowDesign,
  activeTool: CadToolType = CadToolType.SELECT,
  selectedPanelIndex: Int? = null,
  onSelectPanel: ((Int) -> Unit)? = null,
  showGrid: Boolean = true,
  showDimensions: Boolean = true,
  onPlaceHandle: ((Int, Float, Float) -> Unit)? = null,
  onEditWidth: (() -> Unit)? = null,
  onEditHeight: (() -> Unit)? = null,
  onEditTransomHeight: (() -> Unit)? = null,
  onEditPanelWidth: ((Int) -> Unit)? = null,
  onMoveDivider: ((Int, Float) -> Unit)? = null,
  snapGuideFraction: Float? = null,
  /** Snap divider drags and taps to a millimeter step (10 mm by default). */
  snapStepMm: Int = 10,
  viewMode: CanvasViewMode = CanvasViewMode.BODY,
  /** Receives live zoom factor and a reset callback so the toolbar can offer identical controls. */
  onTransformState: ((Float, () -> Unit) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val geom = GeometryEngine.computeGeometry(design)
  val appLang = LocalAppLanguage.current

  // Zoom & Pan state
  var scaleFactor by remember { mutableFloatStateOf(1.0f) }
  var panOffset by remember { mutableStateOf(Offset.Zero) }

  val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
    scaleFactor = (scaleFactor * zoomChange).coerceIn(0.5f, 4.0f)
    panOffset += offsetChange
  }

  val requestReset = {
    scaleFactor = 1.0f
    panOffset = Offset.Zero
  }
  // Expose the live zoom factor (state read) and a reset action to the surrounding toolbar.
  onTransformState?.invoke(scaleFactor, requestReset)

  val baseFrameColorHex = design.customColorHex ?: design.frameFinish.colorHex
  val frameColor = Color(baseFrameColorHex)
  val sashColor = design.sashFinish.let { Color(it.colorHex) }
  val handleColor = Color(design.handleColor.colorHex)

  val isDarkFrame = design.frameFinish.name.contains("ANTHRACITE") ||
      design.frameFinish.name.contains("BLACK") ||
      design.frameFinish.name.contains("BROWN") ||
      (design.customColorHex != null && design.customColorHex < 0xFF888888)

  val frameBorderColor = if (isDarkFrame) Color(0xFF1E293B) else Color(0xFF475569)
  val beadBorderColor = if (isDarkFrame) Color(0xFF334155) else Color(0xFF94A3B8)
  val outerGlassColor = Color(design.outerGlassTint.colorHex).copy(alpha = 0.28f)
  // Aluminum is drawn with sharper, cooler technical lines; PVC with warmer weld-ready lines.
  val isAluminum = design.materialFamily == com.example.domain.model.MaterialFamily.ALUMINUM
  val frameCentroidColor = if (isAluminum) Color(0xFFB45309) else Color(0xFF0369A1)

  Box(
    modifier = modifier
      .fillMaxWidth()
      .background(
        if (viewMode == CanvasViewMode.REPRESENTATION) Color(0xFF060B14) else Color(0xFFF8FAFC)
      )
  ) {
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer(
          scaleX = scaleFactor,
          scaleY = scaleFactor,
          translationX = panOffset.x,
          translationY = panOffset.y
        )
        .transformable(state = transformState)
        .pointerInput(design, geom, onPlaceHandle, onEditWidth, onEditHeight, onEditPanelWidth, onEditTransomHeight, activeTool) {
          detectTapGestures(
            onDoubleTap = {
              scaleFactor = 1.0f
              panOffset = Offset.Zero
            },
            onTap = { tapOffset ->
              val canvasW = size.width.toFloat()
              val canvasH = size.height.toFloat()
              val marginX = 75f
              val marginY = 75f
              val drawW = canvasW - (marginX * 2)
              val drawH = canvasH - (marginY * 2)
              if (drawW <= 0 || drawH <= 0) return@detectTapGestures

              val fitScale = minOf(drawW / geom.outerFrameWidthMm, drawH / geom.outerFrameHeightMm)
              val originX = marginX + (drawW - (geom.outerFrameWidthMm * fitScale)) / 2f
              val originY = marginY + (drawH - (geom.outerFrameHeightMm * fitScale)) / 2f
              val frameW = geom.outerFrameWidthMm * fitScale
              val frameH = geom.outerFrameHeightMm * fitScale
              val offsetDist = 42f

              // 1. Check if user tapped Top overall width dimension line
              val topDimY = originY - offsetDist
              if (showDimensions && onEditWidth != null &&
                tapOffset.x in (originX - 25f)..(originX + frameW + 25f) &&
                tapOffset.y in (topDimY - 30f)..(topDimY + 30f)
              ) {
                onEditWidth.invoke()
                return@detectTapGestures
              }

              // 2. Check if user tapped Left overall height dimension line
              val leftDimX = originX - offsetDist
              if (showDimensions && onEditHeight != null &&
                tapOffset.x in (leftDimX - 35f)..(leftDimX + 35f) &&
                tapOffset.y in (originY - 25f)..(originY + frameH + 25f)
              ) {
                onEditHeight.invoke()
                return@detectTapGestures
              }

              // 3. Check if user tapped Transom
              if (geom.transomPanel != null) {
                val tY = originY + (geom.transomPanel.yMm * fitScale)
                val tH = geom.transomPanel.heightMm * fitScale
                if (tapOffset.x in originX..(originX + frameW) && tapOffset.y in tY..(tY + tH)) {
                  onEditTransomHeight?.invoke()
                  return@detectTapGestures
                }
              }

              // 4. Check divider drag / tap
              geom.dividers.forEachIndexed { dIdx, d ->
                val dX = originX + (d.xMm * fitScale)
                val dY = originY + (d.yMm * fitScale)
                val dW = d.widthMm * fitScale
                val dH = d.heightMm * fitScale
                if (tapOffset.x in (dX - 20f)..(dX + dW + 20f) && tapOffset.y in dY..(dY + dH)) {
                  onMoveDivider?.invoke(dIdx, (d.xMm + d.widthMm / 2f) / geom.outerFrameWidthMm)
                }
              }

              // 5. Check panels and panel width dimensions
              geom.panels.forEachIndexed { idx, p ->
                val pX = originX + (p.xMm * fitScale)
                val pY = originY + (p.yMm * fitScale)
                val pW = p.widthMm * fitScale
                val pH = p.heightMm * fitScale

                // Bottom panel width tick tap
                val bottomDimY = originY + frameH + 28f
                if (showDimensions && onEditPanelWidth != null &&
                  tapOffset.x in pX..(pX + pW) &&
                  tapOffset.y in (bottomDimY - 25f)..(bottomDimY + 35f)
                ) {
                  onEditPanelWidth.invoke(idx)
                  onSelectPanel?.invoke(idx)
                  return@detectTapGestures
                }

                if (tapOffset.x in pX..(pX + pW) && tapOffset.y in pY..(pY + pH)) {
                  onSelectPanel?.invoke(idx)
                  // Only operables receive a dragged handle; fixed panes ignore the placement tap.
                  val isOperable = design.panels.getOrNull(idx)?.openingType != OpeningType.FIXED &&
                    design.panels.getOrNull(idx)?.openingType != null
                  if (p.isSash && isOperable) onPlaceHandle?.invoke(
                    idx,
                    ((tapOffset.x - pX) / pW).coerceIn(0.02f, 0.98f),
                    ((tapOffset.y - pY) / pH).coerceIn(0.02f, 0.98f)
                  )
                }
              }
            }
          )
        }
    ) {
      val canvasW = size.width
      val canvasH = size.height

      val marginX = 75f
      val marginY = 75f
      val drawW = canvasW - (marginX * 2)
      val drawH = canvasH - (marginY * 2)

      if (drawW <= 0 || drawH <= 0) return@Canvas

      val fitScale = minOf(drawW / geom.outerFrameWidthMm, drawH / geom.outerFrameHeightMm)

      val originX = marginX + (drawW - (geom.outerFrameWidthMm * fitScale)) / 2f
      val originY = marginY + (drawH - (geom.outerFrameHeightMm * fitScale)) / 2f

      val frameW = geom.outerFrameWidthMm * fitScale
      val frameH = geom.outerFrameHeightMm * fitScale
      val frameFace = geom.frameProfileFaceMm * fitScale
      val mullionFace = geom.mullionProfileFaceMm * fitScale
      val beadWidth = 14f * fitScale

      // 1. Technical CAD Grid with sub-grid
      if (showGrid) drawCadGrid(canvasW, canvasH, dark = viewMode == CanvasViewMode.REPRESENTATION, snapStepMm = snapStepMm)

      // 2. Outer Frame Profile by Shape
      when (design.windowShape) {
        WindowShape.RECTANGLE -> {
          // Fill & border
          drawRect(frameColor, Offset(originX, originY), Size(frameW, frameH))
          drawRect(frameBorderColor, Offset(originX, originY), Size(frameW, frameH), style = Stroke(width = 2.5f))

          // 45° mitre corner lines. Aluminum is mechanically jointed, so each corner also gets
          // a small cleat mark that PVC (welded) never shows.
          drawLine(frameBorderColor, Offset(originX, originY), Offset(originX + frameFace, originY + frameFace), 1.5f)
          drawLine(frameBorderColor, Offset(originX + frameW, originY), Offset(originX + frameW - frameFace, originY + frameFace), 1.5f)
          drawLine(frameBorderColor, Offset(originX, originY + frameH), Offset(originX + frameFace, originY + frameH - frameFace), 1.5f)
          drawLine(frameBorderColor, Offset(originX + frameW, originY + frameH), Offset(originX + frameW - frameFace, originY + frameH - frameFace), 1.5f)
          if (isAluminum) {
            listOf(
              Offset(originX + frameFace * 0.3f, originY + frameFace * 0.3f) to Offset(originX + frameFace, originY + frameFace),
              Offset(originX + frameW - frameFace * 0.3f, originY + frameFace * 0.3f) to Offset(originX + frameW - frameFace, originY + frameFace),
              Offset(originX + frameFace * 0.3f, originY + frameH - frameFace * 0.3f) to Offset(originX + frameFace, originY + frameH - frameFace),
              Offset(originX + frameW - frameFace * 0.3f, originY + frameH - frameFace * 0.3f) to Offset(originX + frameW - frameFace, originY + frameH - frameFace)
            ).forEach { (a, b) ->
              drawLine(frameCentroidColor, a, b, 1f)
              drawCircle(frameCentroidColor, 2.2f, (a + b) / 2f, style = Stroke(1f))
            }
          }

          // Inner frame perimeter line
          drawRect(frameBorderColor, Offset(originX + frameFace, originY + frameFace), Size(frameW - frameFace * 2, frameH - frameFace * 2), style = Stroke(width = 1.5f))
        }
        WindowShape.ARCHED -> {
          val archHPx = (geom.archHeightMm * fitScale).coerceIn(40f, frameH * 0.45f)
          drawArchedFrame(originX, originY, frameW, frameH, archHPx, frameFace, frameColor, frameBorderColor)
        }
        WindowShape.TRIANGLE -> {
          drawTriangleFrame(originX, originY, frameW, frameH, frameFace, frameColor, frameBorderColor)
        }
        WindowShape.TRAPEZOIDAL -> {
          val slopeHPx = (geom.archHeightMm * fitScale).coerceIn(40f, frameH * 0.4f)
          drawTrapezoidFrame(originX, originY, frameW, frameH, slopeHPx, frameFace, frameColor, frameBorderColor)
        }
      }

      // 3. Transom if enabled
      if (geom.transomPanel != null) {
        val tY = originY + (geom.transomPanel.yMm * fitScale)
        val tW = geom.transomPanel.widthMm * fitScale
        val tH = geom.transomPanel.heightMm * fitScale

        // Transom Glass
        drawRect(outerGlassColor, Offset(originX + frameFace, tY), Size(tW, tH))
        drawRect(frameBorderColor, Offset(originX + frameFace, tY), Size(tW, tH), style = Stroke(width = 1.5f))
        // Glazing bead
        drawRect(beadBorderColor, Offset(originX + frameFace + beadWidth, tY + beadWidth), Size(tW - beadWidth * 2, tH - beadWidth * 2), style = Stroke(width = 0.8f))

        // Decorative Georgian bars in transom
        if (design.hasGrids) {
          drawDecorativeGrids(originX + frameFace + beadWidth, tY + beadWidth, tW - beadWidth * 2, tH - beadWidth * 2, 1, 3)
        }

        // Transom horizontal mullion
        drawRect(frameColor, Offset(originX + frameFace, tY + tH), Size(tW, mullionFace))
        drawRect(frameBorderColor, Offset(originX + frameFace, tY + tH), Size(tW, mullionFace), style = Stroke(width = 1.5f))
      }

      // 4. Panels & Sashes
      geom.panels.forEachIndexed { idx, p ->
        val pX = originX + (p.xMm * fitScale)
        val pY = originY + (p.yMm * fitScale)
        val pW = p.widthMm * fitScale
        val pH = p.heightMm * fitScale
        val panelConfig = design.panels.getOrNull(idx)
        val opening = panelConfig?.openingType ?: OpeningType.FIXED
        val isSelected = selectedPanelIndex == idx

        // Glass pane background (heat view colors each pane by its glazing U-value class)
        val panelGlassColor = if (viewMode == CanvasViewMode.GLASS_HEAT) {
          val heat = 0.14f + (panelConfig?.glazingType?.thicknessMm ?: 20) / 160f
          Color(panelConfig?.glassTint?.colorHex ?: design.outerGlassTint.colorHex)
            .copy(alpha = heat.coerceIn(0.12f, 0.62f))
        } else Color(panelConfig?.glassTint?.colorHex ?: design.outerGlassTint.colorHex).copy(alpha = 0.28f)
        drawRect(panelGlassColor, Offset(pX, pY), Size(pW, pH))
        drawRect(frameBorderColor, Offset(pX, pY), Size(pW, pH), style = Stroke(width = 1.5f))

        if (p.isSash) {
          val panelSashColor = panelConfig?.sashFinish?.let { Color(it.colorHex) } ?: sashColor
          val sashFace = minOf(65f, p.widthMm / 4, p.heightMm / 4) * fitScale

          drawRect(panelSashColor, Offset(pX, pY), Size(pW, pH), style = Stroke(width = sashFace))
          drawRect(frameBorderColor, Offset(pX, pY), Size(pW, pH), style = Stroke(width = 2f))
          drawRect(frameBorderColor, Offset(pX + sashFace, pY + sashFace), Size(pW - (sashFace * 2), pH - (sashFace * 2)), style = Stroke(width = 1.5f))

          // Glazing bead inside sash
          val innerX = pX + sashFace + beadWidth
          val innerY = pY + sashFace + beadWidth
          val innerW = pW - (sashFace + beadWidth) * 2
          val innerH = pH - (sashFace + beadWidth) * 2
          if (innerW > 10 && innerH > 10) {
            drawRect(beadBorderColor, Offset(innerX, innerY), Size(innerW, innerH), style = Stroke(width = 0.8f))
          }

          // Decorative Georgian Muntin Grids
          if (design.hasGrids && innerW > 40 && innerH > 40) {
            drawDecorativeGrids(innerX, innerY, innerW, innerH, design.gridRows, design.gridCols)
          }

          // Mitre joints on sash corners (45°)
          drawLine(frameBorderColor, Offset(pX, pY), Offset(pX + sashFace, pY + sashFace), 1.2f)
          drawLine(frameBorderColor, Offset(pX + pW, pY), Offset(pX + pW - sashFace, pY + sashFace), 1.2f)
          drawLine(frameBorderColor, Offset(pX, pY + pH), Offset(pX + sashFace, pY + pH - sashFace), 1.2f)
          drawLine(frameBorderColor, Offset(pX + pW, pY + pH), Offset(pX + pW - sashFace, pY + pH - sashFace), 1.2f)

          // ISO Opening direction symbols
          drawOpeningSymbol(opening, pX, pY, pW, pH, panelConfig?.openingDirection ?: design.openingDirection)

          // Hardware Handle
          if (panelConfig?.hasHandle != false) {
            val defaultX = when (opening) {
              OpeningType.CASEMENT_LEFT -> 1f - sashFace / (2f * pW)
              OpeningType.TOP_HUNG, OpeningType.TILT -> 0.5f
              else -> sashFace / (2f * pW)
            }
            val hx = pX + pW * (panelConfig?.handleX ?: defaultX)
            val hy = pY + pH * (panelConfig?.handleY ?: 0.5f)

            // Realistic Door/Window Lever Handle
            drawHandleGraphic(hx, hy, handleColor)
          }
        } else {
          // Fixed pane
          val fbX = pX + beadWidth
          val fbY = pY + beadWidth
          val fbW = pW - beadWidth * 2
          val fbH = pH - beadWidth * 2
          if (fbW > 10 && fbH > 10) {
            drawRect(beadBorderColor, Offset(fbX, fbY), Size(fbW, fbH), style = Stroke(width = 0.8f))
            if (design.hasGrids && fbW > 40 && fbH > 40) {
              drawDecorativeGrids(fbX, fbY, fbW, fbH, design.gridRows, design.gridCols)
            }
          }
          drawFixedSymbol(pX, pY, pW, pH)
        }

        // Selection Highlight Border
        if (isSelected) {
          drawRect(
            color = Color(0xFF0284C7),
            topLeft = Offset(pX - 3f, pY - 3f),
            size = Size(pW + 6f, pH + 6f),
            style = Stroke(width = 3.5f)
          )
          // Corner grips hint that the panel is editable / resizable
          listOf(
            Offset(pX, pY), Offset(pX + pW, pY), Offset(pX, pY + pH), Offset(pX + pW, pY + pH)
          ).forEach { c ->
            drawRect(Color.White, Offset(c.x - 3.5f, c.y - 3.5f), Size(7f, 7f))
            drawRect(Color(0xFF0284C7), Offset(c.x - 3.5f, c.y - 3.5f), Size(7f, 7f), style = Stroke(1.6f))
          }
        }
      }

      // 5. Vertical and Horizontal Dividers (Mullions & Transoms)
      geom.dividers.forEach { d ->
        val topLeft = Offset(originX + d.xMm * fitScale, originY + d.yMm * fitScale)
        val extent = Size(d.widthMm * fitScale, d.heightMm * fitScale)
        drawRect(frameColor, topLeft, extent)
        drawRect(frameBorderColor, topLeft, extent, style = Stroke(1.5f))

        // Center line on divider
        if (d.widthMm > d.heightMm) {
          val midY = topLeft.y + extent.height / 2f
          drawLine(beadBorderColor, Offset(topLeft.x, midY), Offset(topLeft.x + extent.width, midY), 0.8f)
        } else {
          val midX = topLeft.x + extent.width / 2f
          drawLine(beadBorderColor, Offset(midX, topLeft.y), Offset(midX, topLeft.y + extent.height), 0.8f)
        }
      }

      // 6. Multi-Axis CAD Dimensions (Top Overall, Left Overall, Right Transom Breakdown, Bottom Panel Widths)
      if (showDimensions) {
        drawCadDimensions(
          originX = originX,
          originY = originY,
          widthPx = frameW,
          heightPx = frameH,
          design = design,
          geom = geom,
          fitScale = fitScale,
          lang = appLang
        )
      }
    }

    // Zoom & Reset Controls Overlay (Matching Mockup bottom-left)
    Row(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      FilledTonalIconButton(
        onClick = { scaleFactor = (scaleFactor * 1.25f).coerceAtMost(4.0f) },
        modifier = Modifier.size(38.dp)
      ) {
        Icon(Icons.Default.ZoomIn, contentDescription = Localization.get("zoom_in", appLang))
      }
      FilledTonalIconButton(
        onClick = { scaleFactor = (scaleFactor / 1.25f).coerceAtLeast(0.5f) },
        modifier = Modifier.size(38.dp)
      ) {
        Icon(Icons.Default.ZoomOut, contentDescription = Localization.get("zoom_out", appLang))
      }
      FilledTonalIconButton(
        onClick = {
          scaleFactor = 1.0f
          panOffset = Offset.Zero
        },
        modifier = Modifier.size(38.dp)
      ) {
        Icon(Icons.Default.FitScreen, contentDescription = Localization.get("reset_zoom", appLang))
      }
    }
  }
}

private fun DrawScope.drawCadGrid(w: Float, h: Float, dark: Boolean = false, snapStepMm: Int = 10) {
  val majorGridColor = if (dark) Color(0xFF1E3A5F) else Color(0xFFCBD5E1)
  val minorGridColor = if (dark) Color(0xFF122237) else Color(0xFFF1F5F9)
  val minorStep = 20f
  val majorStep = if (snapStepMm in 5..100) (snapStepMm * 5).toFloat() else 100f

  var x = 0f
  while (x < w) {
    val isMajor = (x % majorStep) < minorStep
    drawLine(
      if (isMajor) majorGridColor else minorGridColor,
      Offset(x, 0f),
      Offset(x, h),
      strokeWidth = if (isMajor) 0.8f else 0.5f
    )
    x += minorStep
  }
  var y = 0f
  while (y < h) {
    val isMajor = (y % majorStep) < minorStep
    drawLine(
      if (isMajor) majorGridColor else minorGridColor,
      Offset(0f, y),
      Offset(w, y),
      strokeWidth = if (isMajor) 0.8f else 0.5f
    )
    y += minorStep
  }
}

private fun DrawScope.drawDecorativeGrids(
  x: Float, y: Float, w: Float, h: Float,
  rows: Int, cols: Int
) {
  val gridColor = Color(0xCCFFFFFF)
  val rCount = rows.coerceIn(1, 6)
  val cCount = cols.coerceIn(1, 6)

  // Horizontal muntin bars
  for (i in 1..rCount) {
    val barY = y + (h * (i.toFloat() / (rCount + 1)))
    drawLine(gridColor, Offset(x, barY), Offset(x + w, barY), strokeWidth = 2f)
  }
  // Vertical muntin bars
  for (j in 1..cCount) {
    val barX = x + (w * (j.toFloat() / (cCount + 1)))
    drawLine(gridColor, Offset(barX, y), Offset(barX, y + h), strokeWidth = 2f)
  }
}

private fun DrawScope.drawHandleGraphic(hx: Float, hy: Float, color: Color) {
  // Escutcheon base plate
  drawRoundRect(
    color = Color(0xFF475569),
    topLeft = Offset(hx - 5f, hy - 18f),
    size = Size(10f, 36f),
    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
  )
  drawRoundRect(
    color = color,
    topLeft = Offset(hx - 4f, hy - 17f),
    size = Size(8f, 34f),
    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
  )
  // Lever stem and handle
  drawCircle(Color(0xFF334155), 4.5f, Offset(hx, hy))
  drawRoundRect(
    color = color,
    topLeft = Offset(hx - 3f, hy - 2f),
    size = Size(6f, 26f),
    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
  )
}

private fun DrawScope.drawArchedFrame(
  originX: Float, originY: Float, w: Float, h: Float, archH: Float, face: Float,
  fillColor: Color, borderColor: Color
) {
  val outerPath = Path().apply {
    moveTo(originX, originY + archH)
    quadraticTo(originX + w / 2f, originY - archH * 0.4f, originX + w, originY + archH)
    lineTo(originX + w, originY + h)
    lineTo(originX, originY + h)
    close()
  }
  drawPath(outerPath, fillColor)
  drawPath(outerPath, borderColor, style = Stroke(width = 2.5f))
}

private fun DrawScope.drawTriangleFrame(
  originX: Float, originY: Float, w: Float, h: Float, face: Float,
  fillColor: Color, borderColor: Color
) {
  val outerPath = Path().apply {
    moveTo(originX + w / 2f, originY)
    lineTo(originX + w, originY + h)
    lineTo(originX, originY + h)
    close()
  }
  drawPath(outerPath, fillColor)
  drawPath(outerPath, borderColor, style = Stroke(width = 2.5f))
}

private fun DrawScope.drawTrapezoidFrame(
  originX: Float, originY: Float, w: Float, h: Float, slopeH: Float, face: Float,
  fillColor: Color, borderColor: Color
) {
  val outerPath = Path().apply {
    moveTo(originX, originY + slopeH)
    lineTo(originX + w, originY)
    lineTo(originX + w, originY + h)
    lineTo(originX, originY + h)
    close()
  }
  drawPath(outerPath, fillColor)
  drawPath(outerPath, borderColor, style = Stroke(width = 2.5f))
}

private fun DrawScope.drawOpeningSymbol(
  opening: OpeningType,
  x: Float,
  y: Float,
  w: Float,
  h: Float,
  direction: OpeningDirection
) {
  val isDashed = direction == OpeningDirection.INWARD
  val effect = if (isDashed) PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f) else null
  val symbolColor = Color(0xFF0284C7)

  when (opening) {
    OpeningType.CASEMENT_LEFT -> {
      val path = Path().apply {
        moveTo(x + w, y + (h / 2f))
        lineTo(x, y)
        moveTo(x + w, y + (h / 2f))
        lineTo(x, y + h)
      }
      drawPath(path, symbolColor, style = Stroke(width = 2.5f, pathEffect = effect))
    }
    OpeningType.CASEMENT_RIGHT -> {
      val path = Path().apply {
        moveTo(x, y + (h / 2f))
        lineTo(x + w, y)
        moveTo(x, y + (h / 2f))
        lineTo(x + w, y + h)
      }
      drawPath(path, symbolColor, style = Stroke(width = 2.5f, pathEffect = effect))
    }
    OpeningType.DOUBLE_CASEMENT -> {
      val midX = x + (w / 2f)
      val leftPath = Path().apply {
        moveTo(midX, y + (h / 2f))
        lineTo(x, y)
        moveTo(midX, y + (h / 2f))
        lineTo(x, y + h)
      }
      val rightPath = Path().apply {
        moveTo(midX, y + (h / 2f))
        lineTo(x + w, y)
        moveTo(midX, y + (h / 2f))
        lineTo(x + w, y + h)
      }
      drawPath(leftPath, symbolColor, style = Stroke(width = 2.5f, pathEffect = effect))
      drawPath(rightPath, symbolColor, style = Stroke(width = 2.5f, pathEffect = effect))
      drawLine(symbolColor, Offset(midX, y), Offset(midX, y + h), strokeWidth = 2f)
    }
    OpeningType.TILT_AND_TURN -> {
      val path = Path().apply {
        moveTo(x, y + (h / 2f))
        lineTo(x + w, y)
        moveTo(x, y + (h / 2f))
        lineTo(x + w, y + h)
        moveTo(x + (w / 2f), y)
        lineTo(x, y + h)
        moveTo(x + (w / 2f), y)
        lineTo(x + w, y + h)
      }
      drawPath(path, symbolColor, style = Stroke(width = 2.5f, pathEffect = effect))
    }
    OpeningType.TILT -> {
      val path = Path().apply {
        moveTo(x + (w / 2f), y)
        lineTo(x, y + h)
        moveTo(x + (w / 2f), y)
        lineTo(x + w, y + h)
      }
      drawPath(path, symbolColor, style = Stroke(width = 2.5f, pathEffect = effect))
    }
    OpeningType.TOP_HUNG -> {
      val path = Path().apply {
        moveTo(x + (w / 2f), y + h)
        lineTo(x, y)
        moveTo(x + (w / 2f), y + h)
        lineTo(x + w, y)
      }
      drawPath(path, symbolColor, style = Stroke(width = 2.5f, pathEffect = effect))
    }
    OpeningType.SLIDING_LEFT -> {
      val arrowY = y + (h / 2f)
      drawLine(symbolColor, Offset(x + (w * 0.75f), arrowY), Offset(x + (w * 0.25f), arrowY), strokeWidth = 3f)
      drawLine(symbolColor, Offset(x + (w * 0.25f), arrowY), Offset(x + (w * 0.35f), arrowY - 14f), strokeWidth = 3f)
      drawLine(symbolColor, Offset(x + (w * 0.25f), arrowY), Offset(x + (w * 0.35f), arrowY + 14f), strokeWidth = 3f)
      drawLine(symbolColor.copy(alpha = 0.5f), Offset(x + 10f, y + h - 8f), Offset(x + w - 10f, y + h - 8f), strokeWidth = 2f)
    }
    OpeningType.SLIDING_RIGHT -> {
      val arrowY = y + (h / 2f)
      drawLine(symbolColor, Offset(x + (w * 0.25f), arrowY), Offset(x + (w * 0.75f), arrowY), strokeWidth = 3f)
      drawLine(symbolColor, Offset(x + (w * 0.75f), arrowY), Offset(x + (w * 0.65f), arrowY - 14f), strokeWidth = 3f)
      drawLine(symbolColor, Offset(x + (w * 0.75f), arrowY), Offset(x + (w * 0.65f), arrowY + 14f), strokeWidth = 3f)
      drawLine(symbolColor.copy(alpha = 0.5f), Offset(x + 10f, y + h - 8f), Offset(x + w - 10f, y + h - 8f), strokeWidth = 2f)
    }
    OpeningType.FIXED -> {}
  }
}

private fun DrawScope.drawFixedSymbol(x: Float, y: Float, w: Float, h: Float) {
  val cX = x + (w / 2f)
  val cY = y + (h / 2f)
  val lineColor = Color(0xFF64748B)
  drawLine(lineColor, Offset(cX - 12f, cY), Offset(cX + 12f, cY), strokeWidth = 1.5f)
  drawLine(lineColor, Offset(cX, cY - 12f), Offset(cX, cY + 12f), strokeWidth = 1.5f)
}

private fun DrawScope.drawCadDimensions(
  originX: Float,
  originY: Float,
  widthPx: Float,
  heightPx: Float,
  design: WindowDesign,
  geom: com.example.domain.engine.DesignGeometry,
  fitScale: Float,
  lang: AppLanguage
) {
  val dimColor = Color(0xFF0284C7)
  val textPaint = android.graphics.Paint().apply {
    color = android.graphics.Color.parseColor("#0F172A")
    textSize = 28f
    isAntiAlias = true
    textAlign = android.graphics.Paint.Align.CENTER
    typeface = android.graphics.Typeface.DEFAULT_BOLD
  }
  val smallPaint = android.graphics.Paint().apply {
    color = android.graphics.Color.parseColor("#334155")
    textSize = 22f
    isAntiAlias = true
    textAlign = android.graphics.Paint.Align.CENTER
    typeface = android.graphics.Typeface.DEFAULT
  }

  // 1. TOP DIMENSION LINE (Overall Width) - Matching Reference Image
  val topDimY = originY - 32f
  drawLine(Color(0xFF94A3B8), Offset(originX, originY - 6f), Offset(originX, topDimY - 10f), 1f)
  drawLine(Color(0xFF94A3B8), Offset(originX + widthPx, originY - 6f), Offset(originX + widthPx, topDimY - 10f), 1f)
  drawLine(dimColor, Offset(originX, topDimY), Offset(originX + widthPx, topDimY), 2f)
  // Arrowheads
  drawArrowHead(originX, topDimY, isRight = false, dimColor)
  drawArrowHead(originX + widthPx, topDimY, isRight = true, dimColor)
  // Text
  drawContext.canvas.nativeCanvas.drawText(
    "${Localization.formatDigits(design.widthMm, lang)} mm",
    originX + widthPx / 2f,
    topDimY - 10f,
    textPaint
  )

  // 2. LEFT DIMENSION LINE (Overall Height) - Matching Reference Image
  val leftDimX = originX - 32f
  drawLine(Color(0xFF94A3B8), Offset(originX - 6f, originY), Offset(leftDimX - 10f, originY), 1f)
  drawLine(Color(0xFF94A3B8), Offset(originX - 6f, originY + heightPx), Offset(leftDimX - 10f, originY + heightPx), 1f)
  drawLine(dimColor, Offset(leftDimX, originY), Offset(leftDimX, originY + heightPx), 2f)
  drawArrowHeadVertical(leftDimX, originY, isDown = false, dimColor)
  drawArrowHeadVertical(leftDimX, originY + heightPx, isDown = true, dimColor)
  drawContext.canvas.nativeCanvas.save()
  drawContext.canvas.nativeCanvas.rotate(-90f, leftDimX - 12f, originY + heightPx / 2f)
  drawContext.canvas.nativeCanvas.drawText(
    "${Localization.formatDigits(design.heightMm, lang)} mm",
    leftDimX - 12f,
    originY + heightPx / 2f,
    textPaint
  )
  drawContext.canvas.nativeCanvas.restore()

  // 3. RIGHT DIMENSION BREAKDOWN (e.g. Transom vs Main Height)
  if (design.hasTransom && geom.transomPanel != null) {
    val rightDimX = originX + widthPx + 32f
    val tHPx = geom.transomPanel.heightMm * fitScale
    val mainHPx = heightPx - tHPx
    // Transom height
    drawLine(Color(0xFF94A3B8), Offset(originX + widthPx + 6f, originY), Offset(rightDimX + 10f, originY), 1f)
    drawLine(Color(0xFF94A3B8), Offset(originX + widthPx + 6f, originY + tHPx), Offset(rightDimX + 10f, originY + tHPx), 1f)
    drawLine(dimColor, Offset(rightDimX, originY), Offset(rightDimX, originY + tHPx), 1.5f)
    drawArrowHeadVertical(rightDimX, originY, isDown = false, dimColor)
    drawArrowHeadVertical(rightDimX, originY + tHPx, isDown = true, dimColor)
    drawContext.canvas.nativeCanvas.save()
    drawContext.canvas.nativeCanvas.rotate(90f, rightDimX + 16f, originY + tHPx / 2f)
    drawContext.canvas.nativeCanvas.drawText(
      "${Localization.formatDigits(design.transomHeightMm, lang)} mm",
      rightDimX + 16f,
      originY + tHPx / 2f,
      smallPaint
    )
    drawContext.canvas.nativeCanvas.restore()

    // Main section height
    drawLine(Color(0xFF94A3B8), Offset(originX + widthPx + 6f, originY + heightPx), Offset(rightDimX + 10f, originY + heightPx), 1f)
    drawLine(dimColor, Offset(rightDimX, originY + tHPx), Offset(rightDimX, originY + heightPx), 1.5f)
    drawArrowHeadVertical(rightDimX, originY + tHPx, isDown = false, dimColor)
    drawArrowHeadVertical(rightDimX, originY + heightPx, isDown = true, dimColor)
    drawContext.canvas.nativeCanvas.save()
    drawContext.canvas.nativeCanvas.rotate(90f, rightDimX + 16f, originY + tHPx + mainHPx / 2f)
    drawContext.canvas.nativeCanvas.drawText(
      "${Localization.formatDigits(design.heightMm - design.transomHeightMm, lang)} mm",
      rightDimX + 16f,
      originY + tHPx + mainHPx / 2f,
      smallPaint
    )
    drawContext.canvas.nativeCanvas.restore()
  }

  // 4. BOTTOM DIMENSION TICKS (Individual panel widths)
  val bottomDimY = originY + heightPx + 24f
  geom.panels.forEach { p ->
    val pX = originX + (p.xMm * fitScale)
    val pW = p.widthMm * fitScale
    drawLine(Color(0xFF94A3B8), Offset(pX, originY + heightPx + 4f), Offset(pX, bottomDimY + 12f), 1f)
    drawLine(Color(0xFF94A3B8), Offset(pX + pW, originY + heightPx + 4f), Offset(pX + pW, bottomDimY + 12f), 1f)
    drawLine(dimColor, Offset(pX, bottomDimY), Offset(pX + pW, bottomDimY), 1.5f)
    drawArrowHead(pX, bottomDimY, isRight = false, dimColor)
    drawArrowHead(pX + pW, bottomDimY, isRight = true, dimColor)

    drawContext.canvas.nativeCanvas.drawText(
      "${Localization.formatDigits(p.widthMm.toInt(), lang)} mm",
      pX + pW / 2f,
      bottomDimY + 22f,
      smallPaint
    )
  }
}

private fun DrawScope.drawArrowHead(x: Float, y: Float, isRight: Boolean, color: Color) {
  val size = 6f
  val dir = if (isRight) -1f else 1f
  val path = Path().apply {
    moveTo(x, y)
    lineTo(x + dir * size * 1.5f, y - size)
    lineTo(x + dir * size * 1.5f, y + size)
    close()
  }
  drawPath(path, color)
}

private fun DrawScope.drawArrowHeadVertical(x: Float, y: Float, isDown: Boolean, color: Color) {
  val size = 6f
  val dir = if (isDown) -1f else 1f
  val path = Path().apply {
    moveTo(x, y)
    lineTo(x - size, y + dir * size * 1.5f)
    lineTo(x + size, y + dir * size * 1.5f)
    close()
  }
  drawPath(path, color)
}
