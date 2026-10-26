package com.example.weather.data

import android.content.Context
import com.example.weather.model.TemperatureUnit
import com.example.weather.model.Units
import com.example.weather.ui.theme.DisplayPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ActivityInterest(val label: String) {
    Running("Koşu"),
    Walking("Yürüyüş"),
    Cycling("Bisiklet"),
    Picnic("Piknik"),
    Camping("Kamp"),
    Hiking("Doğa yürüyüşü"),
    Beach("Deniz ve plaj"),
    OtherOutdoor("Diğer açık hava"),
}

/** Optional and freely combined; nothing about the person is assumed from them. */
enum class OutfitStyle(val label: String) {
    Sporty("Spor"),
    Classic("Klasik"),
    Casual("Günlük"),
    Minimal("Minimal"),
    Modest("Tesettür / örtünme"),
    Layered("Katmanlı giyim"),
}

data class ProfilePrefs(
    /** Shown as "Merhaba, …"; empty until the user types one. */
    val name: String = "",
    val unit: TemperatureUnit = TemperatureUnit.Celsius,
    val activities: Set<ActivityInterest> = emptySet(),
    val styles: Set<OutfitStyle> = emptySet(),
    val notifyWeather: Boolean = true,
    val notifyPlans: Boolean = true,
    val notifyRuns: Boolean = true,
    val notifyPlanB: Boolean = true,
    val largeText: Boolean = false,
    val highContrast: Boolean = false,
    val reduceMotion: Boolean = false,
)

interface ProfileStore {
    val prefs: StateFlow<ProfilePrefs>
    fun update(change: (ProfilePrefs) -> ProfilePrefs)
    fun reset()
}

/**
 * Profil choices in SharedPreferences, like the city (LocationStore) and voice settings already
 * are; Room keeps only the wardrobe. Every change is also pushed to [Units] and [DisplayPrefs],
 * which the screens, workers and HavaCan read.
 */
class PreferencesProfileStore(context: Context) : ProfileStore {
    private val prefs0 = context.getSharedPreferences("havacan_profile", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())
    override val prefs: StateFlow<ProfilePrefs> = state.asStateFlow()

    init {
        apply(state.value)
    }

    override fun update(change: (ProfilePrefs) -> ProfilePrefs) {
        val next = change(state.value)
        prefs0.edit()
            .putString(K_NAME, next.name.trim().take(MAX_NAME))
            .putString(K_UNIT, next.unit.name)
            .putStringSet(K_ACTIVITIES, next.activities.map { it.name }.toSet())
            .putStringSet(K_STYLES, next.styles.map { it.name }.toSet())
            .putBoolean(K_WEATHER, next.notifyWeather)
            .putBoolean(K_PLANS, next.notifyPlans)
            .putBoolean(K_RUNS, next.notifyRuns)
            .putBoolean(K_PLAN_B, next.notifyPlanB)
            .putBoolean(K_LARGE, next.largeText)
            .putBoolean(K_CONTRAST, next.highContrast)
            .putBoolean(K_MOTION, next.reduceMotion)
            .apply()
        state.value = next.copy(name = next.name.trim().take(MAX_NAME))
        apply(state.value)
    }

    override fun reset() {
        prefs0.edit().clear().apply()
        state.value = ProfilePrefs()
        apply(state.value)
    }

    private fun read() = ProfilePrefs(
        name = prefs0.getString(K_NAME, "").orEmpty(),
        unit = TemperatureUnit.entries.firstOrNull { it.name == prefs0.getString(K_UNIT, null) } ?: TemperatureUnit.Celsius,
        activities = prefs0.getStringSet(K_ACTIVITIES, emptySet()).orEmpty()
            .mapNotNull { n -> ActivityInterest.entries.firstOrNull { it.name == n } }.toSet(),
        styles = prefs0.getStringSet(K_STYLES, emptySet()).orEmpty()
            .mapNotNull { n -> OutfitStyle.entries.firstOrNull { it.name == n } }.toSet(),
        notifyWeather = prefs0.getBoolean(K_WEATHER, true),
        notifyPlans = prefs0.getBoolean(K_PLANS, true),
        notifyRuns = prefs0.getBoolean(K_RUNS, true),
        notifyPlanB = prefs0.getBoolean(K_PLAN_B, true),
        largeText = prefs0.getBoolean(K_LARGE, false),
        highContrast = prefs0.getBoolean(K_CONTRAST, false),
        reduceMotion = prefs0.getBoolean(K_MOTION, false),
    )

    private fun apply(p: ProfilePrefs) {
        Units.temperature = p.unit
        DisplayPrefs.largeText = p.largeText
        DisplayPrefs.highContrast = p.highContrast
        DisplayPrefs.reduceMotion = p.reduceMotion
    }

    private companion object {
        const val MAX_NAME = 30
        const val K_NAME = "name"
        const val K_UNIT = "unit"
        const val K_ACTIVITIES = "activities"
        const val K_STYLES = "styles"
        const val K_WEATHER = "notify_weather"
        const val K_PLANS = "notify_plans"
        const val K_RUNS = "notify_runs"
        const val K_PLAN_B = "notify_plan_b"
        const val K_LARGE = "large_text"
        const val K_CONTRAST = "high_contrast"
        const val K_MOTION = "reduce_motion"
    }
}
