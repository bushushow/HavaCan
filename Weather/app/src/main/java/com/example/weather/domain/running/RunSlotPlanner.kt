package com.example.weather.domain.running

import com.example.weather.model.Forecast
import com.example.weather.model.HourlyWeather
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * A possible run start. [score] is the average of the 0–100 hour scores the run covers
 * (RunningSuitabilityCalculator); a run that touches a blocked hour scores 0 and carries the
 * [blocker]. [hours] are the forecast hours the run overlaps, first one first.
 */
data class RunSlot(
    val start: LocalDateTime,
    val score: Int,
    val blocker: RunningBlocker?,
    val hours: List<HourlyWeather>,
) {
    val first: HourlyWeather get() = hours.first()
}

/** What Koşu Planla can say about one day. */
sealed interface RunDayOutlook {
    /** Best starts first; [suggestions] is empty only when [blocker] explains why. */
    data class Ready(val suggestions: List<RunSlot>, val blocker: RunningBlocker?) : RunDayOutlook

    /** The day is past the end of the forecast; it reaches the day in [daysUntilAvailable] days. */
    data class NoForecastYet(val daysUntilAvailable: Long) : RunDayOutlook

    /** It's today and every start hour has passed. */
    data object NoHoursLeft : RunDayOutlook
}

/**
 * Picks run start times for a day from the hourly forecast. Pure and deterministic: same
 * forecast, same answer. Scores are HavaCan's comfort estimate, not a safety guarantee.
 */
class RunSlotPlanner(private val calculator: RunningSuitabilityCalculator = RunningSuitabilityCalculator()) {

    fun outlook(forecast: Forecast, date: LocalDate, durationMinutes: Int, now: LocalDateTime, count: Int = 3): RunDayOutlook {
        val dates = forecast.hourly.map { it.date }
        val lastDate = dates.maxOrNull()?.let(LocalDate::parse) ?: return RunDayOutlook.NoForecastYet(0)
        if (date.isAfter(lastDate)) return RunDayOutlook.NoForecastYet(ChronoUnit.DAYS.between(lastDate, date))

        val starts = forecast.hourly
            .filter { it.date == date.toString() }
            .map { LocalDateTime.parse(it.time) }
            .filter { it.isAfter(now) }
        if (starts.isEmpty()) return RunDayOutlook.NoHoursLeft

        val slots = starts.mapNotNull { slot(forecast, it, durationMinutes) }
        val good = slots.filter { it.blocker == null && it.score > 0 }
            .sortedWith(compareByDescending<RunSlot> { it.score }.thenBy { it.start })
            .take(count)
        val blocker = if (good.isNotEmpty()) {
            null
        } else {
            // The most common reason, ignoring darkness when something else is going on
            val reasons = slots.mapNotNull { it.blocker }
            (reasons.filter { it != RunningBlocker.Dark }.ifEmpty { reasons })
                .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        }
        return RunDayOutlook.Ready(good, blocker)
    }

    /** Scores a run starting at [start]; null when the forecast doesn't cover all of it. */
    fun slot(forecast: Forecast, start: LocalDateTime, durationMinutes: Int): RunSlot? {
        val end = start.plusMinutes(durationMinutes.coerceAtLeast(1).toLong() - 1)
        val byKey = forecast.hourly.associateBy { it.time }
        val hours = generateSequence(start.truncatedTo(ChronoUnit.HOURS)) { it.plusHours(1) }
            .takeWhile { !it.isAfter(end) }
            .map { byKey[KEY.format(it)] }
            .toList()
            .let { found -> if (found.any { it == null }) return null else found.filterNotNull() }
        val scores = hours.map(calculator::score)
        val blocker = scores.firstNotNullOfOrNull { it.blocker }
        val score = if (blocker != null) 0 else scores.map { it.score }.average().toInt()
        return RunSlot(start, score, blocker, hours)
    }

    private companion object {
        val KEY: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00")
    }
}

/** The start hour of a run, e.g. 07:00, for the hour cards. */
val RunSlot.time: LocalTime get() = start.toLocalTime()
