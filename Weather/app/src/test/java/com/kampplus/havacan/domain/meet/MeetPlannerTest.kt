package com.kampplus.havacan.domain.meet

import com.kampplus.havacan.domain.plans.RiskReason
import com.kampplus.havacan.model.City
import com.kampplus.havacan.model.CurrentWeather
import com.kampplus.havacan.model.Forecast
import com.kampplus.havacan.model.HourlyWeather
import com.kampplus.havacan.model.MeetPerson
import com.kampplus.havacan.model.MeetWindow
import com.kampplus.havacan.model.WeatherCondition
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeetPlannerTest {
    private val now = LocalDateTime.parse("2026-10-25T10:00")
    private val saturday = LocalDate.parse("2026-10-31")
    private val kadikoy = City(1, "Kadıköy", 40.99, 29.03)
    private val besiktas = City(2, "Beşiktaş", 41.04, 29.0)
    private val ali = MeetPerson("Ali", kadikoy)
    private val ayse = MeetPerson("Ayşe", besiktas)

    /** Mild and dry all day, except: rain in [wet] hours, and [cold] hours feel 4°. */
    private fun forecast(wet: Set<Int> = emptySet(), cold: Set<Int> = emptySet()) = Forecast(
        timezone = "Europe/Istanbul",
        current = CurrentWeather("2026-10-25T10:00", 18.0, 18.0, WeatherCondition.Clear, true, 5.0),
        hourly = (0L..7L).flatMap { d ->
            val date = now.toLocalDate().plusDays(d)
            (0..23).map { h ->
                HourlyWeather(
                    time = "%sT%02d:00".format(date, h),
                    temperature = 19.0,
                    apparentTemperature = if (h in cold) 4.0 else 19.0,
                    precipitationProbability = if (h in wet) 80 else 5,
                    precipitation = if (h in wet) 2.0 else 0.0,
                    condition = if (h in wet) WeatherCondition.Rain else WeatherCondition.Clear,
                    isDay = h in 7..18,
                    windSpeed = 8.0,
                    windGusts = 15.0,
                    uvIndex = 2.0
                )
            }
        },
        today = null
    )

    private fun plan(forecasts: Map<City, Forecast>, windows: Set<MeetWindow> = setOf(MeetWindow.Afternoon), date: LocalDate = saturday) =
        MeetPlanner.plan(listOf(ali, ayse), listOf(kadikoy, besiktas), date, windows, { forecasts[it] }, now)

    @Test
    fun dryHoursWinOverRainyOnes() {
        val result = plan(mapOf(kadikoy to forecast(wet = setOf(12, 13)), besiktas to forecast(wet = setOf(12, 13)))) as MeetOutcome.Ready
        assertEquals(3, result.options.size)
        assertTrue(result.options.all { it.everyoneDry })
        assertTrue(result.options.none { it.time.hour in 12..13 })
    }

    @Test
    fun theDrierPlaceIsChosen() {
        val result = plan(mapOf(kadikoy to forecast(wet = (12..16).toSet()), besiktas to forecast())) as MeetOutcome.Ready
        assertEquals("Beşiktaş", result.options.first().place.name)
    }

    @Test
    fun rainAtSomeonesHomeIsShownAndCostsTheOption() {
        val result = plan(mapOf(kadikoy to forecast(wet = setOf(14)), besiktas to forecast())) as MeetOutcome.Ready
        val best = result.options.first()
        assertTrue(best.everyoneDry)
        assertFalse(best.time.hour == 14)
    }

    @Test
    fun whenEveryHourIsRainyTheRiskIsSaid() {
        val rain = (0..23).toSet()
        val result = plan(mapOf(kadikoy to forecast(wet = rain), besiktas to forecast(wet = rain))) as MeetOutcome.Ready
        assertEquals(RiskReason.Rain, result.options.first().risk)
    }

    @Test
    fun oneHourIsOfferedOnce() {
        val result = plan(mapOf(kadikoy to forecast(), besiktas to forecast())) as MeetOutcome.Ready
        assertEquals(result.options.size, result.options.map { it.time }.distinct().size)
    }

    @Test
    fun aMildHourBeatsAColdOne() {
        val cold = (12..15).toSet()
        val result = plan(mapOf(kadikoy to forecast(cold = cold), besiktas to forecast(cold = cold))) as MeetOutcome.Ready
        assertEquals(16, result.options.first().time.hour)
    }

    @Test
    fun aMeetNeedsTwoPeople() {
        val result = MeetPlanner.plan(listOf(ali), listOf(kadikoy), saturday, setOf(MeetWindow.Evening), { forecast() }, now)
        assertEquals(MeetOutcome.NeedPeople, result)
    }

    @Test
    fun daysBeyondTheForecastSayWhen() {
        val result = plan(emptyMap(), date = now.toLocalDate().plusDays(18))
        assertEquals(MeetOutcome.NoForecastYet(3), result)
    }

    @Test
    fun passedHoursTodayAreLeftOut() {
        val noon = now.withHour(12)
        val result = MeetPlanner.plan(listOf(ali, ayse), listOf(kadikoy), noon.toLocalDate(), setOf(MeetWindow.Morning), {
            forecast()
        }, noon)
        assertEquals(MeetOutcome.NoHoursLeft, result)
    }
}
