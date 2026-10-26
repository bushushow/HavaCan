package com.kampplus.havacan.domain.meet

import com.kampplus.havacan.domain.plans.PlanAdvisor
import com.kampplus.havacan.domain.plans.RiskReason
import com.kampplus.havacan.model.City
import com.kampplus.havacan.model.Forecast
import com.kampplus.havacan.model.HourlyWeather
import com.kampplus.havacan.model.MeetPerson
import com.kampplus.havacan.model.MeetWindow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.max

/** The weather one person has at home at the meeting hour (for getting there). */
data class PersonWeather(
    val person: MeetPerson,
    val hour: HourlyWeather?,
    val risk: RiskReason?
)

/**
 * One possible meet-up: when, where, the weather there, and everyone's weather at home.
 * [comfort] (0–100) only orders the options; the screen shows the weather, not the number.
 */
data class MeetOption(
    val time: LocalDateTime,
    val place: City,
    val hour: HourlyWeather,
    val risk: RiskReason?,
    val people: List<PersonWeather>,
    val comfort: Int
) {
    val everyoneDry: Boolean get() = risk == null && people.none { it.risk != null }
}

sealed interface MeetOutcome {
    /** At least two people are needed to meet. */
    data object NeedPeople : MeetOutcome

    /** The day is past the forecast; it comes into range in [daysUntilAvailable] days. */
    data class NoForecastYet(val daysUntilAvailable: Long) : MeetOutcome

    /** Every chosen hour on that day has passed. */
    data object NoHoursLeft : MeetOutcome

    /** Best first; options without any weather risk always come before risky ones. */
    data class Ready(val options: List<MeetOption>) : MeetOutcome
}

/**
 * Picks the best hour and place for a group from the hourly forecasts. Pure and deterministic.
 * The weather at the meeting place counts most; each person's weather at home (for the trip)
 * counts too. Risk comes from [PlanAdvisor.risk], the same rules Planlarım and Plan B use.
 */
object MeetPlanner {
    private const val FORECAST_DAYS = 16L
    private const val PLACE_WEIGHT = 0.7
    private const val HOME_RISK_PENALTY = 15

    fun plan(
        people: List<MeetPerson>,
        places: List<City>,
        date: LocalDate,
        windows: Set<MeetWindow>,
        forecasts: (City) -> Forecast?,
        now: LocalDateTime,
        count: Int = 3
    ): MeetOutcome {
        if (people.size < 2) return MeetOutcome.NeedPeople
        val days = ChronoUnit.DAYS.between(now.toLocalDate(), date)
        if (days >= FORECAST_DAYS) return MeetOutcome.NoForecastYet(days - FORECAST_DAYS + 1)

        val starts = windows.sortedBy { it.ordinal }
            .flatMap { it.hours }
            .map { date.atTime(it, 0) }
            .filter { it.isAfter(now) }
        if (starts.isEmpty()) return MeetOutcome.NoHoursLeft

        val options = places.distinctBy { it.latitude to it.longitude }.flatMap { place ->
            starts.mapNotNull { start -> option(place, start, people, forecasts) }
        }
        val ranked = options.sortedWith(
            compareByDescending<MeetOption> { it.risk == null }
                .thenByDescending { it.comfort }
                .thenBy { it.time }
        )
        // One option per hour, so the list offers real alternatives rather than the same time twice
        return MeetOutcome.Ready(ranked.distinctBy { it.time }.take(count))
    }

    private fun option(place: City, start: LocalDateTime, people: List<MeetPerson>, forecasts: (City) -> Forecast?): MeetOption? {
        val key = "%sT%02d:00".format(start.toLocalDate(), start.hour)
        val hour = forecasts(place)?.hourly?.firstOrNull { it.time == key } ?: return null
        val home = people.map { person ->
            val h = forecasts(person.city)?.hourly?.firstOrNull { it.time == key }
            PersonWeather(person, h, h?.let(PlanAdvisor::risk))
        }
        val homeComfort = home.mapNotNull { it.hour }.map(::comfort).average().takeIf { !it.isNaN() } ?: comfort(hour).toDouble()
        val penalty = home.count { it.risk != null } * HOME_RISK_PENALTY
        val score = (comfort(hour) * PLACE_WEIGHT + homeComfort * (1 - PLACE_WEIGHT)).toInt() - penalty
        return MeetOption(start, place, hour, PlanAdvisor.risk(hour), home, score.coerceIn(0, 100))
    }

    /** How pleasant an hour is to be outside: mild, dry, calm and light is best. */
    fun comfort(h: HourlyWeather): Int {
        var score = 100.0
        val feels = h.apparentTemperature
        score -= max(0.0, 15 - feels) * 3
        score -= max(0.0, feels - 26) * 4
        score -= (h.precipitationProbability ?: 0) * 0.5
        score -= max(0.0, (h.windSpeed ?: 0.0) - 20) * 1.5
        if (!h.isDay) score -= 10
        return score.coerceIn(0.0, 100.0).toInt()
    }
}
