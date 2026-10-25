package com.example.weather.ui.city

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.weather.data.CityRepository
import com.example.weather.data.filterByQuery
import com.example.weather.data.remote.ApiClient
import com.example.weather.data.sampleCities
import com.example.weather.model.City
import com.google.gson.JsonParseException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import retrofit2.HttpException
import java.io.IOException

/** Queries shorter than this filter the built-in list; longer ones hit the API. */
const val MIN_API_QUERY_LENGTH = 2
internal const val SEARCH_DEBOUNCE_MS = 400L

private const val KEY_QUERY = "query"
private const val KEY_SELECTED = "selectedCityId"

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class CityViewModel(
    private val repository: CityRepository,
    private val savedStateHandle: SavedStateHandle,
    private val suggestions: List<City> = sampleCities,
) : ViewModel() {

    // Compose state for the text field (updated synchronously, so typing never lags),
    // mirrored into SavedStateHandle so it survives rotation and process death.
    var query by mutableStateOf(savedStateHandle.get<String>(KEY_QUERY).orEmpty())
        private set

    var selectedCityId by mutableStateOf(savedStateHandle.get<Long>(KEY_SELECTED))
        private set

    private val queryFlow = MutableStateFlow(query)
    private val retryCount = MutableStateFlow(0)

    val uiState: StateFlow<CityUiState> =
        combine(
            queryFlow.debounce { if (it.isApiQuery()) SEARCH_DEBOUNCE_MS else 0L },
            retryCount,
        ) { query, _ -> query }
            // flatMapLatest cancels the previous search, so a slow old response can't overwrite a newer one
            .flatMapLatest { query -> search(query) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = if (query.isApiQuery()) CityUiState.Loading else localResult(query),
            )

    fun onQueryChange(value: String) {
        query = value
        savedStateHandle[KEY_QUERY] = value
        queryFlow.value = value
    }

    fun onCitySelected(city: City) {
        selectedCityId = city.id
        savedStateHandle[KEY_SELECTED] = city.id
    }

    fun retry() {
        retryCount.value++
    }

    private fun search(query: String) = flow {
        if (!query.isApiQuery()) {
            emit(localResult(query))
            return@flow
        }
        emit(CityUiState.Loading)
        val state = try {
            val cities = repository.searchCities(query)
            if (cities.isEmpty()) CityUiState.Empty(query.trim()) else CityUiState.Success(cities)
        } catch (e: IOException) {
            CityUiState.Error(ErrorReason.NoConnection)
        } catch (e: HttpException) {
            CityUiState.Error(ErrorReason.Server)
        } catch (e: JsonParseException) {
            CityUiState.Error(ErrorReason.Unknown)
        }
        // CancellationException is deliberately not caught: it must propagate for flatMapLatest to work
        emit(state)
    }

    private fun localResult(query: String): CityUiState {
        val matches = suggestions.filterByQuery(query)
        return if (matches.isEmpty()) CityUiState.Empty(query.trim()) else CityUiState.Success(matches, isSuggestion = true)
    }

    private fun String.isApiQuery() = trim().length >= MIN_API_QUERY_LENGTH

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CityViewModel(
                    repository = CityRepository(ApiClient.cityApi),
                    savedStateHandle = createSavedStateHandle(),
                )
            }
        }
    }
}
