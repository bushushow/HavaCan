package com.example.weather.model

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitsTest {
    @After
    fun reset() {
        Units.temperature = TemperatureUnit.Celsius
    }

    @Test
    fun celsiusIsShownAsIs() {
        assertEquals(16, 16.4.deg())
        assertEquals(-3, (-2.6).deg())
    }

    @Test
    fun fahrenheitConvertsEveryShownValue() {
        Units.temperature = TemperatureUnit.Fahrenheit
        assertEquals(32, 0.0.deg())
        assertEquals(61, 16.0.deg()) // 60.8
        assertEquals(-4, (-20.0).deg())
    }
}
