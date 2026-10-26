package com.kampplus.havacan.model

import com.google.gson.annotations.SerializedName

/**
 * A city. Field names follow the Open-Meteo Geocoding API response so the same
 * model serves the static sample list and the API search.
 *
 * Fields the API sometimes omits are nullable: Gson ignores Kotlin defaults and null-safety.
 */
data class City(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("country_code") val countryCode: String? = null,
    val country: String? = null,
    val admin1: String? = null,
    @SerializedName("feature_code") val featureCode: String? = null,
    val population: Long? = null
)
