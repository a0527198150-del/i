package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = MinimalistContainer,
    onPrimary = MinimalistOnContainer,
    primaryContainer = MinimalistContainer,
    onPrimaryContainer = MinimalistOnContainer,
    secondary = MinimalistAccent,
    onSecondary = MinimalistBg,
    background = MinimalistBg,
    surface = Color.White,
    onBackground = MinimalistTextPrimary,
    onSurface = MinimalistTextPrimary,
    error = MinimalistError
  )

private val LightColorScheme =
  lightColorScheme(
    primary = MinimalistAccent,
    onPrimary = Color.White,
    primaryContainer = MinimalistContainer,
    onPrimaryContainer = MinimalistOnContainer,
    secondary = MinimalistAccent,
    onSecondary = Color.White,
    background = MinimalistBg,
    surface = Color.White,
    onBackground = MinimalistTextPrimary,
    onSurface = MinimalistTextPrimary,
    surfaceVariant = MinimalistNavBg,
    onSurfaceVariant = MinimalistTextSecondary,
    outline = MinimalistBorder,
    outlineVariant = MinimalistLightBorder,
    error = MinimalistError
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Set default to false to showcase the custom Clean Minimalism theme exactly as requested
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

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
