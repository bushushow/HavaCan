package com.example.weather.ui

import android.util.Log
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.weather.R
import com.example.weather.data.sampleCities
import com.example.weather.ui.city.CityScreen

private const val TAG = "Hava"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HavaApp() {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { innerPadding ->
        CityScreen(
            cities = sampleCities,
            onCityClick = { city -> Log.d(TAG, "Tıklandı: ${city.name}") },
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        )
    }
}
