package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NetraAmberPrimary,
    onPrimary = NetraNavyDeep,
    primaryContainer = NetraNavySurface,
    onPrimaryContainer = NetraAmberPrimary,
    secondary = NetraAmberWarm,
    onSecondary = NetraNavyDeep,
    secondaryContainer = NetraNavyCard,
    onSecondaryContainer = NewsprintIvory,
    tertiary = Color(0xFF4ADE80),
    onTertiary = NetraNavyDeep,
    background = NetraNavyDeep,
    onBackground = NewsprintIvory,
    surface = NetraNavyPrimary,
    onSurface = NewsprintIvory,
    surfaceVariant = NetraNavyCard,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF334155),
    error = Color(0xFFF87171),
    onError = NetraNavyDeep
)

private val LightColorScheme = lightColorScheme(
    primary = NetraNavyPrimary,
    onPrimary = NewsprintIvory,
    primaryContainer = NetraAmberMuted,
    onPrimaryContainer = NetraNavyDeep,
    secondary = NetraAmberDeep,
    onSecondary = Color.White,
    secondaryContainer = NewsprintPaper,
    onSecondaryContainer = InkCharcoal,
    tertiary = VerifiedEmerald,
    onTertiary = Color.White,
    background = NewsprintIvory,
    onBackground = InkCharcoal,
    surface = Color.White,
    onSurface = InkCharcoal,
    surfaceVariant = NewsprintPaper,
    onSurfaceVariant = InkSlate,
    outline = NewsprintRule,
    error = BreakingCrimson,
    onError = Color.White
)

@Composable
fun NetraNewsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    survivalLowPowerMode: Boolean = false,
    content: @Composable () -> Unit
) {
    // In Survival power tier (<5% battery), prefer high-contrast dark OLED scheme to save display mAh
    val colorScheme = if (survivalLowPowerMode || darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
