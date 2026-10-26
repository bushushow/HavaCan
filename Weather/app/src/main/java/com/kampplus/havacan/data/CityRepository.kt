package com.kampplus.havacan.data

import com.kampplus.havacan.data.remote.CityApi
import com.kampplus.havacan.model.City

/** Online place search, used when the on-phone list of provinces and districts has no match. */
interface CityRepository {
    suspend fun searchCities(query: String, countryCode: String? = null): List<City>
}

/** Open-Meteo Geocoding, so the UI never deals with raw API quirks. */
class OpenMeteoCityRepository(private val api: CityApi) : CityRepository {

    /**
     * Keeps only populated places (the API also returns airports, mountains, …) and lists
     * Turkish results first, otherwise in the API's relevance order.
     */
    override suspend fun searchCities(query: String, countryCode: String?): List<City> =
        api.searchCities(name = query.trim(), countryCode = countryCode)
            .results
            .orEmpty()
            .filter { it.featureCode?.startsWith("PPL") ?: true }
            .sortedBy { if (it.countryCode == "TR") 0 else 1 }
}
