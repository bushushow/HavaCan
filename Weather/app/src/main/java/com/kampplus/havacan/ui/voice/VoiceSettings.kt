package com.kampplus.havacan.ui.voice

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VoiceSettings(
    /** "HavaCan ile Sesli Kullanım": the voice guide button on every main screen, turn-taking sessions. */
    val guideEnabled: Boolean = false,
    /** Read typed chat answers aloud too (spoken questions always get a spoken answer). */
    val speakTypedReplies: Boolean = false
)

interface VoiceSettingsStore {
    val settings: StateFlow<VoiceSettings>
    fun update(change: (VoiceSettings) -> VoiceSettings)
}

class PreferencesVoiceSettingsStore(context: Context) : VoiceSettingsStore {
    private val prefs = context.getSharedPreferences("havacan_voice", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(
        VoiceSettings(
            guideEnabled = prefs.getBoolean(KEY_GUIDE, false),
            speakTypedReplies = prefs.getBoolean(KEY_SPEAK_TYPED, false)
        )
    )
    override val settings: StateFlow<VoiceSettings> = state.asStateFlow()

    override fun update(change: (VoiceSettings) -> VoiceSettings) {
        val next = change(state.value)
        prefs.edit().putBoolean(KEY_GUIDE, next.guideEnabled).putBoolean(KEY_SPEAK_TYPED, next.speakTypedReplies).apply()
        state.value = next
    }

    private companion object {
        const val KEY_GUIDE = "guide_enabled"
        const val KEY_SPEAK_TYPED = "speak_typed"
    }
}
