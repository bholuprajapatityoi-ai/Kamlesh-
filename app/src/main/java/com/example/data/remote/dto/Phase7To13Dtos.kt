package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Phase 7 — Personal Memory
@JsonClass(generateAdapter = true)
data class MemoryCreateRequestDto(
    @Json(name = "category") val category: String,
    @Json(name = "content") val content: String,
    @Json(name = "family_shared") val familyShared: Boolean = false,
    @Json(name = "explicit_user_confirmed") val explicitUserConfirmed: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class MemoryUpdateRequestDto(
    @Json(name = "category") val category: String? = null,
    @Json(name = "content") val content: String? = null,
    @Json(name = "family_shared") val familyShared: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class MemoryRecordDto(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "category") val category: String,
    @Json(name = "content") val content: String,
    @Json(name = "expires_at") val expiresAt: String? = null,
    @Json(name = "family_shared") val familyShared: Boolean = false,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String,
)

@JsonClass(generateAdapter = true)
data class MemoryListResponseDto(
    @Json(name = "total") val total: Int,
    @Json(name = "categories") val categories: List<String> = emptyList(),
    @Json(name = "items") val items: List<MemoryRecordDto> = emptyList(),
)

// Phase 8 — Phone Preferences & Contact Aliases
@JsonClass(generateAdapter = true)
data class PhonePreferencesDto(
    @Json(name = "confirm_before_call") val confirmBeforeCall: Boolean,
    @Json(name = "allow_volume_control") val allowVolumeControl: Boolean,
    @Json(name = "allow_media_control") val allowMediaControl: Boolean,
    @Json(name = "allow_contact_lookup") val allowContactLookup: Boolean,
    @Json(name = "updated_at") val updatedAt: String = "",
)

@JsonClass(generateAdapter = true)
data class PhonePreferencesUpdateDto(
    @Json(name = "confirm_before_call") val confirmBeforeCall: Boolean? = null,
    @Json(name = "allow_volume_control") val allowVolumeControl: Boolean? = null,
    @Json(name = "allow_media_control") val allowMediaControl: Boolean? = null,
    @Json(name = "allow_contact_lookup") val allowContactLookup: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class ContactAliasCreateDto(
    @Json(name = "nickname") val nickname: String,
    @Json(name = "contact_name") val contactName: String,
    @Json(name = "phone_hint") val phoneHint: String = "",
)

@JsonClass(generateAdapter = true)
data class ContactAliasDto(
    @Json(name = "id") val id: String,
    @Json(name = "nickname") val nickname: String,
    @Json(name = "contact_name") val contactName: String,
    @Json(name = "phone_hint") val phoneHint: String = "",
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "updated_at") val updatedAt: String = "",
)

// Phase 9 — WhatsApp Messaging Preferences & Templates
@JsonClass(generateAdapter = true)
data class MessagingPreferencesDto(
    @Json(name = "default_language") val defaultLanguage: String,
    @Json(name = "always_confirm_before_open") val alwaysConfirmBeforeOpen: Boolean,
    @Json(name = "signature_text") val signatureText: String = "",
    @Json(name = "updated_at") val updatedAt: String = "",
)

@JsonClass(generateAdapter = true)
data class MessagingPreferencesUpdateDto(
    @Json(name = "default_language") val defaultLanguage: String? = null,
    @Json(name = "always_confirm_before_open") val alwaysConfirmBeforeOpen: Boolean? = null,
    @Json(name = "signature_text") val signatureText: String? = null,
)

@JsonClass(generateAdapter = true)
data class MessagingTemplateCreateDto(
    @Json(name = "title") val title: String,
    @Json(name = "language") val language: String = "Hinglish",
    @Json(name = "body") val body: String,
)

@JsonClass(generateAdapter = true)
data class MessagingTemplateDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "language") val language: String,
    @Json(name = "body") val body: String,
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "updated_at") val updatedAt: String = "",
)

// Phase 10 — Daily Life Assistant
@JsonClass(generateAdapter = true)
data class ReminderCreateDto(
    @Json(name = "title") val title: String,
    @Json(name = "notes") val notes: String = "",
    @Json(name = "remind_at") val remindAt: String,
    @Json(name = "timezone_name") val timezoneName: String = "Asia/Kolkata",
    @Json(name = "recurrence") val recurrence: String = "NONE",
    @Json(name = "notify_enabled") val notifyEnabled: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class ReminderUpdateDto(
    @Json(name = "title") val title: String? = null,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "is_completed") val isCompleted: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class ReminderDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "notes") val notes: String = "",
    @Json(name = "remind_at") val remindAt: String,
    @Json(name = "timezone_name") val timezoneName: String = "Asia/Kolkata",
    @Json(name = "recurrence") val recurrence: String = "NONE",
    @Json(name = "is_completed") val isCompleted: Boolean = false,
    @Json(name = "notify_enabled") val notifyEnabled: Boolean = true,
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "updated_at") val updatedAt: String = "",
)

@JsonClass(generateAdapter = true)
data class TaskCreateDto(
    @Json(name = "title") val title: String,
    @Json(name = "priority") val priority: String = "MEDIUM",
)

@JsonClass(generateAdapter = true)
data class TaskUpdateDto(
    @Json(name = "title") val title: String? = null,
    @Json(name = "is_completed") val isCompleted: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class TaskDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "priority") val priority: String = "MEDIUM",
    @Json(name = "due_date") val dueDate: String? = null,
    @Json(name = "is_completed") val isCompleted: Boolean = false,
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "updated_at") val updatedAt: String = "",
)

@JsonClass(generateAdapter = true)
data class TimetableCreateDto(
    @Json(name = "day_of_week") val dayOfWeek: String,
    @Json(name = "start_time") val startTime: String,
    @Json(name = "end_time") val endTime: String,
    @Json(name = "subject_or_activity") val subjectOrActivity: String,
    @Json(name = "location") val location: String = "",
)

@JsonClass(generateAdapter = true)
data class TimetableDto(
    @Json(name = "id") val id: String,
    @Json(name = "day_of_week") val dayOfWeek: String,
    @Json(name = "start_time") val startTime: String,
    @Json(name = "end_time") val endTime: String,
    @Json(name = "subject_or_activity") val subjectOrActivity: String,
    @Json(name = "location") val location: String = "",
)

@JsonClass(generateAdapter = true)
data class CalendarEventCreateDto(
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String = "",
    @Json(name = "start_at") val startAt: String,
    @Json(name = "end_at") val endAt: String,
    @Json(name = "timezone_name") val timezoneName: String = "Asia/Kolkata",
)

@JsonClass(generateAdapter = true)
data class CalendarEventDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String = "",
    @Json(name = "start_at") val startAt: String,
    @Json(name = "end_at") val endAt: String,
    @Json(name = "timezone_name") val timezoneName: String = "Asia/Kolkata",
)

// Phase 11 — Family Mode
@JsonClass(generateAdapter = true)
data class FamilyGroupCreateDto(
    @Json(name = "name") val name: String,
)

@JsonClass(generateAdapter = true)
data class FamilyGroupDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "owner_user_id") val ownerUserId: String,
    @Json(name = "member_count") val memberCount: Int,
    @Json(name = "created_at") val createdAt: String = "",
)

@JsonClass(generateAdapter = true)
data class FamilyInvitationCreateDto(
    @Json(name = "group_id") val groupId: String,
    @Json(name = "invitee_email") val inviteeEmail: String,
    @Json(name = "role") val role: String = "FATHER",
    @Json(name = "display_label") val displayLabel: String = "Papa",
    @Json(name = "father_friendly_mode") val fatherFriendlyMode: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class FamilyInvitationDto(
    @Json(name = "id") val id: String,
    @Json(name = "group_id") val groupId: String,
    @Json(name = "invitee_email") val inviteeEmail: String,
    @Json(name = "role") val role: String,
    @Json(name = "display_label") val displayLabel: String,
    @Json(name = "father_friendly_mode") val fatherFriendlyMode: Boolean,
    @Json(name = "status") val status: String,
    @Json(name = "invite_code") val inviteCode: String? = null,
    @Json(name = "expires_at") val expiresAt: String = "",
)

@JsonClass(generateAdapter = true)
data class FamilyInvitationAcceptDto(
    @Json(name = "invite_code") val inviteCode: String,
)

@JsonClass(generateAdapter = true)
data class FamilyMemberDto(
    @Json(name = "id") val id: String,
    @Json(name = "group_id") val groupId: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "email") val email: String,
    @Json(name = "role") val role: String,
    @Json(name = "display_label") val displayLabel: String,
    @Json(name = "father_friendly_mode") val fatherFriendlyMode: Boolean,
    @Json(name = "share_shared_reminders") val shareSharedReminders: Boolean,
    @Json(name = "share_family_routines") val shareFamilyRoutines: Boolean,
    @Json(name = "joined_at") val joinedAt: String = "",
)

@JsonClass(generateAdapter = true)
data class FamilyMemberUpdateDto(
    @Json(name = "father_friendly_mode") val fatherFriendlyMode: Boolean? = null,
    @Json(name = "share_shared_reminders") val shareSharedReminders: Boolean? = null,
    @Json(name = "share_family_routines") val shareFamilyRoutines: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class SharedReminderCreateDto(
    @Json(name = "group_id") val groupId: String,
    @Json(name = "title") val title: String,
    @Json(name = "notes") val notes: String = "",
    @Json(name = "remind_at") val remindAt: String,
)

@JsonClass(generateAdapter = true)
data class SharedReminderDto(
    @Json(name = "id") val id: String,
    @Json(name = "group_id") val groupId: String,
    @Json(name = "created_by_user_id") val createdByUserId: String,
    @Json(name = "title") val title: String,
    @Json(name = "notes") val notes: String = "",
    @Json(name = "remind_at") val remindAt: String,
    @Json(name = "is_completed") val isCompleted: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class FamilyDashboardDto(
    @Json(name = "groups") val groups: List<FamilyGroupDto> = emptyList(),
    @Json(name = "members") val members: List<FamilyMemberDto> = emptyList(),
    @Json(name = "pending_invitations") val pendingInvitations: List<FamilyInvitationDto> = emptyList(),
    @Json(name = "shared_reminders") val sharedReminders: List<SharedReminderDto> = emptyList(),
)

// Phase 12 — Security Dashboard
@JsonClass(generateAdapter = true)
data class ActiveSessionDto(
    @Json(name = "id") val id: String,
    @Json(name = "is_current") val isCurrent: Boolean,
    @Json(name = "revoked") val revoked: Boolean,
    @Json(name = "expires_at") val expiresAt: String,
    @Json(name = "created_at") val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class SecurityEventDto(
    @Json(name = "id") val id: String,
    @Json(name = "event_type") val eventType: String,
    @Json(name = "severity") val severity: String,
    @Json(name = "detail") val detail: String,
    @Json(name = "ip_address") val ipAddress: String = "",
    @Json(name = "created_at") val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class UserSecuritySettingsDto(
    @Json(name = "pin_lock_enabled") val pinLockEnabled: Boolean,
    @Json(name = "confirm_sensitive_actions") val confirmSensitiveActions: Boolean,
    @Json(name = "microphone_allowed") val microphoneAllowed: Boolean,
    @Json(name = "wake_word_monitoring_allowed") val wakeWordMonitoringAllowed: Boolean,
    @Json(name = "updated_at") val updatedAt: String = "",
)

@JsonClass(generateAdapter = true)
data class UserSecuritySettingsUpdateDto(
    @Json(name = "pin_lock_enabled") val pinLockEnabled: Boolean? = null,
    @Json(name = "confirm_sensitive_actions") val confirmSensitiveActions: Boolean? = null,
    @Json(name = "microphone_allowed") val microphoneAllowed: Boolean? = null,
    @Json(name = "wake_word_monitoring_allowed") val wakeWordMonitoringAllowed: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class SecurityDashboardDto(
    @Json(name = "settings") val settings: UserSecuritySettingsDto,
    @Json(name = "active_sessions") val activeSessions: List<ActiveSessionDto> = emptyList(),
    @Json(name = "recent_events") val recentEvents: List<SecurityEventDto> = emptyList(),
)

// Phase 13 — Smart Automation
@JsonClass(generateAdapter = true)
data class AutomationConditionDto(
    @Json(name = "condition_type") val conditionType: String,
    @Json(name = "condition_value") val conditionValue: String,
)

@JsonClass(generateAdapter = true)
data class AutomationActionDto(
    @Json(name = "action_type") val actionType: String,
    @Json(name = "action_payload") val actionPayload: String,
)

@JsonClass(generateAdapter = true)
data class AutomationCreateDto(
    @Json(name = "name") val name: String,
    @Json(name = "trigger_type") val triggerType: String,
    @Json(name = "trigger_value") val triggerValue: String,
    @Json(name = "is_enabled") val isEnabled: Boolean = true,
    @Json(name = "requires_confirmation") val requiresConfirmation: Boolean = false,
    @Json(name = "confirmed_sensitive") val confirmedSensitive: Boolean = false,
    @Json(name = "conditions") val conditions: List<AutomationConditionDto> = emptyList(),
    @Json(name = "actions") val actions: List<AutomationActionDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class AutomationDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "trigger_type") val triggerType: String,
    @Json(name = "trigger_value") val triggerValue: String,
    @Json(name = "is_enabled") val isEnabled: Boolean,
    @Json(name = "requires_confirmation") val requiresConfirmation: Boolean,
    @Json(name = "conditions") val conditions: List<AutomationConditionDto> = emptyList(),
    @Json(name = "actions") val actions: List<AutomationActionDto> = emptyList(),
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "updated_at") val updatedAt: String = "",
)

@JsonClass(generateAdapter = true)
data class AutomationExecutionLogCreateDto(
    @Json(name = "status") val status: String = "EXECUTED",
    @Json(name = "summary") val summary: String,
)

@JsonClass(generateAdapter = true)
data class AutomationExecutionLogDto(
    @Json(name = "id") val id: String,
    @Json(name = "rule_id") val ruleId: String,
    @Json(name = "status") val status: String,
    @Json(name = "summary") val summary: String,
    @Json(name = "executed_at") val executedAt: String,
)

@JsonClass(generateAdapter = true)
data class MemoryAuditEventDto(
    @Json(name = "id") val id: String,
    @Json(name = "memory_id") val memoryId: String? = null,
    @Json(name = "action") val action: String,
    @Json(name = "category") val category: String,
    @Json(name = "detail") val detail: String,
    @Json(name = "created_at") val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class DailyRoutineCreateDto(
    @Json(name = "name") val name: String,
    @Json(name = "trigger_time") val triggerTime: String,
    @Json(name = "summary_prompt") val summaryPrompt: String = "",
    @Json(name = "is_enabled") val isEnabled: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class DailyRoutineDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "trigger_time") val triggerTime: String,
    @Json(name = "summary_prompt") val summaryPrompt: String = "",
    @Json(name = "is_enabled") val isEnabled: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class NotificationPreferencesDto(
    @Json(name = "morning_greeting_enabled") val morningGreetingEnabled: Boolean,
    @Json(name = "evening_summary_enabled") val eveningSummaryEnabled: Boolean,
    @Json(name = "reminder_notifications_enabled") val reminderNotificationsEnabled: Boolean,
    @Json(name = "battery_alert_threshold") val batteryAlertThreshold: Int,
    @Json(name = "updated_at") val updatedAt: String = "",
)

@JsonClass(generateAdapter = true)
data class NotificationPreferencesUpdateDto(
    @Json(name = "morning_greeting_enabled") val morningGreetingEnabled: Boolean? = null,
    @Json(name = "evening_summary_enabled") val eveningSummaryEnabled: Boolean? = null,
    @Json(name = "reminder_notifications_enabled") val reminderNotificationsEnabled: Boolean? = null,
    @Json(name = "battery_alert_threshold") val batteryAlertThreshold: Int? = null,
)

@JsonClass(generateAdapter = true)
data class AutomationUpdateDto(
    @Json(name = "name") val name: String? = null,
    @Json(name = "trigger_type") val triggerType: String? = null,
    @Json(name = "trigger_value") val triggerValue: String? = null,
    @Json(name = "is_enabled") val isEnabled: Boolean? = null,
    @Json(name = "requires_confirmation") val requiresConfirmation: Boolean? = null,
    @Json(name = "conditions") val conditions: List<AutomationConditionDto>? = null,
    @Json(name = "actions") val actions: List<AutomationActionDto>? = null,
)
