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
        /** ISO 3166-1 alpha-2; null searches worldwide (Retrofit drops null query params). */
        @Query("countryCode") countryCode: String? = null,
    ): CitySearchResponse
}
