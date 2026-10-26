package com.kampplus.havacan.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.gson.JsonParseException
import com.kampplus.havacan.data.LocationStore
import com.kampplus.havacan.data.PreferencesLocationStore
import com.kampplus.havacan.data.WeatherRepository
import com.kampplus.havacan.data.provinceCities
import com.kampplus.havacan.data.remote.ApiClient
import com.kampplus.havacan.domain.insight.WeatherInsightBuilder
import com.kampplus.havacan.domain.running.RunningSuitabilityCalculator
import com.kampplus.havacan.model.City
import com.kampplus.havacan.ui.city.ErrorReason
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException

private const val KEY_MODE = "mode"
private const val KEY_SELECTED_TIME = "selectedTime"
private const val NORMAL_STRIP_HOURS = 13

class HomeViewModel(
    private val repository: WeatherRepository,
    private val locationStore: LocationStore,
    private val savedStateHandle: SavedStateHandle,
    private val runningCalculator: RunningSuitabilityCalculator = RunningSuitabilityCalculator()
) : ViewModel() {

    private val city = MutableStateFlow(locationStore.load() ?: DEFAULT_CITY)
    private val mode = savedStateHandle.getStateFlow(KEY_MODE, HomeMode.Normal.name)
    private val selectedTime = savedStateHandle.getStateFlow<String?>(KEY_SELECTED_TIME, null)
    private val load = MutableStateFlow<WeatherLoadState>(WeatherLoadState.Loading)
    private var loadJob: Job? = null

    val uiState: StateFlow<HomeUiState> =
        combine(city, mode, load, selectedTime) { city, mode, load, selected ->
            val forecast = (load as? WeatherLoadState.Success)?.forecast
            val upcoming = forecast?.upcoming.orEmpty()
            HomeUiState(
                city = city,
                mode = HomeMode.valueOf(mode),
                load = load,
                hourly = upcoming.take(NORMAL_STRIP_HOURS),
                selectedTime = selected?.takeIf { time -> upcoming.any { it.time == time } },
                insight = WeatherInsightBuilder.build(upcoming),
                runningAdvice = forecast?.let { runningCalculator.advise(upcoming, it.todayDate) }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(city.value))

    init {
        refresh()
    }

    fun refresh(force: Boolean = false) {
        val target = city.value
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (load.value !is WeatherLoadState.Success || force) load.value = WeatherLoadState.Loading
            load.value = try {
                val forecast = repository.forecast(target.latitude, target.longitude, forceRefresh = force)
                if (forecast == null) WeatherLoadState.Empty else WeatherLoadState.Success(forecast)
            } catch (e: IOException) {
                WeatherLoadState.Error(ErrorReason.NoConnection)
            } catch (e: HttpException) {
                WeatherLoadState.Error(ErrorReason.Server)
            } catch (e: JsonParseException) {
                WeatherLoadState.Error(ErrorReason.Unknown)
            }
        }
    }

    fun retry() = refresh(force = true)

    fun onCityChosen(chosen: City) {
        if (chosen.id == city.value.id) return
        locationStore.save(chosen)
        city.value = chosen
        savedStateHandle[KEY_SELECTED_TIME] = null
        load.value = WeatherLoadState.Loading
        refresh()
    }

    fun toggleRunnerMode() {
        val next = if (mode.value == HomeMode.Runner.name) HomeMode.Normal else HomeMode.Runner
        savedStateHandle[KEY_MODE] = next.name
    }

    /** Selecting an hour only re-reads the forecast already in memory; it never hits the network. */
    fun onHourSelected(time: String) {
        savedStateHandle[KEY_SELECTED_TIME] = time
    }

    companion object {
        val DEFAULT_CITY: City = provinceCities.first { it.name == "Ankara" }

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    repository = WeatherRepository(ApiClient.forecastApi),
                    locationStore = PreferencesLocationStore(this[APPLICATION_KEY]!!),
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }
}
