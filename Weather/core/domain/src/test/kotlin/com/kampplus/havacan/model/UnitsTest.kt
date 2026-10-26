package com.kampplus.havacan.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitsTest {
    @Test
    fun celsiusIsShownAsIs() {
        assertEquals(16, 16.4.deg(TemperatureUnit.Celsius))
        assertEquals(-3, (-2.6).deg(TemperatureUnit.Celsius))
    }

    @Test
    fun fahrenheitConvertsEveryShownValue() {
        val f = TemperatureUnit.Fahrenheit
        assertEquals(32, 0.0.deg(f))
        assertEquals(61, 16.0.deg(f)) // 60.8
        assertEquals(-4, (-20.0).deg(f))
    }
}
