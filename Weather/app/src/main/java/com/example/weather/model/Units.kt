package com.example.weather.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt

enum class TemperatureUnit(val label: String, val symbol: String) {
    Celsius("Celsius", "°C"),
    Fahrenheit("Fahrenheit", "°F");

    fun fromCelsius(c: Double): Double = if (this == Celsius) c else c * 9 / 5 + 32
}

/**
 * The unit every screen, notification and spoken answer uses. Forecasts stay in Celsius (all
 * rules work in Celsius); only what's shown is converted. Snapshot state, so screens redraw at once.
 */
object Units {
    var temperature by mutableStateOf(TemperatureUnit.Celsius)
}

/** A Celsius value in the chosen unit, rounded for display: `"${t.deg()}°"`. */
fun Double.deg(): Int = Units.temperature.fromCelsius(this).roundToInt()
