package com.openpaw.app.presentation.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton manager for Speech-to-Text (STT) and Text-to-Speech (TTS).
 *
 * Also owns the [voiceTrigger] SharedFlow used to activate voice input
 * from outside the app (FloatingBubbleService, OpenPawQsTile).
 *
 * The trigger emits a timestamp; ChatViewModel filters out stale triggers
 * (>5 s old) to avoid replaying on ViewModel recreation.
 */
@Singleton
class VoiceInputManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // ── STT state ─────────────────────────────────────────────────────────────

    enum class SttState { IDLE, LISTENING, PROCESSING, ERROR }

    private val _sttState = MutableStateFlow(SttState.IDLE)
    val sttState: StateFlow<SttState> = _sttState.asStateFlow()

    // ── TTS enable ────────────────────────────────────────────────────────────

    private val _ttsEnabled = MutableStateFlow(false)
    val ttsEnabled: StateFlow<Boolean> = _ttsEnabled.asStateFlow()

    fun setTtsEnabled(enabled: Boolean) { _ttsEnabled.value = enabled }

    // ── External voice trigger (replay=1 so late subscribers still get it) ───

    private val _voiceTrigger = MutableSharedFlow<Long>(replay = 1)
    val voiceTrigger: SharedFlow<Long> = _voiceTrigger.asSharedFlow()

    /** Called by FloatingBubbleService / QsTile / onNewIntent to start voice. */
    fun triggerVoiceStart() {
        _voiceTrigger.tryEmit(System.currentTimeMillis())
    }

    // ── Internal instances ────────────────────────────────────────────────────

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        initTts()
    }

    /**
     * Try Google TTS engine first (neural voices). If it fails to init,
     * fall back to the system default TTS engine.
     */
    private fun initTts() {
        tts = TextToSpeech(context, { status ->
            if (status == TextToSpeech.SUCCESS) {
                configureTts()
            } else {
                // Google TTS not available – retry with system default
                tts = TextToSpeech(context) { s ->
                    ttsReady = (s == TextToSpeech.SUCCESS)
                    if (ttsReady) configureTts()
                }
            }
        }, "com.google.android.tts")
    }

    private fun configureTts() {
        ttsReady = true
        val locale = Locale.getDefault()
        tts?.language = locale

        // Pick the highest-quality OFFLINE voice for the user's language.
        // Google TTS ships several voice qualities (Normal / High / Very High).
        val bestVoice: Voice? = tts?.voices
            ?.filter { v ->
                !v.isNetworkConnectionRequired &&
                v.locale.language == locale.language &&
                (v.features == null || !v.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED))
            }
            ?.maxByOrNull { it.quality }
        if (bestVoice != null) tts?.voice = bestVoice

        tts?.setSpeechRate(1.15f)  // slightly faster = less robotic
        tts?.setPitch(1.0f)
    }

    // ── STT ───────────────────────────────────────────────────────────────────

    /** Must be called from the Main thread (SpeechRecognizer requirement). */
    fun startListening(onResult: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _sttState.value = SttState.ERROR
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { sr ->
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p: Bundle?) { _sttState.value = SttState.LISTENING }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(dB: Float) {}
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech()          { _sttState.value = SttState.PROCESSING }
                override fun onPartialResults(p: Bundle?) {}
                override fun onEvent(t: Int, p: Bundle?) {}

                override fun onError(error: Int) {
                    _sttState.value = SttState.ERROR
                }

                override fun onResults(results: Bundle?) {
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull() ?: ""
                    _sttState.value = SttState.IDLE
                    if (text.isNotBlank()) onResult(text)
                }
            })
            sr.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                // Shorter silence thresholds = faster response after user stops speaking
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 700L)
            })
        }
    }

    fun stopListening() {
        recognizer?.stopListening()
        _sttState.value = SttState.IDLE
    }

    // ── TTS ───────────────────────────────────────────────────────────────────

    fun speak(text: String) {
        if (!ttsReady || !_ttsEnabled.value || text.isBlank()) return
        // Strip markdown symbols so TTS reads cleanly
        val clean = text
            .replace(Regex("\\*+"), "")
            .replace(Regex("`+"), "")
            .replace(Regex("#+\\s*"), "")
            .trim()
            .take(500)           // hard cap so TTS doesn't run forever
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "tts_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() { tts?.stop() }

    /**
     * Speak [text] regardless of the in-app TTS toggle.
     * Used by FloatingBubbleService where TTS is the primary output channel.
     */
    fun speakAlways(text: String) {
        if (!ttsReady || text.isBlank()) return
        val clean = text
            .replace(Regex("\\*+"), "")
            .replace(Regex("`+"), "")
            .replace(Regex("#+\\s*"), "")
            .trim()
            .take(500)
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "tts_${System.currentTimeMillis()}")
    }
}
