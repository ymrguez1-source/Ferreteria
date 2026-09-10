package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = TerracottaLight,
    onPrimary = Color.White,
    primaryContainer = TerracottaDark,
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = SteelLight,
    onSecondary = Color.White,
    secondaryContainer = SteelDark,
    onSecondaryContainer = Color(0xFFD1E4FF),
    tertiary = AmberAccent,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = Color(0xFFEDE0DB),
    onSurface = Color(0xFFEDE0DB),
    outline = DarkOutline,
    error = Color(0xFFFFB4AB)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = TerracottaDark,
    secondary = SteelSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1E4FF),
    onSecondaryContainer = SteelDark,
    tertiary = AmberAccent,
    background = WarmBackground,
    surface = WarmSurface,
    surfaceVariant = WarmSurfaceVariant,
    onBackground = Color(0xFF201A18),
    onSurface = Color(0xFF201A18),
    outline = WarmOutline,
    error = CrimsonDanger
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep brand hardware styling consistent
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
