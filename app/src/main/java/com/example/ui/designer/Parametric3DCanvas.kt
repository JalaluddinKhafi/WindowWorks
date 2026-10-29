package com.example.ui.designer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.core.CurrencyHelper
import com.example.domain.engine.GeometryEngine
import com.example.domain.model.OpeningDirection
import com.example.domain.model.OpeningType
import com.example.domain.model.WindowDesign
import kotlin.math.*

data class Point3D(val x: Float, val y: Float, val z: Float)

data class Polygon3D(
  val vertices: List<Point3D>,
  val baseColor: Color,
  val normalZ: Float, // for depth sorting
  val isGlass: Boolean = false
)

@Composable
fun Parametric3DCanvas(
  design: WindowDesign,
  modifier: Modifier = Modifier
) {
  // Orbit camera rotation angles (degrees)
  var yawDeg by remember { mutableFloatStateOf(-25f) }
  var pitchDeg by remember { mutableFloatStateOf(15f) }
  var zoomScale by remember { mutableFloatStateOf(1.0f) }
  var sashOpenAngleDeg by remember { mutableFloatStateOf(35f) }
  var isPerspective by remember { mutableStateOf(true) }

  val geom = GeometryEngine.computeGeometry(design)

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(Color(0xFF0F172A))
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .pointerInput(Unit) {
          detectDragGestures { change, dragAmount ->
            change.consume()
            yawDeg += dragAmount.x * 0.4f
            pitchDeg = (pitchDeg - dragAmount.y * 0.4f).coerceIn(-75f, 75f)
          }
        }
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasW = size.width
        val canvasH = size.height
        val centerX = canvasW / 2f
        val centerY = canvasH / 2f

        val baseScale = (minOf(canvasW, canvasH) * 0.55f / maxOf(design.widthMm, design.heightMm)) * zoomScale
        val frameDepthMm = 70f

        val mesh = buildWindow3DMesh(
          design = design,
          geom = geom,
          frameDepthMm = frameDepthMm,
          sashAngleDeg = sashOpenAngleDeg
        )

        render3DMesh(
          mesh = mesh,
          yawDeg = yawDeg,
          pitchDeg = pitchDeg,
          scale = baseScale,
          centerX = centerX,
          centerY = centerY,
          isPerspective = isPerspective
        )
      }

      // 3D Top Badge & Reset
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Color(0x991E293B)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Default.ViewInAr, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
            Text(
              text = "چرخش ۳ بعدی (لمس و کشیدن)",
              style = MaterialTheme.typography.labelMedium,
              color = Color.White
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          IconButton(
            onClick = {
              yawDeg = -25f
              pitchDeg = 15f
              zoomScale = 1.0f
              sashOpenAngleDeg = 35f
            },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(Icons.Default.Refresh, contentDescription = "تنظیم مجدد دوربین", tint = Color.White)
          }
          IconButton(
            onClick = { zoomScale = 1.0f },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(Icons.Default.FitScreen, contentDescription = "اندازه متناسب", tint = Color.White)
          }
        }
      }
    }

    // Controls
    Surface(
      color = Color(0xFF1E293B),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        // Sash Opening Angle Slider
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "زاویه بازشو: ${CurrencyHelper.toPersianDigits(sashOpenAngleDeg.toInt())}°",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFFBBF24),
            modifier = Modifier.width(105.dp)
          )
          Slider(
            value = sashOpenAngleDeg,
            onValueChange = { sashOpenAngleDeg = it },
            valueRange = 0f..90f,
            modifier = Modifier.weight(1f)
          )
        }

        // Zoom & Projection
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text(
              text = "بزرگ‌نمایی:",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF94A3B8)
            )
            Slider(
              value = zoomScale,
              onValueChange = { zoomScale = it },
              valueRange = 0.6f..2.2f,
              modifier = Modifier.weight(1f)
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          FilterChip(
            selected = isPerspective,
            onClick = { isPerspective = !isPerspective },
            label = {
              Text(
                text = if (isPerspective) "پرسپکتیو" else "ایزومتریک",
                style = MaterialTheme.typography.labelSmall
              )
            }
          )
        }
      }
    }
  }
}

private fun buildWindow3DMesh(
  design: WindowDesign,
  geom: com.example.domain.engine.DesignGeometry,
  frameDepthMm: Float,
  sashAngleDeg: Float
): List<Polygon3D> {
  val polys = mutableListOf<Polygon3D>()

  // Outer frame color
  val frameColor = Color(design.frameFinish.colorHex)
  val frameDarkColor = frameColor.copy(
    red = (frameColor.red * 0.72f),
    green = (frameColor.green * 0.72f),
    blue = (frameColor.blue * 0.72f)
  )

  // Sash color (distinct from frame!)
  val sashColor = Color(design.sashFinish.colorHex)
  val sashDarkColor = sashColor.copy(
    red = (sashColor.red * 0.72f),
    green = (sashColor.green * 0.72f),
    blue = (sashColor.blue * 0.72f)
  )

  // Glass colors (transparent with realistic highlight)
  val outerGlassTintHex = design.outerGlassTint.colorHex
  val glassColor = Color(outerGlassTintHex).copy(alpha = 0.28f)

  val halfW = design.widthMm / 2f
  val halfH = design.heightMm / 2f
  val halfD = frameDepthMm / 2f

  // 1. Outer Frame Box
  addBoxFrame(
    polys = polys,
    x = -halfW,
    y = -halfH,
    z = -halfD,
    w = design.widthMm.toFloat(),
    h = design.heightMm.toFloat(),
    d = frameDepthMm,
    faceWidth = geom.frameProfileFaceMm,
    frontColor = frameColor,
    sideColor = frameDarkColor
  )

  // 2. Transom Mullion & Glass
  if (geom.transomPanel != null) {
    val tY = -halfH + geom.transomPanel.yMm + geom.transomPanel.heightMm
    addSolidBar(
      polys = polys,
      x = -halfW + geom.frameProfileFaceMm,
      y = tY,
      z = -halfD,
      w = design.widthMm - (geom.frameProfileFaceMm * 2),
      h = geom.mullionProfileFaceMm,
      d = frameDepthMm,
      frontColor = frameColor,
      sideColor = frameDarkColor
    )

    // Transom Glass plane
    val tGlassY = -halfH + geom.transomPanel.yMm
    addGlassPlane(
      polys = polys,
      x = -halfW + geom.frameProfileFaceMm,
      y = tGlassY,
      z = 0f,
      w = geom.transomPanel.widthMm,
      h = geom.transomPanel.heightMm,
      color = glassColor
    )
  }

  // 3. Panels & Operable Sashes
  geom.panels.forEachIndexed { idx, p ->
    val panelX = -halfW + p.xMm
    val panelY = -halfH + p.yMm
    val panelW = p.widthMm
    val panelH = p.heightMm
    val panelConfig = design.panels.getOrNull(idx)
    val opening = panelConfig?.openingType ?: OpeningType.FIXED
    val direction = panelConfig?.openingDirection ?: OpeningDirection.INWARD

    // Panel-specific sash color if overridden, otherwise design sashFinish
    val thisSashColor = panelConfig?.sashFinish?.let { Color(it.colorHex) } ?: sashColor
    val thisSashDarkColor = thisSashColor.copy(
      red = (thisSashColor.red * 0.72f),
      green = (thisSashColor.green * 0.72f),
      blue = (thisSashColor.blue * 0.72f)
    )

    // Panel glass tint
    val thisGlassColor = Color(panelConfig?.glassTint?.colorHex ?: outerGlassTintHex).copy(alpha = 0.28f)

    if (!p.isSash) {
      // Fixed Glass Pane
      addGlassPlane(
        polys = polys,
        x = panelX,
        y = panelY,
        z = 0f,
        w = panelW,
        h = panelH,
        color = thisGlassColor
      )
    } else {
      // Operable Sash Profile with 3D Rotation!
      val sashDepth = frameDepthMm * 0.82f
      val sashFace = 65f

      val isLeftHinged = opening == OpeningType.CASEMENT_LEFT
      val hingeX = if (isLeftHinged) panelX else panelX + panelW

      // Inward vs outward opening direction:
      val dirSign = if (direction == OpeningDirection.INWARD) 1f else -1f
      val rotDeg = (if (isLeftHinged) -sashAngleDeg else sashAngleDeg) * dirSign
      val rad = Math.toRadians(rotDeg.toDouble()).toFloat()

      addRotatedSash(
        polys = polys,
        originX = panelX,
        originY = panelY,
        originZ = -halfD + 8f,
        w = panelW,
        h = panelH,
        d = sashDepth,
        faceWidth = sashFace,
        hingeX = hingeX,
        angleRad = rad,
        frontColor = thisSashColor,
        sideColor = thisSashDarkColor,
        glassColor = thisGlassColor
      )
    }

    // Mullion divider between panels
    if (idx < geom.panels.size - 1) {
      val mX = panelX + panelW
      addSolidBar(
        polys = polys,
        x = mX,
        y = panelY,
        z = -halfD,
        w = geom.mullionProfileFaceMm,
        h = panelH,
        d = frameDepthMm,
        frontColor = frameColor,
        sideColor = frameDarkColor
      )
    }
  }

  return polys
}

private fun addBoxFrame(
  polys: MutableList<Polygon3D>,
  x: Float, y: Float, z: Float,
  w: Float, h: Float, d: Float,
  faceWidth: Float,
  frontColor: Color,
  sideColor: Color
) {
  addSolidBar(polys, x, y, z, w, faceWidth, d, frontColor, sideColor)
  addSolidBar(polys, x, y + h - faceWidth, z, w, faceWidth, d, frontColor, sideColor)
  addSolidBar(polys, x, y + faceWidth, z, faceWidth, h - (faceWidth * 2), d, frontColor, sideColor)
  addSolidBar(polys, x + w - faceWidth, y + faceWidth, z, faceWidth, h - (faceWidth * 2), d, frontColor, sideColor)
}

private fun addSolidBar(
  polys: MutableList<Polygon3D>,
  x: Float, y: Float, z: Float,
  w: Float, h: Float, d: Float,
  frontColor: Color,
  sideColor: Color
) {
  val p000 = Point3D(x, y, z)
  val p100 = Point3D(x + w, y, z)
  val p110 = Point3D(x + w, y + h, z)
  val p010 = Point3D(x, y + h, z)

  val p001 = Point3D(x, y, z + d)
  val p101 = Point3D(x + w, y, z + d)
  val p111 = Point3D(x + w, y + h, z + d)
  val p011 = Point3D(x, y + h, z + d)

  polys.add(Polygon3D(listOf(p001, p101, p111, p011), frontColor, z + d))
  polys.add(Polygon3D(listOf(p100, p000, p010, p110), sideColor, z))
  polys.add(Polygon3D(listOf(p000, p001, p011, p010), sideColor, (z + (d / 2f))))
  polys.add(Polygon3D(listOf(p101, p100, p110, p111), sideColor, (z + (d / 2f))))
  polys.add(Polygon3D(listOf(p000, p100, p101, p001), sideColor, (z + (d / 2f))))
  polys.add(Polygon3D(listOf(p011, p111, p110, p010), sideColor, (z + (d / 2f))))
}

private fun addGlassPlane(
  polys: MutableList<Polygon3D>,
  x: Float, y: Float, z: Float,
  w: Float, h: Float,
  color: Color
) {
  val p0 = Point3D(x, y, z)
  val p1 = Point3D(x + w, y, z)
  val p2 = Point3D(x + w, y + h, z)
  val p3 = Point3D(x, y + h, z)

  polys.add(Polygon3D(listOf(p0, p1, p2, p3), color, z, isGlass = true))
}

private fun addRotatedSash(
  polys: MutableList<Polygon3D>,
  originX: Float, originY: Float, originZ: Float,
  w: Float, h: Float, d: Float,
  faceWidth: Float,
  hingeX: Float,
  angleRad: Float,
  frontColor: Color,
  sideColor: Color,
  glassColor: Color
) {
  fun rotatePoint(p: Point3D): Point3D {
    val relX = p.x - hingeX
    val relZ = p.z - originZ
    val cosA = cos(angleRad)
    val sinA = sin(angleRad)
    val rotX = (relX * cosA) - (relZ * sinA)
    val rotZ = (relX * sinA) + (relZ * cosA)
    return Point3D(hingeX + rotX, p.y, originZ + rotZ)
  }

  fun addRotatedQuad(v0: Point3D, v1: Point3D, v2: Point3D, v3: Point3D, color: Color, isGlass: Boolean = false) {
    val r0 = rotatePoint(v0)
    val r1 = rotatePoint(v1)
    val r2 = rotatePoint(v2)
    val r3 = rotatePoint(v3)
    val avgZ = (r0.z + r1.z + r2.z + r3.z) / 4f
    polys.add(Polygon3D(listOf(r0, r1, r2, r3), color, avgZ, isGlass))
  }

  val x = originX
  val y = originY
  val z = originZ

  val p001 = Point3D(x, y, z + d)
  val p101 = Point3D(x + w, y, z + d)
  val p111 = Point3D(x + w, y + h, z + d)
  val p011 = Point3D(x, y + h, z + d)

  val p000 = Point3D(x, y, z)
  val p100 = Point3D(x + w, y, z)
  val p110 = Point3D(x + w, y + h, z)
  val p010 = Point3D(x, y + h, z)

  // Outer sash faces
  addRotatedQuad(p001, p101, p111, p011, frontColor)
  addRotatedQuad(p100, p000, p010, p110, sideColor)
  addRotatedQuad(p000, p001, p011, p010, sideColor)
  addRotatedQuad(p101, p100, p110, p111, sideColor)
  addRotatedQuad(p000, p100, p101, p001, sideColor)
  addRotatedQuad(p011, p111, p110, p010, sideColor)

  // Glass inside sash
  val g0 = Point3D(x + faceWidth, y + faceWidth, z + (d / 2f))
  val g1 = Point3D(x + w - faceWidth, y + faceWidth, z + (d / 2f))
  val g2 = Point3D(x + w - faceWidth, y + h - faceWidth, z + (d / 2f))
  val g3 = Point3D(x + faceWidth, y + h - faceWidth, z + (d / 2f))
  addRotatedQuad(g0, g1, g2, g3, glassColor, isGlass = true)
}

private fun DrawScope.render3DMesh(
  mesh: List<Polygon3D>,
  yawDeg: Float,
  pitchDeg: Float,
  scale: Float,
  centerX: Float,
  centerY: Float,
  isPerspective: Boolean
) {
  val radYaw = Math.toRadians(yawDeg.toDouble()).toFloat()
  val radPitch = Math.toRadians(pitchDeg.toDouble()).toFloat()

  val cosYaw = cos(radYaw)
  val sinYaw = sin(radYaw)
  val cosPitch = cos(radPitch)
  val sinPitch = sin(radPitch)

  class ProjectedPoly(
    val points2D: List<Offset>,
    val avgZ: Float,
    val color: Color,
    val isGlass: Boolean
  )

  val projectedPolys = mutableListOf<ProjectedPoly>()

  for (poly in mesh) {
    var sumZ = 0f
    val points2D = poly.vertices.map { v ->
      val x1 = (v.x * cosYaw) + (v.z * sinYaw)
      val y1 = v.y
      val z1 = (-v.x * sinYaw) + (v.z * cosYaw)

      val x2 = x1
      val y2 = (y1 * cosPitch) - (z1 * sinPitch)
      val z2 = (y1 * sinPitch) + (z1 * cosPitch)

      sumZ += z2

      val projScale = if (isPerspective) {
        val distance = 3000f
        distance / (distance - z2)
      } else 1.0f

      val px = centerX + (x2 * scale * projScale)
      val py = centerY + (y2 * scale * projScale)
      Offset(px, py)
    }

    val avgZ = sumZ / poly.vertices.size
    projectedPolys.add(ProjectedPoly(points2D, avgZ, poly.baseColor, poly.isGlass))
  }

  // Sort back to front
  projectedPolys.sortBy { it.avgZ }

  for (p in projectedPolys) {
    if (p.points2D.size >= 3) {
      val path = Path().apply {
        moveTo(p.points2D[0].x, p.points2D[0].y)
        for (i in 1 until p.points2D.size) {
          lineTo(p.points2D[i].x, p.points2D[i].y)
        }
        close()
      }

      drawPath(path, p.color)

      if (!p.isGlass) {
        drawPath(path, Color(0x660F172A), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f))
      } else {
        drawPath(path, Color(0x8838BDF8), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.0f))
      }
    }
  }
}
