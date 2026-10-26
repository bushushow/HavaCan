package com.example.weather.ui.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.weather.BuildConfig
import com.example.weather.data.AppAssistantData
import com.example.weather.data.AppGraph
import com.example.weather.data.BackendAssistant
import com.example.weather.domain.voice.AssistantCard
import com.example.weather.domain.voice.AssistantReply
import com.example.weather.domain.voice.HavaCanAssistant
import com.example.weather.domain.voice.TurkishText
import com.example.weather.domain.voice.VoiceDestination
import com.example.weather.ui.plans.WorkPlanScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

enum class VoicePhase { Idle, Listening, Thinking, Speaking }

data class ChatMessage(
    val id: Long,
    val fromUser: Boolean,
    val title: String,
    val body: String? = null,
    val card: AssistantCard? = null,
    /** "17.02" */
    val time: String,
) {
    val spoken: String get() = TurkishText.forSpeech(listOfNotNull(title, body).joinToString(" "))
}

data class VoiceGuideState(
    val settings: VoiceSettings = VoiceSettings(),
    /** A voice session the user started; ends on "kapat", the stop button, silence or the time limit. */
    val sessionActive: Boolean = false,
    val phase: VoicePhase = VoicePhase.Idle,
    /** What the recognizer heard so far in this turn. */
    val heard: String = "",
    /** Microphone input level 0–1 while listening, for the animation. */
    val level: Float = 0f,
    /** A problem the user should know about (no Turkish voice, no microphone permission…). */
    val notice: String? = null,
    val recognitionAvailable: Boolean = true,
    val tts: TtsStatus = TtsStatus.Starting,
    /** TalkBack (or another screen reader) is on: it reads the answers, HavaCan's own voice stays quiet. */
    val screenReader: Boolean = false,
)

/**
 * One voice session for the whole app, shared by the chat and every main screen.
 *
 * Turn-taking, never open-ended listening: the user starts a session; HavaCan listens for one
 * sentence, answers, and only after its voice has finished (plus a short gap, so the microphone
 * can't pick up its own speech) listens again. Silence twice, "sesli rehberi kapat", the stop
 * button, leaving the app or [sessionLimitMillis] end the session.
 *
 * Commands go through [HavaCanAssistant]: navigation is emitted on [navigation] for the app shell
 * to carry out; anything that writes waits for the user's "evet" there.
 */
class VoiceGuideController(
    private val assistant: HavaCanAssistant,
    private val input: SpeechInput,
    private val output: SpeechOutput,
    private val settingsStore: VoiceSettingsStore,
    private val clock: () -> LocalDateTime = LocalDateTime::now,
    private val sessionLimitMillis: Long = 5 * 60_000L,
) : ViewModel() {

    private val _state = MutableStateFlow(
        VoiceGuideState(settings = settingsStore.settings.value, recognitionAvailable = input.isAvailable, tts = output.status),
    )
    val state: StateFlow<VoiceGuideState> = _state.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _navigation = MutableSharedFlow<VoiceDestination>(extraBufferCapacity = 4)
    val navigation: SharedFlow<VoiceDestination> = _navigation.asSharedFlow()

    /** Answers for the screen reader to read out while it's on. */
    private val _announcements = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val announcements: SharedFlow<String> = _announcements.asSharedFlow()

    /** Bumped by every new step, so a late callback from an old turn does nothing. */
    private var turn = 0
    private var turnTaking = false
    private var misses = 0
    private var limitJob: Job? = null
    private var nextId = 0L

    init {
        output.onStatusChanged = { status -> _state.update { it.copy(tts = status) } }
        viewModelScope.launch { settingsStore.settings.collect { s -> _state.update { it.copy(settings = s) } } }
    }

    // region Settings

    fun setGuideEnabled(enabled: Boolean, welcome: Boolean = true) {
        settingsStore.update { it.copy(guideEnabled = enabled) }
        if (enabled && welcome) {
            say("Sesli rehber açık. Ekrandaki mikrofon düğmesine dokunup konuşabilirsin. Kapatmak için sesli rehberi kapat demen yeterli.")
        } else if (!enabled) {
            stopSession(goodbye = false)
        }
    }

    /** Gizlilik ve veriler > Sohbeti temizle. */
    fun clearMessages() {
        _messages.value = emptyList()
        assistant.resetDialog()
    }

    fun setSpeakTypedReplies(enabled: Boolean) = settingsStore.update { it.copy(speakTypedReplies = enabled) }

    fun setScreenReaderActive(active: Boolean) = _state.update { it.copy(screenReader = active) }

    fun refreshAvailability() = _state.update { it.copy(recognitionAvailable = input.isAvailable, tts = output.status) }

    fun dismissNotice() = _state.update { it.copy(notice = null) }

    // endregion

    // region Session

    /**
     * Starts listening. With [continuous] HavaCan keeps taking turns until told to stop; otherwise
     * (the chat's microphone with the guide off) it listens once, plus any follow-up a plan needs.
     */
    fun startSession(continuous: Boolean = _state.value.settings.guideEnabled) {
        if (!input.isAvailable) {
            _state.update { it.copy(recognitionAvailable = false, notice = "Bu telefonda konuşma tanıma hizmeti bulunamadı.") }
            return
        }
        output.stop()
        turnTaking = continuous
        misses = 0
        _state.update { it.copy(sessionActive = true, notice = null) }
        limitJob?.cancel()
        limitJob = viewModelScope.launch {
            delay(sessionLimitMillis)
            if (_state.value.sessionActive) endSession("Oturum süresi doldu, sesli rehberi kapatıyorum.")
        }
        listen()
    }

    /** The stop button. [goodbye] confirms it out loud, which matters when you can't see the screen. */
    fun stopSession(goodbye: Boolean = true) {
        val wasActive = _state.value.sessionActive
        turn++
        input.cancel()
        output.stop()
        limitJob?.cancel()
        assistant.resetDialog()
        _state.update { it.copy(sessionActive = false, phase = VoicePhase.Idle, heard = "", level = 0f) }
        if (goodbye && wasActive) say("Sesli rehber kapandı.")
    }

    /** Leaving the app: the microphone and the voice stop at once, silently. */
    fun onAppHidden() = stopSession(goodbye = false)

    fun toggleSession() {
        if (_state.value.sessionActive) stopSession() else startSession()
    }

    /**
     * The microphone button: starts a session, stops the one that's listening, or cuts HavaCan
     * short and listens now (also how the next turn starts while a screen reader is on).
     */
    fun tapMicrophone(continuous: Boolean = _state.value.settings.guideEnabled) {
        val s = _state.value
        when {
            !s.sessionActive -> startSession(continuous)
            s.phase == VoicePhase.Listening -> stopSession()
            else -> {
                output.stop()
                listen()
            }
        }
    }

    private fun listen() {
        val myTurn = ++turn
        _state.update { it.copy(phase = VoicePhase.Listening, heard = "", level = 0f) }
        input.listen(
            onLevel = { level -> if (turn == myTurn) _state.update { it.copy(level = level) } },
            onPartial = { text -> if (turn == myTurn) _state.update { it.copy(heard = text) } },
            onResult = { text ->
                if (turn == myTurn) {
                    misses = 0
                    process(text, spoken = true)
                }
            },
            onError = { error -> if (turn == myTurn) onListenError(error) },
        )
    }

    private fun onListenError(error: SpeechError) {
        when (error) {
            SpeechError.NoSpeech -> {
                misses++
                if (misses >= MAX_MISSES) {
                    endSession("Seni duyamadım, sesli rehberi kapatıyorum.")
                } else {
                    respond("Seni duyamadım. Tekrar söyler misin?", relisten = true)
                }
            }
            SpeechError.Busy -> {
                val myTurn = turn
                misses++
                viewModelScope.launch {
                    delay(BUSY_RETRY_MS)
                    if (turn == myTurn && _state.value.sessionActive && misses < MAX_MISSES) listen() else if (turn == myTurn) endSession(null)
                }
            }
            SpeechError.NoPermission -> {
                endSession(null)
                _state.update { it.copy(notice = "Mikrofon izni yok. Profil > HavaCan ile Sesli Kullanım bölümünden izin verebilirsin.") }
            }
            SpeechError.Network -> fail("Konuşma tanıma için internet bağlantısı gerekiyor.")
            SpeechError.Language -> fail("Telefonda Türkçe konuşma tanıma yüklü değil.")
            SpeechError.Unavailable, SpeechError.Other -> fail("Konuşma tanıma şu an çalışmıyor. Biraz sonra tekrar dene.")
        }
    }

    private fun fail(message: String) {
        _state.update { it.copy(notice = message) }
        endSession(message)
    }

    private fun endSession(goodbye: String?) {
        turn++
        input.cancel()
        limitJob?.cancel()
        assistant.resetDialog()
        _state.update { it.copy(sessionActive = false, phase = VoicePhase.Idle, heard = "", level = 0f) }
        if (goodbye != null) respond(goodbye, relisten = false)
    }

    // endregion

    // region Chat

    fun submitTyped(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        if (_state.value.sessionActive) stopSession(goodbye = false)
        output.stop()
        process(clean, spoken = false)
    }

    /** The speaker button on a message. */
    fun speakMessage(message: ChatMessage) {
        if (_state.value.sessionActive) stopSession(goodbye = false)
        say(message.spoken)
    }

    private fun process(text: String, spoken: Boolean) {
        addMessage(ChatMessage(id = nextId++, fromUser = true, title = text, time = now()))
        val myTurn = ++turn
        _state.update { it.copy(phase = VoicePhase.Thinking, heard = text, level = 0f) }
        viewModelScope.launch {
            val reply = runCatching { assistant.handle(text) }.getOrElse { AssistantReply("Bir sorun oldu.", "Tekrar dener misin?") }
            if (turn != myTurn) return@launch // stopped while thinking
            addMessage(ChatMessage(id = nextId++, fromUser = false, title = reply.title, body = reply.body, card = reply.card, time = now()))
            reply.navigate?.let { _navigation.emit(it) }
            if (reply.endSession) {
                endSession(reply.spoken)
                return@launch
            }
            val session = _state.value.sessionActive
            // A one-off question still gets its follow-up ("Saat kaçta?") answered by voice
            val relisten = session && (turnTaking || assistant.isInDialog)
            if (session && !relisten) {
                limitJob?.cancel()
                _state.update { it.copy(sessionActive = false) }
            }
            if (spoken || _state.value.settings.speakTypedReplies) {
                respond(reply.spoken, relisten)
            } else {
                _state.update { it.copy(phase = VoicePhase.Idle) }
            }
        }
    }

    private fun addMessage(message: ChatMessage) = _messages.update { (it + message).takeLast(MAX_MESSAGES) }

    // endregion

    /** Speaks outside a session (welcome, goodbye, the speaker button). */
    private fun say(text: String) = respond(TurkishText.forSpeech(text), relisten = false)

    /** Says [text], then listens again if [relisten]. Only one voice ever speaks: ours or the screen reader's. */
    private fun respond(text: String, relisten: Boolean) {
        val myTurn = ++turn
        if (_state.value.screenReader) {
            // TalkBack reads it in the user's own voice settings. We can't tell when it's done, so
            // the next turn starts with a tap on the microphone instead of opening it under its voice.
            _announcements.tryEmit(text)
            _state.update { it.copy(phase = VoicePhase.Idle, level = 0f) }
            return
        }
        val voice = output.status
        if (voice == TtsStatus.NoTurkish || voice == TtsStatus.Unavailable) {
            _state.update {
                it.copy(
                    phase = VoicePhase.Idle,
                    notice = if (voice == TtsStatus.NoTurkish) "Telefonda Türkçe ses paketi yok; yanıtlar yalnızca yazılı." else "Metin okuma (TTS) bu telefonda çalışmıyor; yanıtlar yalnızca yazılı.",
                )
            }
            if (relisten) relistenAfterGap(myTurn)
            return
        }
        _state.update { it.copy(phase = VoicePhase.Speaking, level = 0f) }
        output.speak(text) {
            if (turn != myTurn) return@speak
            _state.update { it.copy(phase = VoicePhase.Idle) }
            if (relisten) relistenAfterGap(myTurn)
        }
    }

    private fun relistenAfterGap(myTurn: Int) {
        viewModelScope.launch {
            delay(MIC_GAP_MS)
            if (turn == myTurn && _state.value.sessionActive) listen()
        }
    }

    private fun now(): String = "%02d.%02d".format(clock().hour, clock().minute)

    override fun onCleared() {
        input.release()
        output.release()
    }

    companion object {
        /** Pause between HavaCan's voice ending and the microphone opening. */
        const val MIC_GAP_MS = 400L
        private const val BUSY_RETRY_MS = 600L
        private const val MAX_MISSES = 2
        private const val MAX_MESSAGES = 60

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                val graph = AppGraph.get(app)
                val ai = BuildConfig.HAVACAN_API_URL.takeIf { it.isNotBlank() }?.let(::BackendAssistant)
                VoiceGuideController(
                    assistant = HavaCanAssistant(AppAssistantData(graph, WorkPlanScheduler(app), ai)),
                    input = AndroidSpeechInput(app),
                    output = AndroidSpeechOutput(app),
                    settingsStore = PreferencesVoiceSettingsStore(app),
                )
            }
        }
    }
}
