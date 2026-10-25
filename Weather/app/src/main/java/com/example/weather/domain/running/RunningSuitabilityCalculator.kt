package com.example.weather.domain.running

import com.example.weather.model.HourlyWeather
import com.example.weather.model.WeatherCondition

/**
 * Product thresholds for "is this a good hour to run?". These are HavaCan's own starting
 * assumptions for a general recreational runner, not meteorological or medical facts, and are
 * meant to be tuned (or personalised later). Units: °C, km/h, mm, %.
 */
data class RunningThresholds(
    /** Apparent temperature range that costs no points. */
    val comfortableMin: Double = 8.0,
    val comfortableMax: Double = 18.0,
    val coldPenaltyPerDegree: Double = 4.0,
    val heatPenaltyPerDegree: Double = 5.0,
    /** Outside this apparent temperature range an hour is never suggested. */
    val hardMin: Double = -5.0,
    val hardMax: Double = 30.0,
    /** At or above this precipitation probability an hour is never suggested. */
    val maxPrecipitationProbability: Int = 60,
    val precipitationProbabilityPenalty: Double = 0.6,
    /** Rain actually expected in the hour (mm). */
    val wetThreshold: Double = 0.2,
    val wetPenalty: Double = 35.0,
    val calmWind: Double = 15.0,
    val windPenaltyPerKmh: Double = 1.5,
    val gustPenaltyFrom: Double = 35.0,
    val gustPenalty: Double = 15.0,
    /** At or above this gust speed an hour is never suggested. */
    val maxGusts: Double = 50.0,
    val highUv: Double = 6.0,
    val uvPenalty: Double = 10.0,
    /** Only suggest daylight hours. */
    val requireDaylight: Boolean = true,
    val goodScore: Int = 70,
    val maxWindowHours: Int = 3,
    val lookaheadHours: Int = 24,
)

/** Why an hour can't be suggested at all. */
enum class RunningBlocker(val message: String) {
    Dark("Gün ışığı yok"),
    Thunderstorm("Gök gürültülü fırtına bekleniyor"),
    TooCold("Hava çok soğuk"),
    TooHot("Hava çok sıcak"),
    LikelyRain("Yağış bekleniyor"),
    StrongGusts("Kuvvetli rüzgar bekleniyor"),
}

data class HourScore(val hour: HourlyWeather, val score: Int, val blocker: RunningBlocker?, val isGood: Boolean)

data class RunningWindow(
    val hours: List<HourScore>,
    val dayLabel: String,
    val reasons: List<String>,
) {
    val averageScore: Int get() = hours.map { it.score }.average().toInt()
    /** "07.00–10.00": from the first hour's start to the last hour's end. */
    val rangeLabel: String
        get() = "%s–%02d.00".format(hours.first().hour.hourLabel, (hours.last().hour.hour + 1) % 24)
}

sealed interface RunningAdvice {
    val scores: List<HourScore>

    data class BestWindow(val window: RunningWindow, override val scores: List<HourScore>) : RunningAdvice

    data class NoGoodTime(val mainBlocker: RunningBlocker?, override val scores: List<HourScore>) : RunningAdvice
}

/** Scores each upcoming hour 0–100 and picks the best run of consecutive good hours. Pure, no Android. */
class RunningSuitabilityCalculator(private val t: RunningThresholds = RunningThresholds()) {

    fun advise(upcoming: List<HourlyWeather>, todayDate: String): RunningAdvice {
        val scores = upcoming.take(t.lookaheadHours).map(::score)
        val window = bestWindow(scores, todayDate)
        return if (window != null) {
            RunningAdvice.BestWindow(window, scores)
        } else {
            val blockers = scores.mapNotNull { it.blocker }.filter { it != RunningBlocker.Dark }
            RunningAdvice.NoGoodTime(
                mainBlocker = blockers.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key,
                scores = scores,
            )
        }
    }

    fun score(h: HourlyWeather): HourScore {
        val blocker = blocker(h)
        if (blocker != null) return HourScore(h, 0, blocker, isGood = false)

        var score = 100.0
        val feels = h.apparentTemperature
        if (feels < t.comfortableMin) score -= (t.comfortableMin - feels) * t.coldPenaltyPerDegree
        if (feels > t.comfortableMax) score -= (feels - t.comfortableMax) * t.heatPenaltyPerDegree
        score -= (h.precipitationProbability ?: 0) * t.precipitationProbabilityPenalty
        if ((h.precipitation ?: 0.0) >= t.wetThreshold) score -= t.wetPenalty
        val wind = h.windSpeed ?: 0.0
        if (wind > t.calmWind) score -= (wind - t.calmWind) * t.windPenaltyPerKmh
        if ((h.windGusts ?: 0.0) >= t.gustPenaltyFrom) score -= t.gustPenalty
        if ((h.uvIndex ?: 0.0) >= t.highUv) score -= t.uvPenalty

        val result = score.coerceIn(0.0, 100.0).toInt()
        return HourScore(h, result, null, isGood = result >= t.goodScore)
    }

    private fun blocker(h: HourlyWeather): RunningBlocker? = when {
        t.requireDaylight && !h.isDay -> RunningBlocker.Dark
        h.condition == WeatherCondition.Thunderstorm -> RunningBlocker.Thunderstorm
        h.apparentTemperature < t.hardMin -> RunningBlocker.TooCold
        h.apparentTemperature > t.hardMax -> RunningBlocker.TooHot
        (h.precipitationProbability ?: 0) >= t.maxPrecipitationProbability -> RunningBlocker.LikelyRain
        (h.windGusts ?: 0.0) >= t.maxGusts -> RunningBlocker.StrongGusts
        else -> null
    }

    private fun bestWindow(scores: List<HourScore>, todayDate: String): RunningWindow? {
        // Runs of consecutive good hours, trimmed to the best maxWindowHours-long stretch
        val candidates = mutableListOf<List<HourScore>>()
        var run = mutableListOf<HourScore>()
        for (s in scores + null) {
            if (s != null && s.isGood) {
                run += s
            } else if (run.isNotEmpty()) {
                candidates += bestSlice(run)
                run = mutableListOf()
            }
        }
        // Highest average wins; on a tie the earliest stays (maxBy keeps the first maximum)
        val best = candidates.maxByOrNull { slice -> slice.sumOf { it.score }.toDouble() / slice.size } ?: return null
        return RunningWindow(best, dayLabel(best.first().hour, todayDate), reasons(best))
    }

    private fun bestSlice(run: List<HourScore>): List<HourScore> {
        if (run.size <= t.maxWindowHours) return run
        return run.windowed(t.maxWindowHours).maxBy { slice -> slice.sumOf { it.score } }
    }

    private fun reasons(window: List<HourScore>): List<String> {
        val hours = window.map { it.hour }
        val feels = hours.map { it.apparentTemperature }.average()
        val wind = hours.maxOf { it.windSpeed ?: 0.0 }
        val rainChance = hours.maxOf { it.precipitationProbability ?: 0 }
        val wet = hours.any { (it.precipitation ?: 0.0) >= 0.1 }
        val uv = hours.maxOf { it.uvIndex ?: 0.0 }
        return buildList {
            add(
                when {
                    feels < 12 -> "Serin"
                    feels <= 20 -> "Ilık"
                    else -> "Sıcak"
                },
            )
            add(
                when {
                    wind < 10 -> "Rüzgarsız"
                    wind < 20 -> "Hafif rüzgar"
                    else -> "Rüzgarlı"
                },
            )
            add(if (rainChance < 20 && !wet) "Yağış beklenmiyor" else "Düşük yağış ihtimali")
            if (uv >= t.highUv) add("Yüksek UV")
        }
    }

    private fun dayLabel(h: HourlyWeather, todayDate: String): String {
        val day = if (h.date == todayDate) "Bugün" else "Yarın"
        val part = when (h.hour) {
            in 5..11 -> "sabah"
            in 12..14 -> "öğle"
            in 15..17 -> "öğleden sonra"
            in 18..21 -> "akşam"
            else -> "gece"
        }
        return "$day $part"
    }
}
