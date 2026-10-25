package com.example.weather.model

/** Weather condition derived from the WMO code Open-Meteo returns. */
enum class WeatherCondition(val label: String) {
    Clear("Açık"),
    MainlyClear("Az bulutlu"),
    PartlyCloudy("Parçalı bulutlu"),
    Overcast("Kapalı"),
    Fog("Sisli"),
    Drizzle("Çisenti"),
    Rain("Yağmurlu"),
    HeavyRain("Sağanak yağışlı"),
    Snow("Karlı"),
    Thunderstorm("Gök gürültülü"),
    Unknown("Bilinmiyor");

    val isWet: Boolean get() = this in setOf(Drizzle, Rain, HeavyRain, Thunderstorm)

    companion object {
        /** WMO weather interpretation codes, as documented by Open-Meteo. */
        fun fromWmo(code: Int?): WeatherCondition = when (code) {
            0 -> Clear
            1 -> MainlyClear
            2 -> PartlyCloudy
            3 -> Overcast
            45, 48 -> Fog
            51, 53, 55, 56, 57 -> Drizzle
            61, 63, 66, 80, 81 -> Rain
            65, 67, 82 -> HeavyRain
            71, 73, 75, 77, 85, 86 -> Snow
            95, 96, 99 -> Thunderstorm
            else -> Unknown
        }
    }
}

/** One forecast hour. [time] is local to the forecast location, e.g. "2026-10-25T07:00". */
data class HourlyWeather(
    val time: String,
    val temperature: Double,
    val apparentTemperature: Double,
    val precipitationProbability: Int?,
    val precipitation: Double?,
    val condition: WeatherCondition,
    val isDay: Boolean,
    val windSpeed: Double?,
    val windGusts: Double?,
    val uvIndex: Double?,
) {
    val date: String get() = time.substringBefore('T')
    val hour: Int get() = time.substringAfter('T').take(2).toIntOrNull() ?: 0
    /** "07.00", the Turkish way of writing times. */
    val hourLabel: String get() = "%02d.00".format(hour)
}

data class CurrentWeather(
    val time: String,
    val temperature: Double,
    val apparentTemperature: Double,
    val condition: WeatherCondition,
    val isDay: Boolean,
    val windSpeed: Double?,
)

data class DailySummary(
    val date: String,
    val temperatureMax: Double,
    val temperatureMin: Double,
    val sunrise: String?,
    val sunset: String?,
    val condition: WeatherCondition = WeatherCondition.Unknown,
    val precipitationProbabilityMax: Int? = null,
)

data class Forecast(
    val timezone: String,
    val current: CurrentWeather,
    val hourly: List<HourlyWeather>,
    val today: DailySummary?,
    /** Every forecast day, today first. */
    val daily: List<DailySummary> = listOfNotNull(today),
) {
    /** Index of the hour that contains [current]'s time; hours before it are in the past. */
    val currentHourIndex: Int
        get() = hourly.indexOfFirst { it.time.take(13) == current.time.take(13) }.coerceAtLeast(0)

    /** The current hour and everything after it. */
    val upcoming: List<HourlyWeather> get() = hourly.drop(currentHourIndex)

    val todayDate: String get() = current.time.substringBefore('T')
}
