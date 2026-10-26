package com.kampplus.havacan.ui.city

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.gson.JsonParseException
import com.kampplus.havacan.data.AndroidDeviceLocator
import com.kampplus.havacan.data.AssetDistrictSource
import com.kampplus.havacan.data.CityRepository
import com.kampplus.havacan.data.DeviceLocator
import com.kampplus.havacan.data.Fix
import com.kampplus.havacan.data.PlaceDirectory
import com.kampplus.havacan.data.PlaceItem
import com.kampplus.havacan.data.PreferencesRecentPlaces
import com.kampplus.havacan.data.RecentPlaces
import com.kampplus.havacan.data.remote.ApiClient
import com.kampplus.havacan.model.City
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException

internal const val SEARCH_DEBOUNCE_MS = 250L
private const val KEY_QUERY = "query"
private const val KEY_DISTRICT_QUERY = "districtQuery"
private const val KEY_PLATE = "plate"

enum class ErrorReason { NoConnection, Server, Unknown }

sealed interface SearchState {
    /** Nothing typed: the screen shows recents and the 81 provinces. */
    data object Idle : SearchState
    data object Loading : SearchState

    /** [online] is true when the results came from Open-Meteo (places abroad). */
    data class Results(val items: List<PlaceItem>, val online: Boolean = false) : SearchState
    data class Empty(val query: String) : SearchState
    data class Error(val reason: ErrorReason) : SearchState
}

sealed interface LocateState {
    data object Idle : LocateState
    data object Locating : LocateState
    data class Failed(val message: String, val openSettings: Boolean = false) : LocateState
}

/** The province page: its districts, filtered on the phone as the user types. */
data class DistrictPage(val province: PlaceItem, val districts: List<PlaceItem>)

/**
 * Konum seç: local search over all provinces and districts (Open-Meteo only when nothing local
 * matches, e.g. abroad), recents from real picks, and the phone's own position.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class LocationPickerViewModel(
    private val directory: PlaceDirectory,
    private val repository: CityRepository,
    private val recents: RecentPlaces,
    private val locator: DeviceLocator,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Compose state for the text fields (no lag while typing), mirrored for process death
    var query by mutableStateOf(savedStateHandle.get<String>(KEY_QUERY).orEmpty())
        private set
    var districtQuery by mutableStateOf(savedStateHandle.get<String>(KEY_DISTRICT_QUERY).orEmpty())
        private set

    val provinces: List<PlaceItem> = directory.provinceItems

    private val _recentPlaces = MutableStateFlow(recents.load())
    val recentPlaces: StateFlow<List<City>> = _recentPlaces.asStateFlow()

    val plate: StateFlow<Int?> = savedStateHandle.getStateFlow(KEY_PLATE, null)

    private val queryFlow = MutableStateFlow(query)
    private val districtQueryFlow = MutableStateFlow(districtQuery)
    private val retryCount = MutableStateFlow(0)

    val search: StateFlow<SearchState> = queryFlow
        .debounce { if (it.isBlank()) 0L else SEARCH_DEBOUNCE_MS }
        .combine(retryCount) { q, _ -> q }
        .flatMapLatest { q -> searchFlow(q) } // a slow old search can't overwrite a newer one
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchState.Idle)

    val districtPage: StateFlow<DistrictPage?> = combine(plate, districtQueryFlow) { p, q -> p to q }
        .flatMapLatest { (p, q) ->
            flow {
                val province = p?.let(directory::provinceByPlate)
                if (province == null) {
                    emit(null)
                } else {
                    val needle = com.kampplus.havacan.data.normalizeForSearch(q)
                    val districts = directory.districtsOf(p).filter {
                        needle.isEmpty() ||
                            com.kampplus.havacan.data.normalizeForSearch(it.city.name).contains(needle)
                    }
                    emit(DistrictPage(province, districts))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _locate = MutableStateFlow<LocateState>(LocateState.Idle)
    val locate: StateFlow<LocateState> = _locate.asStateFlow()

    /** A place the user picked (or that their position resolved to); the screen confirms and closes. */
    // A channel, so a pick made while the screen isn't collecting (rotation) is delivered, not dropped
    private val _picked = Channel<PlaceItem>(Channel.BUFFERED)
    val picked: Flow<PlaceItem> = _picked.receiveAsFlow()

    private var locateJob: Job? = null

    fun onQueryChange(value: String) {
        query = value
        savedStateHandle[KEY_QUERY] = value
        queryFlow.value = value
    }

    fun onDistrictQueryChange(value: String) {
        districtQuery = value
        savedStateHandle[KEY_DISTRICT_QUERY] = value
        districtQueryFlow.value = value
    }

    fun openProvince(item: PlaceItem) {
        val plate = item.province?.plate ?: return pick(item)
        onDistrictQueryChange("")
        savedStateHandle[KEY_PLATE] = plate
    }

    fun closeProvince() {
        savedStateHandle[KEY_PLATE] = null
        onDistrictQueryChange("")
    }

    fun retry() {
        retryCount.value++
    }

    /** Remembers the pick for "Son konumlar" and hands it to the screen. */
    fun pick(item: PlaceItem) {
        recents.add(item.city)
        _recentPlaces.value = recents.load()
        _picked.trySend(item)
    }

    fun pickRecent(city: City) {
        val kind = when {
            city.countryCode != "TR" -> PlaceItem.Kind.Abroad
            provinces.any { it.city.id == city.id } -> PlaceItem.Kind.Province
            else -> PlaceItem.Kind.District
        }
        pick(PlaceItem(city, kind))
    }

    /** Called once the location permission was granted (or already was). */
    fun useCurrentLocation() {
        locateJob?.cancel()
        _locate.value = LocateState.Locating
        locateJob = viewModelScope.launch {
            _locate.value = when (val fix = locator.locate()) {
                Fix.NoPermission -> LocateState.Failed("Konum izni verilmedi. Şehrini listeden seçebilirsin.", openSettings = true)
                Fix.ServicesOff -> LocateState.Failed(
                    "Telefonun konum servisi kapalı. Açıp tekrar dene ya da listeden seç.",
                    openSettings = true
                )
                Fix.NotFound -> LocateState.Failed("Konumun şu an bulunamadı. Biraz sonra tekrar dene ya da listeden seç.")
                is Fix.Found -> {
                    val near = directory.nearest(fix.latitude, fix.longitude)
                    if (near != null) {
                        pick(near)
                        LocateState.Idle
                    } else {
                        // Abroad: use the phone's own name for the place, or say we couldn't name it
                        val named = runCatching { locator.nameOf(fix.latitude, fix.longitude) }.getOrNull()
                        if (named == null) {
                            LocateState.Failed("Bulunduğun yerin adını öğrenemedim. Şehrini aramayla seçebilirsin.")
                        } else {
                            val city = City(
                                id = -"%.3f,%.3f".format(java.util.Locale.ROOT, fix.latitude, fix.longitude).hashCode().toLong().let {
                                    if (it ==
                                        0L
                                    ) {
                                        1L
                                    } else {
                                        kotlin.math.abs(it)
                                    }
                                },
                                name = named.first,
                                latitude = fix.latitude,
                                longitude = fix.longitude,
                                country = named.second
                            )
                            pick(PlaceItem(city, PlaceItem.Kind.Abroad))
                            LocateState.Idle
                        }
                    }
                }
            }
        }
    }

    fun onLocationPermissionDenied() {
        _locate.value = LocateState.Failed("Konum izni verilmedi. Şehrini listeden seçebilirsin.", openSettings = true)
    }

    fun dismissLocateError() {
        _locate.value = LocateState.Idle
    }

    private fun searchFlow(q: String) = flow {
        if (q.isBlank()) {
            emit(SearchState.Idle)
            return@flow
        }
        val local = directory.search(q)
        if (local.isNotEmpty()) {
            emit(SearchState.Results(local))
            return@flow
        }
        if (q.trim().length < 2) {
            emit(SearchState.Empty(q.trim()))
            return@flow
        }
        // Nothing in Türkiye's provinces/districts: ask Open-Meteo (places abroad, villages)
        emit(SearchState.Loading)
        val state = try {
            val found = repository.searchCities(q).map { c ->
                PlaceItem(c, if (c.countryCode == "TR") PlaceItem.Kind.District else PlaceItem.Kind.Abroad)
            }
            if (found.isEmpty()) SearchState.Empty(q.trim()) else SearchState.Results(found, online = true)
        } catch (e: IOException) {
            SearchState.Error(ErrorReason.NoConnection)
        } catch (e: HttpException) {
            SearchState.Error(ErrorReason.Server)
        } catch (e: JsonParseException) {
            SearchState.Error(ErrorReason.Unknown)
        }
        emit(state)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                LocationPickerViewModel(
                    directory = PlaceDirectory(AssetDistrictSource(app)),
                    repository = CityRepository(ApiClient.cityApi),
                    recents = PreferencesRecentPlaces(app),
                    locator = AndroidDeviceLocator(app),
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }
}
