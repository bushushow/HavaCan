package com.kampplus.havacan.ui.home

import com.kampplus.havacan.domain.insight.WeatherInsight
import com.kampplus.havacan.domain.running.RunningAdvice
import com.kampplus.havacan.model.City
import com.kampplus.havacan.model.Forecast
import com.kampplus.havacan.model.HourlyWeather
import com.kampplus.havacan.ui.common.ErrorReason
import com.kampplus.havacan.ui.common.LoadState

enum class HomeMode { Normal, Runner }

/** Everything the home screen shows. All weather numbers come from Open-Meteo; nothing here is demo data. */
data class HomeUiState(
    val city: City,
    val mode: HomeMode = HomeMode.Normal,
    val load: LoadState<Forecast> = LoadState.Loading,
    /** The coming hours shown in the normal strip (now + 12). */
    val hourly: List<HourlyWeather> = emptyList(),
    /** Time key of the selected hour; null means "now". */
    val selectedTime: String? = null,
    val insight: WeatherInsight? = null,
    val runningAdvice: RunningAdvice? = null
) {
    val forecast: Forecast? get() = (load as? LoadState.Success)?.value

    /** The hour the hero and scene describe. */
    val selectedHour: HourlyWeather?
        get() = forecast?.upcoming?.let { hours -> hours.firstOrNull { it.time == selectedTime } ?: hours.firstOrNull() }

    val isNowSelected: Boolean get() = selectedHour?.time == forecast?.upcoming?.firstOrNull()?.time
}
