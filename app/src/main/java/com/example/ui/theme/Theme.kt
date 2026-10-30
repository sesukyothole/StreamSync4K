package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    primaryContainer = NeonCyanDark.copy(alpha = 0.25f),
    onPrimaryContainer = NeonCyanLight,
    secondary = NeonViolet,
    onSecondary = Color.Black,
    secondaryContainer = NeonVioletDark.copy(alpha = 0.25f),
    onSecondaryContainer = Color.White,
    tertiary = AccentGold4K,
    background = ObsidianBlack,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextMuted,
    error = LiveRed,
    onError = Color.White
)

@Composable
fun StreamSyncTheme(
    content: @Composable () -> Unit
) {
    // Media streaming is best experienced in dark mode
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
