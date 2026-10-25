package com.example.weather.ui.city

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.weather.R
import com.example.weather.data.province
import com.example.weather.data.sampleCities
import com.example.weather.model.City
import com.example.weather.ui.theme.ForeignCityColor
import com.example.weather.ui.theme.WeatherTheme
import com.example.weather.ui.theme.regionColor

@Composable
fun CityList(
    cities: List<City>,
    onCityClick: (City) -> Unit,
    modifier: Modifier = Modifier,
    selectedCityId: Long? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(cities, key = { it.id }) { city ->
            val province = city.province
            CityCard(
                name = city.name,
                subtitle = province?.let { stringResource(R.string.region_subtitle, it.region.label) }
                    ?: city.country,
                badge = province?.plate?.toString()?.padStart(2, '0') ?: city.countryCode ?: "?",
                badgeColor = province?.let { regionColor(it.region) } ?: ForeignCityColor,
                selected = city.id == selectedCityId,
                onClick = { onCityClick(city) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CityListPreview() {
    WeatherTheme {
        CityList(cities = sampleCities, onCityClick = {}, selectedCityId = sampleCities[1].id)
    }
}
