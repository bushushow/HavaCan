package com.kampplus.havacan.ui.run

import com.kampplus.havacan.data.FilePlanStore
import com.kampplus.havacan.data.LocationStore
import com.kampplus.havacan.data.WeatherRepository
import com.kampplus.havacan.data.provinceCities
import com.kampplus.havacan.data.remote.CurrentDto
import com.kampplus.havacan.data.remote.ForecastApi
import com.kampplus.havacan.data.remote.ForecastResponse
import com.kampplus.havacan.data.remote.HourlyDto
import com.kampplus.havacan.domain.running.RunningBlocker
import com.kampplus.havacan.model.City
import com.kampplus.havacan.model.Plan
import com.kampplus.havacan.model.Reminder
import com.kampplus.havacan.model.RunDetails
import com.kampplus.havacan.ui.plans.PlanScheduler
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class RunPlannerViewModelTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val ankara = provinceCities.first { it.name == "Ankara" }
    private val izmir = provinceCities.first { it.name == "İzmir" }
    private val now = LocalDateTime.parse("2026-10-25T10:00")
    private val scheduled = mutableListOf<String>()
    private var online = true
    private var notificationsAllowed = true

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** Today and tomorrow: 08–09 ideal, 07 cool, warm otherwise, rain 14–15, dark outside 07–17. */
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
            if (!online) throw IOException("offline")
            val times = listOf("2026-10-25", "2026-10-26").flatMap { d -> (0..23).map { "%sT%02d:00".format(d, it) } }
            fun hourOf(t: String) = t.substringAfter('T').take(2).toInt()
            return ForecastResponse(
                timezone = "Europe/Istanbul",
                current = CurrentDto("2026-10-25T10:00", 15.0, 14.0, 1, 1, 5.0, 0.0),
                hourly = HourlyDto(
                    time = times,
                    temperature = times.map { 15.0 },
                    apparentTemperature = times.map {
                        when (hourOf(it)) {
                            7 -> 6.0
                            8, 9 -> 14.0
                            else -> 22.0
                        }
                    },
                    precipitationProbability = times.map { if (hourOf(it) in 14..15) 70 else 5 },
                    precipitation = times.map { 0.0 },
                    weatherCode = times.map { if (hourOf(it) in 14..15) 61 else 1 },
                    windSpeed = times.map { 5.0 },
                    windGusts = times.map { 10.0 },
                    uvIndex = times.map { 2.0 },
                    isDay = times.map { if (hourOf(it) in 7..17) 1 else 0 }
                )
            )
        }
    }

    private lateinit var file: File

    private fun store(): FilePlanStore {
        file = tmp.newFolder().resolve("plans.json")
        return FilePlanStore(file, dispatcher)
    }

    private fun viewModel(store: FilePlanStore, planId: String? = null, start: String? = null) = RunPlannerViewModel(
        plans = store,
        weather = WeatherRepository(api),
        location = object : LocationStore {
            override fun load(): City = ankara
            override fun save(city: City) = Unit
        },
        scheduler = object : PlanScheduler {
            override fun schedule(plan: Plan) {
                scheduled += plan.id
            }
            override fun cancel(planId: String) {
                scheduled -= planId
            }
        },
        canNotify = { notificationsAllowed },
        planId = planId,
        initialStart = start,
        now = { now }
    )

    @Test
    fun opensOnTomorrowWithTheBestHourChosen() = runTest(dispatcher) {
        val vm = viewModel(store())
        advanceUntilIdle()
        val s = vm.uiState.value
        assertEquals(LocalDate.parse("2026-10-26"), s.date)
        assertEquals(listOf(8, 9, 7), s.suggestions.map { it.start.hour })
        assertEquals(LocalTime.of(8, 0), s.start)
        assertEquals(97, s.selectedSlot!!.score)
    }

    @Test
    fun savesARunThatPlanlarimReads() = runTest(dispatcher) {
        val store = store()
        val vm = viewModel(store)
        advanceUntilIdle()
        vm.selectSlot(vm.uiState.value.suggestions[1])
        vm.selectGoal(RunGoal.Five)
        vm.setDuration(30)
        vm.setPlaceName("Seğmenler Parkı")
        vm.save()
        vm.save() // a double tap must not create a second plan
        advanceUntilIdle()

        val plan = store.plans.value.single()
        assertEquals("5 km koşu", plan.title)
        assertEquals("2026-10-26", plan.date)
        assertEquals("09:00", plan.time)
        assertEquals(RunDetails(5, 30), plan.run)
        assertEquals(LocalDateTime.parse("2026-10-26T09:30"), plan.endDateTime)
        assertEquals("Seğmenler Parkı", plan.placeName)
        assertEquals(30L, plan.reminderMinutesBefore)
        assertTrue(plan.follow)
        assertEquals(listOf(plan.id), scheduled)
        assertEquals(ReminderResult.Scheduled, vm.uiState.value.saved!!.reminder)

        // Still there when the app is closed and opened again
        val reopened = FilePlanStore(file, dispatcher)
        reopened.load()
        assertEquals(plan, reopened.plans.value.single())
    }

    @Test
    fun withoutNotificationsTheUserIsToldTheReminderWontCome() = runTest(dispatcher) {
        notificationsAllowed = false
        val vm = viewModel(store())
        advanceUntilIdle()
        assertTrue(vm.needsNotificationPermission())
        vm.save()
        advanceUntilIdle()
        assertEquals(ReminderResult.NotificationsBlocked, vm.uiState.value.saved!!.reminder)
    }

    @Test
    fun reminderOffIsSavedAsNone() = runTest(dispatcher) {
        val store = store()
        val vm = viewModel(store)
        advanceUntilIdle()
        vm.setReminderOn(false)
        vm.setFollow(false)
        assertFalse(vm.needsNotificationPermission())
        vm.save()
        advanceUntilIdle()
        assertNull(store.plans.value.single().reminderMinutesBefore)
        assertEquals(ReminderResult.Off, vm.uiState.value.saved!!.reminder)
    }

    @Test
    fun offlineShowsAnErrorButAManualRunCanStillBeSaved() = runTest(dispatcher) {
        online = false
        val store = store()
        val vm = viewModel(store)
        advanceUntilIdle()
        assertEquals(ForecastState.Failed, vm.uiState.value.forecast)
        assertTrue(vm.uiState.value.suggestions.isEmpty())
        assertNull(vm.uiState.value.start)

        vm.save()
        assertNotNull(vm.uiState.value.error)
        vm.selectStart(LocalTime.of(18, 30))
        assertNull(vm.uiState.value.selectedSlot) // no forecast, no made-up score
        vm.save()
        advanceUntilIdle()
        assertEquals("18:30", store.plans.value.single().time)

        online = true
        vm.retry()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.forecast is ForecastState.Ready)
    }

    @Test
    fun aLongerRunIsRescoredForTheHoursItCovers() = runTest(dispatcher) {
        val vm = viewModel(store())
        advanceUntilIdle()
        vm.selectStart(LocalTime.of(13, 30))
        vm.setDuration(60)
        assertEquals(RunningBlocker.LikelyRain, vm.uiState.value.selectedSlot!!.blocker)
    }

    @Test
    fun pastDatesAndPastTimesAreRefused() = runTest(dispatcher) {
        val vm = viewModel(store())
        advanceUntilIdle()
        vm.selectDate(LocalDate.parse("2026-10-24"))
        assertEquals(LocalDate.parse("2026-10-26"), vm.uiState.value.date)
        vm.selectDay(DayChoice.Today)
        vm.selectStart(LocalTime.of(9, 0))
        vm.save()
        assertNotNull(vm.uiState.value.error)
        assertNull(vm.uiState.value.saved)
    }

    @Test
    fun changingThePlaceLoadsItsForecast() = runTest(dispatcher) {
        val vm = viewModel(store())
        advanceUntilIdle()
        vm.setCity(izmir)
        advanceUntilIdle()
        assertEquals(izmir, vm.uiState.value.city)
        assertTrue(vm.uiState.value.forecast is ForecastState.Ready)
    }

    @Test
    fun editingKeepsTheIdAndUpdatesTheRun() = runTest(dispatcher) {
        val store = store()
        store.upsert(Plan("r1", "2 km koşu", "2026-10-26", "07:00", ankara, run = RunDetails(2, 15), reminder = Reminder.OneHour))
        val vm = viewModel(store, planId = "r1")
        advanceUntilIdle()
        val s = vm.uiState.value
        assertTrue(s.isEditing)
        assertEquals(RunGoal.Two, s.goal)
        assertEquals(15, s.durationMinutes)
        assertEquals(LocalTime.of(7, 0), s.start)
        assertEquals(60L, s.reminderMinutes)

        vm.selectGoal(RunGoal.Ten)
        vm.save()
        advanceUntilIdle()
        val saved = store.plans.value.single()
        assertEquals("r1", saved.id)
        assertEquals(RunDetails(10, 15), saved.run)
    }

    @Test
    fun theHomeScreensSuggestedStartIsKept() = runTest(dispatcher) {
        val vm = viewModel(store(), start = "2026-10-25T16:00")
        advanceUntilIdle()
        assertEquals(LocalDate.parse("2026-10-25"), vm.uiState.value.date)
        assertEquals(LocalTime.of(16, 0), vm.uiState.value.start)
    }

    @Test
    fun reminderIsALengthOfTimeBeforeTheRun() = runTest(dispatcher) {
        val store = store()
        val vm = viewModel(store)
        advanceUntilIdle() // starts tomorrow 08:00
        vm.setReminderMinutes(90)
        assertEquals(LocalDateTime.parse("2026-10-26T06:30"), vm.uiState.value.reminderAt)

        // Ten hours before reaches the evening before
        vm.setReminderMinutes(600)
        assertEquals(LocalDateTime.parse("2026-10-25T22:00"), vm.uiState.value.reminderAt)

        vm.setReminderMinutes(2) // too short, ignored
        assertEquals(600L, vm.uiState.value.reminderMinutes)

        vm.save()
        advanceUntilIdle()
        assertEquals(600L, store.plans.value.single().reminderMinutesBefore)
    }

    @Test
    fun reminderLabelsReadNaturally() {
        assertEquals("15 dk önce", RunPlannerViewModel.beforeLabel(15))
        assertEquals("1 saat önce", RunPlannerViewModel.beforeLabel(60))
        assertEquals("1 sa 30 dk önce", RunPlannerViewModel.beforeLabel(90))
    }
}
