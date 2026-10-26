package com.kampplus.havacan.ui.home

import com.kampplus.havacan.data.provinceCities
import com.kampplus.havacan.domain.insight.WeatherInsightBuilder
import com.kampplus.havacan.model.CurrentWeather
import com.kampplus.havacan.model.Forecast
import com.kampplus.havacan.model.HourlyWeather
import com.kampplus.havacan.model.TemperatureUnit
import com.kampplus.havacan.model.WeatherCondition
import com.kampplus.havacan.ui.common.LoadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HavaCanTipsTest {
    private fun hour(h: Int, rain: Int, feels: Double = 15.0) = HourlyWeather(
        time = "2026-10-26T%02d:00".format(h),
        temperature = 16.0,
        apparentTemperature = feels,
        precipitationProbability = rain,
        precipitation = 0.0,
        condition = if (rain >= 60) WeatherCondition.Rain else WeatherCondition.Clear,
        isDay = true,
        windSpeed = 5.0,
        windGusts = 8.0,
        uvIndex = 2.0
    )

    private fun state(hours: List<HourlyWeather>): HomeUiState {
        val forecast =
            Forecast("Europe/Istanbul", CurrentWeather("2026-10-26T10:00", 16.0, 15.0, WeatherCondition.Clear, true, 5.0), hours, null)
        return HomeUiState(
            city = provinceCities.first(),
            load = LoadState.Success(forecast),
            insight = WeatherInsightBuilder.build(forecast.upcoming)
        )
    }

    @Test
    fun rainComingIsTheFirstTip() {
        val s = state(listOf(hour(10, 5), hour(11, 5), hour(15, 70)))
        assertTrue(s.hasRainNews())
        assertEquals("Saat 15.00 gibi yağmur bekleniyor (%70).", havaCanTips(s, TemperatureUnit.Celsius).first())
    }

    @Test
    fun dryDayNamesTheCoolestHourAndColdGetsAdvice() {
        val s = state(listOf(hour(10, 5, feels = 3.0), hour(11, 5)))
        assertFalse(s.hasRainNews())
        val tips = havaCanTips(s, TemperatureUnit.Celsius)
        assertTrue(tips.first().startsWith("Yağmur yok!"))
        assertTrue(tips.any { it.contains("Bere ve eldiven") })
    }
}
