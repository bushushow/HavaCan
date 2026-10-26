package com.kampplus.havacan.ui.navigation

import android.net.Uri
import com.kampplus.havacan.ui.home.components.HomeTab

/**
 * Every screen the app can navigate to. Tabs are top-level routes; the place picker and
 * Koşu Planla are full-screen routes pushed on top. Koşu Planla's inputs are optional query
 * parameters (an empty path segment would not match), read by its ViewModel from SavedStateHandle.
 */
object HavaRoutes {
    const val WEATHER = "weather"
    const val PLANS = "plans"
    const val CHAT = "havacan"
    const val WARDROBE = "wardrobe"
    const val PROFILE = "profile"
    const val CITY = "city"

    const val ARG_PLAN_ID = "planId"
    const val ARG_START = "start"
    const val RUN = "run?$ARG_PLAN_ID={$ARG_PLAN_ID}&$ARG_START={$ARG_START}"

    /** Koşu Planla for a new run (optionally at the home screen's suggested start) or an existing one. */
    fun run(planId: String? = null, start: String? = null): String {
        val query = listOfNotNull(
            planId?.let { "$ARG_PLAN_ID=${Uri.encode(it)}" },
            start?.let { "$ARG_START=${Uri.encode(it)}" }
        )
        return if (query.isEmpty()) "run" else "run?" + query.joinToString("&")
    }

    val tabs = listOf(WEATHER, PLANS, CHAT, WARDROBE, PROFILE)
}

val HomeTab.route: String
    get() = when (this) {
        HomeTab.Weather -> HavaRoutes.WEATHER
        HomeTab.Plans -> HavaRoutes.PLANS
        HomeTab.HavaCan -> HavaRoutes.CHAT
        HomeTab.Wardrobe -> HavaRoutes.WARDROBE
        HomeTab.Profile -> HavaRoutes.PROFILE
    }

/** The tab a route belongs to; null for full-screen routes (place picker, Koşu Planla). */
fun tabOf(route: String?): HomeTab? = HomeTab.entries.firstOrNull { it.route == route }
