package com.example.ui.designer

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.*

/** Studio palette shared by the designer surfaces so new components stay visually consistent. */
object StudioColors {
  val Background = Color(0xFF0B1220)
  val Surface = Color(0xFF0F172A)
  val SurfaceElevated = Color(0xFF111C33)
  val SurfaceRaised = Color(0xFF1B2942)
  val Outline = Color(0xFF243352)
  val OutlineSoft = Color(0xFF334155)
  val TextPrimary = Color(0xFFF1F5F9)
  val TextMuted = Color(0xFF94A3B8)
  val Accent = Color(0xFF38BDF8)
  val AccentStrong = Color(0xFF0284C7)
  val Success = Color(0xFF10B981)
  val Warning = Color(0xFFFBBF24)
  val Danger = Color(0xFFF87171)
  val Upvc = Color(0xFF0EA5E9)
  val Aluminum = Color(0xFFF59E0B)
}

/** Brand accent per material family — used by chips, badges and the canvas frame hint. */
fun MaterialFamily?.accentColor(): Color = when (this) {
  MaterialFamily.ALUMINUM -> StudioColors.Aluminum
  MaterialFamily.UPVC -> StudioColors.Upvc
  null -> StudioColors.Accent
}

@Composable
fun MaterialFamilyChip(
  family: MaterialFamily,
  selectedMaterial: MaterialFamily,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val accent = family.accentColor()
  val isSelected = family == selectedMaterial
  val container by animateColorAsState(
    if (isSelected) accent.copy(alpha = 0.18f) else StudioColors.SurfaceRaised,
    label = "materialChip"
  )
  val border by animateColorAsState(
    if (isSelected) accent else StudioColors.Outline,
    label = "materialChipBorder"
  )
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = container,
    border = BorderStroke(if (isSelected) 2.dp else 1.dp, border),
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onClick)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
          modifier = Modifier
            .size(10.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(accent)
        )
        Text(
          text = MaterialCatalog.shortFamilyTitle(family),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = StudioColors.TextPrimary
        )
        if (isSelected) {
          Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(16.dp)
          )
        }
      }
      Text(
        text = if (family == MaterialFamily.UPVC) "پروفیل چندمحفظه‌ای با تقویت فولادی"
        else "مقطع اکسترود شده با پل حرارتی",
        style = MaterialTheme.typography.labelSmall,
        color = StudioColors.TextMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

/**
 * Material bar shown on top of the design studio. Choosing PVC or aluminum swaps the profile-system
 * list, the allowed opening types and the manufacturing rules of the whole design.
 */
@Composable
fun MaterialSelectionBar(
  design: WindowDesign,
  onDesignChange: (WindowDesign) -> Unit,
  compact: Boolean = false,
  modifier: Modifier = Modifier
) {
  val systems = MaterialCatalog.forFamily(design.materialFamily)
  val activeSystem = systems.firstOrNull { it.code == design.profileSystem }
    ?: MaterialCatalog.defaultFor(design.materialFamily)

  Surface(
    shape = RoundedCornerShape(if (compact) 0.dp else 14.dp),
    color = StudioColors.SurfaceElevated,
    border = BorderStroke(1.dp, StudioColors.Outline),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "جنس سازه",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = StudioColors.TextPrimary
          )
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = design.materialFamily.accentColor().copy(alpha = 0.16f)
          ) {
            Text(
              text = MaterialCatalog.familyTitle(design.materialFamily),
              style = MaterialTheme.typography.labelSmall,
              color = design.materialFamily.accentColor(),
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
          }
        }
        Text(
          text = "${activeSystem.faceWidthMm} mm",
          style = MaterialTheme.typography.labelSmall,
          color = StudioColors.TextMuted
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MaterialFamily.entries.forEach { family ->
          MaterialFamilyChip(
            family = family,
            selectedMaterial = design.materialFamily,
            onClick = {
              if (family == design.materialFamily) return@MaterialFamilyChip
              val target = MaterialCatalog.defaultFor(family)
              onDesignChange(
                design.copy(
                  materialFamily = family,
                  profileSystem = target.code,
                  profileFaceWidthMm = target.faceWidthMm,
                  hasThermalBreak = family == MaterialFamily.UPVC || target.code != "Alu 50 Normal",
                  // Aluminum cannot host PVC-only tilt & turn hardware in this catalog.
                  panels = if (family == MaterialFamily.ALUMINUM) {
                    design.panels.map {
                      if (it.openingType == OpeningType.TILT_AND_TURN)
                        it.copy(openingType = OpeningType.TILT) else it
                    }
                  } else design.panels
                )
              )
            },
            modifier = Modifier.weight(1f)
          )
        }
      }

      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(systems, key = { it.code }) { system ->
          val selected = system.code == design.profileSystem
          FilterChip(
            selected = selected,
            onClick = {
              onDesignChange(
                design.copy(
                  profileSystem = system.code,
                  profileFaceWidthMm = system.faceWidthMm,
                  hasThermalBreak = design.materialFamily == MaterialFamily.UPVC || system.code != "Alu 50 Normal"
                )
              )
            },
            label = { Text(system.code, style = MaterialTheme.typography.labelSmall) },
            leadingIcon = if (selected) {
              { Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null
          )
        }
      }

      if (design.materialFamily == MaterialFamily.ALUMINUM) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A2438))
            .padding(horizontal = 10.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
              Icons.Default.Thermostat,
              contentDescription = null,
              tint = StudioColors.Aluminum,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = "پل حرارتی (Thermal Break)",
              style = MaterialTheme.typography.labelSmall,
              color = StudioColors.TextPrimary
            )
          }
          Switch(
            checked = design.hasThermalBreak,
            onCheckedChange = { onDesignChange(design.copy(hasThermalBreak = it)) },
            modifier = Modifier.height(24.dp)
          )
        }
      }
    }
  }
}
