package com.nexus.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B6EF3),
    onPrimary = Color.White,
    secondaryContainer = Color(0xFFD9E6FF),
    onSecondaryContainer = Color(0xFF0B2A5E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB4FF),
    onPrimary = Color(0xFF002A6B),
    secondaryContainer = Color(0xFF1F3A66),
    onSecondaryContainer = Color(0xFFD9E6FF),
)

// Wraps the app so every screen uses the same colors. Follows the phone's dark mode.
@Composable
fun NexusTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
