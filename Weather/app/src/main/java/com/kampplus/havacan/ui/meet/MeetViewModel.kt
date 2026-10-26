package com.kampplus.havacan.ui.meet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kampplus.havacan.data.DEFAULT_CITY
import com.kampplus.havacan.data.LocationStore
import com.kampplus.havacan.data.PlanRepository
import com.kampplus.havacan.data.PlanScheduler
import com.kampplus.havacan.data.WeatherRepository
import com.kampplus.havacan.di.AppContainer
import com.kampplus.havacan.domain.meet.MeetOption
import com.kampplus.havacan.domain.meet.MeetOutcome
import com.kampplus.havacan.domain.meet.MeetPlanner
import com.kampplus.havacan.model.City
import com.kampplus.havacan.model.Forecast
import com.kampplus.havacan.model.MeetDetails
import com.kampplus.havacan.model.MeetPerson
import com.kampplus.havacan.model.MeetWindow
import com.kampplus.havacan.model.Plan
import com.kampplus.havacan.model.PlanType
import com.kampplus.havacan.model.Reminder
import com.kampplus.havacan.model.deg
import com.kampplus.havacan.ui.home.HomeViewModel
import com.kampplus.havacan.ui.plans.WorkPlanScheduler
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MeetUiState(
    val today: LocalDate,
    val title: String = "",
    val people: List<MeetPerson> = emptyList(),
    val date: LocalDate,
    val windows: Set<MeetWindow> = setOf(MeetWindow.Afternoon),
    /** Where to meet; null lets HavaCan pick among everyone's places. */
    val place: City? = null,
    val forecasts: Map<String, Forecast> = emptyMap(),
    val loading: Boolean = false,
    /** Places whose forecast could not be loaded; their weather is shown as unknown, never guessed. */
    val failed: List<String> = emptyList(),
    val outcome: MeetOutcome? = null,
    val selected: Int = 0,
    val saving: Boolean = false,
    val saved: Plan? = null
) {
    val options: List<MeetOption> get() = (outcome as? MeetOutcome.Ready)?.options.orEmpty()
    val chosen: MeetOption? get() = options.getOrNull(selected)

    /** The next best option, offered as Plan B. */
    val planB: MeetOption? get() = options.filterIndexed { i, _ -> i != selected }.firstOrNull()
}

/**
 * Weather Meet: a group meet-up planned around everyone's forecast. The result is saved as an
 * ordinary [Plan], so Planlarım watches the weather, reminds and offers Plan B like for any plan.
 */
class MeetViewModel(
    private val plans: PlanRepository,
    private val weather: WeatherRepository,
    location: LocationStore,
    myName: String,
    private val scheduler: PlanScheduler,
    private val now: () -> LocalDateTime = LocalDateTime::now
) : ViewModel() {

    private val _uiState: MutableStateFlow<MeetUiState>
    val uiState: StateFlow<MeetUiState>

    init {
        val today = now().toLocalDate()
        val me = MeetPerson(myName.ifBlank { "Sen" }, location.load() ?: DEFAULT_CITY)
        _uiState = MutableStateFlow(MeetUiState(today = today, people = listOf(me), date = today.plusDays(1)))
        uiState = _uiState.asStateFlow()
        loadForecasts()
    }

    fun setTitle(title: String) = _uiState.update { it.copy(title = title.take(50)) }

    fun addPerson(name: String, city: City) {
        val clean = name.trim().ifBlank { return }
        _uiState.update { it.copy(people = it.people + MeetPerson(clean.take(30), city), selected = 0) }
        loadForecasts()
    }

    fun removePerson(index: Int) {
        // The first row is the user; they're always part of their own meet-up
        if (index <= 0) return
        _uiState.update { it.copy(people = it.people.filterIndexed { i, _ -> i != index }, selected = 0) }
        recompute()
    }

    fun selectDate(date: LocalDate) {
        if (date.isBefore(now().toLocalDate())) return
        _uiState.update { it.copy(date = date, selected = 0) }
        recompute()
    }

    fun toggleWindow(window: MeetWindow) {
        _uiState.update { s ->
            val next = if (window in s.windows) s.windows - window else s.windows + window
            // At least one part of the day stays chosen
            if (next.isEmpty()) s else s.copy(windows = next, selected = 0)
        }
        recompute()
    }

    fun setPlace(city: City?) {
        _uiState.update { it.copy(place = city, selected = 0) }
        loadForecasts()
    }

    fun selectOption(index: Int) = _uiState.update { it.copy(selected = index) }

    fun retry() {
        _uiState.update { it.copy(failed = emptyList()) }
        loadForecasts(retryFailed = true)
    }

    /** Fetches every place's forecast not loaded yet, then re-plans. */
    private fun loadForecasts(retryFailed: Boolean = false) {
        val s = _uiState.value
        val cities = (s.people.map { it.city } + listOfNotNull(s.place)).distinctBy(::key)
            .filter { key(it) !in s.forecasts && (retryFailed || it.name !in s.failed) }
        if (cities.isEmpty()) {
            recompute()
            return
        }
        _uiState.update { it.copy(loading = true) }
        viewModelScope.launch {
            for (city in cities) {
                val forecast = runCatching { weather.forecast(city.latitude, city.longitude) }.getOrNull()
                _uiState.update {
                    if (forecast != null) {
                        it.copy(forecasts = it.forecasts + (key(city) to forecast), failed = it.failed - city.name)
                    } else {
                        it.copy(failed = (it.failed + city.name).distinct())
                    }
                }
            }
            _uiState.update { it.copy(loading = false) }
            recompute()
        }
    }

    private fun recompute() {
        _uiState.update { s ->
            val places = listOfNotNull(s.place).ifEmpty { s.people.map { it.city } }
            val outcome = MeetPlanner.plan(s.people, places, s.date, s.windows, { s.forecasts[key(it)] }, now())
            s.copy(outcome = outcome, selected = s.selected.coerceIn(0, ((outcome as? MeetOutcome.Ready)?.options?.size ?: 1) - 1))
        }
    }

    /** Saves the chosen option as a plan; a second tap while saving does nothing. */
    fun save() {
        val s = _uiState.value
        val option = s.chosen ?: return
        if (s.saving || s.saved != null) return
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            val plan = Plan(
                id = UUID.randomUUID().toString(),
                title = s.title.trim().ifBlank { defaultTitle(s.people) },
                date = option.time.toLocalDate().toString(),
                time = "%02d:00".format(option.time.hour),
                city = option.place,
                type = PlanType.Outdoor,
                follow = true,
                reminder = Reminder.OneHour,
                meet = MeetDetails(s.people)
            )
            runCatching {
                plans.upsert(plan)
                scheduler.schedule(plan)
            }.onSuccess {
                _uiState.update { it.copy(saving = false, saved = plan) }
            }.onFailure {
                _uiState.update { it.copy(saving = false) }
            }
        }
    }

    companion object {
        private val TURKISH = Locale.forLanguageTag("tr")
        private val InviteDate = DateTimeFormatter.ofPattern("d MMMM EEEE", TURKISH)

        fun key(city: City) = "%.3f,%.3f".format(Locale.ROOT, city.latitude, city.longitude)

        fun defaultTitle(people: List<MeetPerson>): String =
            "Buluşma: " + people.drop(1).joinToString(", ") { it.name }.ifBlank { people.firstOrNull()?.name.orEmpty() }

        /** A ready-to-send invitation with the real forecast for the chosen time and place. */
        fun inviteText(title: String, option: MeetOption): String {
            val h = option.hour
            val rain = h.precipitationProbability?.let { ", yağış ihtimali %$it" }.orEmpty()
            return buildString {
                appendLine("☀️ $title")
                appendLine("📅 ${option.time.format(InviteDate)}, ${"%02d.00".format(option.time.hour)}")
                appendLine("📍 ${option.place.name}")
                appendLine("🌤 ${h.condition.label}, ${h.temperature.deg()}°$rain")
                append("HavaCan ile planlandı.")
            }
        }

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                val graph = AppContainer.get(app)
                MeetViewModel(
                    plans = graph.plans,
                    weather = graph.weather,
                    location = graph.location,
                    myName = graph.profile.prefs.value.name,
                    scheduler = graph.scheduler
                )
            }
        }
    }
}
