package com.example.weather.ui.voice

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

enum class SpeechError { NoSpeech, NoPermission, Network, Language, Busy, Unavailable, Other }

/** One listening turn at a time; the caller decides when the next one starts. */
interface SpeechInput {
    val isAvailable: Boolean
    fun listen(onLevel: (Float) -> Unit, onPartial: (String) -> Unit, onResult: (String) -> Unit, onError: (SpeechError) -> Unit)
    fun cancel()
    fun release()
}

enum class TtsStatus { Starting, Ready, NoTurkish, Unavailable }

interface SpeechOutput {
    val status: TtsStatus
    /** Told when [status] changes (the engine starts up asynchronously). */
    var onStatusChanged: ((TtsStatus) -> Unit)?
    /** [onDone] runs on the main thread once the voice has finished, failed or was stopped. */
    fun speak(text: String, onDone: () -> Unit)
    fun stop()
    fun release()
}

/**
 * Android's standard SpeechRecognizer, used for a single user-started turn only (never left
 * listening): it stops by itself when the user pauses, and every callback lands on the main thread.
 */
class AndroidSpeechInput(private val context: Context) : SpeechInput {
    private var recognizer: SpeechRecognizer? = null
    private var active = false

    override val isAvailable: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)

    override fun listen(onLevel: (Float) -> Unit, onPartial: (String) -> Unit, onResult: (String) -> Unit, onError: (SpeechError) -> Unit) {
        if (!isAvailable) return onError(SpeechError.Unavailable)
        val r = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
        active = true
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) {
                if (active) onLevel(((rmsdB + 2f) / 12f).coerceIn(0f, 1f))
            }
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onError(error: Int) {
                if (!active) return
                active = false
                onError(mapError(error))
            }
            override fun onResults(results: Bundle?) {
                if (!active) return
                active = false
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (text.isBlank()) onError(SpeechError.NoSpeech) else onResult(text)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (active && !text.isNullOrBlank()) onPartial(text)
            }
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        r.startListening(intent)
    }

    override fun cancel() {
        active = false
        recognizer?.cancel()
    }

    override fun release() {
        active = false
        recognizer?.destroy()
        recognizer = null
    }

    private fun mapError(code: Int): SpeechError = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechError.NoSpeech
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechError.NoPermission
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER -> SpeechError.Network
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> SpeechError.Busy
        else -> if (Build.VERSION.SDK_INT >= 31 &&
            (code == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED || code == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)
        ) SpeechError.Language else SpeechError.Other
    }
}

/** The phone's text-to-speech engine, in Turkish, on the assistant audio stream. */
class AndroidSpeechOutput(context: Context) : SpeechOutput {
    private val main = Handler(Looper.getMainLooper())
    private val callbacks = mutableMapOf<String, () -> Unit>()
    private var queued: Pair<String, () -> Unit>? = null
    private var counter = 0

    override var status: TtsStatus = TtsStatus.Starting
        private set

    override var onStatusChanged: ((TtsStatus) -> Unit)? = null

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { result ->
        main.post {
            status = if (result != TextToSpeech.SUCCESS) TtsStatus.Unavailable else setTurkish()
            onStatusChanged?.invoke(status)
            queued?.let { (text, done) -> queued = null; speak(text, done) }
        }
    }

    private fun setTurkish(): TtsStatus {
        val r = tts.setLanguage(Locale.forLanguageTag("tr-TR"))
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) return TtsStatus.NoTurkish
        tts.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = finish(utteranceId)
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = finish(utteranceId)
            override fun onError(utteranceId: String?, errorCode: Int) = finish(utteranceId)
            override fun onStop(utteranceId: String?, interrupted: Boolean) = finish(utteranceId)
        })
        return TtsStatus.Ready
    }

    private fun finish(id: String?) {
        main.post { id?.let { callbacks.remove(it) }?.invoke() }
    }

    override fun speak(text: String, onDone: () -> Unit) {
        when (status) {
            TtsStatus.Starting -> queued = text to onDone
            TtsStatus.Ready -> {
                val id = "havacan-${counter++}"
                callbacks[id] = onDone
                if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) != TextToSpeech.SUCCESS) finish(id)
            }
            TtsStatus.NoTurkish, TtsStatus.Unavailable -> main.post(onDone)
        }
    }

    override fun stop() {
        queued = null
        tts.stop()
        // onStop isn't promised by every engine: release anyone still waiting
        val pending = callbacks.values.toList()
        callbacks.clear()
        pending.forEach { main.post(it) }
    }

    override fun release() {
        callbacks.clear()
        tts.shutdown()
    }
}
