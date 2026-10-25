package com.example.weather.ui

import android.util.Log
import androidx.compose.runtime.Composable
import com.example.weather.data.sampleCities
import com.example.weather.ui.city.CityScreen

private const val TAG = "Hava"

@Composable
fun HavaApp() {
    CityScreen(
        cities = sampleCities,
        onCityClick = { city -> Log.d(TAG, "Tıklandı: ${city.name}") },
    )
}
