package com.example.weather.ui

import android.util.Log
import androidx.compose.runtime.Composable
import com.example.weather.ui.city.CityRoute

private const val TAG = "Hava"

@Composable
fun HavaApp() {
    CityRoute(onCityClick = { city -> Log.d(TAG, "Tıklandı: ${city.name}") })
}
