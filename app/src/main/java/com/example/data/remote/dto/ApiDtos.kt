package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HealthResponseDto(
    @Json(name = "status") val status: String,
    @Json(name = "backend_ok") val backendOk: Boolean,
    @Json(name = "database_ok") val databaseOk: Boolean,
    @Json(name = "database_error") val databaseError: String? = null,
    @Json(name = "redis_ok") val redisOk: Boolean = false,
    @Json(name = "missing_config") val missingConfig: List<String> = emptyList(),
    @Json(name = "timestamp") val timestamp: String? = null,
)

@JsonClass(generateAdapter = true)
data class DatabaseHealthResponseDto(
    @Json(name = "status") val status: String,
    @Json(name = "database_ok") val databaseOk: Boolean,
    @Json(name = "database_engine") val databaseEngine: String = "postgresql",
    @Json(name = "latency_ms") val latencyMs: Double = 0.0,
    @Json(name = "redis_ok") val redisOk: Boolean = false,
    @Json(name = "database_error") val databaseError: String? = null,
    @Json(name = "timestamp") val timestamp: String? = null,
)

@JsonClass(generateAdapter = true)
data class SystemStatusResponseDto(
    @Json(name = "app") val app: String,
    @Json(name = "version") val version: String,
    @Json(name = "environment") val environment: String,
    @Json(name = "database_status") val databaseStatus: String,
    @Json(name = "redis_status") val redisStatus: String = "unconfigured",
    @Json(name = "openai_configured") val openaiConfigured: Boolean,
    @Json(name = "email_delivery_configured") val emailDeliveryConfigured: Boolean,
    @Json(name = "missing_config") val missingConfig: List<String> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class SystemConfigResponseDto(
    @Json(name = "api_version") val apiVersion: String,
    @Json(name = "supported_languages") val supportedLanguages: List<String> = emptyList(),
    @Json(name = "wake_phrase") val wakePhrase: String = "Hey Golu",
    @Json(name = "email_delivery_configured") val emailDeliveryConfigured: Boolean = false,
    @Json(name = "online_ai_available") val onlineAiAvailable: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class RegisterRequestDto(
    @Json(name = "full_name") val fullName: String,
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "confirm_password") val confirmPassword: String,
)

@JsonClass(generateAdapter = true)
data class LoginRequestDto(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
)

@JsonClass(generateAdapter = true)
data class RefreshRequestDto(
    @Json(name = "refresh_token") val refreshToken: String,
)

@JsonClass(generateAdapter = true)
data class ForgotPasswordRequestDto(
    @Json(name = "email") val email: String,
)

@JsonClass(generateAdapter = true)
data class ResetPasswordRequestDto(
    @Json(name = "token") val token: String,
    @Json(name = "new_password") val newPassword: String,
    @Json(name = "confirm_password") val confirmPassword: String,
)

@JsonClass(generateAdapter = true)
data class VerifyEmailConfirmRequestDto(
    @Json(name = "token") val token: String,
)

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "id") val id: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "email") val email: String,
    @Json(name = "preferred_title") val preferredTitle: String = "Boss",
    @Json(name = "is_verified") val isVerified: Boolean = false,
    @Json(name = "created_at") val createdAt: String = "",
)

@JsonClass(generateAdapter = true)
data class AuthResponseDto(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "token_type") val tokenType: String = "bearer",
    @Json(name = "expires_in") val expiresIn: Long = 900,
    @Json(name = "user") val user: UserDto,
)

@JsonClass(generateAdapter = true)
data class MessageResponseDto(
    @Json(name = "message") val message: String,
    @Json(name = "email_sent") val emailSent: Boolean? = null,
    @Json(name = "delivery_configured") val deliveryConfigured: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class AiChatRequestDto(
    @Json(name = "message") val message: String,
    @Json(name = "language") val language: String = "Hinglish",
    @Json(name = "response_style") val responseStyle: String = "Playful Boss Mode",
    @Json(name = "session_id") val sessionId: String? = null,
)

@JsonClass(generateAdapter = true)
data class AiChatResponseDto(
    @Json(name = "reply") val reply: String,
    @Json(name = "route") val route: String,
    @Json(name = "intent") val intent: String,
    @Json(name = "session_id") val sessionId: String,
    @Json(name = "requires_confirmation") val requiresConfirmation: Boolean = false,
    @Json(name = "provider") val provider: String = "offline",
    @Json(name = "model") val model: String? = null,
    @Json(name = "prompt_tokens") val promptTokens: Int = 0,
    @Json(name = "completion_tokens") val completionTokens: Int = 0,
    @Json(name = "total_tokens") val totalTokens: Int = 0,
)

@JsonClass(generateAdapter = true)
data class AiIntentRequestDto(
    @Json(name = "text") val text: String,
)

@JsonClass(generateAdapter = true)
data class AiIntentResponseDto(
    @Json(name = "intent") val intent: String,
    @Json(name = "recommended_route") val recommendedRoute: String,
    @Json(name = "confidence") val confidence: Double,
)

@JsonClass(generateAdapter = true)
data class AiStatusResponseDto(
    @Json(name = "online_ai_configured") val onlineAiConfigured: Boolean,
    @Json(name = "provider") val provider: String = "openai",
    @Json(name = "model") val model: String,
    @Json(name = "offline_engine_ready") val offlineEngineReady: Boolean,
)

@JsonClass(generateAdapter = true)
data class ClearContextRequestDto(
    @Json(name = "session_id") val sessionId: String? = null,
)

@JsonClass(generateAdapter = true)
data class VoiceSettingsDto(
    @Json(name = "language") val language: String,
    @Json(name = "voice_gender") val voiceGender: String,
    @Json(name = "speech_speed") val speechSpeed: Float,
    @Json(name = "response_style") val responseStyle: String,
)

@JsonClass(generateAdapter = true)
data class VoiceSettingsUpdateDto(
    @Json(name = "language") val language: String? = null,
    @Json(name = "voice_gender") val voiceGender: String? = null,
    @Json(name = "speech_speed") val speechSpeed: Float? = null,
    @Json(name = "response_style") val responseStyle: String? = null,
)

@JsonClass(generateAdapter = true)
data class AssistantSettingsDto(
    @Json(name = "greeting_message") val greetingMessage: String,
    @Json(name = "keep_conversation_history") val keepConversationHistory: Boolean,
    @Json(name = "privacy_redact_logs") val privacyRedactLogs: Boolean,
    @Json(name = "wake_word_enabled") val wakeWordEnabled: Boolean,
    @Json(name = "wake_phrase") val wakePhrase: String = "Hey Golu",
)

@JsonClass(generateAdapter = true)
data class AssistantSettingsUpdateDto(
    @Json(name = "greeting_message") val greetingMessage: String? = null,
    @Json(name = "keep_conversation_history") val keepConversationHistory: Boolean? = null,
    @Json(name = "privacy_redact_logs") val privacyRedactLogs: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class WakeWordSettingsDto(
    @Json(name = "wake_word_enabled") val wakeWordEnabled: Boolean,
    @Json(name = "wake_phrase") val wakePhrase: String = "Hey Golu",
)

@JsonClass(generateAdapter = true)
data class WakeWordSettingsUpdateDto(
    @Json(name = "wake_word_enabled") val wakeWordEnabled: Boolean,
)

@JsonClass(generateAdapter = true)
data class DeviceRegisterRequestDto(
    @Json(name = "device_identifier") val deviceIdentifier: String,
    @Json(name = "device_name") val deviceName: String = "Android Device",
    @Json(name = "platform") val platform: String = "android",
    @Json(name = "os_version") val osVersion: String = "",
    @Json(name = "app_version") val appVersion: String = "1.1.0",
    @Json(name = "push_token") val pushToken: String = "",
)

@JsonClass(generateAdapter = true)
data class DeviceDto(
    @Json(name = "id") val id: String,
    @Json(name = "device_identifier") val deviceIdentifier: String,
    @Json(name = "device_name") val deviceName: String,
    @Json(name = "platform") val platform: String,
    @Json(name = "os_version") val osVersion: String,
    @Json(name = "app_version") val appVersion: String,
    @Json(name = "push_token_masked") val pushTokenMasked: String = "",
    @Json(name = "is_active") val isActive: Boolean = true,
    @Json(name = "last_seen_at") val lastSeenAt: String = "",
    @Json(name = "created_at") val createdAt: String = "",
)

@JsonClass(generateAdapter = true)
data class NotificationCreateRequestDto(
    @Json(name = "title") val title: String,
    @Json(name = "body") val body: String = "",
    @Json(name = "category") val category: String = "REMINDER",
    @Json(name = "channel") val channel: String = "IN_APP",
)

@JsonClass(generateAdapter = true)
data class NotificationRecordDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "body") val body: String = "",
    @Json(name = "category") val category: String = "REMINDER",
    @Json(name = "channel") val channel: String = "IN_APP",
    @Json(name = "status") val status: String = "PENDING",
    @Json(name = "retry_count") val retryCount: Int = 0,
    @Json(name = "created_at") val createdAt: String = "",
)
