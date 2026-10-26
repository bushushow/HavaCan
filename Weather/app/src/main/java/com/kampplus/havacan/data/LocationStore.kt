package com.kampplus.havacan.data

import android.content.Context
import com.google.gson.Gson
import com.kampplus.havacan.model.City

/** Remembers the last chosen city across app launches. */
interface LocationStore {
    fun load(): City?
    fun save(city: City)
}

class PreferencesLocationStore(context: Context) : LocationStore {
    private val prefs = context.getSharedPreferences("havacan", Context.MODE_PRIVATE)
    private val gson = Gson()

    override fun load(): City? = prefs.getString(KEY, null)?.let { runCatching { gson.fromJson(it, City::class.java) }.getOrNull() }

    override fun save(city: City) {
        prefs.edit().putString(KEY, gson.toJson(city)).apply()
    }

    private companion object {
        const val KEY = "selected_city"
    }
}
