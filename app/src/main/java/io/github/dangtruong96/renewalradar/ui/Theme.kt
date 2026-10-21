package io.github.dangtruong96.renewalradar.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B5563),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBFE9F2),
    onPrimaryContainer = Color(0xFF00363F),
    secondary = Color(0xFFB26A00),
    onSecondary = Color.White,
    error = Color(0xFFB3261E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BD3E0),
    onPrimary = Color(0xFF00363F),
    primaryContainer = Color(0xFF004E5A),
    onPrimaryContainer = Color(0xFFBFE9F2),
    secondary = Color(0xFFFFC857),
    onSecondary = Color(0xFF3A2A00),
    error = Color(0xFFF2B8B5)
)

@Composable
fun RenewalRadarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
