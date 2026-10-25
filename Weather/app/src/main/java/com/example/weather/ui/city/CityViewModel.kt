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
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.example.weather.data.AssetDistrictSource
import com.example.weather.data.CityRepository
import com.example.weather.data.Countries
import com.example.weather.data.DistrictSource
import com.example.weather.data.TURKIYE
import com.example.weather.data.TurkishProvinces
import com.example.weather.data.filterByQuery
import com.example.weather.data.remote.ApiClient
import com.example.weather.data.provinceCities
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
private const val KEY_COUNTRY = "country"
private const val KEY_BROWSING = "browsingPlate"

/** Everything a search depends on; any change restarts the search. */
private data class SearchRequest(val query: String, val countryCode: String?, val browsingPlate: Int?)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class CityViewModel(
    private val repository: CityRepository,
    private val savedStateHandle: SavedStateHandle,
    private val districts: DistrictSource,
    private val provinces: List<City> = provinceCities,
) : ViewModel() {

    // Compose state for the text field (updated synchronously, so typing never lags),
    // mirrored into SavedStateHandle so it survives rotation and process death.
    var query by mutableStateOf(savedStateHandle.get<String>(KEY_QUERY).orEmpty())
        private set

    var selectedCityId by mutableStateOf(savedStateHandle.get<Long>(KEY_SELECTED))
        private set

    /** ISO country the search is limited to; null means worldwide. Türkiye by default. */
    val countryCode: StateFlow<String?> = savedStateHandle.getStateFlow(KEY_COUNTRY, TURKIYE)

    /** Plate code of the province whose districts are listed, or null. */
    val browsingPlate: StateFlow<Int?> = savedStateHandle.getStateFlow(KEY_BROWSING, null)

    private val queryFlow = MutableStateFlow(query)
    private val retryCount = MutableStateFlow(0)

    val uiState: StateFlow<CityUiState> =
        combine(queryFlow, countryCode, browsingPlate) { q, c, b -> SearchRequest(q, c, b) }
            .debounce { if (it.browsingPlate == null && it.query.isApiQuery()) SEARCH_DEBOUNCE_MS else 0L }
            .combine(retryCount) { request, _ -> request }
            // flatMapLatest cancels the previous search, so a slow old response can't overwrite a newer one
            .flatMapLatest { search(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = CityUiState.Loading,
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

    /** null = worldwide. Leaves any district list and clears the search. */
    fun onCountrySelected(code: String?) {
        savedStateHandle[KEY_COUNTRY] = code
        savedStateHandle[KEY_BROWSING] = null
        onQueryChange("")
    }

    fun onBrowseDistricts(plate: Int) {
        savedStateHandle[KEY_BROWSING] = plate
        onQueryChange("")
    }

    fun onExitDistricts() {
        savedStateHandle[KEY_BROWSING] = null
        onQueryChange("")
    }

    fun retry() {
        retryCount.value++
    }

    private fun search(request: SearchRequest) = flow {
        val (query, country, plate) = request
        if (plate != null) {
            emit(districtResult(plate, query))
            return@flow
        }
        if (!query.isApiQuery()) {
            emit(if (country == TURKIYE) localResult(query) else CityUiState.Prompt(Countries.byCode(country)?.name))
            return@flow
        }
        emit(CityUiState.Loading)
        val state = try {
            val cities = repository.searchCities(query, country)
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

    /** The province's centre first, then its districts; typing filters them locally. */
    private suspend fun districtResult(plate: Int, query: String): CityUiState {
        val province = provinces.firstOrNull { TurkishProvinces.find(it.admin1)?.plate == plate }
            ?: return CityUiState.Empty(query.trim())
        // The central district often has the province's own name ("Kırklareli"); list it once
        val matches = (listOf(province) + districts.districtsOf(plate)).distinctBy { it.name }.filterByQuery(query)
        return if (matches.isEmpty()) {
            CityUiState.Empty(query.trim())
        } else {
            CityUiState.Success(matches, isSuggestion = true, districtsOf = province.name)
        }
    }

    private fun localResult(query: String): CityUiState {
        val matches = provinces.filterByQuery(query)
        return if (matches.isEmpty()) CityUiState.Empty(query.trim()) else CityUiState.Success(matches, isSuggestion = true)
    }

    private fun String.isApiQuery() = trim().length >= MIN_API_QUERY_LENGTH

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CityViewModel(
                    repository = CityRepository(ApiClient.cityApi),
                    savedStateHandle = createSavedStateHandle(),
                    districts = AssetDistrictSource(this[APPLICATION_KEY]!!),
                )
            }
        }
    }
}
