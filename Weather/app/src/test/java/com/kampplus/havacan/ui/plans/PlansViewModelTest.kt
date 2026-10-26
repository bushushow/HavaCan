package com.kampplus.havacan.ui.plans

import com.kampplus.havacan.data.CalendarEvent
import com.kampplus.havacan.data.FilePlanStore
import com.kampplus.havacan.data.LocationStore
import com.kampplus.havacan.data.OpenMeteoWeatherRepository
import com.kampplus.havacan.data.PlanScheduler
import com.kampplus.havacan.data.WeatherRepository
import com.kampplus.havacan.data.provinceCities
import com.kampplus.havacan.data.remote.CurrentDto
import com.kampplus.havacan.data.remote.ForecastApi
import com.kampplus.havacan.data.remote.ForecastResponse
import com.kampplus.havacan.data.remote.HourlyDto
import com.kampplus.havacan.domain.plans.PlanOutlook
import com.kampplus.havacan.model.City
import com.kampplus.havacan.model.Plan
import com.kampplus.havacan.model.PlanSource
import com.kampplus.havacan.model.PlanType
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class PlansViewModelTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val ankara = provinceCities.first { it.name == "Ankara" }
    private val now = LocalDateTime.parse("2026-10-25T10:00")
    private val scheduled = mutableListOf<String>()
    private var events = emptyList<CalendarEvent>()
    private var calendarAllowed = false

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** Dry until 15:00, rain from 16:00 to 18:00. */
    private val api = object : ForecastApi {
        override suspend fun forecast(
            latitude: Double,
            longitude: Double,
            current: String,
            hourly: String,
            daily: String,
            timezone: String,
            forecastDays: Int
        ): ForecastResponse {
            val hours = (0..23)
            return ForecastResponse(
                timezone = "Europe/Istanbul",
                current = CurrentDto("2026-10-25T10:00", 15.0, 14.0, 1, 1, 5.0, 0.0),
                hourly = HourlyDto(
                    time = hours.map { "2026-10-25T%02d:00".format(it) },
                    temperature = hours.map { 15.0 },
                    apparentTemperature = hours.map { 14.0 },
                    precipitationProbability = hours.map { if (it in 16..18) 70 else 5 },
                    precipitation = hours.map { 0.0 },
                    weatherCode = hours.map { if (it in 16..18) 61 else 1 },
                    windSpeed = hours.map { 6.0 },
                    windGusts = hours.map { 12.0 },
                    uvIndex = hours.map { 1.0 },
                    isDay = hours.map { if (it in 7..17) 1 else 0 }
                )
            )
        }
    }

    private fun TestScope.viewModel(store: FilePlanStore): PlansViewModel {
        val vm = PlansViewModel(
            plans = store,
            weather = OpenMeteoWeatherRepository(api),
            location = object : LocationStore {
                override fun load(): City = ankara
                override fun save(city: City) = Unit
            },
            calendar = object : CalendarSource {
                override fun hasPermission() = calendarAllowed
                override suspend fun upcomingEvents() = events
            },
            scheduler = object : PlanScheduler {
                override fun schedule(plan: Plan) {
                    scheduled += plan.id
                }
                override fun cancel(planId: String) {
                    scheduled -= planId
                }
            },
            now = { now }
        )
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        advanceUntilIdle()
        return vm
    }

    @Test
    fun rainyPlanGetsRiskAndEarlierSuggestion() = runTest(dispatcher) {
        val store = FilePlanStore(tmp.newFolder().resolve("plans.json"), dispatcher)
        val vm = viewModel(store)
        vm.save(Plan("a", "Parkta buluşma", "2026-10-25", "17:00", ankara))
        vm.save(Plan("b", "Sinema", "2026-10-25", "17:00", ankara, type = PlanType.Indoor))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.items.first { it.plan.id == "a" }.outlook is PlanOutlook.Risk)
        assertTrue(state.items.first { it.plan.id == "b" }.outlook is PlanOutlook.Fine)
        assertEquals("a", state.suggestion!!.plan.id)
        assertEquals(15, state.suggestion!!.alternative!!.hour)
        assertEquals(listOf("a", "b"), scheduled)
    }

    @Test
    fun planBMoveClearsTheRisk() = runTest(dispatcher) {
        val store = FilePlanStore(tmp.newFolder().resolve("plans.json"), dispatcher)
        val vm = viewModel(store)
        vm.save(Plan("a", "Parkta buluşma", "2026-10-25", "17:00", ankara))
        advanceUntilIdle()
        val item = vm.uiState.value.suggestion!!
        vm.moveTo(item.plan, item.alternative!!)
        advanceUntilIdle()
        val moved = vm.uiState.value.items.single()
        assertEquals("15:00", moved.plan.time)
        assertTrue(moved.outlook is PlanOutlook.Fine)
        assertEquals(null, vm.uiState.value.suggestion)
    }

    @Test
    fun calendarEventsAreImportedOnceGranted() = runTest(dispatcher) {
        val store = FilePlanStore(tmp.newFolder().resolve("plans.json"), dispatcher)
        events = listOf(CalendarEvent(42, "Sabah koşusu", LocalDateTime.parse("2026-10-25T12:00"), "Kuğulu Park"))
        val vm = viewModel(store)
        assertEquals(0, vm.uiState.value.items.size)

        calendarAllowed = true
        vm.onCalendarPermissionResult(granted = true)
        advanceUntilIdle()
        val imported = vm.uiState.value.items.single().plan
        assertEquals("cal-42", imported.id)
        assertEquals(PlanSource.Calendar, imported.source)
        assertEquals("Kuğulu Park", imported.placeName)
        assertTrue(vm.uiState.value.calendarConnected)
    }

    @Test
    fun weekStripStartsTodayAndPastPlansAreHidden() = runTest(dispatcher) {
        val store = FilePlanStore(tmp.newFolder().resolve("plans.json"), dispatcher)
        val vm = viewModel(store)
        vm.save(Plan("old", "Kahvaltı", "2026-10-25", "07:00", ankara))
        advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals(7, state.days.size)
        assertEquals("2026-10-25", state.days.first().date.toString())
        assertEquals("Paz", state.days.first().dayName)
        assertTrue(state.items.isEmpty())
    }
}
