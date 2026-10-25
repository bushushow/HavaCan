package com.example.weather.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/** Open-Meteo Forecast API. Units are the API defaults: °C, km/h, mm. */
interface ForecastApi {
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = CURRENT_FIELDS,
        @Query("hourly") hourly: String = HOURLY_FIELDS,
        @Query("daily") daily: String = DAILY_FIELDS,
        // "auto" returns times in the location's own time zone, not the phone's
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 3,
    ): ForecastResponse

    companion object {
        const val CURRENT_FIELDS =
            "temperature_2m,apparent_temperature,weather_code,is_day,wind_speed_10m,precipitation"
        const val HOURLY_FIELDS =
            "temperature_2m,apparent_temperature,precipitation_probability,precipitation," +
                "weather_code,wind_speed_10m,wind_gusts_10m,uv_index,is_day"
        const val DAILY_FIELDS = "temperature_2m_max,temperature_2m_min,sunrise,sunset"
    }
}
