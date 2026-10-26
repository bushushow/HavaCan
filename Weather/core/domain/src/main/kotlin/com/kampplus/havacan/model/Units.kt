package com.kampplus.havacan.model

import kotlin.math.roundToInt

enum class TemperatureUnit(val label: String, val symbol: String) {
    Celsius("Celsius", "°C"),
    Fahrenheit("Fahrenheit", "°F");

    fun fromCelsius(c: Double): Double = if (this == Celsius) c else c * 9 / 5 + 32
}

/**
 * A Celsius value in [unit], rounded for display: `"${t.deg(unit)}°"`. Forecasts stay in Celsius
 * (every rule works in Celsius); only what is shown is converted. Screens use the composable
 * `deg()` in ui/theme, which reads the user's unit from the composition.
 */
fun Double.deg(unit: TemperatureUnit): Int = unit.fromCelsius(this).roundToInt()
