package com.example.domain.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.content.ContextCompat
import com.example.domain.model.GoluLanguage
import com.example.domain.model.GoluSettings
import com.example.domain.model.VoiceGender
import com.example.domain.model.VoiceState
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Phase 4 Complete Voice System:
 * - Real SpeechRecognizer (Hindi, Hinglish, English)
 * - Real TextToSpeech (Male/Female voice preference, Indian accent, speed control)
 * - AudioFocus management & phone call interruption handling
 * - Pause/Resume & Stop Speaking controls
 * - Single-microphone lock coordination with WakeWordEngine
 */
class VoiceEngine(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false

    private var currentSettings: GoluSettings = GoluSettings()
    private var pendingSentences: MutableList<String> = mutableListOf()
    private var currentSpeakingText: String = ""
    private var pausedByCallOrUser = false
    private var isListeningActive = false

    private var onFinalTranscript: ((String) -> Unit)? = null
    var onBeforeMicAcquire: (() -> Unit)? = null
    var onMicReleased: (() -> Unit)? = null

    private var audioFocusRequest: AudioFocusRequest? = null

    private val audioFocusListener =
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    // Phone call or other priority audio interruption
                    mainHandler.post {
                        if (isListeningActive) {
                            cancelListeningInternal(resetToIdle = true)
                        }
                        if (_voiceState.value == VoiceState.SPEAKING) {
                            pauseSpeaking()
                        }
                    }
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    // Do not auto-resume unexpectedly if user manually paused
                }
            }
        }

    init {
        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        textToSpeech =
            TextToSpeech(context.applicationContext) { status ->
                ttsReady = (status == TextToSpeech.SUCCESS)
                if (ttsReady) {
                    applyTtsPreferences(currentSettings)
                    textToSpeech?.setOnUtteranceProgressListener(
                        object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                mainHandler.post {
                                    _voiceState.value = VoiceState.SPEAKING
                                }
                            }

                            override fun onDone(utteranceId: String?) {
                                mainHandler.post {
                                    if (pendingSentences.isNotEmpty() && !pausedByCallOrUser) {
                                        speakNextSentence()
                                    } else if (!pausedByCallOrUser) {
                                        abandonAudioFocus()
                                        if (_voiceState.value == VoiceState.SPEAKING) {
                                            _voiceState.value = VoiceState.IDLE
                                        }
                                    }
                                }
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                                mainHandler.post {
                                    abandonAudioFocus()
                                    _lastError.value = "Text-to-Speech playback error."
                                    _voiceState.value = VoiceState.ERROR
                                }
                            }
                        }
                    )
                }
            }
    }

    fun updateSettings(settings: GoluSettings) {
        currentSettings = settings
        if (ttsReady) {
            applyTtsPreferences(settings)
        }
    }

    private fun applyTtsPreferences(settings: GoluSettings) {
        val tts = textToSpeech ?: return
        val targetLocale =
            when (settings.language) {
                GoluLanguage.HINDI -> Locale.forLanguageTag("hi-IN")
                GoluLanguage.HINGLISH,
                GoluLanguage.ENGLISH -> Locale.forLanguageTag("en-IN")
            }

        val langResult = tts.setLanguage(targetLocale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA ||
            langResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            tts.setLanguage(Locale.ENGLISH)
        }

        // Attempt to pick a matching Indian Male/Female installed TTS voice if available
        val voices = runCatching { tts.voices }.getOrNull().orEmpty()
        val matchingLocaleVoices =
            voices.filter {
                it.locale.language.equals(targetLocale.language, ignoreCase = true) &&
                    (it.locale.country.equals("IN", ignoreCase = true) || it.locale.country.isBlank())
            }
        val femaleHints = listOf("female", "hie", "hid", "ene", "enc", "wavenet-a", "wavenet-d", "-f-")
        val maleHints = listOf("male", "hia", "hib", "end", "enf", "wavenet-b", "wavenet-c", "-m-")

        val preferredVoice =
            when (settings.voiceGender) {
                VoiceGender.FEMALE ->
                    matchingLocaleVoices.firstOrNull { v ->
                        femaleHints.any { v.name.lowercase().contains(it) }
                    }
                VoiceGender.MALE ->
                    matchingLocaleVoices.firstOrNull { v ->
                        maleHints.any { v.name.lowercase().contains(it) }
                    }
            } ?: matchingLocaleVoices.firstOrNull()

        if (preferredVoice != null) {
            runCatching { tts.voice = preferredVoice }
        }

        val naturalPitch =
            when (settings.voiceGender) {
                VoiceGender.MALE -> 0.92f
                VoiceGender.FEMALE -> 1.10f
            }
        tts.setPitch(naturalPitch)
        tts.setSpeechRate(settings.speechSpeed.coerceIn(0.6f, 1.6f))
    }

    fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun startListening(onResult: (String) -> Unit) {
        mainHandler.post {
            if (!hasMicPermission()) {
                _lastError.value = "Microphone permission is required for voice commands."
                _voiceState.value = VoiceState.ERROR
                return@post
            }
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _lastError.value =
                    "Speech recognition service is not available on this device. You can still type commands."
                _voiceState.value = VoiceState.ERROR
                return@post
            }

            // Stop any active TTS first and pause wake-word to prevent dual mic sessions
            stopSpeakingInternal(resetState = false)
            onBeforeMicAcquire?.invoke()

            if (!requestAudioFocus()) {
                _lastError.value = "Could not acquire audio focus (another call or media session is active)."
                _voiceState.value = VoiceState.ERROR
                onMicReleased?.invoke()
                return@post
            }

            onFinalTranscript = onResult
            _lastError.value = null
            _partialTranscript.value = ""
            _audioLevel.value = 0f

            cancelListeningInternal(resetToIdle = false)

            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer
            recognizer.setRecognitionListener(
                object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListeningActive = true
                        _voiceState.value = VoiceState.LISTENING
                    }

                    override fun onBeginningOfSpeech() {
                        _voiceState.value = VoiceState.LISTENING
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        _audioLevel.value = normalized
                    }

                    override fun onBufferReceived(buffer: ByteArray?) = Unit

                    override fun onEndOfSpeech() {
                        _audioLevel.value = 0f
                        _voiceState.value = VoiceState.PROCESSING
                    }

                    override fun onError(error: Int) {
                        isListeningActive = false
                        _audioLevel.value = 0f
                        releaseRecognizer()
                        abandonAudioFocus()
                        val msg = mapSpeechError(error)
                        if (error == SpeechRecognizer.ERROR_NO_MATCH ||
                            error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                        ) {
                            _lastError.value = msg
                            _voiceState.value = VoiceState.IDLE
                        } else {
                            _lastError.value = msg
                            _voiceState.value = VoiceState.ERROR
                        }
                        onMicReleased?.invoke()
                    }

                    override fun onResults(results: Bundle?) {
                        isListeningActive = false
                        _audioLevel.value = 0f
                        val matches =
                            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim().orEmpty()
                        releaseRecognizer()
                        abandonAudioFocus()
                        onMicReleased?.invoke()

                        if (text.isNotBlank()) {
                            _partialTranscript.value = text
                            _voiceState.value = VoiceState.PROCESSING
                            onFinalTranscript?.invoke(text)
                        } else {
                            _lastError.value = "Didn't catch that, Boss. Please try speaking again."
                            _voiceState.value = VoiceState.IDLE
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches =
                            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()?.trim().orEmpty()
                        if (partial.isNotBlank()) {
                            _partialTranscript.value = partial
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                }
            )

            val intent =
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentSettings.language.localeTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
            recognizer.startListening(intent)
        }
    }

    fun stopListening() {
        mainHandler.post {
            cancelListeningInternal(resetToIdle = true)
            onMicReleased?.invoke()
        }
    }

    private fun cancelListeningInternal(resetToIdle: Boolean) {
        isListeningActive = false
        _audioLevel.value = 0f
        runCatching { speechRecognizer?.cancel() }
        releaseRecognizer()
        abandonAudioFocus()
        if (resetToIdle && _voiceState.value == VoiceState.LISTENING) {
            _voiceState.value = VoiceState.IDLE
        }
    }

    private fun releaseRecognizer() {
        runCatching { speechRecognizer?.destroy() }
        speechRecognizer = null
    }

    fun speak(text: String) {
        val clean = text.trim()
        if (clean.isBlank()) {
            _voiceState.value = VoiceState.IDLE
            return
        }
        mainHandler.post {
            if (!ttsReady || textToSpeech == null) {
                _voiceState.value = VoiceState.IDLE
                return@post
            }
            if (!requestAudioFocus()) {
                _voiceState.value = VoiceState.IDLE
                return@post
            }
            pausedByCallOrUser = false
            currentSpeakingText = clean
            pendingSentences = splitIntoSentences(clean).toMutableList()
            applyTtsPreferences(currentSettings)
            speakNextSentence()
        }
    }

    private fun speakNextSentence() {
        val tts = textToSpeech ?: return
        if (pendingSentences.isEmpty()) {
            abandonAudioFocus()
            _voiceState.value = VoiceState.IDLE
            return
        }
        val next = pendingSentences.removeAt(0)
        _voiceState.value = VoiceState.SPEAKING
        val utteranceId = UUID.randomUUID().toString()
        tts.speak(next, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun pauseSpeaking() {
        mainHandler.post {
            if (_voiceState.value == VoiceState.SPEAKING) {
                pausedByCallOrUser = true
                runCatching { textToSpeech?.stop() }
                abandonAudioFocus()
                _voiceState.value = VoiceState.PAUSED
            }
        }
    }

    fun resumeSpeaking() {
        mainHandler.post {
            if (_voiceState.value == VoiceState.PAUSED) {
                if (pendingSentences.isEmpty() && currentSpeakingText.isNotBlank()) {
                    pendingSentences = mutableListOf(currentSpeakingText)
                }
                if (pendingSentences.isNotEmpty() && requestAudioFocus()) {
                    pausedByCallOrUser = false
                    speakNextSentence()
                } else {
                    _voiceState.value = VoiceState.IDLE
                }
            }
        }
    }

    fun stopSpeaking() {
        mainHandler.post {
            stopSpeakingInternal(resetState = true)
        }
    }

    private fun stopSpeakingInternal(resetState: Boolean) {
        pausedByCallOrUser = false
        pendingSentences.clear()
        runCatching { textToSpeech?.stop() }
        abandonAudioFocus()
        if (resetState) {
            _voiceState.value = VoiceState.IDLE
        }
    }

    fun setProcessing() {
        _voiceState.value = VoiceState.PROCESSING
    }

    fun setIdle() {
        if (_voiceState.value != VoiceState.SPEAKING) {
            _voiceState.value = VoiceState.IDLE
        }
    }

    fun clearError() {
        _lastError.value = null
        if (_voiceState.value == VoiceState.ERROR) {
            _voiceState.value = VoiceState.IDLE
        }
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req =
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(audioFocusListener)
                    .build()
            audioFocusRequest = req
            audioManager.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { runCatching { audioManager.abandonAudioFocusRequest(it) } }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            runCatching { audioManager.abandonAudioFocus(audioFocusListener) }
        }
    }

    private fun splitIntoSentences(text: String): List<String> =
        text.split(Regex("(?<=[.!?।\\n])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(text) }

    private fun mapSpeechError(error: Int): String =
        when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Microphone audio capture error."
            SpeechRecognizer.ERROR_CLIENT -> "Speech recognizer session cancelled."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied."
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition network unavailable."
            SpeechRecognizer.ERROR_NO_MATCH -> "Kuch samajh nahi aaya, Boss. Phir se boliye."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Microphone is busy. Please wait a second."
            SpeechRecognizer.ERROR_SERVER -> "Speech recognition service error."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected, Boss."
            else -> "Voice recognition error (code $error)."
        }

    fun release() {
        mainHandler.post {
            cancelListeningInternal(resetToIdle = true)
            stopSpeakingInternal(resetState = true)
            runCatching { textToSpeech?.shutdown() }
            textToSpeech = null
        }
    }
}
