package com.example.weather.data

import com.example.weather.model.City
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Runs against the app's real district file (src/main/assets/districts_tr.json). */
class PlaceDirectoryTest {
    private val real = DistrictSource { plate ->
        val json = File("src/main/assets/districts_tr.json").readText(Charsets.UTF_8)
        val all: Map<String, List<List<Any>>> = Gson().fromJson(json, object : TypeToken<Map<String, List<List<Any>>>>() {}.type)
        val province = TurkishProvinces.all.first { it.plate == plate }
        all[plate.toString()].orEmpty().map { row ->
            City((row[1] as Number).toLong(), row[0] as String, (row[2] as Number).toDouble(), (row[3] as Number).toDouble(), "TR", "Türkiye", province.name)
        }
    }
    private val directory = PlaceDirectory(real)

    private suspend fun first(query: String) = directory.search(query).first()

    @Test
    fun provincesComeFirstAsProvinces() = runTest {
        listOf("Ankara", "Bursa", "İstanbul", "istanbul", "ISTANBUL").forEach { q ->
            val top = first(q)
            assertEquals(q, PlaceItem.Kind.Province, top.kind)
        }
        assertEquals("İstanbul", first("istanbul").city.name)
    }

    @Test
    fun districtsCarryTheirRealProvince() = runTest {
        mapOf("Çankaya" to "Ankara", "cankaya" to "Ankara", "Gemlik" to "Bursa", "Kadıköy" to "İstanbul", "kadikoy" to "İstanbul").forEach { (q, province) ->
            val top = first(q)
            assertEquals(q, PlaceItem.Kind.District, top.kind)
            assertEquals(q, province, top.city.admin1)
            assertEquals("İlçe · $province", top.subtitle)
        }
    }

    @Test
    fun districtAndProvinceTogether() = runTest {
        assertEquals("Gemlik", first("gemlik bursa").city.name)
        assertEquals("Bursa, Gemlik", first("gemlik").shortLabel)
    }

    @Test
    fun unknownNamesFindNothing() = runTest {
        assertTrue(directory.search("Berlin").isEmpty())
        assertTrue(directory.search("   ").isEmpty())
    }

    @Test
    fun adanaListsItsDistrictsButNotItself() = runTest {
        val names = directory.districtsOf(1).map { it.city.name }
        assertTrue(names.containsAll(listOf("Aladağ", "Ceyhan", "Çukurova", "Feke", "İmamoğlu", "Karaisalı", "Karataş")))
        assertFalse("Adana" in names)
        assertTrue(directory.districtsOf(1).all { it.kind == PlaceItem.Kind.District })
    }

    @Test
    fun nearestFindsTheDistrictOfAGpsFix() = runTest {
        val gemlik = first("gemlik").city
        val near = directory.nearest(gemlik.latitude + 0.01, gemlik.longitude - 0.01)
        assertEquals("Gemlik", near?.city?.name)
        assertNull("Berlin is not in Türkiye", directory.nearest(52.52, 13.405))
    }
}
