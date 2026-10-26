package com.kampplus.havacan.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CitySearchTest {
    private fun names(query: String) = provinceCities.filterByQuery(query).map { it.name }

    @Test
    fun blankQueryReturnsAllCities() {
        assertEquals(provinceCities.size, provinceCities.filterByQuery("  ").size)
    }

    @Test
    fun matchesAnywhereInTheName() {
        assertEquals(listOf("Kahramanmaraş"), names("maraş"))
        assertEquals(listOf("Trabzon"), names("trab"))
        // "kir" matches ı as well as i, anywhere in the name
        assertEquals(setOf("Çankırı", "Diyarbakır", "Kırklareli", "Kırşehir", "Tekirdağ", "Kırıkkale"), names("kir").toSet())
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
