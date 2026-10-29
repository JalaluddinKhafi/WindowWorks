package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = WorkshopCyanLight,
  onPrimary = WorkshopNavyDark,
  primaryContainer = WorkshopBlue,
  onPrimaryContainer = Color.White,
  secondary = WorkshopAmberLight,
  onSecondary = WorkshopNavyDark,
  secondaryContainer = WorkshopAmber,
  onSecondaryContainer = Color.White,
  tertiary = WorkshopGreenLight,
  onTertiary = WorkshopNavyDark,
  background = WorkshopNavyDark,
  onBackground = Slate50,
  surface = WorkshopNavyMedium,
  onSurface = Slate50,
  surfaceVariant = Slate800,
  onSurfaceVariant = Slate300,
  outline = Slate500
)

private val LightColorScheme = lightColorScheme(
  primary = WorkshopBlue,
  onPrimary = Color.White,
  primaryContainer = Slate200,
  onPrimaryContainer = WorkshopNavyDark,
  secondary = WorkshopCyan,
  onSecondary = Color.White,
  secondaryContainer = Slate100,
  onSecondaryContainer = WorkshopNavyDark,
  tertiary = WorkshopAmber,
  onTertiary = Color.White,
  background = Slate50,
  onBackground = Slate900,
  surface = Color.White,
  onSurface = Slate900,
  surfaceVariant = Slate100,
  onSurfaceVariant = Slate700,
  outline = Slate400
)

@Composable
fun WindowWorkshopTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
