package com.example.weather.ui.city

import androidx.lifecycle.SavedStateHandle
import com.example.weather.data.CityRepository
import com.example.weather.data.remote.CityApi
import com.example.weather.data.sampleCities
import com.example.weather.model.City
import com.example.weather.model.CitySearchResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CityViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val api = FakeCityApi()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        CityViewModel(CityRepository(api), handle)

    /** Collects every state the ViewModel emits while [block] runs. */
    private fun TestScope.statesOf(vm: CityViewModel, block: suspend () -> Unit): List<CityUiState> {
        val states = mutableListOf<CityUiState>()
        backgroundScope.launch(dispatcher) { vm.uiState.toList(states) }
        launch { block() }
        // Plenty of virtual time for debounce + fake network delay
        advanceTimeBy(10_000)
        runCurrent()
        return states
    }

    @Test
    fun blankQueryShowsSuggestionsWithoutCallingApi() = runTest(dispatcher) {
        val vm = viewModel()
        val states = statesOf(vm) {}
        assertEquals(CityUiState.Success(sampleCities, isSuggestion = true), states.last())
        assertEquals(0, api.calls)
    }

    @Test
    fun searchGoesLoadingThenSuccess() = runTest(dispatcher) {
        api.response = CitySearchResponse(listOf(ankara))
        val vm = viewModel()
        val states = statesOf(vm) { vm.onQueryChange("ank") }
        assertEquals(CityUiState.Loading, states[states.size - 2])
        assertEquals(CityUiState.Success(listOf(ankara)), states.last())
    }

    @Test
    fun missingResultsFieldMeansEmpty() = runTest(dispatcher) {
        api.response = CitySearchResponse(results = null)
        val vm = viewModel()
        val states = statesOf(vm) { vm.onQueryChange("xyzq") }
        assertEquals(CityUiState.Empty("xyzq"), states.last())
    }

    @Test
    fun networkFailureIsNoConnectionErrorAndRetryRecovers() = runTest(dispatcher) {
        api.failure = IOException("offline")
        val vm = viewModel()
        val states = statesOf(vm) {
            vm.onQueryChange("ank")
            delay(1_000)
            api.failure = null
            api.response = CitySearchResponse(listOf(ankara))
            vm.retry()
        }
        assertTrue(CityUiState.Error(ErrorReason.NoConnection) in states)
        assertEquals(CityUiState.Success(listOf(ankara)), states.last())
    }

    @Test
    fun httpFailureIsServerError() = runTest(dispatcher) {
        api.failure = HttpException(Response.error<Any>(503, "".toResponseBody()))
        val vm = viewModel()
        val states = statesOf(vm) { vm.onQueryChange("ank") }
        assertEquals(CityUiState.Error(ErrorReason.Server), states.last())
    }

    @Test
    fun typingQuicklyCallsApiOnceForTheFinalQuery() = runTest(dispatcher) {
        val vm = viewModel()
        statesOf(vm) {
            vm.onQueryChange("an")
            advanceTimeBy(100)
            vm.onQueryChange("ank")
            advanceTimeBy(100)
            vm.onQueryChange("anka")
        }
        assertEquals(listOf("anka"), api.queries)
    }

    @Test
    fun turkishResultsComeFirstAndNonPlacesAreDropped() = runTest(dispatcher) {
        val georgian = City(1, "Ankara", 42.0, 43.0, countryCode = "GE", featureCode = "PPL")
        val airport = City(2, "Ankara Esenboğa", 40.1, 32.9, countryCode = "TR", featureCode = "AIRP")
        api.response = CitySearchResponse(listOf(georgian, airport, ankara))
        val vm = viewModel()
        val states = statesOf(vm) { vm.onQueryChange("ankara") }
        assertEquals(CityUiState.Success(listOf(ankara, georgian)), states.last())
    }

    @Test
    fun queryAndSelectionAreRestoredFromSavedState() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        viewModel(handle).apply {
            onQueryChange("ank")
            onCitySelected(ankara)
        }
        val restored = viewModel(handle)
        assertEquals("ank", restored.query)
        assertEquals(ankara.id, restored.selectedCityId)
    }

    private val ankara = City(323786, "Ankara", 39.91987, 32.85427, countryCode = "TR", admin1 = "Ankara", featureCode = "PPLC")

    private class FakeCityApi : CityApi {
        var response = CitySearchResponse(emptyList())
        var failure: Exception? = null
        val queries = mutableListOf<String>()
        val calls get() = queries.size

        override suspend fun searchCities(name: String, count: Int, language: String, format: String): CitySearchResponse {
            queries += name
            delay(50)
            failure?.let { throw it }
            return response
        }
    }
}
