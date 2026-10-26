package com.kampplus.havacan.ui.home

import com.kampplus.havacan.domain.insight.WeatherInsight
import com.kampplus.havacan.domain.running.RunningAdvice
import com.kampplus.havacan.model.City
import com.kampplus.havacan.model.Forecast
import com.kampplus.havacan.model.HourlyWeather
import com.kampplus.havacan.ui.city.ErrorReason

enum class HomeMode { Normal, Runner }

sealed interface WeatherLoadState {
    data object Loading : WeatherLoadState
    data class Success(val forecast: Forecast) : WeatherLoadState
    data object Empty : WeatherLoadState
    data class Error(val reason: ErrorReason) : WeatherLoadState
}

/** Everything the home screen shows. All weather numbers come from Open-Meteo; nothing here is demo data. */
data class HomeUiState(
    val city: City,
    val mode: HomeMode = HomeMode.Normal,
    val load: WeatherLoadState = WeatherLoadState.Loading,
    /** The coming hours shown in the normal strip (now + 12). */
    val hourly: List<HourlyWeather> = emptyList(),
    /** Time key of the selected hour; null means "now". */
    val selectedTime: String? = null,
    val insight: WeatherInsight? = null,
    val runningAdvice: RunningAdvice? = null
) {
    val forecast: Forecast? get() = (load as? WeatherLoadState.Success)?.forecast

    /** The hour the hero and scene describe. */
    val selectedHour: HourlyWeather?
        get() = forecast?.upcoming?.let { hours -> hours.firstOrNull { it.time == selectedTime } ?: hours.firstOrNull() }

    val isNowSelected: Boolean get() = selectedHour?.time == forecast?.upcoming?.firstOrNull()?.time
}
