package com.kampplus.havacan.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.kampplus.havacan.model.TemperatureUnit
import com.kampplus.havacan.model.deg

/**
 * Profil choices that change how every screen looks. MainActivity provides them from the
 * ProfileStore flow, so a change in Profil redraws the app at once without global state.
 */
data class DisplaySettings(
    val unit: TemperatureUnit = TemperatureUnit.Celsius,
    /** Text 20 % larger on top of Android's own font size. */
    val largeText: Boolean = false,
    /** Secondary text in the main ink colour instead of the softer one. */
    val highContrast: Boolean = false,
    /** Stops scene animations even when Android's animation setting is on. */
    val reduceMotion: Boolean = false
)

val LocalDisplaySettings = staticCompositionLocalOf { DisplaySettings() }

/** A Celsius value in the user's unit, for screens: `"${t.deg()}°"`. */
@Composable
@ReadOnlyComposable
fun Double.deg(): Int = deg(LocalDisplaySettings.current.unit)
