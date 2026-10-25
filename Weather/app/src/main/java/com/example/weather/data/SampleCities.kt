package com.example.weather.data

import com.example.weather.model.City

/** Fixed city list for the first screen. Ids and coordinates come from the Open-Meteo Geocoding API. */
val sampleCities: List<City> = listOf(
    turkishCity(325363, "Adana", 36.98615, 35.32531),
    turkishCity(323786, "Ankara", 39.91987, 32.85427),
    turkishCity(323777, "Antalya", 36.90812, 30.69556),
    turkishCity(750269, "Bursa", 40.19559, 29.06013),
    turkishCity(316541, "Diyarbakır", 37.91363, 40.21721),
    turkishCity(315368, "Erzurum", 39.90861, 41.27694),
    turkishCity(315202, "Eskişehir", 39.77667, 30.52056),
    turkishCity(314830, "Gaziantep", 37.05944, 37.3825),
    turkishCity(745044, "İstanbul", 41.01384, 28.94966),
    turkishCity(311046, "İzmir", 38.41273, 27.13838),
    turkishCity(308464, "Kayseri", 38.73222, 35.48528),
    turkishCity(306571, "Konya", 37.87135, 32.48464),
    turkishCity(740264, "Samsun", 41.27976, 36.3361),
    turkishCity(738648, "Trabzon", 41.005, 39.72694),
    turkishCity(298117, "Van", 38.49457, 43.38323),
)

private fun turkishCity(id: Long, name: String, latitude: Double, longitude: Double) = City(
    id = id,
    name = name,
    latitude = latitude,
    longitude = longitude,
    countryCode = "TR",
    country = "Türkiye",
    admin1 = name,
)
