package com.kampplus.havacan.ui.city

import androidx.lifecycle.SavedStateHandle
import com.kampplus.havacan.data.CityRepository
import com.kampplus.havacan.data.DeviceLocator
import com.kampplus.havacan.data.DistrictSource
import com.kampplus.havacan.data.Fix
import com.kampplus.havacan.data.PlaceDirectory
import com.kampplus.havacan.data.PlaceItem
import com.kampplus.havacan.data.RecentPlaces
import com.kampplus.havacan.data.remote.CityApi
import com.kampplus.havacan.model.City
import com.kampplus.havacan.model.CitySearchResponse
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationPickerViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private fun district(name: String, id: Long, province: String, lat: Double, lon: Double) =
        City(id, name, lat, lon, "TR", "Türkiye", province)

    private val districts = DistrictSource { plate ->
        when (plate) {
            1 -> listOf(district("Ceyhan", 1, "Adana", 37.03, 35.81), district("Çukurova", 2, "Adana", 37.05, 35.28))
            16 -> listOf(district("Gemlik", 3, "Bursa", 40.43, 29.15))
            else -> emptyList()
        }
    }

    private class FakeApi : CityApi {
        var calls = 0
        var fail = false
        override suspend fun searchCities(
            name: String,
            count: Int,
            language: String,
            format: String,
            countryCode: String?
        ): CitySearchResponse {
            calls++
            if (fail) throw IOException("offline")
            return CitySearchResponse(listOf(City(99, "Berlin", 52.52, 13.4, "DE", "Almanya", "Berlin", "PPLC")))
        }
    }

    private class MemoryRecents : RecentPlaces {
        val list = mutableListOf<City>()
        override fun load() = list.toList()
        override fun add(city: City) {
            list.removeAll { it.id == city.id }
            list.add(0, city)
        }
    }

    private class FakeLocator(var fix: Fix, var name: Pair<String, String?>? = null) : DeviceLocator {
        override suspend fun locate() = fix
        override suspend fun nameOf(latitude: Double, longitude: Double) = name
    }

    private val api = FakeApi()
    private val recents = MemoryRecents()
    private val locator = FakeLocator(Fix.NotFound)
    private lateinit var vm: LocationPickerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        vm = LocationPickerViewModel(PlaceDirectory(districts), CityRepository(api), recents, locator, SavedStateHandle())
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun localSearchNeedsNoNetwork() = runTest(dispatcher) {
        backgroundScope.launch { vm.search.collect {} }
        vm.onQueryChange("gemlik")
        advanceUntilIdle()
        val results = vm.search.value as SearchState.Results
        assertEquals("Gemlik", results.items.first().city.name)
        assertEquals(0, api.calls)
    }

    @Test
    fun abroadGoesOnlineAndOfflineIsAnError() = runTest(dispatcher) {
        backgroundScope.launch { vm.search.collect {} }
        vm.onQueryChange("Berlin")
        advanceUntilIdle()
        val results = vm.search.value as SearchState.Results
        assertTrue(results.online)
        assertEquals(PlaceItem.Kind.Abroad, results.items.single().kind)
        api.fail = true
        vm.retry()
        advanceUntilIdle()
        assertEquals(SearchState.Error(ErrorReason.NoConnection), vm.search.value)
    }

    @Test
    fun provincePageListsOnlyItsDistrictsAndFilters() = runTest(dispatcher) {
        backgroundScope.launch { vm.districtPage.collect {} }
        vm.openProvince(vm.provinces.first { it.city.name == "Adana" })
        advanceUntilIdle()
        assertEquals(listOf("Ceyhan", "Çukurova"), vm.districtPage.value!!.districts.map { it.city.name })
        vm.onDistrictQueryChange("cuk")
        advanceUntilIdle()
        assertEquals(listOf("Çukurova"), vm.districtPage.value!!.districts.map { it.city.name })
    }

    @Test
    fun picksBecomeRecents() = runTest(dispatcher) {
        val picked = mutableListOf<PlaceItem>()
        backgroundScope.launch { vm.picked.collect { picked += it } }
        assertTrue(vm.recentPlaces.value.isEmpty())
        vm.pick(PlaceItem(district("Gemlik", 3, "Bursa", 40.43, 29.15), PlaceItem.Kind.District))
        advanceUntilIdle()
        assertEquals(listOf("Gemlik"), vm.recentPlaces.value.map { it.name })
        assertEquals("Gemlik", picked.single().city.name)
    }

    @Test
    fun currentLocationPicksTheNearestDistrict() = runTest(dispatcher) {
        val picked = mutableListOf<PlaceItem>()
        backgroundScope.launch { vm.picked.collect { picked += it } }
        locator.fix = Fix.Found(40.44, 29.14)
        vm.useCurrentLocation()
        advanceUntilIdle()
        assertEquals("Gemlik", picked.single().city.name)
        assertEquals(LocateState.Idle, vm.locate.value)
    }

    @Test
    fun locationProblemsAreExplainedNeverGuessed() = runTest(dispatcher) {
        val picked = mutableListOf<PlaceItem>()
        backgroundScope.launch { vm.picked.collect { picked += it } }
        locator.fix = Fix.Found(52.52, 13.40) // abroad and the phone can't name it
        vm.useCurrentLocation()
        advanceUntilIdle()
        assertTrue(vm.locate.value is LocateState.Failed)
        locator.fix = Fix.ServicesOff
        vm.useCurrentLocation()
        advanceUntilIdle()
        assertTrue((vm.locate.value as LocateState.Failed).openSettings)
        vm.onLocationPermissionDenied()
        assertTrue(vm.locate.value is LocateState.Failed)
        assertTrue(picked.isEmpty())
    }
}
