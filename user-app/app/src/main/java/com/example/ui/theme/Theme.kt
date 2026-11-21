package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.view.WindowCompat

val LocalThemeManager = staticCompositionLocalOf<com.example.data.ThemeManager> {
    error("No ThemeManager provided")
}

val LocalAppPalette = staticCompositionLocalOf { LightAppPalette }

private val DarkColorScheme =
  darkColorScheme(
    primary = FigmaCrimsonStart,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4C0519),
    onPrimaryContainer = Color(0xFFFFD6E0),
    secondary = Slate200,
    onSecondary = Slate900,
    secondaryContainer = Color(0xFF252739),
    onSecondaryContainer = Color(0xFFF4F5FA),
    tertiary = QuickActionCompareIcon,
    onTertiary = Color.White,
    background = Color(0xFF0F101A),
    onBackground = Color(0xFFF4F5FA),
    surface = Color(0xFF1A1C2B),
    onSurface = Color(0xFFF4F5FA),
    surfaceVariant = Color(0xFF252739),
    onSurfaceVariant = Color(0xFFC2C5D6),
    outline = Color(0xFF3B3E54),
    outlineVariant = Color(0xFF2D3045),
    error = FigmaCrimsonStart,
    errorContainer = Color(0xFF4C0519),
    onErrorContainer = Color(0xFFFFD1D8)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = FigmaCrimsonLink,
    onPrimary = Color.White,
    primaryContainer = MockupPinkBg,
    onPrimaryContainer = FigmaCrimsonText,
    secondary = FigmaTextPrimary,
    onSecondary = Color.White,
    secondaryContainer = Slate100,
    onSecondaryContainer = FigmaTextPrimary,
    tertiary = QuickActionCompareIcon,
    onTertiary = Color.White,
    background = FigmaBg,
    onBackground = FigmaTextPrimary,
    surface = Color.White,
    onSurface = FigmaTextPrimary,
    surfaceVariant = FigmaSurfaceGradMid,
    onSurfaceVariant = FigmaTextSecondary,
    outline = Slate200,
    outlineVariant = Slate100,
    error = FigmaCrimsonEnd,
    errorContainer = Red50,
    onErrorContainer = FigmaCrimsonLink
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is disabled by default to show our custom sleek theme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        val barColor = if (darkTheme) Slate900 else FigmaBg
        @Suppress("DEPRECATION")
        window.statusBarColor = barColor.toArgb()
        @Suppress("DEPRECATION")
        window.navigationBarColor = barColor.toArgb()
        WindowCompat.getInsetsController(window, view).apply {
          isAppearanceLightStatusBars = !darkTheme
          isAppearanceLightNavigationBars = !darkTheme
        }
      }
    }
  }

  val palette = if (darkTheme) DarkAppPalette else LightAppPalette
  CompositionLocalProvider(LocalAppPalette provides palette) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}
