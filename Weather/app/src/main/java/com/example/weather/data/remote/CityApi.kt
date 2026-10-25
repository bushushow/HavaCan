package com.example.weather.data.remote

import com.example.weather.model.CitySearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

/** Open-Meteo Geocoding API: https://open-meteo.com/en/docs/geocoding-api */
interface CityApi {
    @GET("v1/search")
    suspend fun searchCities(
        @Query("name") name: String,
        @Query("count") count: Int = 20,
        @Query("language") language: String = "tr",
        @Query("format") format: String = "json",
    ): CitySearchResponse
}
