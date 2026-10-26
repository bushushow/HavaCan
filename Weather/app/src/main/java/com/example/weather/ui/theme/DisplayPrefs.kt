package com.example.weather.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Profil > Erişilebilirlik choices, read while drawing so the whole app follows them at once. */
object DisplayPrefs {
    /** Text 20 % larger on top of Android's own font size. */
    var largeText by mutableStateOf(false)
    /** Secondary text in full navy instead of the softer grey-blue. */
    var highContrast by mutableStateOf(false)
    /** Stops scene animations even when Android's animation setting is on. */
    var reduceMotion by mutableStateOf(false)
}
