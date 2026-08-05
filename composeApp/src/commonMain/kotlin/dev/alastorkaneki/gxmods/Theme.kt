package dev.alastorkaneki.gxmods

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GxDarkColors = darkColorScheme(
    primary = Color(0xFFFF2D78),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF53102B),
    onPrimaryContainer = Color(0xFFFFD9E4),
    secondary = Color(0xFFC98BFF),
    onSecondary = Color(0xFF25003D),
    secondaryContainer = Color(0xFF35124A),
    onSecondaryContainer = Color(0xFFF0D9FF),
    tertiary = Color(0xFF59E6FF),
    background = Color(0xFF07050A),
    onBackground = Color(0xFFF7F0FA),
    surface = Color(0xFF0E0A12),
    onSurface = Color(0xFFF7F0FA),
    surfaceVariant = Color(0xFF1B1421),
    onSurfaceVariant = Color(0xFFD0C2D5),
    outline = Color(0xFF6C566F),
    outlineVariant = Color(0xFF382A3C),
    error = Color(0xFFFF6B78),
)

@Composable
fun GxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GxDarkColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
