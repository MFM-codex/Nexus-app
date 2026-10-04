package com.nexus.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light mode: soft grey page, white "cards" on top (like Facebook)
private val LightColors = lightColorScheme(
    primary = Color(0xFF1877F2),
    onPrimary = Color.White,
    background = Color(0xFFF0F2F5),
    surface = Color.White,
    surfaceVariant = Color(0xFFE4E6EB),
    onSurfaceVariant = Color(0xFF65676B),
    secondaryContainer = Color(0xFFE7F3FF),
    onSecondaryContainer = Color(0xFF0B2A5E),
    primaryContainer = Color(0xFFD6E8FF),
    onPrimaryContainer = Color(0xFF0B2A5E),
)

// Dark mode: near-black page, dark grey cards
private val DarkColors = darkColorScheme(
    primary = Color(0xFF4599FF),
    onPrimary = Color(0xFF00214D),
    background = Color(0xFF18191A),
    surface = Color(0xFF242526),
    surfaceVariant = Color(0xFF3A3B3C),
    onSurfaceVariant = Color(0xFFB0B3B8),
    secondaryContainer = Color(0xFF263951),
    onSecondaryContainer = Color(0xFFD6E8FF),
    primaryContainer = Color(0xFF1F3A66),
    onPrimaryContainer = Color(0xFFD6E8FF),
)

// Wraps the app so every screen uses the same colors. Follows the phone's dark mode.
@Composable
fun NexusTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
