package com.example.weather.model

/**
 * A city. Field names follow the Open-Meteo Geocoding API response so the same
 * model serves the static sample list now and the API search in Uygulama 4.
 */
data class City(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val countryCode: String? = null,
    val country: String? = null,
    val admin1: String? = null,
)
