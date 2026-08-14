package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ImmersiveDarkColorScheme = darkColorScheme(
  primary = ImmersivePrimary,
  onPrimary = ImmersiveOnPrimary,
  primaryContainer = ImmersivePrimaryContainer,
  onPrimaryContainer = ImmersiveOnPrimaryContainer,
  secondary = ImmersiveSecondary,
  onSecondary = ImmersiveOnSecondary,
  background = ImmersiveDarkBg,
  surface = ImmersiveSurface,
  surfaceVariant = ImmersiveSurfaceContainer,
  outline = ImmersiveOutline,
  outlineVariant = ImmersiveOutlineVariant,
  onBackground = ImmersiveTextPrimary,
  onSurface = ImmersiveTextPrimary,
  onSurfaceVariant = ImmersiveTextSecondary
)

@Composable
fun AIBodyTrackingTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  val colorScheme = ImmersiveDarkColorScheme
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        window.statusBarColor = ImmersiveSurface.toArgb()
        window.navigationBarColor = ImmersiveSurface.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
      }
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  AIBodyTrackingTheme(darkTheme, dynamicColor, content)
}

