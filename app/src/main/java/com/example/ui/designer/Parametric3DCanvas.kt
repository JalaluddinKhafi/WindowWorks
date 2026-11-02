package com.example.ui.designer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.CurrencyHelper
import com.example.domain.engine.GeometryEngine
import com.example.domain.model.*
import kotlinx.coroutines.delay
import kotlin.math.*

data class Point3D(val x: Float, val y: Float, val z: Float)

data class Polygon3D(
  val vertices: List<Point3D>,
  val baseColor: Color,
  val normalZ: Float, // for depth sorting
  val isGlass: Boolean = false
)

enum class CameraPreset(val titleFa: String, val yaw: Float, val pitch: Float) {
  FRONT("روبرو", 0f, 0f),
  SIDE("کنار", 90f, 0f),
  TOP("بالا", 0f, 85f),
  ISOMETRIC("ایزومتریک", -35f, 25f)
}

enum class ThreeDInspectorTab(val titleFa: String) {
  PROFILE("پروفیل"),
  GLASS("شیشه"),
  HANDLE("دستگیره")
}

@Composable
fun Parametric3DCanvas(
  design: WindowDesign,
  onUpdateDesign: ((WindowDesign) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  var yawDeg by remember { mutableFloatStateOf(-30f) }
  var pitchDeg by remember { mutableFloatStateOf(20f) }
  var zoomScale by remember { mutableFloatStateOf(1.0f) }
  var panOffset by remember { mutableStateOf(Offset.Zero) }
  var sashOpenAngleDeg by remember { mutableFloatStateOf(35f) }
  var isPerspective by remember { mutableStateOf(true) }
  var autoOrbit by remember { mutableStateOf(false) }
  var showInspector by remember { mutableStateOf(true) }
  var selectedTab by remember { mutableStateOf(ThreeDInspectorTab.PROFILE) }

  // Auto-orbit animation loop
  LaunchedEffect(autoOrbit) {
    while (autoOrbit) {
      delay(30)
      yawDeg = (yawDeg + 0.7f) % 360f
    }
  }

  val geom = remember(design) { GeometryEngine.computeGeometry(design) }
  val mesh = remember(design, geom, sashOpenAngleDeg) {
    buildWindow3DMesh(design, geom, 70f, sashOpenAngleDeg)
  }

  val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
    zoomScale = (zoomScale * zoomChange).coerceIn(0.5f, 3.5f)
    panOffset += offsetChange
  }

  // Work with a local design copy for responsive live customization
  var localDesign by remember(design) { mutableStateOf(design) }

  Row(modifier = modifier.fillMaxSize()) {
    // 3D Canvas Box
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
        .background(
          Brush.verticalGradient(
            listOf(
              Color(0xFF0F172A),
              Color(0xFF1E293B),
              Color(0xFF0F172A)
            )
          )
        )
        .transformable(state = transformState)
        .pointerInput(Unit) {
          detectDragGestures { change, dragAmount ->
            change.consume()
            autoOrbit = false
            yawDeg += dragAmount.x * 0.45f
            pitchDeg = (pitchDeg - dragAmount.y * 0.45f).coerceIn(-85f, 85f)
          }
        }
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasW = size.width
        val canvasH = size.height

        // 1. Draw Architectural Room Setting (Ceiling lights, wall casing, reflective tile floor)
        drawArchitecturalRoomSetting(canvasW, canvasH)

        // 2. Project & Render Window 3D Mesh
        val centerX = canvasW / 2f + panOffset.x
        val centerY = canvasH / 2f + panOffset.y - 30f

        val maxDim = maxOf(localDesign.widthMm, localDesign.heightMm).toFloat().coerceAtLeast(100f)
        val baseScale = (minOf(canvasW, canvasH) * 0.52f / maxDim) * zoomScale

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

      // Top Status Overlay (Mockup 3D header)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Color(0xCC0F172A)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.ViewInAr, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
            Text(
              text = "پیش‌نمایش سه بعدی",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
        }

        // Camera presets row
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Color(0xCC0F172A)
        ) {
          Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            CameraPreset.entries.forEach { preset ->
              val isSelected = abs(yawDeg - preset.yaw) < 5f && abs(pitchDeg - preset.pitch) < 5f
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected) Color(0xFF0284C7) else Color.Transparent
              ) {
                IconButton(
                  onClick = {
                    yawDeg = preset.yaw
                    pitchDeg = preset.pitch
                    panOffset = Offset.Zero
                    autoOrbit = false
                  },
                  modifier = Modifier.size(32.dp)
                ) {
                  Text(
                    text = preset.titleFa,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                  )
                }
              }
            }
          }
        }
      }

      // Floating On-Canvas Controls (Matching Reference: 360°, Zoom +, Zoom -)
      Row(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // 360° Orbit Button
        Button(
          onClick = { autoOrbit = !autoOrbit },
          colors = ButtonDefaults.buttonColors(
            containerColor = if (autoOrbit) Color(0xFF0284C7) else Color(0xCC0F172A)
          ),
          shape = RoundedCornerShape(12.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
          Spacer(modifier = Modifier.width(4.dp))
          Text("360°", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
        }

        // Zoom In
        FilledTonalIconButton(
          onClick = { zoomScale = (zoomScale * 1.2f).coerceAtMost(3.5f) },
          colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color(0xCC0F172A)),
          modifier = Modifier.size(40.dp)
        ) {
          Icon(Icons.Default.ZoomIn, contentDescription = "بزرگ‌نمایی", tint = Color.White)
        }

        // Zoom Out
        FilledTonalIconButton(
          onClick = { zoomScale = (zoomScale / 1.2f).coerceAtLeast(0.5f) },
          colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color(0xCC0F172A)),
          modifier = Modifier.size(40.dp)
        ) {
          Icon(Icons.Default.ZoomOut, contentDescription = "کوچک‌نمایی", tint = Color.White)
        }

        // Reset
        FilledTonalIconButton(
          onClick = {
            yawDeg = -30f
            pitchDeg = 20f
            zoomScale = 1.0f
            panOffset = Offset.Zero
            autoOrbit = false
          },
          colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color(0xCC0F172A)),
          modifier = Modifier.size(40.dp)
        ) {
          Icon(Icons.Default.FitScreen, contentDescription = "تنظیم زاویه", tint = Color.White)
        }
      }

      // Open/Close Sash Floating Pill
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xDD0F172A),
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(16.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "بازشو: ${CurrencyHelper.toPersianDigits(sashOpenAngleDeg.toInt())}°",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFFBBF24)
          )
          Slider(
            value = sashOpenAngleDeg,
            onValueChange = { sashOpenAngleDeg = it },
            valueRange = 0f..90f,
            modifier = Modifier.width(100.dp)
          )
        }
      }
    }

    // Right-Side 3D Material & Appearance Inspector (Matching Top-Right of Reference)
    AnimatedVisibility(visible = showInspector) {
      Surface(
        color = Color(0xFF0F172A),
        tonalElevation = 8.dp,
        modifier = Modifier
          .width(260.dp)
          .fillMaxHeight()
          .border(width = 1.dp, color = Color(0xFF334155))
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          // Three Top Tabs: دستگیره | شیشه | پروفیل
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
              .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            ThreeDInspectorTab.entries.reversed().forEach { tab ->
              val isSelected = selectedTab == tab
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isSelected) Color(0xFF0284C7) else Color.Transparent)
                  .clickable { selectedTab = tab }
                  .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = tab.titleFa,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  color = Color.White
                )
              }
            }
          }

          when (selectedTab) {
            ThreeDInspectorTab.PROFILE -> {
              // 1. Profile Color Swatches (10 swatches)
              Text("رنگ پروفیل", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
              val profileColors = listOf(
                0xFFF8FAFC, 0xFFE2E8F0, 0xFF94A3B8, 0xFF64748B,
                0xFF334155, 0xFF1E293B, 0xFF5D4037, 0xFF8D6E63,
                0xFFB45309, 0xFF0284C7
              )
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in profileColors.chunked(5)) {
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { hex ->
                      val isSelected = (localDesign.customColorHex ?: localDesign.frameFinish.colorHex) == hex
                      Box(
                        modifier = Modifier
                          .size(36.dp)
                          .clip(RoundedCornerShape(8.dp))
                          .background(Color(hex))
                          .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF475569),
                            shape = RoundedCornerShape(8.dp)
                          )
                          .clickable {
                            localDesign = localDesign.copy(customColorHex = hex)
                          }
                      )
                    }
                  }
                }
              }

              HorizontalDivider(color = Color(0xFF334155))

              // 2. Coating Type (نوع پوشش)
              Text("نوع پوشش", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                CoatingType.entries.forEach { coating ->
                  val isSelected = localDesign.coatingType == coating
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B),
                    modifier = Modifier
                      .weight(1f)
                      .clickable { localDesign = localDesign.copy(coatingType = coating) }
                  ) {
                    Text(
                      text = coating.titlePersian.take(5),
                      style = MaterialTheme.typography.labelSmall,
                      color = Color.White,
                      modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                      textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                  }
                }
              }

              HorizontalDivider(color = Color(0xFF334155))

              // 3. Handle Color (رنگ دستگیره)
              Text("رنگ دستگیره", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                HandleColor.entries.forEach { hc ->
                  val isSelected = localDesign.handleColor == hc
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                      .weight(1f)
                      .clip(RoundedCornerShape(8.dp))
                      .background(if (isSelected) Color(0xFF1E293B) else Color.Transparent)
                      .clickable { localDesign = localDesign.copy(handleColor = hc) }
                      .padding(4.dp)
                  ) {
                    Box(
                      modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(hc.colorHex))
                        .border(
                          width = if (isSelected) 2.dp else 1.dp,
                          color = if (isSelected) Color(0xFF38BDF8) else Color.Gray,
                          shape = CircleShape
                        )
                    )
                    Text(hc.name.take(4), style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                  }
                }
              }
            }

            ThreeDInspectorTab.GLASS -> {
              Text("رنگ و شفافیت شیشه", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
              GlassTint.entries.forEach { tint ->
                val isSelected = localDesign.outerGlassTint == tint
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Color(0xFF1E293B) else Color.Transparent)
                    .clickable { localDesign = localDesign.copy(outerGlassTint = tint) }
                    .padding(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(24.dp)
                      .clip(CircleShape)
                      .background(Color(tint.colorHex))
                  )
                  Text(tint.titlePersian, color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
              }

              HorizontalDivider(color = Color(0xFF334155))

              // Decorative Georgian Grids toggle
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("طرح پنجره فرانسوی (جورجین بار):", color = Color.White, style = MaterialTheme.typography.bodySmall)
                Switch(
                  checked = localDesign.hasGrids,
                  onCheckedChange = { localDesign = localDesign.copy(hasGrids = it) }
                )
              }
            }

            ThreeDInspectorTab.HANDLE -> {
              Text("تنظیمات دستگیره و یراق", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("قفل چندنقطه‌ای ضدسرقت:", color = Color.White, style = MaterialTheme.typography.bodySmall)
                Switch(
                  checked = localDesign.panels.any { it.lockPoints > 1 },
                  onCheckedChange = { hasLock ->
                    localDesign = localDesign.copy(
                      panels = localDesign.panels.map { it.copy(lockPoints = if (hasLock) 4 else 1) }
                    )
                  }
                )
              }
            }
          }

          Spacer(modifier = Modifier.weight(1f))

          // Apply Changes Button (Matching Mockup: اعمال تغییرات with checkmark)
          Button(
            onClick = {
              onUpdateDesign?.invoke(localDesign)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("اعمال تغییرات", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

private fun DrawScope.drawArchitecturalRoomSetting(w: Float, h: Float) {
  // 1. Ceiling & Upper wall gradient (Warm indoor lighting glow)
  val horizonY = h * 0.72f
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(
        Color(0xFF0F172A),
        Color(0xFF1E293B),
        Color(0xFF334155)
      ),
      startY = 0f,
      endY = horizonY
    ),
    topLeft = Offset.Zero,
    size = androidx.compose.ui.geometry.Size(w, horizonY)
  )

  // 2. Tile Floor with perspective grid and reflection
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(
        Color(0xFF334155),
        Color(0xFF1E293B),
        Color(0xFF0F172A)
      ),
      startY = horizonY,
      endY = h
    ),
    topLeft = Offset(0f, horizonY),
    size = androidx.compose.ui.geometry.Size(w, h - horizonY)
  )

  // Perspective floor grid lines
  val floorCenter = Offset(w / 2f, horizonY)
  for (angle in -6..6) {
    val spreadX = (w / 2f) + (angle * (w * 0.14f))
    drawLine(
      color = Color(0x3364748B),
      start = floorCenter,
      end = Offset(spreadX, h),
      strokeWidth = 1.2f
    )
  }

  // Horizon baseboard / skirting line
  drawLine(
    color = Color(0xFF64748B),
    start = Offset(0f, horizonY),
    end = Offset(w, horizonY),
    strokeWidth = 2.5f
  )
}

fun buildWindow3DMesh(
  design: WindowDesign,
  geom: com.example.domain.engine.DesignGeometry,
  frameDepthMm: Float,
  sashAngleDeg: Float
): List<Polygon3D> {
  val polys = mutableListOf<Polygon3D>()

  // Outer frame colors
  val baseFrameHex = design.customColorHex ?: design.frameFinish.colorHex
  val frameColor = Color(baseFrameHex)
  val frameDarkColor = frameColor.copy(
    red = (frameColor.red * 0.72f),
    green = (frameColor.green * 0.72f),
    blue = (frameColor.blue * 0.72f)
  )

  // Sash colors
  val sashColor = Color(design.sashFinish.colorHex)
  val sashDarkColor = sashColor.copy(
    red = (sashColor.red * 0.72f),
    green = (sashColor.green * 0.72f),
    blue = (sashColor.blue * 0.72f)
  )

  // Glass colors
  val outerGlassTintHex = design.outerGlassTint.colorHex
  val glassColor = Color(outerGlassTintHex).copy(alpha = 0.32f)

  val halfW = design.widthMm / 2f
  val halfH = design.heightMm / 2f
  val halfD = frameDepthMm / 2f

  // 1. Outer Frame Extrusion (4 perimeter bars)
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
    val direction = panelConfig?.openingDirection ?: design.openingDirection

    val thisSashColor = panelConfig?.sashFinish?.let { Color(it.colorHex) } ?: sashColor
    val thisSashDarkColor = thisSashColor.copy(
      red = (thisSashColor.red * 0.72f),
      green = (thisSashColor.green * 0.72f),
      blue = (thisSashColor.blue * 0.72f)
    )
    val thisGlassColor = Color(panelConfig?.glassTint?.colorHex ?: outerGlassTintHex).copy(alpha = 0.32f)

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
      val sashDepth = frameDepthMm * 0.82f
      val sashFace = minOf(65f, panelW / 4f, panelH / 4f)
      val local = mutableListOf<Polygon3D>()

      // 4 perimeter sash profile bars
      addBoxFrame(
        local,
        panelX,
        panelY,
        -halfD + 8f,
        panelW,
        panelH,
        sashDepth,
        sashFace,
        thisSashColor,
        thisSashDarkColor
      )

      // Glass inside sash
      addGlassPlane(
        local,
        panelX + sashFace,
        panelY + sashFace,
        0f,
        panelW - sashFace * 2f,
        panelH - sashFace * 2f,
        thisGlassColor
      )

      // Handle on operable sash
      if (panelConfig?.hasHandle != false) {
        val defaultX = when (opening) {
          OpeningType.CASEMENT_LEFT -> 1f - sashFace / (2f * panelW)
          OpeningType.TOP_HUNG, OpeningType.TILT -> 0.5f
          else -> sashFace / (2f * panelW)
        }
        val hx = panelX + panelW * (panelConfig?.handleX ?: defaultX)
        val hy = panelY + panelH * (panelConfig?.handleY ?: 0.5f)

        val handleBaseColor = Color(design.handleColor.colorHex)
        val handleSideColor = handleBaseColor.copy(
          red = handleBaseColor.red * 0.75f,
          green = handleBaseColor.green * 0.75f,
          blue = handleBaseColor.blue * 0.75f
        )

        // Handle base escutcheon
        addSolidBar(local, hx - 8f, hy - 10f, halfD + 14f, 16f, 24f, 10f, handleBaseColor, handleSideColor)
        // Handle lever
        addSolidBar(local, hx - 5f, hy, halfD + 20f, 10f, 65f, 12f, handleBaseColor, handleSideColor)
      }

      // Kinematic transformation of sash (Casement swing, sliding offset, tilt)
      val sign = if (direction == OpeningDirection.INWARD) 1f else -1f
      val angle = Math.toRadians((sashAngleDeg * sign).toDouble()).toFloat()

      fun transform(v: Point3D): Point3D {
        if (opening == OpeningType.SLIDING_LEFT || opening == OpeningType.SLIDING_RIGHT) {
          val slideSign = if (opening == OpeningType.SLIDING_LEFT) -1f else 1f
          return v.copy(
            x = v.x + slideSign * panelW * 0.8f * (sashAngleDeg / 90f),
            z = v.z + 25f
          )
        }
        if (opening == OpeningType.TOP_HUNG || opening == OpeningType.TILT || opening == OpeningType.TILT_AND_TURN) {
          val hingeY = if (opening == OpeningType.TOP_HUNG) panelY else panelY + panelH
          val tilt = if (opening == OpeningType.TILT_AND_TURN) angle / 3f else angle
          val dy = v.y - hingeY
          return v.copy(
            y = hingeY + dy * cos(tilt) - v.z * sin(tilt),
            z = dy * sin(tilt) + v.z * cos(tilt)
          )
        }

        // Casement (left, right, double)
        val isLeftHinged = opening == OpeningType.CASEMENT_LEFT
        val hingeX = if (isLeftHinged) panelX else panelX + panelW
        val a = if (isLeftHinged) -angle else angle
        val dx = v.x - hingeX
        return v.copy(
          x = hingeX + dx * cos(a) - v.z * sin(a),
          z = dx * sin(a) + v.z * cos(a)
        )
      }

      polys.addAll(local.map { poly -> poly.copy(vertices = poly.vertices.map(::transform)) })
    }
  }

  // 4. Dividers / Mullions extrusion
  geom.dividers.forEach { divider ->
    addSolidBar(
      polys,
      -halfW + divider.xMm,
      -halfH + divider.yMm,
      -halfD,
      divider.widthMm,
      divider.heightMm,
      frameDepthMm,
      frameColor,
      frameDarkColor
    )
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
  addSolidBar(polys, x, y + faceWidth, z, faceWidth, h - (faceWidth * 2f), d, frontColor, sideColor)
  addSolidBar(polys, x + w - faceWidth, y + faceWidth, z, faceWidth, h - (faceWidth * 2f), d, frontColor, sideColor)
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

  // Directional Light vector in camera space
  val lightX = 0.35f
  val lightY = -0.65f
  val lightZ = 0.65f
  val lightLen = sqrt(lightX * lightX + lightY * lightY + lightZ * lightZ)
  val lx = lightX / lightLen
  val ly = lightY / lightLen
  val lz = lightZ / lightLen

  class ProjectedPoly(
    val points2D: List<Offset>,
    val avgZ: Float,
    val shadedColor: Color,
    val isGlass: Boolean
  )

  val projectedPolys = mutableListOf<ProjectedPoly>()
  val cameraDistance = maxOf(3000f, mesh.asSequence().flatMap { it.vertices.asSequence() }
    .maxOfOrNull { maxOf(abs(it.x), abs(it.y), abs(it.z)) }?.times(4f) ?: 3000f)

  for (poly in mesh) {
    if (poly.vertices.size < 3) continue

    var sumZ = 0f
    val rotVerts = poly.vertices.map { v ->
      val x1 = (v.x * cosYaw) + (v.z * sinYaw)
      val y1 = v.y
      val z1 = (-v.x * sinYaw) + (v.z * cosYaw)

      val x2 = x1
      val y2 = (y1 * cosPitch) - (z1 * sinPitch)
      val z2 = (y1 * sinPitch) + (z1 * cosPitch)

      sumZ += z2
      Point3D(x2, y2, z2)
    }

    // Normal calculation for lighting
    val v0 = rotVerts[0]
    val v1 = rotVerts[1]
    val v2 = rotVerts[2]
    val ax = v1.x - v0.x
    val ay = v1.y - v0.y
    val az = v1.z - v0.z
    val bx = v2.x - v0.x
    val by = v2.y - v0.y
    val bz = v2.z - v0.z

    val nx = ay * bz - az * by
    val ny = az * bx - ax * bz
    val nz = ax * by - ay * bx
    val nLen = sqrt(nx * nx + ny * ny + nz * nz)

    val shadedColor = if (!poly.isGlass && nLen > 0.0001f) {
      val dot = (nx * lx + ny * ly + nz * lz) / nLen
      val diffuse = dot.coerceIn(0.25f, 1.0f)
      poly.baseColor.copy(
        red = (poly.baseColor.red * diffuse).coerceIn(0f, 1f),
        green = (poly.baseColor.green * diffuse).coerceIn(0f, 1f),
        blue = (poly.baseColor.blue * diffuse).coerceIn(0f, 1f)
      )
    } else poly.baseColor

    val points2D = rotVerts.map { rv ->
      val projScale = if (isPerspective) {
        cameraDistance / (cameraDistance - rv.z).coerceAtLeast(100f)
      } else 1.0f

      val px = centerX + (rv.x * scale * projScale)
      val py = centerY + (rv.y * scale * projScale)
      Offset(px, py)
    }

    val avgZ = sumZ / poly.vertices.size
    projectedPolys.add(ProjectedPoly(points2D, avgZ, shadedColor, poly.isGlass))
  }

  // Painter's Algorithm: Sort back to front
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

      drawPath(path, p.shadedColor)

      if (!p.isGlass) {
        drawPath(path, Color(0x550F172A), style = Stroke(width = 1.2f))
      } else {
        // Specular highlight outline on glass
        drawPath(path, Color(0x9967E8F9), style = Stroke(width = 1.0f))
      }
    }
  }
}
