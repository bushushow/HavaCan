package com.example.weather.ui.home

import com.example.weather.domain.insight.WeatherInsight
import com.example.weather.domain.insight.WeatherInsightBuilder
import com.example.weather.model.HourlyWeather
import com.example.weather.model.WeatherCondition
import com.example.weather.ui.home.character.HavaCanEvent
import com.example.weather.ui.home.character.HavaCanState
import com.example.weather.ui.home.character.HavaCanStateMachine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLogicTest {

    private fun hour(h: Int, temp: Double = 15.0, rain: Int = 0, condition: WeatherCondition = WeatherCondition.Clear) =
        HourlyWeather("2026-10-25T%02d:00".format(h), temp, temp, rain, 0.0, condition, true, 5.0, 10.0, 1.0)

    @Test
    fun insightWarnsAboutComingRain() {
        val insight = WeatherInsightBuilder.build(listOf(hour(14), hour(15), hour(16, rain = 70), hour(17, rain = 80)))
        assertEquals(16, (insight as WeatherInsight.RainComing).hour.hour)
    }

    @Test
    fun insightSaysRainNow() {
        val insight = WeatherInsightBuilder.build(listOf(hour(14, condition = WeatherCondition.Rain), hour(15)))
        assertTrue(insight is WeatherInsight.RainNow)
    }

    @Test
    fun dryInsightPointsAtCoolestHour() {
        val insight = WeatherInsightBuilder.build(listOf(hour(14, 22.0), hour(18, 16.0), hour(22, 11.0)))
        assertEquals(22, (insight as WeatherInsight.Dry).coolest.hour)
    }

    @Test
    fun characterStateMachine() {
        val m = HavaCanStateMachine
        assertEquals(HavaCanState.Happy, m.reduce(HavaCanState.Idle, HavaCanEvent.Tapped, runner = false))
        assertEquals(HavaCanState.RunnerTransition, m.reduce(HavaCanState.Idle, HavaCanEvent.ModeChanged(true), runner = true))
        assertEquals(HavaCanState.Running, m.reduce(HavaCanState.RunnerTransition, HavaCanEvent.ReactionFinished, runner = true))
        assertEquals(HavaCanState.Idle, m.reduce(HavaCanState.Running, HavaCanEvent.ModeChanged(false), runner = false))
        // A weather change doesn't cut the mode switch short
        assertEquals(
            HavaCanState.RunnerTransition,
            m.reduce(HavaCanState.RunnerTransition, HavaCanEvent.WeatherChanged, runner = true),
        )
        assertEquals(HavaCanState.Running, m.reduce(HavaCanState.Happy, HavaCanEvent.ReactionFinished, runner = true))
    }

    @Test
    fun shakeNeedsTwoPeaksAndHasCooldown() {
        val d = ShakeDetector()
        val strong = 30f // ~3 g
        assertFalse(d.onReading(strong, 0f, 0f, 1_000))
        assertTrue(d.onReading(strong, 0f, 0f, 1_300))
        // Within cooldown: ignored
        assertFalse(d.onReading(strong, 0f, 0f, 1_600))
        assertFalse(d.onReading(strong, 0f, 0f, 1_900))
        // Gentle movement never counts
        assertFalse(d.onReading(5f, 5f, 9.8f, 4_000))
        assertFalse(d.onReading(strong, 0f, 0f, 5_000))
        assertTrue(d.onReading(strong, 0f, 0f, 5_400))
    }
}
