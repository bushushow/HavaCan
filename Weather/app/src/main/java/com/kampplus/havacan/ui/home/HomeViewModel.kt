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
import com.kampplus.havacan.data.DEFAULT_CITY
import com.kampplus.havacan.data.LocationStore
import com.kampplus.havacan.data.WeatherRepository
import com.kampplus.havacan.di.AppContainer
import com.kampplus.havacan.domain.insight.WeatherInsightBuilder
import com.kampplus.havacan.domain.running.RunningSuitabilityCalculator
import com.kampplus.havacan.model.City
import com.kampplus.havacan.model.Forecast
import com.kampplus.havacan.ui.common.ErrorReason
import com.kampplus.havacan.ui.common.LoadState
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
    private val load = MutableStateFlow<LoadState<Forecast>>(LoadState.Loading)
    private var loadJob: Job? = null

    val uiState: StateFlow<HomeUiState> =
        combine(city, mode, load, selectedTime) { city, mode, load, selected ->
            val forecast = (load as? LoadState.Success)?.value
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
            if (load.value !is LoadState.Success || force) load.value = LoadState.Loading
            load.value = try {
                val forecast = repository.forecast(target.latitude, target.longitude, forceRefresh = force)
                if (forecast == null) LoadState.Empty else LoadState.Success(forecast)
            } catch (e: IOException) {
                LoadState.Error(ErrorReason.NoConnection)
            } catch (e: HttpException) {
                LoadState.Error(ErrorReason.Server)
            } catch (e: JsonParseException) {
                LoadState.Error(ErrorReason.Unknown)
            }
        }
    }

    fun retry() = refresh(force = true)

    fun onCityChosen(chosen: City) {
        if (chosen.id == city.value.id) return
        locationStore.save(chosen)
        city.value = chosen
        savedStateHandle[KEY_SELECTED_TIME] = null
        load.value = LoadState.Loading
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
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = AppContainer.get(this[APPLICATION_KEY]!!)
                HomeViewModel(
                    repository = container.weather,
                    locationStore = container.location,
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }
}
