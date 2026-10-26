package com.kampplus.havacan.ui.home.components

import com.kampplus.havacan.model.CurrentWeather
import com.kampplus.havacan.model.Forecast
import com.kampplus.havacan.model.HourlyWeather
import com.kampplus.havacan.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Test

class ForecastCardTest {
    private fun hour(date: String, h: Int) = HourlyWeather(
        time = "%sT%02d:00".format(date, h),
        temperature = 15.0,
        apparentTemperature = 14.0,
        precipitationProbability = 10,
        precipitation = 0.0,
        condition = WeatherCondition.Clear,
        isDay = h in 7..18,
        windSpeed = 3.0,
        windGusts = 6.0,
        uvIndex = 1.0,
        humidity = 84
    )

    private val forecast = Forecast(
        timezone = "Europe/Istanbul",
        current = CurrentWeather("2026-10-26T15:20", 16.0, 15.0, WeatherCondition.Clear, true, 3.0, 84),
        hourly = listOf("2026-10-26", "2026-10-27", "2026-10-28").flatMap { d -> (0..23).map { hour(d, it) } },
        today = null
    )

    @Test
    fun todayShowsTheNext24HoursAcrossMidnight() {
        val hours = chartHours(forecast, "2026-10-26")
        assertEquals(24, hours.size)
        assertEquals("2026-10-26T15:00", hours.first().time)
        assertEquals("2026-10-27T14:00", hours.last().time)
    }

    @Test
    fun anotherDayShowsThatWholeDay() {
        val hours = chartHours(forecast, "2026-10-27")
        assertEquals(24, hours.size)
        assertEquals("2026-10-27T00:00", hours.first().time)
    }

    @Test
    fun pickingADaySelectsNowOrMidday() {
        assertEquals("2026-10-26T15:00", hourForDay(forecast, "2026-10-26")?.time)
        assertEquals("2026-10-28T12:00", hourForDay(forecast, "2026-10-28")?.time)
    }

    @Test
    fun dayNames() {
        assertEquals("Bugün", dayName("2026-10-26", "2026-10-26"))
        assertEquals("Sal", dayName("2026-10-27", "2026-10-26"))
        assertEquals("Salı", dayName("2026-10-27", "2026-10-26", full = true))
    }

    @Test
    fun onlyForecastDaysCanBePicked() {
        assertEquals(setOf("2026-10-26", "2026-10-27", "2026-10-28"), pickableDates(forecast))
    }
}
