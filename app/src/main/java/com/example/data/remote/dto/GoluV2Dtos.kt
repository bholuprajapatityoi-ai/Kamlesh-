package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ScreenUnderstandRequestDto(
    @Json(name = "screen_text") val screenText: String,
    val action: String = "EXPLAIN",
    val question: String = "",
    @Json(name = "app_context") val appContext: String = "",
    val language: String = "Hinglish",
    @Json(name = "consent_granted") val consentGranted: Boolean,
)

@JsonClass(generateAdapter = true)
data class ScreenUnderstandResponseDto(
    val action: String,
    val answer: String,
    @Json(name = "extracted_highlights") val extractedHighlights: List<String> = emptyList(),
    val provider: String,
    @Json(name = "ai_backed") val aiBacked: Boolean,
)

@JsonClass(generateAdapter = true)
data class CameraAnalyzeRequestDto(
    @Json(name = "image_base64") val imageBase64: String,
    val question: String,
    val mode: String = "SCENE",
    val language: String = "Hinglish",
    @Json(name = "consent_granted") val consentGranted: Boolean,
)

@JsonClass(generateAdapter = true)
data class CameraAnalyzeResponseDto(
    val mode: String,
    val description: String,
    @Json(name = "detected_text") val detectedText: String = "",
    val objects: List<String> = emptyList(),
    val provider: String,
    @Json(name = "ai_backed") val aiBacked: Boolean,
)

@JsonClass(generateAdapter = true)
data class DocumentUploadRequestDto(
    val title: String,
    @Json(name = "mime_type") val mimeType: String,
    @Json(name = "text_content") val textContent: String = "",
    @Json(name = "file_base64") val fileBase64: String = "",
    @Json(name = "consent_granted") val consentGranted: Boolean,
)

@JsonClass(generateAdapter = true)
data class DocumentResponseDto(
    val id: String,
    val title: String,
    @Json(name = "mime_type") val mimeType: String,
    @Json(name = "file_size_bytes") val fileSizeBytes: Int,
    val summary: String,
    @Json(name = "key_points") val keyPoints: List<String> = emptyList(),
    @Json(name = "preview_text") val previewText: String = "",
    @Json(name = "created_at") val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class DocumentQueryRequestDto(
    val question: String,
    val language: String = "Hinglish",
)

@JsonClass(generateAdapter = true)
data class DocumentQueryResponseDto(
    @Json(name = "document_id") val documentId: String,
    val answer: String,
    @Json(name = "matched_excerpts") val matchedExcerpts: List<String> = emptyList(),
    @Json(name = "ai_backed") val aiBacked: Boolean,
)

@JsonClass(generateAdapter = true)
data class NotificationIngestRequestDto(
    @Json(name = "source_app") val sourceApp: String,
    val title: String,
    @Json(name = "body_preview") val bodyPreview: String,
    @Json(name = "consent_granted") val consentGranted: Boolean,
    @Json(name = "create_reminder_if_important") val createReminderIfImportant: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class NotificationInsightResponseDto(
    val id: String,
    @Json(name = "source_app") val sourceApp: String,
    val title: String,
    @Json(name = "body_preview") val bodyPreview: String,
    val category: String,
    @Json(name = "priority_score") val priorityScore: Int,
    @Json(name = "is_important") val isImportant: Boolean,
    @Json(name = "ai_summary") val aiSummary: String,
    @Json(name = "converted_reminder_id") val convertedReminderId: String? = null,
    @Json(name = "created_at") val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class NotificationSummaryResponseDto(
    @Json(name = "total_count") val totalCount: Int,
    @Json(name = "important_count") val importantCount: Int,
    val summary: String,
    val items: List<NotificationInsightResponseDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class KnowledgeNoteRequestDto(
    val title: String,
    val category: String = "Custom Notes",
    val content: String,
    val tags: List<String> = emptyList(),
    @Json(name = "family_shared") val familyShared: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class KnowledgeNoteResponseDto(
    val id: String,
    val title: String,
    val category: String,
    val content: String,
    val tags: List<String> = emptyList(),
    @Json(name = "family_shared") val familyShared: Boolean,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String,
)

@JsonClass(generateAdapter = true)
data class TranslateRequestDto(
    val text: String,
    @Json(name = "source_language") val sourceLanguage: String = "auto",
    @Json(name = "target_language") val targetLanguage: String = "Hindi",
    val mode: String = "TEXT",
)

@JsonClass(generateAdapter = true)
data class TranslationResponseDto(
    val id: String,
    @Json(name = "source_language") val sourceLanguage: String,
    @Json(name = "target_language") val targetLanguage: String,
    @Json(name = "source_text") val sourceText: String,
    @Json(name = "translated_text") val translatedText: String,
    val mode: String,
    @Json(name = "ai_backed") val aiBacked: Boolean,
    @Json(name = "created_at") val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class ResearchRequestDto(
    val query: String,
    val language: String = "English",
)

@JsonClass(generateAdapter = true)
data class CitationItemDto(
    val title: String,
    val url: String,
    val snippet: String,
)

@JsonClass(generateAdapter = true)
data class ResearchReportResponseDto(
    val id: String,
    val query: String,
    val summary: String,
    @Json(name = "verified_findings") val verifiedFindings: String,
    @Json(name = "uncertain_points") val uncertainPoints: String,
    val citations: List<CitationItemDto> = emptyList(),
    @Json(name = "provider_used") val providerUsed: String,
    @Json(name = "created_at") val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class OfflineManifestResponseDto(
    @Json(name = "offline_capable_features") val offlineCapableFeatures: List<String> = emptyList(),
    @Json(name = "online_required_features") val onlineRequiredFeatures: List<String> = emptyList(),
    @Json(name = "local_command_examples") val localCommandExamples: List<String> = emptyList(),
    @Json(name = "cloud_ai_note") val cloudAiNote: String = "",
)

@JsonClass(generateAdapter = true)
data class PlannerSuggestRequestDto(
    @Json(name = "goal_focus") val goalFocus: String,
    @Json(name = "available_hours") val availableHours: Int = 6,
    val language: String = "Hinglish",
)

@JsonClass(generateAdapter = true)
data class PlannedItemProposalDto(
    val title: String,
    @Json(name = "item_type") val itemType: String = "TASK",
    val priority: String = "MEDIUM",
    @Json(name = "suggested_slot") val suggestedSlot: String = "",
    val reason: String = "",
)

@JsonClass(generateAdapter = true)
data class PlannerSuggestResponseDto(
    val summary: String,
    val proposals: List<PlannedItemProposalDto> = emptyList(),
    @Json(name = "requires_user_approval") val requiresUserApproval: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class PlannerApproveRequestDto(
    val items: List<PlannedItemProposalDto>,
)

@JsonClass(generateAdapter = true)
data class CodingAssistRequestDto(
    val prompt: String,
    val language: String = "kotlin",
    @Json(name = "task_type") val taskType: String = "EXPLAIN",
)

@JsonClass(generateAdapter = true)
data class CodingAssistResponseDto(
    val id: String,
    val language: String,
    @Json(name = "task_type") val taskType: String,
    val prompt: String,
    @Json(name = "code_output") val codeOutput: String,
    val explanation: String,
    @Json(name = "security_note") val securityNote: String = "",
    @Json(name = "ai_backed") val aiBacked: Boolean,
    @Json(name = "created_at") val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class CreativeRequestDto(
    @Json(name = "asset_type") val assetType: String = "IMAGE",
    val prompt: String,
    val style: String = "Futuristic Navy & Cyan",
)

@JsonClass(generateAdapter = true)
data class CreativeAssetResponseDto(
    val id: String,
    @Json(name = "asset_type") val assetType: String,
    val prompt: String,
    @Json(name = "revised_prompt") val revisedPrompt: String = "",
    @Json(name = "image_url") val imageUrl: String = "",
    @Json(name = "design_notes") val designNotes: String = "",
    val status: String,
    @Json(name = "error_detail") val errorDetail: String = "",
    @Json(name = "created_at") val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class RecordingCreateRequestDto(
    val title: String,
    @Json(name = "duration_seconds") val durationSeconds: Int,
    val transcript: String,
    @Json(name = "local_file_hint") val localFileHint: String = "",
    @Json(name = "generate_summary") val generateSummary: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class RecordingUpdateRequestDto(
    val title: String? = null,
    val transcript: String? = null,
    @Json(name = "regenerate_summary") val regenerateSummary: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class RecordingResponseDto(
    val id: String,
    val title: String,
    @Json(name = "duration_seconds") val durationSeconds: Int,
    val transcript: String,
    val summary: String,
    @Json(name = "lecture_notes") val lectureNotes: String,
    @Json(name = "local_file_hint") val localFileHint: String,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String,
)

@JsonClass(generateAdapter = true)
data class SmartHomeConfigRequestDto(
    @Json(name = "provider_type") val providerType: String = "HOME_ASSISTANT",
    @Json(name = "hub_url") val hubUrl: String,
    @Json(name = "access_token") val accessToken: String,
    val authorize: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class SmartHomeConfigResponseDto(
    @Json(name = "provider_type") val providerType: String,
    @Json(name = "hub_url") val hubUrl: String,
    @Json(name = "token_hint") val tokenHint: String,
    @Json(name = "is_authorized") val isAuthorized: Boolean,
    @Json(name = "connection_state") val connectionState: String,
    val detail: String,
)

@JsonClass(generateAdapter = true)
data class SmartHomeDeviceCreateRequestDto(
    @Json(name = "external_entity_id") val externalEntityId: String,
    val name: String,
    val room: String = "Living Room",
    @Json(name = "device_type") val deviceType: String = "LIGHT",
)

@JsonClass(generateAdapter = true)
data class SmartHomeDeviceCommandRequestDto(
    @Json(name = "turn_on") val turnOn: Boolean,
    @Json(name = "brightness_pct") val brightnessPct: Int = 100,
    @Json(name = "confirmed_sensitive") val confirmedSensitive: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class SmartHomeDeviceResponseDto(
    val id: String,
    @Json(name = "external_entity_id") val externalEntityId: String,
    val name: String,
    val room: String,
    @Json(name = "device_type") val deviceType: String,
    @Json(name = "is_on") val isOn: Boolean,
    @Json(name = "brightness_pct") val brightnessPct: Int,
    val status: String,
    @Json(name = "last_synced_at") val lastSyncedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class ExpenseCreateRequestDto(
    val title: String,
    val category: String = "General",
    val amount: Double,
    val currency: String = "INR",
    val notes: String = "",
)

@JsonClass(generateAdapter = true)
data class BudgetUpdateRequestDto(
    @Json(name = "month_key") val monthKey: String,
    @Json(name = "limit_amount") val limitAmount: Double,
    val currency: String = "INR",
)

@JsonClass(generateAdapter = true)
data class ExpenseRecordResponseDto(
    val id: String,
    val title: String,
    val category: String,
    val amount: Double,
    val currency: String,
    val notes: String,
    @Json(name = "spent_at") val spentAt: String,
)

@JsonClass(generateAdapter = true)
data class ExpenseSummaryResponseDto(
    @Json(name = "month_key") val monthKey: String,
    @Json(name = "budget_limit") val budgetLimit: Double,
    @Json(name = "total_spent") val totalSpent: Double,
    @Json(name = "remaining_budget") val remainingBudget: Double,
    val currency: String,
    @Json(name = "by_category") val byCategory: Map<String, Double> = emptyMap(),
    val items: List<ExpenseRecordResponseDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class PersonalityUpdateRequestDto(
    @Json(name = "response_style") val responseStyle: String? = null,
    @Json(name = "humor_level") val humorLevel: Int? = null,
    @Json(name = "response_length") val responseLength: String? = null,
    @Json(name = "greeting_message") val greetingMessage: String? = null,
    @Json(name = "voice_gender") val voiceGender: String? = null,
    @Json(name = "avatar_style") val avatarStyle: String? = null,
    @Json(name = "custom_system_notes") val customSystemNotes: String? = null,
)

@JsonClass(generateAdapter = true)
data class PersonalityResponseDto(
    @Json(name = "response_style") val responseStyle: String,
    @Json(name = "humor_level") val humorLevel: Int,
    @Json(name = "response_length") val responseLength: String,
    @Json(name = "greeting_message") val greetingMessage: String,
    @Json(name = "voice_gender") val voiceGender: String,
    @Json(name = "avatar_style") val avatarStyle: String,
    @Json(name = "custom_system_notes") val customSystemNotes: String,
)

@JsonClass(generateAdapter = true)
data class AppShortcutUpsertRequestDto(
    @Json(name = "alias_name") val aliasName: String,
    @Json(name = "package_name") val packageName: String,
    @Json(name = "is_favourite") val isFavourite: Boolean = true,
    @Json(name = "shortcut_command") val shortcutCommand: String = "",
    @Json(name = "increment_launch") val incrementLaunch: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class SmartAppPreferenceResponseDto(
    val id: String,
    @Json(name = "alias_name") val aliasName: String,
    @Json(name = "package_name") val packageName: String,
    @Json(name = "is_favourite") val isFavourite: Boolean,
    @Json(name = "launch_count") val launchCount: Int,
    @Json(name = "shortcut_command") val shortcutCommand: String,
    @Json(name = "last_launched_at") val lastLaunchedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class StudyAskRequestDto(
    val subject: String,
    val topic: String,
    val language: String = "English",
    val difficulty: String = "Intermediate",
)

@JsonClass(generateAdapter = true)
data class StudyProgressUpdateRequestDto(
    @Json(name = "mastery_pct") val masteryPct: Int,
)

@JsonClass(generateAdapter = true)
data class StudySessionResponseDto(
    val id: String,
    val subject: String,
    val topic: String,
    val language: String,
    val difficulty: String,
    val explanation: String,
    @Json(name = "practice_questions") val practiceQuestions: List<String> = emptyList(),
    @Json(name = "mastery_pct") val masteryPct: Int,
    @Json(name = "created_at") val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class TravelPlanRequestDto(
    val origin: String = "",
    val destination: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val interests: String = "food, landmarks, transit",
)

@JsonClass(generateAdapter = true)
data class TravelPlaceItemDto(
    val name: String,
    val category: String,
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@JsonClass(generateAdapter = true)
data class TravelPlanResponseDto(
    val id: String,
    val origin: String,
    val destination: String,
    @Json(name = "route_summary") val routeSummary: String,
    @Json(name = "nearby_places") val nearbyPlaces: List<TravelPlaceItemDto> = emptyList(),
    @Json(name = "itinerary_notes") val itineraryNotes: String,
    @Json(name = "live_data_notice") val liveDataNotice: String,
    @Json(name = "created_at") val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class SyncPushRequestDto(
    @Json(name = "device_identifier") val deviceIdentifier: String,
    val scope: String = "ALL",
    @Json(name = "conflict_policy") val conflictPolicy: String = "SERVER_WINS",
)

@JsonClass(generateAdapter = true)
data class SyncCheckpointResponseDto(
    val id: String,
    @Json(name = "device_identifier") val deviceIdentifier: String,
    val scope: String,
    @Json(name = "conflict_policy") val conflictPolicy: String,
    @Json(name = "synced_items_count") val syncedItemsCount: Int,
    @Json(name = "synced_at") val syncedAt: String,
)

@JsonClass(generateAdapter = true)
data class MultiDeviceOverviewResponseDto(
    val devices: List<Map<String, Any?>> = emptyList(),
    val checkpoints: List<SyncCheckpointResponseDto> = emptyList(),
    @Json(name = "notes_count") val notesCount: Int = 0,
    @Json(name = "memories_count") val memoriesCount: Int = 0,
    @Json(name = "expenses_count") val expensesCount: Int = 0,
)

@JsonClass(generateAdapter = true)
data class DiagnosticCheckInputDto(
    @Json(name = "mic_permission") val micPermission: Boolean,
    @Json(name = "camera_permission") val cameraPermission: Boolean,
    @Json(name = "location_permission") val locationPermission: Boolean,
    @Json(name = "notification_listener_enabled") val notificationListenerEnabled: Boolean,
    @Json(name = "device_online") val deviceOnline: Boolean,
)

@JsonClass(generateAdapter = true)
data class DiagnosticReportResponseDto(
    @Json(name = "backend_ok") val backendOk: Boolean,
    @Json(name = "database_ok") val databaseOk: Boolean,
    @Json(name = "redis_ok") val redisOk: Boolean,
    @Json(name = "openai_configured") val openaiConfigured: Boolean,
    @Json(name = "search_api_configured") val searchApiConfigured: Boolean,
    @Json(name = "overall_status") val overallStatus: String,
    @Json(name = "troubleshooting_suggestions") val troubleshootingSuggestions: List<String> = emptyList(),
    @Json(name = "recent_events") val recentEvents: List<Map<String, Any?>> = emptyList(),
)
