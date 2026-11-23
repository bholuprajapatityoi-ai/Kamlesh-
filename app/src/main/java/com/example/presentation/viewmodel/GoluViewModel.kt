package com.example.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.BackendConfig
import com.example.core.BackendConfigStatus
import com.example.core.BackendConnectionManager
import com.example.core.BatteryTelemetry
import com.example.core.DeviceTelemetry
import com.example.core.NetworkResult
import com.example.data.local.SettingsRepository
import com.example.data.remote.GoluApiService
import com.example.data.remote.dto.AssistantSettingsUpdateDto
import com.example.data.remote.dto.VoiceSettingsUpdateDto
import com.example.data.remote.dto.WakeWordSettingsUpdateDto
import com.example.data.repository.AuthRepository
import com.example.di.AppContainer
import com.example.domain.ai.AndroidActionExecutor
import com.example.domain.ai.HybridAiRouter
import com.example.domain.model.ActivityLogItem
import com.example.domain.model.AuthState
import com.example.domain.model.BackendDiagnostics
import com.example.domain.model.ChatMessage
import com.example.domain.model.ConnectionPhase
import com.example.domain.model.GoluLanguage
import com.example.domain.model.GoluSettings
import com.example.domain.model.InstalledAppInfo
import com.example.domain.model.ResponseStyle
import com.example.domain.model.RoutingTarget
import com.example.domain.model.VoiceGender
import com.example.domain.model.VoiceState
import com.example.domain.model.WakeWordState
import com.example.domain.voice.VoiceEngine
import com.example.domain.wakeword.WakeWordEngine
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AuthUiFeedback(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

class GoluViewModel(
    private val appContext: Context,
    private val apiService: GoluApiService,
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val backendConnectionManager: BackendConnectionManager,
    private val voiceEngine: VoiceEngine,
    private val wakeWordEngine: WakeWordEngine,
    private val actionExecutor: AndroidActionExecutor,
    private val hybridAiRouter: HybridAiRouter,
) : ViewModel() {

    val settings: StateFlow<GoluSettings> =
        settingsRepository.settingsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = GoluSettings(),
        )

    val voiceState: StateFlow<VoiceState> = voiceEngine.voiceState
    val partialTranscript: StateFlow<String> = voiceEngine.partialTranscript
    val audioLevel: StateFlow<Float> = voiceEngine.audioLevel
    val voiceError: StateFlow<String?> = voiceEngine.lastError

    val wakeWordState: StateFlow<WakeWordState> = wakeWordEngine.state
    val wakeWordNote: StateFlow<String> = wakeWordEngine.statusNote

    private val _authState = MutableStateFlow<AuthState>(AuthState.Checking)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _authFeedback = MutableStateFlow(AuthUiFeedback())
    val authFeedback: StateFlow<AuthUiFeedback> = _authFeedback.asStateFlow()

    val diagnostics: StateFlow<BackendDiagnostics> = backendConnectionManager.diagnostics

    private val _messages =
        MutableStateFlow(
            listOf(
                ChatMessage(
                    id = "welcome",
                    isFromUser = false,
                    text = "Hello! Boss, GOLU ready hai! Bolke ya type karke poochiye.",
                    routingTarget = RoutingTarget.OFFLINE_ENGINE,
                )
            )
        )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _activities = MutableStateFlow<List<ActivityLogItem>>(emptyList())
    val activities: StateFlow<List<ActivityLogItem>> = _activities.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _battery = MutableStateFlow(DeviceTelemetry.getBatteryTelemetry(appContext))
    val battery: StateFlow<BatteryTelemetry> = _battery.asStateFlow()

    private val _currentTime = MutableStateFlow(DeviceTelemetry.formattedTime())
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    private val _currentDate = MutableStateFlow(DeviceTelemetry.formattedDate())
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    private val _actionToast = MutableStateFlow<String?>(null)
    val actionToast: StateFlow<String?> = _actionToast.asStateFlow()

    private var lastUserPrompt: String? = null
    private var lastPhaseObserved: ConnectionPhase? = null

    init {
        // Coordinate single microphone ownership between VoiceEngine and WakeWordEngine
        voiceEngine.onBeforeMicAcquire = { wakeWordEngine.pauseForActiveMicSession() }
        voiceEngine.onMicReleased = {
            if (settings.value.wakeWordEnabled) {
                wakeWordEngine.resumeAfterActiveMicSession()
            }
        }
        wakeWordEngine.setOnWakeDetectedListener {
            logActivity("Wake-word \"Hey Golu\" detected", "Activated hands-free listening", RoutingTarget.OFFLINE_ENGINE)
            wakeWordEngine.markListeningAfterActivation()
            startVoiceListening(speakWakeAck = true)
        }

        viewModelScope.launch {
            settings.collect { current ->
                voiceEngine.updateSettings(current)
            }
        }

        // Auto-restore session whenever backend transitions to CONNECTED
        viewModelScope.launch {
            diagnostics.collect { diag ->
                if (diag.connectionPhase == ConnectionPhase.CONNECTED &&
                    lastPhaseObserved != ConnectionPhase.CONNECTED &&
                    _authState.value is AuthState.Unauthenticated
                ) {
                    restoreSession()
                }
                lastPhaseObserved = diag.connectionPhase
            }
        }

        // Live telemetry ticker (clock & battery)
        viewModelScope.launch {
            while (true) {
                _currentTime.value = DeviceTelemetry.formattedTime()
                _currentDate.value = DeviceTelemetry.formattedDate()
                _battery.value = DeviceTelemetry.getBatteryTelemetry(appContext)
                delay(15_000L)
            }
        }

        // Load installed apps + start automatic backend connection + restore user session
        refreshInstalledApps()
        backendConnectionManager.start()
        restoreSession()
    }

    fun retryBackendConnection() {
        backendConnectionManager.retryNow()
    }

    fun restoreSession() {
        viewModelScope.launch {
            _authState.value = AuthState.Checking
            when (val res = authRepository.restoreSession()) {
                is NetworkResult.Success -> {
                    _authState.value =
                        if (res.data != null) {
                            syncSettingsFromBackend()
                            AuthState.Authenticated(res.data)
                        } else {
                            AuthState.Unauthenticated
                        }
                }
                is NetworkResult.Error -> {
                    val cached = authRepository.getCachedUser()
                    if (cached != null && res.causeCategory != "AUTH_ERROR") {
                        _authState.value = AuthState.Unauthenticated
                        _authFeedback.value =
                            AuthUiFeedback(
                                errorMessage =
                                    "Saved session could not be verified: ${res.message}",
                            )
                    } else {
                        _authState.value = AuthState.Unauthenticated
                    }
                }
            }
        }
    }

    fun register(fullName: String, email: String, password: String, confirmPassword: String) {
        viewModelScope.launch {
            _authFeedback.value = AuthUiFeedback(isLoading = true)
            when (val res = authRepository.register(fullName, email, password, confirmPassword)) {
                is NetworkResult.Success -> {
                    _authState.value = AuthState.Authenticated(res.data)
                    _authFeedback.value =
                        AuthUiFeedback(successMessage = "Welcome Boss ${res.data.fullName}! Account ready.")
                    logActivity("Account Registered", res.data.email, RoutingTarget.ONLINE_AI_BACKEND)
                    syncSettingsFromBackend()
                }
                is NetworkResult.Error -> {
                    _authFeedback.value = AuthUiFeedback(errorMessage = res.message)
                }
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authFeedback.value = AuthUiFeedback(isLoading = true)
            when (val res = authRepository.login(email, password)) {
                is NetworkResult.Success -> {
                    _authState.value = AuthState.Authenticated(res.data)
                    _authFeedback.value =
                        AuthUiFeedback(successMessage = "Welcome back, Boss ${res.data.fullName}!")
                    logActivity("Signed In", res.data.email, RoutingTarget.ONLINE_AI_BACKEND)
                    syncSettingsFromBackend()
                }
                is NetworkResult.Error -> {
                    _authFeedback.value = AuthUiFeedback(errorMessage = res.message)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _authFeedback.value = AuthUiFeedback(isLoading = true)
            when (val res = authRepository.logout()) {
                is NetworkResult.Success -> {
                    _authState.value = AuthState.Unauthenticated
                    _authFeedback.value = AuthUiFeedback(successMessage = res.data)
                    logActivity("Signed Out", "Tokens cleared from Android Keystore vault", RoutingTarget.OFFLINE_ENGINE)
                }
                is NetworkResult.Error -> {
                    _authFeedback.value = AuthUiFeedback(errorMessage = res.message)
                }
            }
        }
    }

    fun requestPasswordReset(email: String) {
        viewModelScope.launch {
            _authFeedback.value = AuthUiFeedback(isLoading = true)
            when (val res = authRepository.forgotPassword(email)) {
                is NetworkResult.Success -> {
                    val msg =
                        if (res.data.deliveryConfigured == false || res.data.emailSent == false) {
                            "${res.data.message} (Note: SMTP email delivery is not configured on the backend.)"
                        } else {
                            res.data.message
                        }
                    _authFeedback.value = AuthUiFeedback(successMessage = msg)
                }
                is NetworkResult.Error -> {
                    _authFeedback.value = AuthUiFeedback(errorMessage = res.message)
                }
            }
        }
    }

    fun confirmPasswordReset(token: String, newPass: String, confirmPass: String) {
        viewModelScope.launch {
            _authFeedback.value = AuthUiFeedback(isLoading = true)
            when (val res = authRepository.resetPassword(token, newPass, confirmPass)) {
                is NetworkResult.Success -> {
                    _authFeedback.value = AuthUiFeedback(successMessage = res.data.message)
                }
                is NetworkResult.Error -> {
                    _authFeedback.value = AuthUiFeedback(errorMessage = res.message)
                }
            }
        }
    }

    fun clearAuthFeedback() {
        _authFeedback.value = AuthUiFeedback()
    }

    // Phase 4 & 6 Voice + Hybrid AI Flow
    fun startVoiceListening(speakWakeAck: Boolean = false) {
        if (voiceState.value == VoiceState.LISTENING) {
            voiceEngine.stopListening()
            return
        }
        if (speakWakeAck) {
            voiceEngine.speak("Ji Boss!")
        }
        voiceEngine.startListening { transcript ->
            submitPrompt(transcript, speakResponse = true)
        }
    }

    fun stopVoiceListening() = voiceEngine.stopListening()
    fun pauseSpeaking() = voiceEngine.pauseSpeaking()
    fun resumeSpeaking() = voiceEngine.resumeSpeaking()
    fun stopSpeaking() = voiceEngine.stopSpeaking()
    fun clearVoiceError() = voiceEngine.clearError()

    fun submitPrompt(
        rawText: String,
        speakResponse: Boolean = true,
        confirmedSensitive: Boolean = false,
    ) {
        val text = rawText.trim()
        if (text.isBlank()) return
        lastUserPrompt = text

        appendMessage(ChatMessage(id = UUID.randomUUID().toString(), isFromUser = true, text = text))
        _isThinking.value = true
        voiceEngine.setProcessing()

        viewModelScope.launch {
            val result = hybridAiRouter.processInput(text, settings.value, confirmedSensitive)
            _isThinking.value = false

            appendMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    isFromUser = false,
                    text = result.reply,
                    routingTarget = result.routingTarget,
                    intentType = result.intentType,
                    isError = result.isError,
                    pendingConfirmationCommand =
                        if (result.requiresConfirmation &&
                            result.pendingMemoryContent == null &&
                            result.pendingWhatsAppDraft == null &&
                            result.pendingCallTarget == null
                        ) {
                            text
                        } else {
                            null
                        },
                    pendingMemoryContent = result.pendingMemoryContent,
                    pendingMemoryCategory = result.pendingMemoryCategory,
                    pendingWhatsAppDraft = result.pendingWhatsAppDraft,
                    pendingWhatsAppRecipient = result.pendingWhatsAppRecipient,
                    pendingCallTarget = result.pendingCallTarget,
                )
            )

            val activitySubtitle =
                if (settings.value.privacyRedactLogs) "Intent: ${result.intentType.name}"
                else text.take(48)
            logActivity(
                title = "GOLU (${result.routingTarget.name.replace("_", " ")})",
                subtitle = activitySubtitle,
                source = result.routingTarget,
            )

            if (speakResponse) {
                voiceEngine.speak(result.reply)
            } else {
                voiceEngine.setIdle()
            }
        }
    }

    fun retryLastPrompt() {
        lastUserPrompt?.let { submitPrompt(it, speakResponse = true) }
    }

    fun clearConversationContext() {
        viewModelScope.launch {
            hybridAiRouter.clearTemporaryContext()
            _messages.value =
                listOf(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        isFromUser = false,
                        text = "Temporary conversation context cleared, Boss! Fresh shuruat karte hain.",
                        routingTarget = RoutingTarget.OFFLINE_ENGINE,
                    )
                )
            logActivity("Context Cleared", "Temporary conversation turns reset", RoutingTarget.OFFLINE_ENGINE)
        }
    }

    // Phase 2 Quick Actions
    fun runQuickAction(actionName: String) {
        val res =
            when (actionName) {
                "Call" -> actionExecutor.openDialer()
                "WhatsApp" -> actionExecutor.openWhatsApp()
                "Alarm" -> actionExecutor.openAlarm()
                "Reminders" -> actionExecutor.openCalendarReminder("GOLU Reminder")
                "Calendar" -> actionExecutor.openCalendarReminder("GOLU Event")
                "Battery" -> {
                    val b = DeviceTelemetry.getBatteryTelemetry(appContext)
                    _battery.value = b
                    val msg = "Boss, battery ${b.levelPercent}% hai${if (b.isCharging) " (Charging)" else ""}."
                    _actionToast.value = msg
                    voiceEngine.speak(msg)
                    logActivity("Battery Checked", msg, RoutingTarget.ANDROID_ACTION)
                    return
                }
                "Settings" -> actionExecutor.openSettings()
                else -> return
            }
        _actionToast.value = res.message
        logActivity("Quick Action: $actionName", res.message, RoutingTarget.ANDROID_ACTION)
    }

    fun launchInstalledApp(app: InstalledAppInfo) {
        val res = actionExecutor.openAppByQuery(app.appName)
        _actionToast.value = res.message
        logActivity("Launched ${app.appName}", app.packageName, RoutingTarget.ANDROID_ACTION)
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            _installedApps.value = actionExecutor.getInstalledLaunchableApps()
        }
    }

    fun openBatteryOptimizationSettings() {
        val res = actionExecutor.openBatteryOptimizationSettings()
        _actionToast.value = res.message
    }

    fun clearActionToast() {
        _actionToast.value = null
    }

    // Phase 5 Wake Word Controls
    fun toggleWakeWord(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateWakeWordEnabled(enabled)
            if (enabled) {
                wakeWordEngine.enableMonitoring()
                logActivity("Hey Golu Enabled", "Foreground local wake-word monitoring started", RoutingTarget.OFFLINE_ENGINE)
            } else {
                wakeWordEngine.disableMonitoring()
                logActivity("Hey Golu Disabled", "Wake-word monitoring stopped", RoutingTarget.OFFLINE_ENGINE)
            }
            pushWakeWordSettings(enabled)
        }
    }

    // Settings updates
    fun setDarkTheme(enabled: Boolean) = viewModelScope.launch { settingsRepository.updateDarkTheme(enabled) }
    fun setVoiceGender(gender: VoiceGender) = viewModelScope.launch {
        settingsRepository.updateVoiceGender(gender)
        pushVoiceSettings(voiceGender = gender.label)
    }
    fun setSpeechSpeed(speed: Float) = viewModelScope.launch {
        settingsRepository.updateSpeechSpeed(speed)
        pushVoiceSettings(speechSpeed = speed)
    }
    fun setLanguage(lang: GoluLanguage) = viewModelScope.launch {
        settingsRepository.updateLanguage(lang)
        pushVoiceSettings(language = lang.label)
    }
    fun setGreeting(greeting: String) = viewModelScope.launch {
        settingsRepository.updateGreeting(greeting)
        pushAssistantSettings(greetingMessage = greeting)
    }
    fun setResponseStyle(style: ResponseStyle) = viewModelScope.launch {
        settingsRepository.updateResponseStyle(style)
        pushVoiceSettings(responseStyle = style.label)
    }
    fun setKeepSessionContext(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.updateKeepSessionContext(enabled)
        pushAssistantSettings(keepConversationHistory = enabled)
    }
    fun setPrivacyRedactLogs(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.updatePrivacyRedactLogs(enabled)
        pushAssistantSettings(privacyRedactLogs = enabled)
    }

    private fun canSyncBackend(): Boolean {
        if (_authState.value !is AuthState.Authenticated) return false
        val eval = BackendConfig.evaluateConfigured()
        return eval.status == BackendConfigStatus.READY_HTTPS ||
            eval.status == BackendConfigStatus.READY_EMULATOR_DEV
    }

    private fun syncSettingsFromBackend() {
        viewModelScope.launch {
            val eval = BackendConfig.evaluateConfigured()
            if (eval.status != BackendConfigStatus.READY_HTTPS &&
                eval.status != BackendConfigStatus.READY_EMULATOR_DEV
            ) return@launch

            runCatching { apiService.getVoiceSettings() }.onSuccess { voice ->
                GoluLanguage.entries.firstOrNull { it.label == voice.language }?.let {
                    settingsRepository.updateLanguage(it)
                }
                VoiceGender.entries.firstOrNull { it.label == voice.voiceGender }?.let {
                    settingsRepository.updateVoiceGender(it)
                }
                settingsRepository.updateSpeechSpeed(voice.speechSpeed)
                ResponseStyle.entries.firstOrNull { it.label == voice.responseStyle }?.let {
                    settingsRepository.updateResponseStyle(it)
                }
            }
            runCatching { apiService.getAssistantSettings() }.onSuccess { asst ->
                settingsRepository.updateGreeting(asst.greetingMessage)
                settingsRepository.updateKeepSessionContext(asst.keepConversationHistory)
                settingsRepository.updatePrivacyRedactLogs(asst.privacyRedactLogs)
            }
        }
    }

    private fun pushVoiceSettings(
        language: String? = null,
        voiceGender: String? = null,
        speechSpeed: Float? = null,
        responseStyle: String? = null,
    ) {
        if (!canSyncBackend()) return
        viewModelScope.launch {
            runCatching {
                apiService.updateVoiceSettings(
                    VoiceSettingsUpdateDto(
                        language = language,
                        voiceGender = voiceGender,
                        speechSpeed = speechSpeed,
                        responseStyle = responseStyle,
                    )
                )
            }
        }
    }

    private fun pushAssistantSettings(
        greetingMessage: String? = null,
        keepConversationHistory: Boolean? = null,
        privacyRedactLogs: Boolean? = null,
    ) {
        if (!canSyncBackend()) return
        viewModelScope.launch {
            runCatching {
                apiService.updateAssistantSettings(
                    AssistantSettingsUpdateDto(
                        greetingMessage = greetingMessage,
                        keepConversationHistory = keepConversationHistory,
                        privacyRedactLogs = privacyRedactLogs,
                    )
                )
            }
        }
    }

    private fun pushWakeWordSettings(enabled: Boolean) {
        if (!canSyncBackend()) return
        viewModelScope.launch {
            runCatching {
                apiService.updateWakeWordSettings(WakeWordSettingsUpdateDto(wakeWordEnabled = enabled))
            }
        }
    }

    private fun appendMessage(msg: ChatMessage) {
        _messages.value = _messages.value + msg
    }

    private fun logActivity(title: String, subtitle: String, source: RoutingTarget) {
        val item =
            ActivityLogItem(
                id = UUID.randomUUID().toString(),
                title = title,
                subtitle = subtitle,
                source = source,
            )
        _activities.value = (listOf(item) + _activities.value).take(50)
    }

    override fun onCleared() {
        super.onCleared()
        backendConnectionManager.release()
        voiceEngine.release()
        wakeWordEngine.release()
    }

    class Factory(
        private val appContext: Context,
        private val container: AppContainer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GoluViewModel(
                appContext = appContext,
                apiService = container.apiService,
                settingsRepository = container.settingsRepository,
                authRepository = container.authRepository,
                backendConnectionManager = container.backendConnectionManager,
                voiceEngine = container.voiceEngine,
                wakeWordEngine = container.wakeWordEngine,
                actionExecutor = container.actionExecutor,
                hybridAiRouter = container.hybridAiRouter,
            ) as T
        }
    }
}
