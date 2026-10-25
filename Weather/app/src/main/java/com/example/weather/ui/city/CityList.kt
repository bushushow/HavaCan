package com.example.weather.ui.city

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.weather.R
import com.example.weather.data.province
import com.example.weather.data.provinceCities
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
    /** When set, Turkish provinces get an "İlçeler" button that calls it. */
    onBrowseDistricts: ((City) -> Unit)? = null,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // A central district (İlkadım) shares its GeoNames id with the province centre (Samsun),
        // so the key includes the name to stay unique
        items(cities.distinctBy { it.id to it.name }, key = { "${it.id}:${it.name}" }) { city ->
            val province = city.province
            CityCard(
                name = city.name,
                subtitle = when {
                    province == null -> listOfNotNull(city.admin1, city.country).distinct().joinToString(", ")
                    province.name == city.name -> stringResource(R.string.region_subtitle, province.region.label)
                    // A district or town: show which province it belongs to
                    else -> stringResource(R.string.province_region_subtitle, province.name, province.region.label)
                },
                badge = province?.plate?.toString()?.padStart(2, '0') ?: city.countryCode ?: "?",
                badgeColor = province?.let { regionColor(it.region) } ?: ForeignCityColor,
                selected = city.id == selectedCityId,
                onClick = { onCityClick(city) },
                trailing = if (onBrowseDistricts != null && province != null && province.name == city.name) {
                    { DistrictsButton(city.name) { onBrowseDistricts(city) } }
                } else {
                    null
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CityListPreview() {
    WeatherTheme {
        CityList(cities = provinceCities, onCityClick = {}, selectedCityId = provinceCities[1].id)
    }
}

@Composable
private fun DistrictsButton(provinceName: String, onClick: () -> Unit) {
    val description = stringResource(R.string.cd_districts, provinceName)
    TextButton(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        Text(stringResource(R.string.districts_button), style = MaterialTheme.typography.labelLarge)
        Icon(painterResource(R.drawable.ic_chevron_right), null, modifier = Modifier.size(18.dp))
    }
}
