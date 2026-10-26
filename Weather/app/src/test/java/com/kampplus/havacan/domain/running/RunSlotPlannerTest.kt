package com.kampplus.havacan.domain.running

import com.kampplus.havacan.model.CurrentWeather
import com.kampplus.havacan.model.Forecast
import com.kampplus.havacan.model.HourlyWeather
import com.kampplus.havacan.model.WeatherCondition
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunSlotPlannerTest {
    private val planner = RunSlotPlanner()
    private val now = LocalDateTime.parse("2026-10-25T10:00")
    private val tomorrow = LocalDate.parse("2026-10-26")

    private fun hour(date: String, h: Int, feels: Double, rain: Int) = HourlyWeather(
        time = "%sT%02d:00".format(date, h),
        temperature = feels + 1,
        apparentTemperature = feels,
        precipitationProbability = rain,
        precipitation = 0.0,
        condition = if (rain >= 60) WeatherCondition.Rain else WeatherCondition.Clear,
        isDay = h in 7..17,
        windSpeed = 5.0,
        windGusts = 10.0,
        uvIndex = 2.0
    )

    /** Today and tomorrow: 07 cool (89), 08–09 ideal (97), 10–17 warm (77), rain 14–15, dark at night. */
    private fun forecast(rainAllDay: Boolean = false): Forecast {
        val hours = listOf("2026-10-25", "2026-10-26").flatMap { date ->
            (0..23).map { h ->
                val feels = when (h) {
                    7 -> 6.0
                    8, 9 -> 14.0
                    else -> 22.0
                }
                hour(date, h, feels, rain = if (rainAllDay || h in 14..15) 70 else 5)
            }
        }
        return Forecast(
            timezone = "Europe/Istanbul",
            current = CurrentWeather("2026-10-25T10:00", 15.0, 14.0, WeatherCondition.Clear, true, 5.0),
            hourly = hours,
            today = null
        )
    }

    @Test
    fun bestThreeStartsComeFirst() {
        val outlook = planner.outlook(forecast(), tomorrow, 30, now) as RunDayOutlook.Ready
        assertEquals(listOf(8, 9, 7), outlook.suggestions.map { it.start.hour })
        assertEquals(listOf(97, 97, 89), outlook.suggestions.map { it.score }) // the 5 % rain chance costs 3 points
        assertNull(outlook.blocker)
    }

    @Test
    fun pastHoursTodayAreNotSuggested() {
        val outlook = planner.outlook(forecast(), now.toLocalDate(), 30, now) as RunDayOutlook.Ready
        assertTrue(outlook.suggestions.all { it.start.isAfter(now) })
        assertEquals(3, outlook.suggestions.size)
    }

    @Test
    fun longRunIntoRainIsBlocked() {
        val slot = planner.slot(forecast(), LocalDateTime.parse("2026-10-26T13:30"), 60)!!
        assertEquals(listOf(13, 14), slot.hours.map { it.hour })
        assertEquals(RunningBlocker.LikelyRain, slot.blocker)
        assertEquals(0, slot.score)
    }

    @Test
    fun aRainyDayExplainsInsteadOfSuggesting() {
        val outlook = planner.outlook(forecast(rainAllDay = true), tomorrow, 30, now) as RunDayOutlook.Ready
        assertTrue(outlook.suggestions.isEmpty())
        assertEquals(RunningBlocker.LikelyRain, outlook.blocker)
    }

    @Test
    fun daysBeyondTheForecastSayWhen() {
        assertEquals(RunDayOutlook.NoForecastYet(4), planner.outlook(forecast(), LocalDate.parse("2026-10-30"), 30, now))
    }

    @Test
    fun lateTonightThereAreNoStartsLeft() {
        val late = LocalDateTime.parse("2026-10-25T23:30")
        assertEquals(RunDayOutlook.NoHoursLeft, planner.outlook(forecast(), late.toLocalDate(), 30, late))
    }

    @Test
    fun aStartOutsideTheForecastHasNoScore() {
        assertNull(planner.slot(forecast(), LocalDateTime.parse("2026-10-26T23:30"), 60))
    }
}
