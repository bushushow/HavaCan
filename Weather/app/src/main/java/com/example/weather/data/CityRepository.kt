package com.example.weather.data

import com.example.weather.data.remote.CityApi
import com.example.weather.model.City

/** Sits between the ViewModel and the API so the UI never deals with raw API quirks. */
class CityRepository(private val api: CityApi) {

    /**
     * Searches cities by name. Keeps only populated places (the API also returns airports,
     * mountains, …) and lists Turkish results first, otherwise in the API's relevance order.
     */
    suspend fun searchCities(query: String, countryCode: String? = null): List<City> =
        api.searchCities(name = query.trim(), countryCode = countryCode)
            .results
            .orEmpty()
            .filter { it.featureCode?.startsWith("PPL") ?: true }
            .sortedBy { if (it.countryCode == "TR") 0 else 1 }
}
