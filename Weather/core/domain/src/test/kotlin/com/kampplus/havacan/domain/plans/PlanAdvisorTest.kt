package com.kampplus.havacan.domain.plans

import com.kampplus.havacan.model.City
import com.kampplus.havacan.model.CurrentWeather
import com.kampplus.havacan.model.Forecast
import com.kampplus.havacan.model.HourlyWeather
import com.kampplus.havacan.model.Plan
import com.kampplus.havacan.model.PlanType
import com.kampplus.havacan.model.WeatherCondition
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanAdvisorTest {
    private val ankara = City(323786, "Ankara", 39.91987, 32.85427, "TR", "Türkiye", "Ankara")
    private val now = LocalDateTime.parse("2026-10-25T10:00")

    private fun hour(h: Int, rain: Int = 0, condition: WeatherCondition = WeatherCondition.Clear, day: Boolean = h in 7..17) =
        HourlyWeather("2026-10-25T%02d:00".format(h), 15.0, 14.0, rain, 0.0, condition, day, 5.0, 12.0, 2.0)

    private fun forecast(vararg hours: HourlyWeather) = Forecast(
        timezone = "Europe/Istanbul",
        current = CurrentWeather("2026-10-25T10:00", 15.0, 14.0, WeatherCondition.Clear, true, 5.0),
        hourly = hours.toList(),
        today = null
    )

    private fun plan(time: String = "17:00", date: String = "2026-10-25", type: PlanType = PlanType.Outdoor) =
        Plan("p", "Parkta buluşma", date, time, ankara, type = type)

    private val rainyEvening = forecast(
        hour(12),
        hour(13),
        hour(14),
        hour(15, rain = 20),
        hour(16, rain = 55),
        hour(17, rain = 70, condition = WeatherCondition.Rain),
        hour(18, rain = 60),
        hour(19, rain = 10, day = false)
    )

    @Test
    fun rainyHourIsARisk() {
        val outlook = PlanAdvisor.outlook(plan(), rainyEvening, now)
        assertEquals(RiskReason.Rain, (outlook as PlanOutlook.Risk).reason)
    }

    @Test
    fun indoorPlansAreNeverFlagged() {
        assertTrue(PlanAdvisor.outlook(plan(type = PlanType.Indoor), rainyEvening, now) is PlanOutlook.Fine)
    }

    @Test
    fun alternativeIsClosestDryDaylightHour() {
        // 16 and 18 are rainy, 19 is dry but dark (plan is in daylight) → 15
        assertEquals(15, PlanAdvisor.alternative(plan(), rainyEvening, now)!!.hour)
    }

    @Test
    fun noAlternativeWhenAllNearbyHoursAreWet() {
        val wet = forecast(*(13..21).map { hour(it, rain = 80) }.toTypedArray())
        assertNull(PlanAdvisor.alternative(plan(), wet, now))
    }

    @Test
    fun pastHoursAreNotSuggested() {
        val lateNow = LocalDateTime.parse("2026-10-25T15:30")
        // 15 has passed, 14 and 13 too → nothing dry left within the window
        assertNull(PlanAdvisor.alternative(plan(), rainyEvening, lateNow))
    }

    @Test
    fun beyondSixteenDaysHasNoForecastYet() {
        val outlook = PlanAdvisor.outlook(plan(date = "2026-11-20"), rainyEvening, now)
        assertEquals(26L, (outlook as PlanOutlook.NoForecastYet).daysUntil)
    }

    @Test
    fun pastPlanAndMissingForecast() {
        assertTrue(PlanAdvisor.outlook(plan(time = "08:00"), rainyEvening, now) is PlanOutlook.Past)
        assertTrue(PlanAdvisor.outlook(plan(), null, now) is PlanOutlook.Unknown)
    }

    @Test
    fun stormAndWindReasons() {
        assertEquals(RiskReason.Storm, PlanAdvisor.risk(hour(17, condition = WeatherCondition.Thunderstorm)))
        assertEquals(RiskReason.Wind, PlanAdvisor.risk(hour(17).copy(windGusts = 60.0)))
        assertNull(PlanAdvisor.risk(hour(17, rain = 30)))
    }
}
