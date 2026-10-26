package com.kampplus.havacan.ui.voice

import com.kampplus.havacan.data.provinceCities
import com.kampplus.havacan.data.wardrobe.ClothingItem
import com.kampplus.havacan.domain.voice.AiDecision
import com.kampplus.havacan.domain.voice.AiTurn
import com.kampplus.havacan.domain.voice.AssistantData
import com.kampplus.havacan.domain.voice.HavaCanAssistant
import com.kampplus.havacan.domain.voice.VoiceDestination
import com.kampplus.havacan.model.City
import com.kampplus.havacan.model.CurrentWeather
import com.kampplus.havacan.model.Forecast
import com.kampplus.havacan.model.Plan
import com.kampplus.havacan.model.WeatherCondition
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceGuideControllerTest {
    private val dispatcher = StandardTestDispatcher()
    private val ankara = provinceCities.first { it.name == "Ankara" }

    /** Records turns; the test plays the user by calling [say] or [silence]. */
    private class FakeInput : SpeechInput {
        var listening = false
        var listens = 0
        private var result: ((String) -> Unit)? = null
        private var error: ((SpeechError) -> Unit)? = null
        override val isAvailable = true
        override fun listen(
            onLevel: (Float) -> Unit,
            onPartial: (String) -> Unit,
            onResult: (String) -> Unit,
            onError: (SpeechError) -> Unit
        ) {
            listening = true
            listens++
            result = onResult
            error = onError
        }
        fun say(text: String) {
            listening = false
            result!!(text)
        }
        fun silence() {
            listening = false
            error!!(SpeechError.NoSpeech)
        }
        override fun cancel() {
            listening = false
        }
        override fun release() = Unit
    }

    private class FakeOutput(private val input: FakeInput) : SpeechOutput {
        override val status = TtsStatus.Ready
        override var onStatusChanged: ((TtsStatus) -> Unit)? = null
        val spoken = mutableListOf<String>()
        var micOpenWhileSpeaking = false
        private var done: (() -> Unit)? = null
        override fun speak(text: String, onDone: () -> Unit) {
            if (input.listening) micOpenWhileSpeaking = true
            spoken += text
            done = onDone
        }
        fun finish() {
            done?.also { done = null }?.invoke()
        }
        override fun stop() {
            done = null
        }
        override fun release() = Unit
    }

    private class Settings(initial: VoiceSettings) : VoiceSettingsStore {
        private val state = MutableStateFlow(initial)
        override val settings: StateFlow<VoiceSettings> = state
        override fun update(change: (VoiceSettings) -> VoiceSettings) {
            state.value = change(state.value)
        }
    }

    private val data = object : AssistantData {
        override fun homeCity(): City = ankara
        override suspend fun forecast(city: City) = Forecast(
            "Europe/Istanbul",
            CurrentWeather("2026-10-26T10:00", 16.0, 15.0, WeatherCondition.Clear, true, 5.0),
            hourly = emptyList(),
            today = null
        )
        override suspend fun plans() = emptyList<Plan>()
        override suspend fun savePlan(plan: Plan) = Unit
        override suspend fun deletePlan(plan: Plan) = Unit
        override suspend fun wardrobe() = emptyList<ClothingItem>()
        override suspend fun askAi(message: String, context: String, history: List<AiTurn>): AiDecision? = null
    }

    private val input = FakeInput()
    private val output = FakeOutput(input)
    private lateinit var controller: VoiceGuideController

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        controller = VoiceGuideController(
            assistant = HavaCanAssistant(data, now = { LocalDateTime.parse("2026-10-26T10:00") }),
            input = input,
            output = output,
            settingsStore = Settings(VoiceSettings(guideEnabled = true))
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** Lets the gap before the microphone pass, but not the 5-minute session limit. */
    private fun TestScope.settle() {
        runCurrent()
        advanceTimeBy(VoiceGuideController.MIC_GAP_MS + 100)
        runCurrent()
    }

    private fun TestScope.turn(text: String) {
        input.say(text)
        settle()
    }

    @Test
    fun takesTurnsAndNeverListensOverItsOwnVoice() = runTest(dispatcher) {
        val destinations = mutableListOf<VoiceDestination>()
        backgroundScope.launch { controller.navigation.collect { destinations += it } }
        controller.startSession()
        assertTrue(input.listening)

        turn("Hava durumunu söyle")
        assertEquals(VoicePhase.Speaking, controller.state.value.phase)
        assertFalse(input.listening)
        assertTrue(output.spoken.last().contains("16 derece"))

        output.finish()
        settle()
        assertTrue("listens again once the voice has finished", input.listening)

        turn("Planlarımı göster")
        assertEquals(listOf(VoiceDestination.Plans), destinations)
        output.finish()
        settle()

        turn("Ana sayfaya dön")
        assertEquals(listOf(VoiceDestination.Plans, VoiceDestination.Home), destinations)
        output.finish()
        settle()

        turn("Sesli rehberi kapat")
        assertFalse(controller.state.value.sessionActive)
        output.finish()
        settle()
        assertFalse("stays closed after saying goodbye", input.listening)
        assertFalse(output.micOpenWhileSpeaking)
        assertEquals(8, controller.messages.value.size)
    }

    @Test
    fun silenceTwiceEndsTheSession() = runTest(dispatcher) {
        controller.startSession()
        input.silence()
        settle()
        output.finish()
        settle()
        assertTrue(input.listening)
        input.silence()
        settle()
        assertFalse(controller.state.value.sessionActive)
        output.finish()
        settle()
        assertFalse(input.listening)
    }

    @Test
    fun stopButtonCancelsALateAnswer() = runTest(dispatcher) {
        controller.startSession()
        input.say("Hava durumunu söyle")
        controller.stopSession(goodbye = false)
        settle()
        assertFalse(controller.state.value.sessionActive)
        assertTrue(output.spoken.isEmpty())
        assertFalse(input.listening)
    }

    @Test
    fun screenReaderSpeaksInsteadOfUs() = runTest(dispatcher) {
        val announced = mutableListOf<String>()
        backgroundScope.launch { controller.announcements.collect { announced += it } }
        controller.setScreenReaderActive(true)
        controller.startSession()
        turn("Hava durumunu söyle")
        assertTrue(output.spoken.isEmpty())
        assertEquals(1, announced.size)
        assertFalse("waits for a tap instead of opening the mic under TalkBack", input.listening)
        assertTrue(controller.state.value.sessionActive)
        controller.tapMicrophone()
        assertTrue(input.listening)
    }

    @Test
    fun typedMessagesAreSilentByDefault() = runTest(dispatcher) {
        controller.submitTyped("Bugün hava nasıl?")
        settle()
        assertTrue(output.spoken.isEmpty())
        assertEquals(2, controller.messages.value.size)
        assertFalse(input.listening)
    }

    @Test
    fun oneShotMicListensOnceUnlessHavaCanAsksBack() = runTest(dispatcher) {
        controller.startSession(continuous = false)
        turn("Plan oluştur")
        output.finish()
        settle()
        assertTrue("HavaCan asked for the plan's name", input.listening)
        turn("Hayır")
        output.finish()
        settle()
        assertFalse(input.listening)
        assertFalse(controller.state.value.sessionActive)
    }
}
