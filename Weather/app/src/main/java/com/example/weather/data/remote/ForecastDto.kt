package com.example.weather.data.remote

import com.google.gson.annotations.SerializedName

// Raw Open-Meteo Forecast API response (https://open-meteo.com/en/docs).
// Every field is nullable: Gson ignores Kotlin null-safety, and the API may omit values.

data class ForecastResponse(
    val timezone: String? = null,
    @SerializedName("utc_offset_seconds") val utcOffsetSeconds: Int? = null,
    val current: CurrentDto? = null,
    val hourly: HourlyDto? = null,
    val daily: DailyDto? = null,
)

data class CurrentDto(
    val time: String? = null,
    @SerializedName("temperature_2m") val temperature: Double? = null,
    @SerializedName("apparent_temperature") val apparentTemperature: Double? = null,
    @SerializedName("weather_code") val weatherCode: Int? = null,
    @SerializedName("is_day") val isDay: Int? = null,
    @SerializedName("wind_speed_10m") val windSpeed: Double? = null,
    val precipitation: Double? = null,
)

data class HourlyDto(
    val time: List<String?>? = null,
    @SerializedName("temperature_2m") val temperature: List<Double?>? = null,
    @SerializedName("apparent_temperature") val apparentTemperature: List<Double?>? = null,
    @SerializedName("precipitation_probability") val precipitationProbability: List<Int?>? = null,
    val precipitation: List<Double?>? = null,
    @SerializedName("weather_code") val weatherCode: List<Int?>? = null,
    @SerializedName("wind_speed_10m") val windSpeed: List<Double?>? = null,
    @SerializedName("wind_gusts_10m") val windGusts: List<Double?>? = null,
    @SerializedName("uv_index") val uvIndex: List<Double?>? = null,
    @SerializedName("is_day") val isDay: List<Int?>? = null,
)

data class DailyDto(
    val time: List<String?>? = null,
    @SerializedName("temperature_2m_max") val temperatureMax: List<Double?>? = null,
    @SerializedName("temperature_2m_min") val temperatureMin: List<Double?>? = null,
    val sunrise: List<String?>? = null,
    val sunset: List<String?>? = null,
)
