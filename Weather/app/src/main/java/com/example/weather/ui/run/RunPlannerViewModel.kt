package com.example.weather.ui.run

import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.weather.data.AppGraph
import com.example.weather.data.LocationStore
import com.example.weather.data.PlanRepository
import com.example.weather.data.WeatherRepository
import com.example.weather.domain.running.RunDayOutlook
import com.example.weather.domain.running.RunSlot
import com.example.weather.domain.running.RunSlotPlanner
import com.example.weather.model.City
import com.example.weather.model.Forecast
import com.example.weather.model.Plan
import com.example.weather.model.PlanType
import com.example.weather.model.Reminder
import com.example.weather.model.RunDetails
import com.example.weather.ui.home.HomeViewModel
import com.example.weather.ui.plans.PlanNotifications
import com.example.weather.ui.plans.PlanScheduler
import com.example.weather.ui.plans.WorkPlanScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

enum class RunGoal(val distanceKm: Int?) { Two(2), Five(5), Ten(10), Free(null) }

enum class DayChoice { Today, Tomorrow, Other }

/** The forecast for the run's location, as far as we know it. */
sealed interface ForecastState {
    data object Loading : ForecastState
    data object Failed : ForecastState
    data class Ready(val forecast: Forecast) : ForecastState
}

/** What happened to the reminder when the run was saved; the user is told exactly this. */
enum class ReminderResult { Scheduled, Off, TooLate, NotificationsBlocked }

data class RunSaved(val plan: Plan, val reminder: ReminderResult, val followBlocked: Boolean)

data class RunPlannerUiState(
    val today: LocalDate,
    val date: LocalDate,
    val city: City,
    val placeName: String = "",
    val forecast: ForecastState = ForecastState.Loading,
    val outlook: RunDayOutlook? = null,
    /** The chosen start; null until a suggestion or a manual time is picked. */
    val start: LocalTime? = null,
    /** Score of the chosen start, when the forecast covers it. */
    val selectedSlot: RunSlot? = null,
    val goal: RunGoal = RunGoal.Five,
    val durationMinutes: Int = 30,
    val reminderOn: Boolean = true,
    /** How long before the start the reminder fires. */
    val reminderMinutes: Long = 30,
    val follow: Boolean = true,
    val isEditing: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val saved: RunSaved? = null,
) {
    val dayChoice: DayChoice
        get() = when (date) {
            today -> DayChoice.Today
            today.plusDays(1) -> DayChoice.Tomorrow
            else -> DayChoice.Other
        }

    val suggestions: List<RunSlot> get() = (outlook as? RunDayOutlook.Ready)?.suggestions.orEmpty()

    /** True when the chosen start is not one of the suggestion cards. */
    val manualStart: Boolean get() = start != null && suggestions.none { it.start.toLocalTime() == start }

    val end: LocalTime? get() = start?.plusMinutes(durationMinutes.toLong())

    /** When the reminder fires for the chosen start. */
    val reminderAt: LocalDateTime? get() = start?.let { LocalDateTime.of(date, it).minusMinutes(reminderMinutes) }
}

/**
 * Koşu Planla: suggests start times from the Open-Meteo forecast of the chosen day and place,
 * and saves the run as a [Plan] in the same store Planlarım reads, so it gets the same reminders,
 * weather watch and Plan B as every other plan.
 */
class RunPlannerViewModel(
    private val plans: PlanRepository,
    private val weather: WeatherRepository,
    location: LocationStore,
    private val scheduler: PlanScheduler,
    private val canNotify: () -> Boolean,
    private val planId: String?,
    /** A start time the home screen already suggested, "2026-10-27T07:00". */
    initialStart: String?,
    private val now: () -> LocalDateTime = LocalDateTime::now,
    private val planner: RunSlotPlanner = RunSlotPlanner(),
) : ViewModel() {

    private val id = planId ?: UUID.randomUUID().toString()
    private var existing: Plan? = null

    private val _uiState: MutableStateFlow<RunPlannerUiState>
    val uiState: StateFlow<RunPlannerUiState>

    init {
        val today = now().toLocalDate()
        val suggested = initialStart?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
            ?.takeIf { !it.toLocalDate().isBefore(today) }
        _uiState = MutableStateFlow(
            RunPlannerUiState(
                today = today,
                // Open on the day the home screen suggested, otherwise tomorrow morning's run
                date = suggested?.toLocalDate() ?: today.plusDays(1),
                city = location.load() ?: HomeViewModel.DEFAULT_CITY,
                start = suggested?.toLocalTime(),
                isEditing = planId != null,
            ),
        )
        uiState = _uiState.asStateFlow()
        viewModelScope.launch {
            if (planId != null) plans.get(planId)?.let(::loadExisting)
            loadForecast()
        }
    }

    private fun loadExisting(plan: Plan) {
        existing = plan
        val run = plan.run
        _uiState.update {
            it.copy(
                date = plan.localDate,
                city = plan.city,
                placeName = plan.placeName.orEmpty(),
                start = plan.localTime,
                goal = RunGoal.entries.firstOrNull { g -> g.distanceKm == run?.distanceKm } ?: RunGoal.Free,
                durationMinutes = run?.durationMinutes ?: 30,
                reminderOn = plan.reminderMinutesBefore != null,
                reminderMinutes = plan.reminderMinutesBefore ?: 30,
                follow = plan.follow,
            )
        }
    }

    fun retry() {
        viewModelScope.launch { loadForecast() }
    }

    private suspend fun loadForecast() {
        _uiState.update { it.copy(forecast = ForecastState.Loading) }
        val city = _uiState.value.city
        val forecast = runCatching { weather.forecast(city.latitude, city.longitude) }.getOrNull()
        // The user may have picked another place while this one was loading
        if (_uiState.value.city != city) return
        _uiState.update {
            it.copy(forecast = if (forecast != null) ForecastState.Ready(forecast) else ForecastState.Failed)
        }
        recompute(keepStart = true)
    }

    /** Re-scores the day; picks the best start unless the user already chose one that still fits. */
    private fun recompute(keepStart: Boolean) {
        _uiState.update { s ->
            val forecast = (s.forecast as? ForecastState.Ready)?.forecast
            val outlook = forecast?.let { planner.outlook(it, s.date, s.durationMinutes, now()) }
            val suggestions = (outlook as? RunDayOutlook.Ready)?.suggestions.orEmpty()
            val start = s.start?.takeIf { keepStart && !LocalDateTime.of(s.date, it).isBefore(now()) }
                ?: suggestions.firstOrNull()?.start?.toLocalTime()
            s.copy(
                outlook = outlook,
                start = start,
                selectedSlot = slotFor(forecast, s.date, start, s.durationMinutes),
            )
        }
    }

    private fun slotFor(forecast: Forecast?, date: LocalDate, start: LocalTime?, minutes: Int): RunSlot? =
        if (forecast == null || start == null) null else planner.slot(forecast, LocalDateTime.of(date, start), minutes)

    fun selectDay(choice: DayChoice) {
        val today = now().toLocalDate()
        when (choice) {
            DayChoice.Today -> selectDate(today)
            DayChoice.Tomorrow -> selectDate(today.plusDays(1))
            DayChoice.Other -> Unit
        }
    }

    fun selectDate(date: LocalDate) {
        if (date.isBefore(now().toLocalDate()) || date == _uiState.value.date) return
        _uiState.update { it.copy(date = date, start = null, error = null) }
        recompute(keepStart = false)
    }

    fun selectSlot(slot: RunSlot) = selectStart(slot.start.toLocalTime())

    fun selectStart(time: LocalTime) {
        _uiState.update { s ->
            val forecast = (s.forecast as? ForecastState.Ready)?.forecast
            s.copy(start = time, selectedSlot = slotFor(forecast, s.date, time, s.durationMinutes), error = null)
        }
    }

    fun selectGoal(goal: RunGoal) = _uiState.update { it.copy(goal = goal) }

    fun setDuration(minutes: Int) {
        if (minutes !in DURATION_RANGE) return
        _uiState.update { it.copy(durationMinutes = minutes, error = null) }
        // A longer run can reach into a wetter or darker hour
        recompute(keepStart = true)
    }

    fun setCity(city: City) {
        if (city == _uiState.value.city) return
        _uiState.update { it.copy(city = city, outlook = null, selectedSlot = null) }
        viewModelScope.launch { loadForecast() }
    }

    fun setPlaceName(name: String) = _uiState.update { it.copy(placeName = name.take(60)) }

    fun setReminderOn(on: Boolean) = _uiState.update { it.copy(reminderOn = on) }

    /** Sets how long before the start the reminder fires. */
    fun setReminderMinutes(minutes: Long) {
        if (minutes < 5 || minutes > 24 * 60) return
        _uiState.update { it.copy(reminderMinutes = minutes, reminderOn = true) }
    }

    fun setFollow(on: Boolean) = _uiState.update { it.copy(follow = on) }

    /** True when saving needs the notification permission first; the screen asks, then calls [save]. */
    fun needsNotificationPermission(): Boolean {
        val s = _uiState.value
        return (s.reminderOn || s.follow) && !canNotify()
    }

    /** Validates and stores the run. Ignored while a save is running, so a double tap saves once. */
    fun save() {
        val s = _uiState.value
        if (s.saving || s.saved != null) return
        val start = s.start
        val error = when {
            start == null -> "Önce bir başlangıç saati seç."
            LocalDateTime.of(s.date, start).isBefore(now()) -> "Bu saat geçti; ileri bir saat seç."
            s.durationMinutes !in DURATION_RANGE -> "Süre 5 ile 300 dakika arasında olmalı."
            else -> null
        }
        if (error != null || start == null) {
            _uiState.update { it.copy(error = error) }
            return
        }
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val reminderMinutes = if (s.reminderOn) s.reminderMinutes else null
            val plan = (existing ?: Plan(id = id, title = "", date = "", time = "", city = s.city)).copy(
                title = title(s.goal),
                date = s.date.toString(),
                time = "%02d:%02d".format(start.hour, start.minute),
                city = s.city,
                placeName = s.placeName.trim().ifBlank { null },
                type = PlanType.Outdoor,
                follow = s.follow,
                reminder = Reminder.None,
                reminderMinutes = reminderMinutes,
                lastNotified = null,
                run = RunDetails(s.goal.distanceKm, s.durationMinutes),
            )
            runCatching {
                plans.upsert(plan)
                scheduler.schedule(plan)
            }.onSuccess {
                val notify = canNotify()
                val fireAt = plan.reminderMinutesBefore?.let { plan.dateTime.minusMinutes(it) }
                val reminderResult = when {
                    fireAt == null -> ReminderResult.Off
                    !notify -> ReminderResult.NotificationsBlocked
                    fireAt.isBefore(now()) -> ReminderResult.TooLate
                    else -> ReminderResult.Scheduled
                }
                _uiState.update { it.copy(saving = false, saved = RunSaved(plan, reminderResult, followBlocked = plan.follow && !notify)) }
            }.onFailure {
                _uiState.update { it.copy(saving = false, error = "Plan kaydedilemedi. Tekrar dene.") }
            }
        }
    }

    fun delete(onDone: () -> Unit) {
        val planId = planId ?: return
        viewModelScope.launch {
            plans.delete(planId)
            scheduler.cancel(planId)
            onDone()
        }
    }

    companion object {
        /** "30 dk önce", "1 saat önce", "1 sa 15 dk önce". */
        fun beforeLabel(minutes: Long): String = when {
            minutes < 60 -> "$minutes dk önce"
            minutes % 60 == 0L -> "${minutes / 60} saat önce"
            else -> "${minutes / 60} sa ${minutes % 60} dk önce"
        }
        val DURATIONS = listOf(15, 30, 45, 60)
        val DURATION_RANGE = 5..300

        fun title(goal: RunGoal): String = goal.distanceKm?.let { "$it km koşu" } ?: "Serbest koşu"

        const val KEY_PLAN_ID = "planId"
        const val KEY_START = "start"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                val graph = AppGraph.get(app)
                val handle: SavedStateHandle = createSavedStateHandle()
                RunPlannerViewModel(
                    plans = graph.plans,
                    weather = graph.weather,
                    location = graph.location,
                    scheduler = WorkPlanScheduler(app),
                    canNotify = { PlanNotifications.canNotify(app) && NotificationManagerCompat.from(app).areNotificationsEnabled() },
                    planId = handle.get<String>(KEY_PLAN_ID),
                    initialStart = handle.get<String>(KEY_START),
                )
            }
        }
    }
}
