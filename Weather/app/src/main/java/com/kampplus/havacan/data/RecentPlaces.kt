package com.kampplus.havacan.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kampplus.havacan.model.City

/** Places the user really picked, newest first. Nothing is pre-filled. */
interface RecentPlaces {
    fun load(): List<City>
    fun add(city: City)
}

class PreferencesRecentPlaces(context: Context, private val max: Int = 6) : RecentPlaces {
    private val prefs = context.getSharedPreferences("havacan", Context.MODE_PRIVATE)
    private val gson = Gson()

    override fun load(): List<City> = prefs.getString(KEY, null)
        ?.let { runCatching { gson.fromJson<List<City>>(it, object : TypeToken<List<City>>() {}.type) }.getOrNull() }
        .orEmpty()

    override fun add(city: City) {
        val next = (listOf(city) + load().filterNot { it.id == city.id }).take(max)
        prefs.edit().putString(KEY, gson.toJson(next)).apply()
    }

    private companion object {
        const val KEY = "recent_places"
    }
}
