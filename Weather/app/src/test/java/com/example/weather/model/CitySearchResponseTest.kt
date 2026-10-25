package com.example.weather.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Parses responses captured from the real Open-Meteo Geocoding API. */
class CitySearchResponseTest {
    private val gson = Gson()

    @Test
    fun parsesResults() {
        val json = """
            {"results":[{"id":323786,"name":"Ankara","latitude":39.91987,"longitude":32.85427,
            "elevation":850.0,"feature_code":"PPLC","country_code":"TR","admin1_id":323784,
            "timezone":"Europe/Istanbul","population":3517182,"country_id":298795,
            "country":"Türkiye Cumhuriyeti","admin1":"Ankara"}],"generationtime_ms":0.55}
        """.trimIndent()

        val city = gson.fromJson(json, CitySearchResponse::class.java).results!!.single()

        assertEquals(323786L, city.id)
        assertEquals("Ankara", city.name)
        assertEquals("TR", city.countryCode)
        assertEquals("PPLC", city.featureCode)
        assertEquals(3517182L, city.population)
    }

    @Test
    fun noMatchHasNoResultsField() {
        val response = gson.fromJson("""{"generationtime_ms":0.09}""", CitySearchResponse::class.java)
        assertNull(response.results)
    }

    @Test
    fun optionalFieldsMayBeMissing() {
        val json = """{"results":[{"id":615742,"name":"Krist’esi","latitude":42.55,"longitude":43.39,"country_code":"GE"}]}"""
        val city = gson.fromJson(json, CitySearchResponse::class.java).results!!.single()
        assertNull(city.admin1)
        assertNull(city.population)
    }
}
