package com.example.domain.model

import com.example.common.GoluConstants
import com.example.core.BackendConfigStatus

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    PAUSED,
    ERROR,
}

enum class WakeWordState {
    DISABLED,
    INITIALIZING,
    MONITORING,
    DETECTED,
    ACTIVATED,
    LISTENING,
    PAUSED,
    ERROR,
}

enum class GoluLanguage(val label: String, val localeTag: String) {
    HINGLISH("Hinglish", "en-IN"),
    HINDI("Hindi", "hi-IN"),
    ENGLISH("English", "en-IN"),
}

enum class VoiceGender(val label: String) {
    MALE("Male"),
    FEMALE("Female"),
}

enum class ResponseStyle(val label: String) {
    PLAYFUL_BOSS("Playful Boss Mode"),
    CONCISE("Concise"),
    DETAILED("Detailed"),
}

enum class IntentType {
    GREETING,
    TIME_QUERY,
    DATE_QUERY,
    BATTERY_QUERY,
    OPEN_APP,
    SET_ALARM,
    SET_REMINDER,
    CALL_CONTACT,
    WHATSAPP_ACTION,
    OPEN_SETTINGS,
    MEMORY_SAVE,
    PHONE_CONTROL,
    DAILY_LIFE_QUERY,
    EXPLANATION,
    FOLLOW_UP,
    SENSITIVE_ACTION,
    GENERAL_CHAT,
}

enum class RoutingTarget {
    OFFLINE_ENGINE,
    ANDROID_ACTION,
    ONLINE_AI_BACKEND,
}

enum class ConnectionPhase(val label: String) {
    CONNECTING("Connecting..."),
    CONNECTED("Connected"),
    OFFLINE("Offline"),
    ERROR("Error"),
    NOT_CONFIGURED("Offline"),
}

data class GoluSettings(
    val isDarkTheme: Boolean = true,
    val voiceGender: VoiceGender = VoiceGender.MALE,
    val speechSpeed: Float = 1.0f,
    val language: GoluLanguage = GoluLanguage.HINGLISH,
    val greeting: String = GoluConstants.DEFAULT_GREETING,
    val responseStyle: ResponseStyle = ResponseStyle.PLAYFUL_BOSS,
    val wakeWordEnabled: Boolean = false,
    val keepSessionContext: Boolean = true,
    val privacyRedactLogs: Boolean = true,
)

data class UserProfile(
    val id: String,
    val fullName: String,
    val email: String,
    val preferredTitle: String = GoluConstants.PRIMARY_USER_TITLE,
    val isVerified: Boolean = false,
    val createdAt: String = "",
)

sealed interface AuthState {
    data object Checking : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val user: UserProfile) : AuthState
}

data class BackendDiagnostics(
    val connectionPhase: ConnectionPhase = ConnectionPhase.CONNECTING,
    val configStatus: BackendConfigStatus = BackendConfigStatus.UNCONFIGURED,
    val baseUrl: String = "",
    val isDeviceOnline: Boolean = false,
    val isBackendReachable: Boolean = false,
    val isDatabaseConnected: Boolean = false,
    val aiConfigured: Boolean = false,
    val emailDeliveryConfigured: Boolean = false,
    val statusSummary: String = "Connecting automatically...",
    val rootCauseDetail: String = "",
    val lastCheckedAt: String = "",
    val lastConnectedAt: String = "",
    val retryAttempt: Int = 0,
    val nextRetrySeconds: Int = 0,
)

data class ChatMessage(
    val id: String,
    val isFromUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val routingTarget: RoutingTarget? = null,
    val intentType: IntentType? = null,
    val isError: Boolean = false,
    val pendingConfirmationCommand: String? = null,
    val pendingMemoryContent: String? = null,
    val pendingMemoryCategory: String? = null,
    val pendingWhatsAppDraft: String? = null,
    val pendingWhatsAppRecipient: String? = null,
    val pendingCallTarget: String? = null,
)

data class ActivityLogItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val source: RoutingTarget,
    val timestamp: Long = System.currentTimeMillis(),
)

data class InstalledAppInfo(
    val appName: String,
    val packageName: String,
)
