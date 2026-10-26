package com.kampplus.havacan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = HavaBlue,
    onPrimary = Color.White,
    primaryContainer = HavaBlueLight,
    onPrimaryContainer = HavaBlueDeep,
    secondary = Sun,
    onSecondary = Navy,
    secondaryContainer = SunLight,
    onSecondaryContainer = Navy,
    tertiary = GoodGreen,
    background = Ivory,
    onBackground = Navy,
    surface = Color.White,
    onSurface = Navy,
    surfaceVariant = SkyTint,
    onSurfaceVariant = NavySoft,
    outline = Color(0xFFD9DEEA),
    error = RiskRed
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6E9BFF),
    onPrimary = Navy,
    primaryContainer = Color(0xFF1C3A8A),
    onPrimaryContainer = HavaBlueLight,
    secondary = Sun,
    onSecondary = Navy,
    secondaryContainer = Color(0xFF4A3A00),
    onSecondaryContainer = SunLight,
    tertiary = GoodGreen,
    background = NightBackground,
    onBackground = NightOnSurface,
    surface = NightSurface,
    onSurface = NightOnSurface,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightOnSurfaceVariant,
    outline = Color(0xFF2E3B6B),
    error = Color(0xFFFF6B6F)
)

/** Gradient behind the city search header; differs between light and dark theme. */
val LocalHeaderGradient = staticCompositionLocalOf { DayHeaderGradient }

/**
 * The app uses its own HavaCan palette instead of wallpaper-based dynamic colour,
 * so the brand looks the same on every device.
 */
@Composable
fun WeatherTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalHeaderGradient provides if (darkTheme) NightHeaderGradient else DayHeaderGradient
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content
        )
    }
}
