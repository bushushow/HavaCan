package com.example.weather.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CitySearchTest {
    private fun names(query: String) = sampleCities.filterByQuery(query).map { it.name }

    @Test
    fun blankQueryReturnsAllCities() {
        assertEquals(sampleCities.size, sampleCities.filterByQuery("  ").size)
    }

    @Test
    fun matchesAnywhereInTheName() {
        assertEquals(listOf("Adana", "Ankara", "Antalya", "Gaziantep", "İstanbul", "Van"), names("an"))
    }

    @Test
    fun ignoresCaseAndTurkishLetters() {
        assertEquals(listOf("İzmir"), names("izmir"))
        assertEquals(listOf("İzmir"), names("İZMİR"))
        assertEquals(listOf("Eskişehir"), names("eskisehir"))
        assertEquals(listOf("Diyarbakır"), names("DIYARBAKIR"))
    }

    @Test
    fun unknownQueryReturnsEmpty() {
        assertEquals(emptyList<String>(), names("xyz"))
    }
}
