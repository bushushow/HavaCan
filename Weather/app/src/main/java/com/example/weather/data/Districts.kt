package com.example.weather.data

import android.content.Context
import com.example.weather.model.City
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

/** The districts (ilçeler) of a Turkish province, by plate code. */
fun interface DistrictSource {
    suspend fun districtsOf(plate: Int): List<City>
}

/**
 * Reads assets/districts_tr.json (973 districts with coordinates, built from GeoNames and
 * Open-Meteo, see docs/data-sources.md) once, off the main thread, and keeps it in memory.
 */
class AssetDistrictSource(private val context: Context) : DistrictSource {
    private val mutex = Mutex()
    private var cache: Map<String, List<List<Any>>>? = null

    override suspend fun districtsOf(plate: Int): List<City> {
        val all = mutex.withLock {
            cache ?: withContext(Dispatchers.IO) {
                context.assets.open(ASSET).bufferedReader(Charsets.UTF_8).use { reader ->
                    val type = object : TypeToken<Map<String, List<List<Any>>>>() {}.type
                    Gson().fromJson<Map<String, List<List<Any>>>>(reader, type)
                }
            }.also { cache = it }
        }
        val province = TurkishProvinces.all.firstOrNull { it.plate == plate } ?: return emptyList()
        return all[plate.toString()].orEmpty()
            .map { row ->
                City(
                    id = (row[1] as Number).toLong(),
                    name = row[0] as String,
                    latitude = (row[2] as Number).toDouble(),
                    longitude = (row[3] as Number).toDouble(),
                    countryCode = "TR",
                    country = "Türkiye",
                    admin1 = province.name,
                )
            }
            .sortedWith(compareBy(turkishCollator) { it.name })
    }

    private companion object {
        const val ASSET = "districts_tr.json"
    }
}

/** Sorts Ç, Ğ, İ, Ö, Ş, Ü where Turkish readers expect them, not after Z. */
val turkishCollator: Collator = Collator.getInstance(Locale.forLanguageTag("tr"))
