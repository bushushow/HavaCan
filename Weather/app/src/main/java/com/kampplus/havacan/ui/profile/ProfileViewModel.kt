package com.kampplus.havacan.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kampplus.havacan.data.AppGraph
import com.kampplus.havacan.data.ProfilePrefs
import com.kampplus.havacan.data.ProfileStore
import com.kampplus.havacan.ui.plans.PlanScheduler
import com.kampplus.havacan.ui.plans.WorkPlanScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The phone calendar (Google Calendar accounts included) as Profil shows it. */
sealed interface CalendarStatus {
    data object NotConnected : CalendarStatus
    data object Connecting : CalendarStatus

    /** [events] in the next 14 days; null while counting. */
    data class Connected(val events: Int?) : CalendarStatus
    data class Error(val message: String) : CalendarStatus
}

data class LocalStats(val plans: Int = 0, val clothes: Int = 0, val outfits: Int = 0)

class ProfileViewModel(
    private val graph: AppGraph,
    private val store: ProfileStore,
    private val scheduler: PlanScheduler
) : ViewModel() {

    val prefs: StateFlow<ProfilePrefs> = store.prefs

    private val _calendar = MutableStateFlow<CalendarStatus>(CalendarStatus.NotConnected)
    val calendar: StateFlow<CalendarStatus> = _calendar.asStateFlow()

    /** What's stored on the phone, for Gizlilik ve veriler. Read from the same stores the screens use. */
    val stats: StateFlow<LocalStats> =
        combine(graph.plans.plans, graph.wardrobe.items, graph.wardrobe.outfits) { plans, items, outfits ->
            LocalStats(plans.size, items.size, outfits.size)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalStats())

    init {
        viewModelScope.launch { graph.plans.load() }
        refreshCalendar()
    }

    fun update(change: (ProfilePrefs) -> ProfilePrefs) = store.update(change)

    /** Connected only when Android really granted calendar access; the event count proves it reads. */
    fun refreshCalendar() {
        if (!graph.calendar.hasReadPermission()) {
            if (_calendar.value !is CalendarStatus.Error) _calendar.value = CalendarStatus.NotConnected
            return
        }
        _calendar.value = CalendarStatus.Connected(null)
        viewModelScope.launch {
            _calendar.value = runCatching { graph.calendar.upcomingEvents() }.fold(
                onSuccess = { CalendarStatus.Connected(it.size) },
                onFailure = { CalendarStatus.Error("Takvim okunamadı. Biraz sonra tekrar dene.") }
            )
        }
    }

    fun onCalendarRequested() {
        _calendar.value = CalendarStatus.Connecting
    }

    fun onCalendarResult(granted: Boolean) {
        if (granted) {
            refreshCalendar()
        } else {
            _calendar.value = CalendarStatus.Error("İzin verilmedi. Android ayarlarından takvim iznini açabilirsin.")
        }
    }

    // region Gizlilik ve veriler: each one runs only after the user confirmed it on screen

    fun deleteAllPlans() {
        viewModelScope.launch {
            graph.plans.load()
            graph.plans.plans.value.forEach { plan ->
                graph.plans.delete(plan.id)
                scheduler.cancel(plan.id)
            }
        }
    }

    fun deleteWardrobe() {
        viewModelScope.launch {
            graph.wardrobe.outfits.first().forEach { graph.wardrobe.deleteOutfit(it.id) }
            graph.wardrobe.allItems().forEach { graph.wardrobe.delete(it) }
            graph.wardrobe.removeOrphanPhotos()
        }
    }

    fun resetProfile() = store.reset()

    // endregion

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                val graph = AppGraph.get(app)
                ProfileViewModel(graph, graph.profile, WorkPlanScheduler(app))
            }
        }
    }
}
