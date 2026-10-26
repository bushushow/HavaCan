package com.kampplus.havacan.data

import com.kampplus.havacan.model.City
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TurkishProvincesTest {
    @Test
    fun hasAll81ProvincesWithUniquePlates() {
        assertEquals(81, TurkishProvinces.all.size)
        assertEquals((1..81).toList(), TurkishProvinces.all.map { it.plate }.sorted())
    }

    @Test
    fun findsProvinceFromApiAdmin1WithSuffix() {
        // The geocoding API returns e.g. "Bursa İli" and "Samsun İli"
        assertEquals(16, TurkishProvinces.find("Bursa İli")?.plate)
        assertEquals(55, TurkishProvinces.find("Samsun İli")?.plate)
    }

    @Test
    fun findIsCaseInsensitiveWithTurkishLetters() {
        assertEquals(35, TurkishProvinces.find("İZMİR")?.plate)
        assertEquals(76, TurkishProvinces.find("ığdır")?.plate)
    }

    @Test
    fun foreignCityHasNoProvince() {
        val tbilisi = City(611717, "Tiflis", 41.69, 44.83, countryCode = "GE", admin1 = "Tiflis")
        assertNull(tbilisi.province)
    }

    @Test
    fun everySampleCityResolvesToAProvince() {
        provinceCities.forEach { city -> assertEquals(city.name, city.province?.name) }
        assert(provinceCities.size >= 10)
    }
}
