package com.kampplus.havacan.ui.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kampplus.havacan.data.CalendarEvent
import com.kampplus.havacan.data.CalendarGateway
import com.kampplus.havacan.data.DEFAULT_CITY
import com.kampplus.havacan.data.LocationStore
import com.kampplus.havacan.data.PlanRepository
import com.kampplus.havacan.data.PlanScheduler
import com.kampplus.havacan.data.WeatherRepository
import com.kampplus.havacan.di.AppContainer
import com.kampplus.havacan.domain.plans.PlanAdvisor
import com.kampplus.havacan.domain.plans.PlanOutlook
import com.kampplus.havacan.model.City
import com.kampplus.havacan.model.Forecast
import com.kampplus.havacan.model.HourlyWeather
import com.kampplus.havacan.model.Plan
import com.kampplus.havacan.model.PlanSource
import com.kampplus.havacan.model.PlanType
import com.kampplus.havacan.model.WeatherCondition
import com.kampplus.havacan.ui.home.HomeViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DayCell(val date: LocalDate, val dayName: String, val condition: WeatherCondition?, val isToday: Boolean)

data class PlanItem(val plan: Plan, val outlook: PlanOutlook, val alternative: HourlyWeather?)

data class PlansUiState(
    val today: LocalDate,
    val homeCity: City,
    val days: List<DayCell> = emptyList(),
    val selectedDate: LocalDate? = null,
    val items: List<PlanItem> = emptyList(),
    /** The soonest plan at risk that has a drier hour nearby. */
    val suggestion: PlanItem? = null,
    val calendarConnected: Boolean = false,
    val hasAnyPlans: Boolean = false
)

/** What the screen needs from the phone calendar. */
interface CalendarSource {
    fun hasPermission(): Boolean
    suspend fun upcomingEvents(): List<CalendarEvent>
}

class PlansViewModel(
    private val plans: PlanRepository,
    private val weather: WeatherRepository,
    private val location: LocationStore,
    private val calendar: CalendarSource,
    private val scheduler: PlanScheduler,
    private val now: () -> LocalDateTime = LocalDateTime::now
) : ViewModel() {

    private val homeCity = MutableStateFlow(location.load() ?: DEFAULT_CITY)
    private val forecasts = MutableStateFlow<Map<String, Forecast>>(emptyMap())
    private val selectedDate = MutableStateFlow<LocalDate?>(null)
    private val calendarConnected = MutableStateFlow(calendar.hasPermission())

    val uiState: StateFlow<PlansUiState> =
        combine(plans.plans, forecasts, selectedDate, calendarConnected, homeCity) { all, forecasts, selected, connected, home ->
            buildState(all, forecasts, selected, connected, home)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlansUiState(now().toLocalDate(), homeCity.value))

    init {
        viewModelScope.launch {
            plans.load()
            plans.plans.collect { refreshForecasts(it) }
        }
    }

    /** Called when the tab is shown: the home city may have changed, the calendar may have new events. */
    fun refresh() {
        homeCity.value = location.load() ?: DEFAULT_CITY
        calendarConnected.value = calendar.hasPermission()
        viewModelScope.launch {
            if (calendarConnected.value) importCalendar()
            refreshForecasts(plans.plans.value)
        }
    }

    fun onCalendarPermissionResult(granted: Boolean) {
        calendarConnected.value = granted
        if (granted) viewModelScope.launch { importCalendar() }
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = if (selectedDate.value == date) null else date
    }

    fun save(plan: Plan) {
        viewModelScope.launch {
            plans.upsert(plan)
            scheduler.schedule(plan)
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            plans.delete(id)
            scheduler.cancel(id)
        }
    }

    /** Plan B: move the plan to [hour] on the same day. */
    fun moveTo(plan: Plan, hour: HourlyWeather) = save(plan.copy(time = "%02d:00".format(hour.hour), lastNotified = null))

    /** Plan B: same time, indoors; HavaCan stops flagging it. */
    fun moveIndoors(plan: Plan) = save(plan.copy(type = PlanType.Indoor, lastNotified = null))

    /** Plan B: keep everything and let HavaCan keep watching. */
    fun keepAndFollow(plan: Plan) = save(plan.copy(follow = true))

    fun newPlanId(): String = UUID.randomUUID().toString()

    private suspend fun importCalendar() {
        val events = runCatching { calendar.upcomingEvents() }.getOrDefault(emptyList())
        if (events.isEmpty()) return
        val existing = plans.plans.value.associateBy { it.id }
        val imported = events.map { CalendarGateway.toPlan(it, homeCity.value, existing["cal-${it.id}"]) }
        plans.upsertAll(imported)
        imported.forEach(scheduler::schedule)
    }

    private suspend fun refreshForecasts(all: List<Plan>) {
        val today = now().toLocalDate()
        val cities = (all.filter { !it.localDate.isBefore(today) }.map { it.city } + homeCity.value).distinctBy { key(it) }
        for (city in cities) {
            val forecast = runCatching { weather.forecast(city.latitude, city.longitude) }.getOrNull() ?: continue
            forecasts.value = forecasts.value + (key(city) to forecast)
        }
    }

    private fun buildState(
        all: List<Plan>,
        forecasts: Map<String, Forecast>,
        selected: LocalDate?,
        connected: Boolean,
        home: City
    ): PlansUiState {
        val at = now()
        val today = at.toLocalDate()
        val homeDaily = forecasts[key(home)]?.daily.orEmpty().associateBy { it.date }
        val days = (0L until 7L).map { offset ->
            val date = today.plusDays(offset)
            DayCell(
                date = date,
                dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, TURKISH).replaceFirstChar { it.titlecase(TURKISH) },
                condition = homeDaily[date.toString()]?.condition,
                isToday = offset == 0L
            )
        }
        val upcoming = all
            .map { plan ->
                val forecast = forecasts[key(plan.city)]
                val outlook = PlanAdvisor.outlook(plan, forecast, at)
                val alternative = if (outlook is PlanOutlook.Risk) PlanAdvisor.alternative(plan, forecast, at) else null
                PlanItem(plan, outlook, alternative)
            }
            .filter { it.outlook !is PlanOutlook.Past }
            .sortedBy { it.plan.dateTime }
        return PlansUiState(
            today = today,
            homeCity = home,
            days = days,
            selectedDate = selected,
            items = if (selected == null) upcoming else upcoming.filter { it.plan.localDate == selected },
            suggestion = upcoming.firstOrNull { it.outlook is PlanOutlook.Risk && it.alternative != null },
            calendarConnected = connected,
            hasAnyPlans = upcoming.isNotEmpty()
        )
    }

    private fun key(city: City) = "%.3f,%.3f".format(Locale.ROOT, city.latitude, city.longitude)

    companion object {
        private val TURKISH = Locale.forLanguageTag("tr")

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                val graph = AppContainer.get(app)
                PlansViewModel(
                    plans = graph.plans,
                    weather = graph.weather,
                    location = graph.location,
                    calendar = object : CalendarSource {
                        override fun hasPermission() = graph.calendar.hasReadPermission()
                        override suspend fun upcomingEvents() = graph.calendar.upcomingEvents()
                    },
                    scheduler = graph.scheduler
                )
            }
        }
    }
}
