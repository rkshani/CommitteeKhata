package com.rk.committee.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Deep green = money / trust. Warm amber = "your turn" highlights.
private val LightColors = lightColorScheme(
    primary = Color(0xFF0B6E4F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8F0D8),
    onPrimaryContainer = Color(0xFF002116),
    secondary = Color(0xFF9A5B00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDB0),
    onSecondaryContainer = Color(0xFF2A1800),
    tertiary = Color(0xFF3D6373),
    onTertiary = Color.White,
    background = Color(0xFFF6F8F4),
    onBackground = Color(0xFF181D1A),
    surface = Color(0xFFF6F8F4),
    onSurface = Color(0xFF181D1A),
    surfaceVariant = Color(0xFFDCE5DD),
    onSurfaceVariant = Color(0xFF404943),
    surfaceContainerLow = Color(0xFFEFF3EE),
    surfaceContainer = Color(0xFFE9EEE8),
    surfaceContainerHigh = Color(0xFFE3E9E3),
    outline = Color(0xFF707973),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

@Composable
fun CommitteeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors, // app is light-only; dark/night mode is not supported
        typography = Typography(),
        content = content,
    )
}
