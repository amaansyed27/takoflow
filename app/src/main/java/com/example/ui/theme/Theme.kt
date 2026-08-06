package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TakoFlowColorScheme = darkColorScheme(
    primary = PrimaryAmber,
    onPrimary = OnPrimaryDark,
    primaryContainer = SurfaceContainerHigh,
    onPrimaryContainer = PrimaryLight,
    background = DarkBackground,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceContainerHighest,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    secondary = SecondaryCharcoal,
    secondaryContainer = SecondaryContainerDark,
    error = ErrorRed,
    scrim = Color.Black
)

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TakoFlowColorScheme, typography = Typography, content = content)
}
