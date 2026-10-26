package com.kampplus.havacan.domain.insight

import com.kampplus.havacan.model.HourlyWeather

/** The normal-mode smart card, always derived from real forecast hours. */
sealed interface WeatherInsight {
    /** It's raining (or very likely) in the current hour. */
    data class RainNow(val hour: HourlyWeather) : WeatherInsight

    /** Rain is likely later; [hour] is the first such hour. */
    data class RainComing(val hour: HourlyWeather) : WeatherInsight

    /** No rain expected; [coolest] is the coldest of the coming hours. */
    data class Dry(val coolest: HourlyWeather) : WeatherInsight
}

object WeatherInsightBuilder {
    /** Precipitation probability (%) from which we warn about rain. */
    const val RAIN_PROBABILITY = 50
    private const val RAIN_MM = 0.2
    private const val LOOKAHEAD_HOURS = 12

    fun build(upcoming: List<HourlyWeather>): WeatherInsight? {
        val hours = upcoming.take(LOOKAHEAD_HOURS)
        val now = hours.firstOrNull() ?: return null
        if (now.isRainy()) return WeatherInsight.RainNow(now)
        hours.drop(1).firstOrNull { it.isRainy() }?.let { return WeatherInsight.RainComing(it) }
        return WeatherInsight.Dry(hours.minBy { it.temperature })
    }

    private fun HourlyWeather.isRainy() =
        (precipitationProbability ?: 0) >= RAIN_PROBABILITY || (precipitation ?: 0.0) >= RAIN_MM || condition.isWet
}
