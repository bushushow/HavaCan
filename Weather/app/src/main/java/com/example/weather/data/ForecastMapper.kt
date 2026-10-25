package com.example.weather.data

import com.example.weather.data.remote.ForecastResponse
import com.example.weather.model.CurrentWeather
import com.example.weather.model.DailySummary
import com.example.weather.model.Forecast
import com.example.weather.model.HourlyWeather
import com.example.weather.model.WeatherCondition

/** Turns the raw API response into the app's model. Returns null when there is nothing usable. */
object ForecastMapper {

    fun map(response: ForecastResponse): Forecast? {
        val current = response.current ?: return null
        val currentTime = current.time ?: return null
        val currentTemperature = current.temperature ?: return null

        val hourly = mapHourly(response)
        if (hourly.isEmpty()) return null

        return Forecast(
            timezone = response.timezone.orEmpty(),
            current = CurrentWeather(
                time = currentTime,
                temperature = currentTemperature,
                apparentTemperature = current.apparentTemperature ?: currentTemperature,
                condition = WeatherCondition.fromWmo(current.weatherCode),
                isDay = current.isDay != 0,
                windSpeed = current.windSpeed,
            ),
            hourly = hourly,
            today = mapToday(response, currentTime.substringBefore('T')),
        )
    }

    private fun mapHourly(response: ForecastResponse): List<HourlyWeather> {
        val h = response.hourly ?: return emptyList()
        val times = h.time ?: return emptyList()
        return times.indices.mapNotNull { i ->
            val time = times[i] ?: return@mapNotNull null
            // An hour without a temperature is not worth showing
            val temperature = h.temperature?.getOrNull(i) ?: return@mapNotNull null
            HourlyWeather(
                time = time,
                temperature = temperature,
                apparentTemperature = h.apparentTemperature?.getOrNull(i) ?: temperature,
                precipitationProbability = h.precipitationProbability?.getOrNull(i),
                precipitation = h.precipitation?.getOrNull(i),
                condition = WeatherCondition.fromWmo(h.weatherCode?.getOrNull(i)),
                isDay = h.isDay?.getOrNull(i)?.let { it != 0 } ?: true,
                windSpeed = h.windSpeed?.getOrNull(i),
                windGusts = h.windGusts?.getOrNull(i),
                uvIndex = h.uvIndex?.getOrNull(i),
            )
        }
    }

    private fun mapToday(response: ForecastResponse, today: String): DailySummary? {
        val d = response.daily ?: return null
        val i = d.time?.indexOf(today)?.takeIf { it >= 0 } ?: return null
        return DailySummary(
            date = today,
            temperatureMax = d.temperatureMax?.getOrNull(i) ?: return null,
            temperatureMin = d.temperatureMin?.getOrNull(i) ?: return null,
            sunrise = d.sunrise?.getOrNull(i),
            sunset = d.sunset?.getOrNull(i),
        )
    }
}
