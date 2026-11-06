package com.rk.committee.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
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

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FD8B0),
    onPrimary = Color(0xFF003826),
    primaryContainer = Color(0xFF00513A),
    onPrimaryContainer = Color(0xFFB8F0D8),
    secondary = Color(0xFFF5BD6B),
    onSecondary = Color(0xFF462A00),
    secondaryContainer = Color(0xFF643F00),
    onSecondaryContainer = Color(0xFFFFDDB0),
    tertiary = Color(0xFFA5CCDF),
    background = Color(0xFF101512),
    onBackground = Color(0xFFDFE4DF),
    surface = Color(0xFF101512),
    onSurface = Color(0xFFDFE4DF),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFBFC9C1),
    surfaceContainerLow = Color(0xFF181D1A),
    surfaceContainer = Color(0xFF1C211E),
    surfaceContainerHigh = Color(0xFF262B28),
    outline = Color(0xFF8A938C),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun CommitteeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = Typography(),
        content = content,
    )
}
