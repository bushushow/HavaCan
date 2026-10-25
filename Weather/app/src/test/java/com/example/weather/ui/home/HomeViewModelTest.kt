package com.example.weather.ui.home

import androidx.lifecycle.SavedStateHandle
import com.example.weather.data.LocationStore
import com.example.weather.data.WeatherRepository
import com.example.weather.data.remote.CurrentDto
import com.example.weather.data.remote.ForecastApi
import com.example.weather.data.remote.ForecastResponse
import com.example.weather.data.remote.HourlyDto
import com.example.weather.data.provinceCities
import com.example.weather.model.City
import com.example.weather.ui.city.ErrorReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val api = FakeForecastApi()
    private val store = FakeStore()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(handle: SavedStateHandle = SavedStateHandle()): HomeViewModel {
        val vm = HomeViewModel(WeatherRepository(api), store, handle)
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        advanceTimeBy(1_000)
        runCurrent()
        return vm
    }

    @Test
    fun loadsForecastForDefaultCity() = runTest(dispatcher) {
        val vm = viewModel()
        val state = vm.uiState.value
        assertEquals("Ankara", state.city.name)
        assertTrue(state.load is WeatherLoadState.Success)
        assertEquals(listOf(39.91987), api.latitudes)
        assertEquals(15.0, state.selectedHour!!.temperature, 0.0)
        assertNotNull(state.runningAdvice)
        assertNotNull(state.insight)
    }

    @Test
    fun selectingHoursAndTogglingModeNeverRefetches() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onHourSelected("2026-10-25T16:00")
        vm.toggleRunnerMode()
        runCurrent()
        val state = vm.uiState.value
        assertEquals(16, state.selectedHour!!.hour)
        assertEquals(HomeMode.Runner, state.mode)
        assertEquals(1, api.latitudes.size)
    }

    @Test
    fun modeAndSelectionSurviveRecreation() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        viewModel(handle).apply {
            toggleRunnerMode()
            onHourSelected("2026-10-25T16:00")
        }
        val restored = viewModel(handle).uiState.value
        assertEquals(HomeMode.Runner, restored.mode)
        assertEquals(16, restored.selectedHour!!.hour)
    }

    @Test
    fun choosingCityLoadsItAndRemembersIt() = runTest(dispatcher) {
        val vm = viewModel()
        val izmir = provinceCities.first { it.name == "İzmir" }
        vm.onCityChosen(izmir)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals("İzmir", vm.uiState.value.city.name)
        assertEquals(izmir, store.saved)
        assertEquals(izmir.latitude, api.latitudes.last(), 0.0)
    }

    @Test
    fun networkErrorThenRetry() = runTest(dispatcher) {
        api.failure = IOException("offline")
        val vm = viewModel()
        assertEquals(WeatherLoadState.Error(ErrorReason.NoConnection), vm.uiState.value.load)
        api.failure = null
        vm.retry()
        advanceTimeBy(1_000)
        runCurrent()
        assertTrue(vm.uiState.value.load is WeatherLoadState.Success)
    }

    @Test
    fun responseWithoutDataIsEmpty() = runTest(dispatcher) {
        api.response = ForecastResponse()
        assertEquals(WeatherLoadState.Empty, viewModel().uiState.value.load)
    }

    private class FakeStore : LocationStore {
        var saved: City? = null
        override fun load() = saved
        override fun save(city: City) { saved = city }
    }

    private class FakeForecastApi : ForecastApi {
        val latitudes = mutableListOf<Double>()
        var failure: Exception? = null
        var response = ForecastResponse(
            timezone = "Europe/Istanbul",
            current = CurrentDto("2026-10-25T14:45", 15.0, 14.0, 1, 1, 6.0, 0.0),
            hourly = HourlyDto(
                time = (14..23).map { "2026-10-25T%02d:00".format(it) },
                temperature = (14..23).map { 15.0 },
                apparentTemperature = (14..23).map { 14.0 },
                precipitationProbability = (14..23).map { 0 },
                precipitation = (14..23).map { 0.0 },
                weatherCode = (14..23).map { 1 },
                windSpeed = (14..23).map { 6.0 },
                windGusts = (14..23).map { 12.0 },
                uvIndex = (14..23).map { 1.0 },
                isDay = (14..23).map { if (it < 18) 1 else 0 },
            ),
        )

        override suspend fun forecast(
            latitude: Double, longitude: Double, current: String, hourly: String,
            daily: String, timezone: String, forecastDays: Int,
        ): ForecastResponse {
            latitudes += latitude
            failure?.let { throw it }
            return response
        }
    }
}
