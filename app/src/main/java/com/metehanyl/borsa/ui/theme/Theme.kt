package com.metehanyl.borsa.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = AccentBlue,
    background = NavyBackground,
    surface = NavySurface,
    surfaceVariant = NavySurfaceVariant,
    onBackground = OnNavy,
    onSurface = OnNavy,
    error = SellRed
)

private val LightColors = lightColorScheme(
    primary = AccentBlue,
    background = LightBackground,
    surface = LightSurface,
    onBackground = OnLight,
    onSurface = OnLight,
    error = SellRed
)

@Composable
fun KureselBorsaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
