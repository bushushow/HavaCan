package com.example.weather.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = SkyPrimary,
    onPrimary = Color.White,
    primaryContainer = SkyPrimaryContainer,
    onPrimaryContainer = SkyOnPrimaryContainer,
    background = SkyBackground,
    onBackground = SkyOnSurface,
    surface = Color.White,
    onSurface = SkyOnSurface,
    surfaceVariant = SkySurfaceVariant,
    onSurfaceVariant = SkyOnSurfaceVariant,
    outline = SkyOutline,
)

private val DarkColorScheme = darkColorScheme(
    primary = NightPrimary,
    onPrimary = NightOnPrimary,
    primaryContainer = NightPrimaryContainer,
    onPrimaryContainer = NightOnPrimaryContainer,
    background = NightBackground,
    onBackground = NightOnSurface,
    surface = NightSurface,
    onSurface = NightOnSurface,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightOnSurfaceVariant,
    outline = NightOutline,
)

/** Gradient behind the app header; differs between light and dark theme. */
val LocalHeaderGradient = staticCompositionLocalOf { DayHeaderGradient }

/**
 * The app uses its own sky palette instead of wallpaper-based dynamic colour,
 * so the brand looks the same on every device.
 */
@Composable
fun WeatherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalHeaderGradient provides if (darkTheme) NightHeaderGradient else DayHeaderGradient,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content
        )
    }
}
