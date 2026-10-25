package com.example.weather.domain.plans

import com.example.weather.model.Forecast
import com.example.weather.model.HourlyWeather
import com.example.weather.model.Plan
import com.example.weather.model.PlanType
import com.example.weather.model.WeatherCondition
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** Why the weather puts a plan at risk. */
enum class RiskReason(val label: String, val noun: String) {
    Storm("Fırtına riski", "Fırtına"),
    Snow("Kar riski", "Kar"),
    Rain("Yağmur riski", "Yağmur"),
    Wind("Kuvvetli rüzgar", "Rüzgar"),
    Heat("Aşırı sıcak", "Sıcak"),
    Cold("Çok soğuk", "Soğuk"),
}

sealed interface PlanOutlook {
    /** Already happened. */
    data object Past : PlanOutlook

    /** Further out than the forecast reaches (Open-Meteo: 16 days). */
    data class NoForecastYet(val daysUntil: Long) : PlanOutlook

    /** Forecast for the plan's location hasn't loaded (offline, error). */
    data object Unknown : PlanOutlook

    data class Fine(val hour: HourlyWeather) : PlanOutlook

    data class Risk(val hour: HourlyWeather, val reason: RiskReason) : PlanOutlook
}

/**
 * Judges a plan against the hourly forecast and finds a nearby drier hour. Thresholds are
 * HavaCan's product assumptions for an outdoor plan, tunable, not meteorological facts.
 */
object PlanAdvisor {
    const val RAIN_PROBABILITY = 50
    private const val RAIN_MM = 0.5
    private const val SNOW_PROBABILITY = 40
    private const val MAX_GUSTS = 50.0
    private const val HOT_FEELS = 35.0
    private const val COLD_FEELS = -5.0
    /** How far from the original time an alternative may be. */
    private const val ALTERNATIVE_WINDOW_HOURS = 4L
    private const val FORECAST_DAYS = 16L

    fun outlook(plan: Plan, forecast: Forecast?, now: LocalDateTime): PlanOutlook {
        if (plan.dateTime.plusHours(1).isBefore(now)) return PlanOutlook.Past
        val days = ChronoUnit.DAYS.between(now.toLocalDate(), plan.localDate)
        if (days >= FORECAST_DAYS) return PlanOutlook.NoForecastYet(days)
        val hour = forecast?.hourly?.firstOrNull { it.time == plan.hourKey } ?: return PlanOutlook.Unknown
        // Indoor plans are shown with their weather but never flagged
        val reason = if (plan.type == PlanType.Indoor) null else risk(hour)
        return if (reason == null) PlanOutlook.Fine(hour) else PlanOutlook.Risk(hour, reason)
    }

    fun risk(h: HourlyWeather): RiskReason? {
        val chance = h.precipitationProbability ?: 0
        return when {
            h.condition == WeatherCondition.Thunderstorm -> RiskReason.Storm
            h.condition == WeatherCondition.Snow && chance >= SNOW_PROBABILITY -> RiskReason.Snow
            chance >= RAIN_PROBABILITY || (h.precipitation ?: 0.0) >= RAIN_MM || h.condition.isWet -> RiskReason.Rain
            (h.windGusts ?: 0.0) >= MAX_GUSTS -> RiskReason.Wind
            h.apparentTemperature >= HOT_FEELS -> RiskReason.Heat
            h.apparentTemperature <= COLD_FEELS -> RiskReason.Cold
            else -> null
        }
    }

    /**
     * The closest hour on the same day, within ±4 h and not in the past, that carries no risk.
     * Daylight hours are preferred when the plan itself is in daylight. Ties go to the earlier hour.
     */
    fun alternative(plan: Plan, forecast: Forecast?, now: LocalDateTime): HourlyWeather? {
        val hours = forecast?.hourly ?: return null
        val original = hours.firstOrNull { it.time == plan.hourKey }
        val planHour = plan.localTime.hour
        return hours
            .asSequence()
            .filter { it.date == plan.date && it.time != plan.hourKey }
            .filter { kotlin.math.abs(it.hour - planHour) <= ALTERNATIVE_WINDOW_HOURS }
            .filter { LocalDateTime.parse(it.time).isAfter(now) }
            .filter { original == null || !original.isDay || it.isDay }
            .filter { risk(it) == null }
            .sortedWith(compareBy({ kotlin.math.abs(it.hour - planHour) }, { it.hour }))
            .firstOrNull()
    }
}
