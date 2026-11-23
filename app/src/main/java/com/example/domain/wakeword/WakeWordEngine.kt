package com.example.domain.wakeword

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.example.domain.model.WakeWordState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Phase 5: On-Device "Hey Golu" Wake-Word State Machine & Controller.
 *
 * Privacy & Android Constraints:
 * - Disabled by default until explicitly enabled by Boss.
 * - Uses local SpeechRecognizer segments with EXTRA_PREFER_OFFLINE=true where supported;
 *   never uploads raw microphone audio continuously to GOLU backend.
 * - Automatically yields microphone to VoiceEngine during active voice commands.
 * - Never bypasses Android lock screen (checks KeyguardManager for private actions).
 */
class WakeWordEngine(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(WakeWordState.DISABLED)
    val state: StateFlow<WakeWordState> = _state.asStateFlow()

    private val _statusNote =
        MutableStateFlow("Wake-word monitoring is disabled by default for privacy.")
    val statusNote: StateFlow<String> = _statusNote.asStateFlow()

    private var recognizer: SpeechRecognizer? = null
    private var isMonitoringRequested = false
    private var isPausedForVoiceSession = false
    private var onWakeDetectedCallback: (() -> Unit)? = null

    private val wakeMatches = listOf("hey golu", "he golu", "hey goalu", "hello golu", "oye golu")

    fun setOnWakeDetectedListener(listener: () -> Unit) {
        onWakeDetectedCallback = listener
    }

    fun isDeviceLocked(): Boolean {
        val kg = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return kg?.isKeyguardLocked == true
    }

    fun enableMonitoring() {
        mainHandler.post {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                _state.value = WakeWordState.ERROR
                _statusNote.value = "RECORD_AUDIO permission required before enabling Hey Golu."
                return@post
            }
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _state.value = WakeWordState.ERROR
                _statusNote.value = "On-device SpeechRecognizer unavailable on this device."
                return@post
            }

            isMonitoringRequested = true
            isPausedForVoiceSession = false
            _state.value = WakeWordState.INITIALIZING
            _statusNote.value = "Starting foreground wake-word monitor..."

            val serviceIntent = Intent(context, WakeWordForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            startLocalListeningCycle()
        }
    }

    fun disableMonitoring() {
        mainHandler.post {
            isMonitoringRequested = false
            isPausedForVoiceSession = false
            stopRecognizerInternal()
            context.stopService(Intent(context, WakeWordForegroundService::class.java))
            _state.value = WakeWordState.DISABLED
            _statusNote.value = "Wake-word monitoring disabled."
        }
    }

    fun pauseForActiveMicSession() {
        mainHandler.post {
            if (!isMonitoringRequested) return@post
            isPausedForVoiceSession = true
            stopRecognizerInternal()
            _state.value = WakeWordState.PAUSED
            _statusNote.value = "Paused while active GOLU microphone session runs."
        }
    }

    fun resumeAfterActiveMicSession() {
        mainHandler.post {
            if (!isMonitoringRequested || !isPausedForVoiceSession) return@post
            isPausedForVoiceSession = false
            startLocalListeningCycle()
        }
    }

    private fun startLocalListeningCycle() {
        if (!isMonitoringRequested || isPausedForVoiceSession) return
        stopRecognizerInternal()

        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = sr
        sr.setRecognitionListener(
            object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    if (isMonitoringRequested && !isPausedForVoiceSession) {
                        _state.value = WakeWordState.MONITORING
                        _statusNote.value = "Listening locally for \"Hey Golu\"..."
                    }
                }

                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit

                override fun onError(error: Int) {
                    stopRecognizerInternal()
                    if (!isMonitoringRequested || isPausedForVoiceSession) return
                    if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                        _state.value = WakeWordState.ERROR
                        _statusNote.value = "Microphone permission revoked."
                        isMonitoringRequested = false
                        return
                    }
                    // Restart monitoring cycle politely with backoff to respect battery
                    _state.value = WakeWordState.MONITORING
                    mainHandler.postDelayed({ startLocalListeningCycle() }, 900L)
                }

                override fun onResults(results: Bundle?) {
                    val candidates =
                        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    if (containsWakePhrase(candidates)) {
                        handleWakeTriggered()
                    } else if (isMonitoringRequested && !isPausedForVoiceSession) {
                        mainHandler.postDelayed({ startLocalListeningCycle() }, 400L)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val candidates =
                        partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            .orEmpty()
                    if (containsWakePhrase(candidates)) {
                        handleWakeTriggered()
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
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        runCatching { sr.startListening(intent) }.onFailure {
            _state.value = WakeWordState.ERROR
            _statusNote.value = "Could not start local wake-word listener: ${it.localizedMessage}"
        }
    }

    private fun containsWakePhrase(candidates: List<String>): Boolean {
        return candidates.any { phrase ->
            val lower = phrase.lowercase().trim()
            wakeMatches.any { lower.contains(it) }
        }
    }

    private fun handleWakeTriggered() {
        stopRecognizerInternal()
        isPausedForVoiceSession = true
        _state.value = WakeWordState.DETECTED
        _statusNote.value = "\"Hey Golu\" detected!"
        mainHandler.post {
            _state.value = WakeWordState.ACTIVATED
            onWakeDetectedCallback?.invoke()
        }
    }

    fun markListeningAfterActivation() {
        if (isMonitoringRequested) {
            _state.value = WakeWordState.LISTENING
        }
    }

    private fun stopRecognizerInternal() {
        runCatching { recognizer?.cancel() }
        runCatching { recognizer?.destroy() }
        recognizer = null
    }

    fun release() {
        disableMonitoring()
    }
}
