package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme =
  darkColorScheme(
    primary = PrimaryPurple,
    secondary = SecondaryBronze,
    tertiary = AccentRed,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = DarkBackground,
    onSecondary = DarkBackground,
    onTertiary = DarkBackground,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = TextSecondary,
    outline = SecondaryBronze
  )

val BoxyShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for dark fantasy
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve dark fantasy aesthetic
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = DarkColorScheme, 
    typography = Typography, 
    shapes = BoxyShapes,
    content = content
  )
}
