package com.example.domain.model

data class ContactMatch(
    val displayName: String,
    val phoneNumber: String,
)

sealed interface ContactLookupResult {
    data class Single(val match: ContactMatch) : ContactLookupResult
    data class Multiple(val matches: List<ContactMatch>) : ContactLookupResult
    data object NotFound : ContactLookupResult
    data object PermissionRequired : ContactLookupResult
}

data class WhatsAppDraft(
    val recipientName: String,
    val phoneNumber: String = "",
    val messageBody: String,
    val language: String = "Hinglish",
)

data class DeviceInfoSnapshot(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkInt: Int,
    val timezoneId: String,
    val exactAlarmPermitted: Boolean,
)

data class Phase7To13UiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
    val fatherFriendlyMode: Boolean = false,
    val pinLockEnabled: Boolean = false,
    val isAppLocked: Boolean = false,
)
