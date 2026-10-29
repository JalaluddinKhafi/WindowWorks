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
import com.example.core.CurrencyHelper
import com.example.domain.engine.GeometryEngine
import com.example.domain.engine.PanelBounds
import com.example.domain.model.OpeningType
import com.example.domain.model.WindowDesign

@Composable
fun Elevation2DCanvas(
  design: WindowDesign,
  selectedPanelIndex: Int? = null,
  onSelectPanel: ((Int) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val geom = GeometryEngine.computeGeometry(design)

  // Zoom & Pan state
  var scaleFactor by remember { mutableFloatStateOf(1.0f) }
  var panOffset by remember { mutableStateOf(Offset.Zero) }

  val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
    scaleFactor = (scaleFactor * zoomChange).coerceIn(0.5f, 3.0f)
    panOffset += offsetChange
  }

  val frameColor = Color(design.frameFinish.colorHex)
  val sashColor = Color(design.sashFinish.colorHex)

  val isDarkFrame = design.frameFinish.name.contains("ANTHRACITE") ||
      design.frameFinish.name.contains("BLACK") ||
      design.frameFinish.name.contains("BROWN")

  val frameBorderColor = if (isDarkFrame) Color(0xFF1E293B) else Color(0xFF64748B)

  // Outer & Inner glass tints
  val outerGlassColor = Color(design.outerGlassTint.colorHex).copy(alpha = 0.28f)

  Box(
    modifier = modifier
      .fillMaxWidth()
      .background(Color(0xFFF1F5F9))
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
        .pointerInput(design, geom) {
          detectTapGestures { tapOffset ->
            if (onSelectPanel == null) return@detectTapGestures

            val canvasW = size.width.toFloat()
            val canvasH = size.height.toFloat()
            val marginX = 50f
            val marginY = 50f
            val drawW = canvasW - (marginX * 2)
            val drawH = canvasH - (marginY * 2)
            if (drawW <= 0 || drawH <= 0) return@detectTapGestures

            val fitScale = minOf(drawW / geom.outerFrameWidthMm, drawH / geom.outerFrameHeightMm)
            val originX = marginX + (drawW - (geom.outerFrameWidthMm * fitScale)) / 2f
            val originY = marginY + (drawH - (geom.outerFrameHeightMm * fitScale)) / 2f

            geom.panels.forEachIndexed { idx, p ->
              val pX = originX + (p.xMm * fitScale)
              val pY = originY + (p.yMm * fitScale)
              val pW = p.widthMm * fitScale
              val pH = p.heightMm * fitScale

              if (tapOffset.x in pX..(pX + pW) && tapOffset.y in pY..(pY + pH)) {
                onSelectPanel(idx)
              }
            }
          }
        }
    ) {
      val canvasW = size.width
      val canvasH = size.height

      val marginX = 50f
      val marginY = 50f
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

      // 1. Grid
      drawGrid(canvasW, canvasH)

      // 2. Outer Frame Profile
      drawRect(
        color = frameColor,
        topLeft = Offset(originX, originY),
        size = Size(frameW, frameH)
      )
      drawRect(
        color = frameBorderColor,
        topLeft = Offset(originX, originY),
        size = Size(frameW, frameH),
        style = Stroke(width = 2.5f)
      )

      // Outer Mitre lines
      drawLine(frameBorderColor, Offset(originX, originY), Offset(originX + frameFace, originY + frameFace), 1.5f)
      drawLine(frameBorderColor, Offset(originX + frameW, originY), Offset(originX + frameW - frameFace, originY + frameFace), 1.5f)
      drawLine(frameBorderColor, Offset(originX, originY + frameH), Offset(originX + frameFace, originY + frameH - frameFace), 1.5f)
      drawLine(frameBorderColor, Offset(originX + frameW, originY + frameH), Offset(originX + frameW - frameFace, originY + frameH - frameFace), 1.5f)

      // 3. Transom if enabled
      if (geom.transomPanel != null) {
        val tY = originY + (geom.transomPanel.yMm * fitScale)
        val tW = geom.transomPanel.widthMm * fitScale
        val tH = geom.transomPanel.heightMm * fitScale

        // Transom Glass
        drawRect(outerGlassColor, Offset(originX + frameFace, tY), Size(tW, tH))
        drawRect(frameBorderColor, Offset(originX + frameFace, tY), Size(tW, tH), style = Stroke(width = 1.5f))

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

        // Glass pane background
        val panelGlassColor = Color(panelConfig?.glassTint?.colorHex ?: design.outerGlassTint.colorHex).copy(alpha = 0.28f)
        drawRect(panelGlassColor, Offset(pX, pY), Size(pW, pH))
        drawRect(frameBorderColor, Offset(pX, pY), Size(pW, pH), style = Stroke(width = 1.5f))

        if (p.isSash) {
          // Sash with distinct sashFinish color!
          val panelSashColor = panelConfig?.sashFinish?.let { Color(it.colorHex) } ?: sashColor
          val sashFace = 65f * fitScale

          drawRect(panelSashColor, Offset(pX, pY), Size(pW, pH), style = Stroke(width = sashFace))
          drawRect(frameBorderColor, Offset(pX, pY), Size(pW, pH), style = Stroke(width = 2f))
          drawRect(frameBorderColor, Offset(pX + sashFace, pY + sashFace), Size(pW - (sashFace * 2), pH - (sashFace * 2)), style = Stroke(width = 1.5f))

          // Mitre joints on sash
          drawLine(frameBorderColor, Offset(pX, pY), Offset(pX + sashFace, pY + sashFace), 1.2f)
          drawLine(frameBorderColor, Offset(pX + pW, pY), Offset(pX + pW - sashFace, pY + sashFace), 1.2f)
          drawLine(frameBorderColor, Offset(pX, pY + pH), Offset(pX + sashFace, pY + pH - sashFace), 1.2f)
          drawLine(frameBorderColor, Offset(pX + pW, pY + pH), Offset(pX + pW - sashFace, pY + pH - sashFace), 1.2f)

          // ISO Opening direction symbols
          drawOpeningSymbol(opening, pX, pY, pW, pH)

          // Handle
          drawHandle(opening, pX, pY, pW, pH, sashFace)
        } else {
          drawFixedSymbol(pX, pY, pW, pH)
        }

        // Selection Highlight Border
        if (isSelected) {
          drawRect(
            color = Color(0xFFD97706), // Vibrant Amber
            topLeft = Offset(pX - 2f, pY - 2f),
            size = Size(pW + 4f, pH + 4f),
            style = Stroke(width = 3.5f)
          )
        }

        // Mullion divider between panels
        if (idx < geom.panels.size - 1) {
          val mX = pX + pW
          drawRect(frameColor, Offset(mX, pY), Size(mullionFace, pH))
          drawRect(frameBorderColor, Offset(mX, pY), Size(mullionFace, pH), style = Stroke(width = 1.5f))
        }

        // Individual panel width dimension
        drawPanelDimension(pX, pY + pH, pW, p.widthMm.toInt())
      }

      // 5. Overall Width & Height Dimensions
      drawOverallDimensions(
        originX = originX,
        originY = originY,
        widthPx = frameW,
        heightPx = frameH,
        widthMm = design.widthMm,
        heightMm = design.heightMm
      )
    }

    // Zoom & Reset Controls Overlay
    Row(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(10.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      FilledTonalIconButton(
        onClick = { scaleFactor = (scaleFactor * 1.2f).coerceAtMost(3.0f) },
        modifier = Modifier.size(36.dp)
      ) {
        Icon(Icons.Default.ZoomIn, contentDescription = "بزرگ‌نمایی")
      }
      FilledTonalIconButton(
        onClick = { scaleFactor = (scaleFactor / 1.2f).coerceAtLeast(0.5f) },
        modifier = Modifier.size(36.dp)
      ) {
        Icon(Icons.Default.ZoomOut, contentDescription = "کوچک‌نمایی")
      }
      FilledTonalIconButton(
        onClick = {
          scaleFactor = 1.0f
          panOffset = Offset.Zero
        },
        modifier = Modifier.size(36.dp)
      ) {
        Icon(Icons.Default.FitScreen, contentDescription = "اندازه پیش‌فرض")
      }
    }

    // Tap instruction hint
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = Color(0xCC1E293B),
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(10.dp)
    ) {
      Text(
        text = "برای انتخاب و ویرایش هر لنگه روی آن ضربه بزنید",
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
      )
    }
  }
}

private fun DrawScope.drawGrid(w: Float, h: Float) {
  val gridColor = Color(0xFFE2E8F0)
  val step = 35f
  var x = 0f
  while (x < w) {
    drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 0.8f)
    x += step
  }
  var y = 0f
  while (y < h) {
    drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 0.8f)
    y += step
  }
}

private fun DrawScope.drawOpeningSymbol(
  opening: OpeningType,
  x: Float,
  y: Float,
  w: Float,
  h: Float
) {
  val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
  val symbolColor = Color(0xFFD97706)

  when (opening) {
    OpeningType.CASEMENT_LEFT -> {
      val path = Path().apply {
        moveTo(x + w, y + (h / 2f))
        lineTo(x, y)
        moveTo(x + w, y + (h / 2f))
        lineTo(x, y + h)
      }
      drawPath(path, symbolColor, style = Stroke(width = 2.5f, pathEffect = dashEffect))
    }
    OpeningType.CASEMENT_RIGHT -> {
      val path = Path().apply {
        moveTo(x, y + (h / 2f))
        lineTo(x + w, y)
        moveTo(x, y + (h / 2f))
        lineTo(x + w, y + h)
      }
      drawPath(path, symbolColor, style = Stroke(width = 2.5f, pathEffect = dashEffect))
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
      drawPath(path, symbolColor, style = Stroke(width = 2.5f, pathEffect = dashEffect))
    }
    OpeningType.SLIDING_LEFT -> {
      val arrowY = y + (h / 2f)
      drawLine(symbolColor, Offset(x + (w * 0.75f), arrowY), Offset(x + (w * 0.25f), arrowY), strokeWidth = 3f)
      drawLine(symbolColor, Offset(x + (w * 0.25f), arrowY), Offset(x + (w * 0.35f), arrowY - 14f), strokeWidth = 3f)
      drawLine(symbolColor, Offset(x + (w * 0.25f), arrowY), Offset(x + (w * 0.35f), arrowY + 14f), strokeWidth = 3f)
    }
    OpeningType.SLIDING_RIGHT -> {
      val arrowY = y + (h / 2f)
      drawLine(symbolColor, Offset(x + (w * 0.25f), arrowY), Offset(x + (w * 0.75f), arrowY), strokeWidth = 3f)
      drawLine(symbolColor, Offset(x + (w * 0.75f), arrowY), Offset(x + (w * 0.65f), arrowY - 14f), strokeWidth = 3f)
      drawLine(symbolColor, Offset(x + (w * 0.75f), arrowY), Offset(x + (w * 0.65f), arrowY + 14f), strokeWidth = 3f)
    }
    OpeningType.TOP_HUNG -> {
      val path = Path().apply {
        moveTo(x + (w / 2f), y + h)
        lineTo(x, y)
        moveTo(x + (w / 2f), y + h)
        lineTo(x + w, y)
      }
      drawPath(path, symbolColor, style = Stroke(width = 2.5f, pathEffect = dashEffect))
    }
    OpeningType.FIXED -> {}
  }
}

private fun DrawScope.drawHandle(
  opening: OpeningType,
  x: Float,
  y: Float,
  w: Float,
  h: Float,
  sashFace: Float
) {
  val handleColor = Color(0xFF0F172A)
  val midY = y + (h / 2f)
  val handleX = when (opening) {
    OpeningType.CASEMENT_LEFT -> x + w - (sashFace / 2f)
    OpeningType.CASEMENT_RIGHT, OpeningType.TILT_AND_TURN -> x + (sashFace / 2f)
    else -> x + (w / 2f)
  }

  drawCircle(color = handleColor, radius = 6f, center = Offset(handleX, midY))
  drawLine(color = handleColor, start = Offset(handleX, midY), end = Offset(handleX, midY + 28f), strokeWidth = 4.5f)
}

private fun DrawScope.drawFixedSymbol(x: Float, y: Float, w: Float, h: Float) {
  val cX = x + (w / 2f)
  val cY = y + (h / 2f)
  val lineColor = Color(0xFF64748B)
  drawLine(lineColor, Offset(cX - 10f, cY), Offset(cX + 10f, cY), strokeWidth = 1.5f)
  drawLine(lineColor, Offset(cX, cY - 10f), Offset(cX, cY + 10f), strokeWidth = 1.5f)
}

private fun DrawScope.drawPanelDimension(x: Float, y: Float, w: Float, widthMm: Int) {
  val paint = android.graphics.Paint().apply {
    color = android.graphics.Color.parseColor("#475569")
    textSize = 22f
    isAntiAlias = true
    textAlign = android.graphics.Paint.Align.CENTER
    typeface = android.graphics.Typeface.DEFAULT
  }

  // Small tick under each panel
  drawLine(Color(0xFF94A3B8), Offset(x, y + 2f), Offset(x, y + 10f), 1f)
  drawLine(Color(0xFF94A3B8), Offset(x + w, y + 2f), Offset(x + w, y + 10f), 1f)
  drawLine(Color(0xFF94A3B8), Offset(x, y + 6f), Offset(x + w, y + 6f), 1f)

  drawContext.canvas.nativeCanvas.drawText(
    "${CurrencyHelper.toPersianDigits(widthMm)}mm",
    x + (w / 2f),
    y + 24f,
    paint
  )
}

private fun DrawScope.drawOverallDimensions(
  originX: Float,
  originY: Float,
  widthPx: Float,
  heightPx: Float,
  widthMm: Int,
  heightMm: Int
) {
  val dimColor = Color(0xFF1E3A8A)
  val offsetDist = 38f

  // Bottom Dimension Line
  val dimY = originY + heightPx + offsetDist
  drawLine(Color(0xFF94A3B8), Offset(originX, originY + heightPx + 10f), Offset(originX, dimY + 6f), 1f)
  drawLine(Color(0xFF94A3B8), Offset(originX + widthPx, originY + heightPx + 10f), Offset(originX + widthPx, dimY + 6f), 1f)
  drawLine(dimColor, Offset(originX, dimY), Offset(originX + widthPx, dimY), 2f)
  drawLine(dimColor, Offset(originX - 5f, dimY + 5f), Offset(originX + 5f, dimY - 5f), 2.5f)
  drawLine(dimColor, Offset(originX + widthPx - 5f, dimY + 5f), Offset(originX + widthPx + 5f, dimY - 5f), 2.5f)

  // Right Dimension Line
  val dimX = originX + widthPx + offsetDist
  drawLine(Color(0xFF94A3B8), Offset(originX + widthPx + 10f, originY), Offset(dimX + 6f, originY), 1f)
  drawLine(Color(0xFF94A3B8), Offset(originX + widthPx + 10f, originY + heightPx), Offset(dimX + 6f, originY + heightPx), 1f)
  drawLine(dimColor, Offset(dimX, originY), Offset(dimX, originY + heightPx), 2f)
  drawLine(dimColor, Offset(dimX - 5f, originY + 5f), Offset(dimX + 5f, originY - 5f), 2.5f)
  drawLine(dimColor, Offset(dimX - 5f, originY + heightPx + 5f), Offset(dimX + 5f, originY + heightPx - 5f), 2.5f)

  val paint = android.graphics.Paint().apply {
    color = android.graphics.Color.parseColor("#1E3A8A")
    textSize = 30f
    isAntiAlias = true
    textAlign = android.graphics.Paint.Align.CENTER
    typeface = android.graphics.Typeface.DEFAULT_BOLD
  }

  val widthText = "${CurrencyHelper.toPersianDigits(widthMm)} mm"
  drawContext.canvas.nativeCanvas.drawText(
    widthText,
    originX + (widthPx / 2f),
    dimY + 30f,
    paint
  )

  val heightText = "${CurrencyHelper.toPersianDigits(heightMm)} mm"
  drawContext.canvas.nativeCanvas.save()
  drawContext.canvas.nativeCanvas.rotate(90f, dimX + 24f, originY + (heightPx / 2f))
  drawContext.canvas.nativeCanvas.drawText(
    heightText,
    dimX + 24f,
    originY + (heightPx / 2f),
    paint
  )
  drawContext.canvas.nativeCanvas.restore()
}
