package com.kampplus.havacan.data

import com.kampplus.havacan.model.City
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A row in the location picker: a province (il), one of its districts (ilçe) or a place abroad. */
data class PlaceItem(val city: City, val kind: Kind) {
    enum class Kind { Province, District, Abroad }

    val province: Province? get() = TurkishProvinces.find(city.admin1)

    /** "Akdeniz Bölgesi", "İlçe · Bursa", "Almanya". Words, so nothing relies on colour alone. */
    val subtitle: String
        get() = when (kind) {
            Kind.Province -> province?.region?.let { "${it.label} Bölgesi" } ?: "İl"
            Kind.District -> "İlçe · ${city.admin1.orEmpty()}"
            Kind.Abroad -> listOfNotNull(city.admin1, city.country).distinct().joinToString(", ")
        }

    /** "Bursa, Gemlik" for a district, the name alone otherwise. */
    val shortLabel: String
        get() = if (kind == Kind.District && city.admin1 != null) "${city.admin1}, ${city.name}" else city.name
}

/**
 * All 81 provinces and 973 districts, searched on the phone: no network, Turkish letters folded
 * ("cankaya" finds Çankaya), every result carries its real province and coordinates.
 */
class PlaceDirectory(
    private val districtSource: DistrictSource,
    private val provinces: List<City> = provinceCities
) {
    private val mutex = Mutex()
    private var districtCache: Map<Int, List<City>>? = null

    val provinceItems: List<PlaceItem> = provinces.map { PlaceItem(it, PlaceItem.Kind.Province) }

    fun provinceByPlate(plate: Int): PlaceItem? = provinceItems.firstOrNull { it.province?.plate == plate }

    suspend fun districtsOf(plate: Int): List<PlaceItem> = all()[plate].orEmpty().map { PlaceItem(it, PlaceItem.Kind.District) }

    private suspend fun all(): Map<Int, List<City>> = mutex.withLock {
        districtCache ?: TurkishProvinces.all.associate { it.plate to districtSource.districtsOf(it.plate) }.also { districtCache = it }
    }

    /**
     * Provinces and districts whose name (or "province name") matches every word of [query].
     * Exact names first, then names starting with it, then the rest; provinces before districts.
     */
    suspend fun search(query: String, limit: Int = 50): List<PlaceItem> {
        val needle = normalizeForSearch(query)
        if (needle.isEmpty()) return emptyList()
        val words = needle.split(' ').filter { it.isNotBlank() }
        val candidates = provinceItems + all().values.flatten().map { PlaceItem(it, PlaceItem.Kind.District) }
        return candidates
            .mapNotNull { item ->
                val name = normalizeForSearch(item.city.name)
                val haystack = if (item.kind == PlaceItem.Kind.District) "$name ${normalizeForSearch(item.city.admin1.orEmpty())}" else name
                if (!words.all { haystack.contains(it) }) return@mapNotNull null
                val rank = when {
                    name == needle -> 0
                    name.startsWith(needle) -> 1
                    name.contains(needle) -> 2
                    else -> 3 // matched across name and province ("gemlik bursa")
                }
                Triple(item, rank, if (item.kind == PlaceItem.Kind.Province) 0 else 1)
            }
            .sortedWith(compareBy<Triple<PlaceItem, Int, Int>>({ it.second }, { it.third }).thenBy(turkishCollator) { it.first.city.name })
            .take(limit)
            .map { it.first }
    }

    /** The district (or province centre) closest to a GPS fix, or null when it's over [maxKm] away, e.g. abroad. */
    suspend fun nearest(latitude: Double, longitude: Double, maxKm: Double = 50.0): PlaceItem? {
        val candidates = all().values.flatten().map { PlaceItem(it, PlaceItem.Kind.District) } + provinceItems
        return candidates
            .minByOrNull { distanceKm(latitude, longitude, it.city.latitude, it.city.longitude) }
            ?.takeIf { distanceKm(latitude, longitude, it.city.latitude, it.city.longitude) <= maxKm }
    }

    companion object {
        fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
            return 2 * 6371.0 * asin(sqrt(a))
        }
    }
}
