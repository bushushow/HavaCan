package com.kampplus.havacan.di

import android.content.Context
import com.kampplus.havacan.BuildConfig
import com.kampplus.havacan.data.AndroidDeviceLocator
import com.kampplus.havacan.data.AssetDistrictSource
import com.kampplus.havacan.data.BackendAssistant
import com.kampplus.havacan.data.CalendarGateway
import com.kampplus.havacan.data.CityRepository
import com.kampplus.havacan.data.DeviceLocator
import com.kampplus.havacan.data.FilePlanStore
import com.kampplus.havacan.data.LocationStore
import com.kampplus.havacan.data.OpenMeteoCityRepository
import com.kampplus.havacan.data.OpenMeteoWeatherRepository
import com.kampplus.havacan.data.PlaceDirectory
import com.kampplus.havacan.data.PlanRepository
import com.kampplus.havacan.data.PlanScheduler
import com.kampplus.havacan.data.PreferencesLocationStore
import com.kampplus.havacan.data.PreferencesProfileStore
import com.kampplus.havacan.data.PreferencesRecentPlaces
import com.kampplus.havacan.data.ProfileStore
import com.kampplus.havacan.data.RecentPlaces
import com.kampplus.havacan.data.WeatherRepository
import com.kampplus.havacan.data.remote.ApiClient
import com.kampplus.havacan.data.wardrobe.BackendClothingAnalyzer
import com.kampplus.havacan.data.wardrobe.PhotoStore
import com.kampplus.havacan.data.wardrobe.RoomWardrobeRepository
import com.kampplus.havacan.data.wardrobe.WardrobeDatabase
import com.kampplus.havacan.data.wardrobe.WardrobeRepository
import com.kampplus.havacan.ui.plans.WorkPlanScheduler
import java.io.File

/**
 * The one place the app's objects are built (manual dependency injection). ViewModels and
 * background workers get everything from here, so a plan saved on screen and the plan the
 * reminder worker reads come from the same store, and there is a single forecast cache.
 * Nothing else constructs repositories.
 */
class AppContainer private constructor(context: Context) {
    private val backendUrl = BuildConfig.HAVACAN_API_URL.takeIf { it.isNotBlank() }

    val weather: WeatherRepository = OpenMeteoWeatherRepository(ApiClient.forecastApi)
    val cities: CityRepository = OpenMeteoCityRepository(ApiClient.cityApi)
    val places = PlaceDirectory(AssetDistrictSource(context))
    val recentPlaces: RecentPlaces = PreferencesRecentPlaces(context)
    val deviceLocator: DeviceLocator = AndroidDeviceLocator(context)
    val plans: PlanRepository = FilePlanStore(File(context.filesDir, "plans.json"))
    val location: LocationStore = PreferencesLocationStore(context)
    val calendar = CalendarGateway(context)

    /** Reminders and the weather watch (WorkManager). */
    val workScheduler = WorkPlanScheduler(context)
    val scheduler: PlanScheduler get() = workScheduler

    /** Loaded first thing so the unit and display choices apply to workers too. */
    val profile: ProfileStore = PreferencesProfileStore(context)
    private val database = WardrobeDatabase.create(context)
    val wardrobe: WardrobeRepository = RoomWardrobeRepository(
        dao = database.wardrobe(),
        photos = PhotoStore(context),
        analyzer = backendUrl?.let(::BackendClothingAnalyzer)
    )

    /** The AI step of the assistant; null when no backend is configured. */
    val assistantBackend: BackendAssistant? = backendUrl?.let(::BackendAssistant)

    companion object {
        @Volatile private var instance: AppContainer? = null

        fun get(context: Context): AppContainer = instance ?: synchronized(this) {
            instance ?: AppContainer(context.applicationContext).also { instance = it }
        }
    }
}
