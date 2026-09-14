package com.rnsgate.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = RnsGreen,
    onPrimary = Color(0xFF003822),
    primaryContainer = RnsGreenDim,
    onPrimaryContainer = RnsOnBg,
    secondary = RnsAmber,
    onSecondary = Color(0xFF1C1400),
    background = RnsBg,
    onBackground = RnsOnBg,
    surface = RnsSurface,
    onSurface = RnsOnBg,
    surfaceVariant = RnsSurfaceHigh,
    onSurfaceVariant = RnsMuted,
    outline = Color(0xFF30363D),
    error = RnsDanger
)

private val LightColors = lightColorScheme(
    primary = RnsGreenDim,
    onPrimary = Color.White,
    background = Color(0xFFF6F8FA),
    onBackground = Color(0xFF1F2328),
    surface = Color.White,
    onSurface = Color(0xFF1F2328)
)

@Composable
fun RnsGateTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
