package com.kampplus.havacan.model

/**
 * Open-Meteo Geocoding search response. When nothing matches, the API omits
 * `results` entirely (the body is just `{"generationtime_ms": …}`), so it must be nullable.
 */
data class CitySearchResponse(
    val results: List<City>? = null
)
