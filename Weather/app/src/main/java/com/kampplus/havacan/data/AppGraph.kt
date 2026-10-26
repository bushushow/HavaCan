package com.kampplus.havacan.data

import android.content.Context
import com.kampplus.havacan.BuildConfig
import com.kampplus.havacan.data.remote.ApiClient
import com.kampplus.havacan.data.wardrobe.BackendClothingAnalyzer
import com.kampplus.havacan.data.wardrobe.PhotoStore
import com.kampplus.havacan.data.wardrobe.WardrobeDatabase
import com.kampplus.havacan.data.wardrobe.WardrobeRepository
import java.io.File

/**
 * App-wide singletons shared by ViewModels and background workers, so a plan saved on screen
 * and the plan the reminder worker reads come from the same store.
 */
class AppGraph private constructor(context: Context) {
    val weather = WeatherRepository(ApiClient.forecastApi)
    val plans: PlanRepository = FilePlanStore(File(context.filesDir, "plans.json"))
    val location: LocationStore = PreferencesLocationStore(context)
    val calendar = CalendarGateway(context)

    /** Loaded first thing so the unit and display choices apply to workers too. */
    val profile: ProfileStore = PreferencesProfileStore(context)
    private val database = WardrobeDatabase.create(context)
    val wardrobe = WardrobeRepository(
        dao = database.wardrobe(),
        photos = PhotoStore(context),
        analyzer = BuildConfig.HAVACAN_API_URL.takeIf { it.isNotBlank() }?.let(::BackendClothingAnalyzer)
    )

    companion object {
        @Volatile private var instance: AppGraph? = null

        fun get(context: Context): AppGraph = instance ?: synchronized(this) {
            instance ?: AppGraph(context.applicationContext).also { instance = it }
        }
    }
}
