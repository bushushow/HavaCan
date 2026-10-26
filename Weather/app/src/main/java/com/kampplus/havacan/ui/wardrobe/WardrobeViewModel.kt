package com.kampplus.havacan.ui.wardrobe

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kampplus.havacan.data.AppGraph
import com.kampplus.havacan.data.LocationStore
import com.kampplus.havacan.data.WeatherRepository
import com.kampplus.havacan.data.wardrobe.AnalysisResult
import com.kampplus.havacan.data.wardrobe.ClothingCategory
import com.kampplus.havacan.data.wardrobe.ClothingItem
import com.kampplus.havacan.data.wardrobe.SavedOutfit
import com.kampplus.havacan.data.wardrobe.Season
import com.kampplus.havacan.data.wardrobe.WardrobeRepository
import com.kampplus.havacan.domain.wardrobe.OutfitEngine
import com.kampplus.havacan.domain.wardrobe.OutfitResult
import com.kampplus.havacan.domain.wardrobe.WeatherContext
import com.kampplus.havacan.ui.home.HomeViewModel
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AnalysisState { Idle, Analyzing, Recognized, Failed, NotConfigured }

/** The add/edit form. Lives in the ViewModel so rotation or a trip to the camera loses nothing. */
data class EditorState(
    val itemId: String? = null,
    val photoPath: String? = null,
    /** Photo the item had before editing, deleted only once the new one is saved. */
    val originalPhotoPath: String? = null,
    val importing: Boolean = false,
    val analysis: AnalysisState = AnalysisState.Idle,
    val name: String = "",
    val category: ClothingCategory? = null,
    val colorName: String? = null,
    val seasons: Set<Season> = emptySet(),
    val error: String? = null,
    val photoError: String? = null
) {
    val isNew: Boolean get() = itemId == null
    val canSave: Boolean get() = photoPath != null && name.isNotBlank() && category != null && colorName != null && !importing
}

data class SavedOutfitView(val outfit: SavedOutfit, val items: List<ClothingItem>)

data class WardrobeUiState(
    val items: List<ClothingItem> = emptyList(),
    val savedOutfits: List<SavedOutfitView> = emptyList(),
    val filter: ClothingCategory? = null
)

/** The current outfit; [generation] bumps on every shake so the screen can replay its animation. */
data class OutfitState(val result: OutfitResult? = null, val generation: Int = 0, val saved: Boolean = false)

class WardrobeViewModel(
    private val repository: WardrobeRepository,
    private val weather: WeatherRepository,
    private val location: LocationStore,
    private val today: () -> LocalDate = LocalDate::now,
    private val random: Random = Random.Default
) : ViewModel() {

    private val filter = MutableStateFlow<ClothingCategory?>(null)

    val uiState: StateFlow<WardrobeUiState> =
        combine(repository.items, repository.outfits, filter) { items, outfits, filter ->
            val byId = items.associateBy { it.id }
            WardrobeUiState(
                items = if (filter == null) items else items.filter { it.category == filter },
                // A saved outfit whose piece was deleted is dropped rather than shown incomplete
                savedOutfits = outfits.mapNotNull { o ->
                    o.itemIds.map {
                        byId[it]
                    }.takeIf { pieces -> pieces.all { it != null } }?.let { SavedOutfitView(o, it.filterNotNull()) }
                },
                filter = filter
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WardrobeUiState())

    val allItemCount: StateFlow<Int> =
        repository.items.combine(filter) { items, _ -> items.size }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _weatherContext = MutableStateFlow<WeatherContext?>(null)
    val weatherContext: StateFlow<WeatherContext?> = _weatherContext.asStateFlow()

    private val _outfit = MutableStateFlow(OutfitState())
    val outfit: StateFlow<OutfitState> = _outfit.asStateFlow()

    private val _editor = MutableStateFlow(EditorState())
    val editor: StateFlow<EditorState> = _editor.asStateFlow()

    val aiConfigured: Boolean get() = repository.aiConfigured

    init {
        refreshWeather()
        viewModelScope.launch { runCatching { repository.removeOrphanPhotos() } }
    }

    fun setFilter(category: ClothingCategory?) {
        filter.value = category
    }

    fun refreshWeather() {
        viewModelScope.launch {
            val city = location.load() ?: HomeViewModel.DEFAULT_CITY
            val forecast = runCatching { weather.forecast(city.latitude, city.longitude) }.getOrNull()
            _weatherContext.value = forecast?.let { f ->
                val nextHours = f.upcoming.take(6)
                WeatherContext(
                    placeName = city.name,
                    feelsLike = f.current.apparentTemperature,
                    rainLikely =
                    f.current.condition.isWet || nextHours.any { (it.precipitationProbability ?: 0) >= 50 || it.condition.isWet },
                    windKmh = f.current.windSpeed ?: 0.0
                )
            }
        }
    }

    /** Shake and the button both land here. Uses only pieces stored in Room. */
    fun generateOutfit() {
        viewModelScope.launch {
            val items = repository.allItems()
            val previous = (_outfit.value.result as? OutfitResult.Ready)?.items?.map { it.id }.orEmpty().toSet()
            val result = OutfitEngine.build(items, _weatherContext.value, today().monthValue, random, avoid = previous)
            _outfit.update { OutfitState(result, it.generation + 1, saved = false) }
        }
    }

    fun saveOutfit() {
        val ready = _outfit.value.result as? OutfitResult.Ready ?: return
        if (_outfit.value.saved) return
        viewModelScope.launch {
            repository.saveOutfit(ready.items.map { it.id }, ready.reason)
            _outfit.update { it.copy(saved = true) }
        }
    }

    fun deleteSavedOutfit(id: String) {
        viewModelScope.launch { repository.deleteOutfit(id) }
    }

    // --- Add / edit a piece

    fun startNew(category: ClothingCategory? = null) {
        cancelEditor()
        _editor.value = EditorState(category = category)
    }

    fun startEdit(item: ClothingItem) {
        cancelEditor()
        _editor.value = EditorState(
            itemId = item.id,
            photoPath = item.photoPath,
            originalPhotoPath = item.photoPath,
            name = item.name,
            category = item.category,
            colorName = item.colorName,
            seasons = item.seasons,
            analysis = AnalysisState.Idle
        )
    }

    fun cameraFile(): File = repository.cameraFile()

    fun onPhotoPicked(uri: Uri) {
        _editor.update { it.copy(importing = true, photoError = null) }
        viewModelScope.launch {
            val path = runCatching { repository.importPhoto(uri) }.getOrElse { e ->
                Log.w("Wardrobe", "Photo import failed for $uri", e)
                _editor.update { it.copy(importing = false, photoError = "Fotoğraf eklenemedi. Başka bir fotoğraf dene.") }
                return@launch
            }
            // A replaced, never-saved photo is not kept around
            _editor.value.photoPath?.takeIf { it != _editor.value.originalPhotoPath }?.let(repository::discardPhoto)
            _editor.update { it.copy(photoPath = path, importing = false, analysis = AnalysisState.Analyzing) }

            // Offline first: colour from the pixels, then let the AI fill what the user hasn't
            repository.detectColor(path)?.let { color ->
                _editor.update { if (it.colorName == null) it.copy(colorName = color.name) else it }
            }
            when (val result = repository.analyze(path)) {
                is AnalysisResult.Recognized -> _editor.update { e ->
                    val g = result.guess
                    e.copy(
                        analysis = if (g.category != null) AnalysisState.Recognized else AnalysisState.Failed,
                        name = e.name.ifBlank { g.name.orEmpty() },
                        category = e.category ?: g.category,
                        colorName = g.colorName ?: e.colorName,
                        seasons = e.seasons.ifEmpty { g.seasons }
                    )
                }
                is AnalysisResult.Unavailable -> _editor.update {
                    it.copy(
                        analysis = if (result.reason ==
                            AnalysisResult.Reason.NotConfigured
                        ) {
                            AnalysisState.NotConfigured
                        } else {
                            AnalysisState.Failed
                        }
                    )
                }
            }
        }
    }

    fun setName(value: String) = _editor.update { it.copy(name = value.take(40), error = null) }
    fun setCategory(value: ClothingCategory) = _editor.update { it.copy(category = value, error = null) }
    fun setColor(value: String) = _editor.update { it.copy(colorName = value, error = null) }
    fun toggleSeason(value: Season) = _editor.update { e ->
        e.copy(seasons = if (value in e.seasons) e.seasons - value else e.seasons + value)
    }

    /** Returns true when saved. */
    fun saveEditor(onSaved: () -> Unit) {
        val e = _editor.value
        val path = e.photoPath
        val category = e.category
        val color = e.colorName
        if (path == null || category == null || color == null || e.name.isBlank()) {
            _editor.update { it.copy(error = "Fotoğraf, ad, kategori ve renk gerekli.") }
            return
        }
        viewModelScope.launch {
            repository.save(
                ClothingItem(
                    id = e.itemId ?: UUID.randomUUID().toString(),
                    photoPath = path,
                    name = e.name.trim(),
                    category = category,
                    colorName = color,
                    seasons = e.seasons,
                    createdAt = e.itemId?.let { repository.item(it)?.createdAt } ?: System.currentTimeMillis()
                )
            )
            e.originalPhotoPath?.takeIf { it != path }?.let(repository::discardPhoto)
            _editor.value = EditorState()
            onSaved()
        }
    }

    fun deleteEditing(onDeleted: () -> Unit) {
        val id = _editor.value.itemId ?: return
        viewModelScope.launch {
            repository.item(id)?.let { repository.delete(it) }
            _editor.value.photoPath?.takeIf { it != _editor.value.originalPhotoPath }?.let(repository::discardPhoto)
            _editor.value = EditorState()
            onDeleted()
        }
    }

    /** Leaving the form without saving: drop a newly imported photo. */
    fun cancelEditor() {
        val e = _editor.value
        e.photoPath?.takeIf { it != e.originalPhotoPath }?.let(repository::discardPhoto)
        _editor.value = EditorState()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val graph = AppGraph.get(this[APPLICATION_KEY]!!)
                WardrobeViewModel(graph.wardrobe, graph.weather, graph.location)
            }
        }
    }
}
