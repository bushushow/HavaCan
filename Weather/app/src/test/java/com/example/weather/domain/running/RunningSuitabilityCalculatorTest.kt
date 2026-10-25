package com.example.weather.domain.running

import com.example.weather.model.HourlyWeather
import com.example.weather.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunningSuitabilityCalculatorTest {
    private val calc = RunningSuitabilityCalculator()

    private fun hour(
        h: Int,
        date: String = "2026-10-26",
        feels: Double = 14.0,
        rain: Int = 0,
        mm: Double = 0.0,
        wind: Double = 5.0,
        gusts: Double = 10.0,
        uv: Double = 2.0,
        day: Boolean = true,
        condition: WeatherCondition = WeatherCondition.Clear,
    ) = HourlyWeather(
        time = "%sT%02d:00".format(date, h),
        temperature = feels + 1,
        apparentTemperature = feels,
        precipitationProbability = rain,
        precipitation = mm,
        condition = condition,
        isDay = day,
        windSpeed = wind,
        windGusts = gusts,
        uvIndex = uv,
    )

    @Test
    fun idealHourScoresFull() {
        assertEquals(100, calc.score(hour(8)).score)
    }

    @Test
    fun hardLimitsBlockAnHour() {
        assertEquals(RunningBlocker.Dark, calc.score(hour(3, day = false)).blocker)
        assertEquals(RunningBlocker.Thunderstorm, calc.score(hour(8, condition = WeatherCondition.Thunderstorm)).blocker)
        assertEquals(RunningBlocker.LikelyRain, calc.score(hour(8, rain = 60)).blocker)
        assertEquals(RunningBlocker.StrongGusts, calc.score(hour(8, gusts = 55.0)).blocker)
        assertEquals(RunningBlocker.TooHot, calc.score(hour(8, feels = 33.0)).blocker)
        assertEquals(RunningBlocker.TooCold, calc.score(hour(8, feels = -8.0)).blocker)
        assertEquals(0, calc.score(hour(8, rain = 60)).score)
    }

    @Test
    fun penaltiesAddUp() {
        // 4 °C below comfort (-16), 30% rain chance (-18), wind 5 km/h over calm (-7.5)
        assertEquals(58, calc.score(hour(8, feels = 4.0, rain = 30, wind = 20.0)).score)
    }

    @Test
    fun picksBestConsecutiveWindowAndLabelsIt() {
        val hours = listOf(
            hour(6, feels = 3.0), // 80 → good but cold
            hour(7), hour(8), hour(9), // 100s
            hour(10, feels = 22.0), // 80
            hour(11, rain = 70), // blocked
        )
        val advice = calc.advise(hours, todayDate = "2026-10-25")
        advice as RunningAdvice.BestWindow
        assertEquals(listOf(7, 8, 9), advice.window.hours.map { it.hour.hour })
        assertEquals("07.00–10.00", advice.window.rangeLabel)
        assertEquals("Yarın sabah", advice.window.dayLabel)
        assertEquals(listOf("Ilık", "Rüzgarsız", "Yağış beklenmiyor"), advice.window.reasons)
    }

    @Test
    fun todayLabelAndColdReason() {
        val advice = calc.advise(listOf(hour(16, date = "2026-10-25", feels = 9.0)), todayDate = "2026-10-25")
        advice as RunningAdvice.BestWindow
        assertEquals("Bugün öğleden sonra", advice.window.dayLabel)
        assertEquals("Serin", advice.window.reasons.first())
    }

    @Test
    fun noGoodTimeReportsMostCommonBlocker() {
        val hours = listOf(hour(9, rain = 80), hour(10, rain = 90), hour(11, gusts = 60.0), hour(22, day = false))
        val advice = calc.advise(hours, todayDate = "2026-10-26")
        advice as RunningAdvice.NoGoodTime
        assertEquals(RunningBlocker.LikelyRain, advice.mainBlocker)
    }

    @Test
    fun onlyLooksAheadTwentyFourHours() {
        val night = (0 until 24).map { hour(it % 24, day = false) }
        val later = hour(10, date = "2026-10-27")
        val advice = calc.advise(night + later, todayDate = "2026-10-26")
        assertTrue(advice is RunningAdvice.NoGoodTime)
    }
}
