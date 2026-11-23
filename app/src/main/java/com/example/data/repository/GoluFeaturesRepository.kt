package com.example.data.repository

import com.example.core.ApiErrorMapper
import com.example.core.BackendConfig
import com.example.core.BackendConfigStatus
import com.example.core.BackendEndpointInfo
import com.example.core.NetworkResult
import com.example.data.remote.GoluApiService
import com.example.data.remote.dto.*

class GoluFeaturesRepository(
    private val api: GoluApiService,
    private val endpointProvider: () -> BackendEndpointInfo = { BackendConfig.evaluateConfigured() },
) {
    private fun ensureConfigured(): NetworkResult.Error? {
        val eval = endpointProvider()
        return if (eval.status != BackendConfigStatus.READY_HTTPS &&
            eval.status != BackendConfigStatus.READY_EMULATOR_DEV
        ) {
            NetworkResult.Error(eval.diagnosticMessage, causeCategory = "UNCONFIGURED")
        } else {
            null
        }
    }

    private suspend fun <T> call(block: suspend () -> T): NetworkResult<T> {
        ensureConfigured()?.let { return it }
        return try {
            NetworkResult.Success(block())
        } catch (t: Throwable) {
            ApiErrorMapper.map(t)
        }
    }

    // Phase 7 — Memory
    suspend fun listMemories(category: String? = null) = call { api.listMemories(category) }
    suspend fun createMemory(category: String, content: String, familyShared: Boolean = false) =
        call {
            api.createMemory(
                MemoryCreateRequestDto(
                    category = category,
                    content = content,
                    familyShared = familyShared,
                    explicitUserConfirmed = true,
                )
            )
        }

    suspend fun updateMemory(id: String, category: String?, content: String?, familyShared: Boolean?) =
        call { api.updateMemory(id, MemoryUpdateRequestDto(category, content, familyShared)) }

    suspend fun deleteMemory(id: String) = call { api.deleteMemory(id) }
    suspend fun clearAllMemories() = call { api.clearAllMemories() }
    suspend fun listMemoryAudit() = call { api.listMemoryAudit() }

    // Phase 8 — Phone Preferences & Contact Aliases
    suspend fun getPhonePreferences() = call { api.getPhonePreferences() }
    suspend fun updatePhonePreferences(req: PhonePreferencesUpdateDto) =
        call { api.updatePhonePreferences(req) }

    suspend fun listContactAliases() = call { api.listContactAliases() }
    suspend fun createContactAlias(nickname: String, contactName: String, phoneHint: String) =
        call { api.createContactAlias(ContactAliasCreateDto(nickname, contactName, phoneHint)) }

    suspend fun deleteContactAlias(id: String) = call { api.deleteContactAlias(id) }

    // Phase 9 — WhatsApp Messaging Preferences & Templates
    suspend fun getMessagingPreferences() = call { api.getMessagingPreferences() }
    suspend fun updateMessagingPreferences(req: MessagingPreferencesUpdateDto) =
        call { api.updateMessagingPreferences(req) }

    suspend fun listMessagingTemplates() = call { api.listMessagingTemplates() }
    suspend fun createMessagingTemplate(title: String, language: String, body: String) =
        call { api.createMessagingTemplate(MessagingTemplateCreateDto(title, language, body)) }

    suspend fun deleteMessagingTemplate(id: String) = call { api.deleteMessagingTemplate(id) }

    // Phase 10 — Daily Life Assistant
    suspend fun listReminders() = call { api.listReminders() }
    suspend fun createReminder(req: ReminderCreateDto) = call { api.createReminder(req) }
    suspend fun updateReminder(id: String, isCompleted: Boolean) =
        call { api.updateReminder(id, ReminderUpdateDto(isCompleted = isCompleted)) }

    suspend fun deleteReminder(id: String) = call { api.deleteReminder(id) }

    suspend fun listTasks() = call { api.listTasks() }
    suspend fun createTask(title: String, priority: String) =
        call { api.createTask(TaskCreateDto(title, priority)) }

    suspend fun updateTask(id: String, isCompleted: Boolean) =
        call { api.updateTask(id, TaskUpdateDto(isCompleted = isCompleted)) }

    suspend fun deleteTask(id: String) = call { api.deleteTask(id) }

    suspend fun listTimetable() = call { api.listTimetable() }
    suspend fun createTimetableEntry(req: TimetableCreateDto) =
        call { api.createTimetableEntry(req) }

    suspend fun deleteTimetableEntry(id: String) = call { api.deleteTimetableEntry(id) }

    suspend fun listCalendarEvents() = call { api.listCalendarEvents() }
    suspend fun createCalendarEvent(req: CalendarEventCreateDto) =
        call { api.createCalendarEvent(req) }

    suspend fun deleteCalendarEvent(id: String) = call { api.deleteCalendarEvent(id) }
    suspend fun listDailyRoutines() = call { api.listDailyRoutines() }
    suspend fun createDailyRoutine(req: DailyRoutineCreateDto) = call { api.createDailyRoutine(req) }
    suspend fun getNotificationPreferences() = call { api.getNotificationPreferences() }
    suspend fun updateNotificationPreferences(req: NotificationPreferencesUpdateDto) =
        call { api.updateNotificationPreferences(req) }

    // Phase 11 — Family Mode
    suspend fun getFamilyDashboard() = call { api.getFamilyDashboard() }
    suspend fun createFamilyGroup(name: String) = call { api.createFamilyGroup(FamilyGroupCreateDto(name)) }
    suspend fun inviteFamilyMember(req: FamilyInvitationCreateDto) =
        call { api.createFamilyInvitation(req) }

    suspend fun acceptFamilyInvitation(code: String) =
        call { api.acceptFamilyInvitation(FamilyInvitationAcceptDto(code)) }

    suspend fun updateFamilyMember(id: String, req: FamilyMemberUpdateDto) =
        call { api.updateFamilyMember(id, req) }

    suspend fun createSharedReminder(req: SharedReminderCreateDto) =
        call { api.createSharedReminder(req) }

    suspend fun deleteSharedReminder(id: String) = call { api.deleteSharedReminder(id) }

    // Phase 12 — Security Dashboard
    suspend fun getSecurityDashboard() = call { api.getSecurityDashboard() }
    suspend fun updateSecuritySettings(req: UserSecuritySettingsUpdateDto) =
        call { api.updateSecuritySettings(req) }

    suspend fun revokeSession(id: String) = call { api.revokeSecuritySession(id) }
    suspend fun deletePersonalData() = call { api.deletePersonalData() }

    // Phase 13 — Smart Automation
    suspend fun listAutomations() = call { api.listAutomations() }
    suspend fun createAutomation(req: AutomationCreateDto) = call { api.createAutomation(req) }
    suspend fun updateAutomation(id: String, req: AutomationUpdateDto) =
        call { api.updateAutomation(id, req) }
    suspend fun enableAutomation(id: String) = call { api.enableAutomation(id) }
    suspend fun disableAutomation(id: String) = call { api.disableAutomation(id) }
    suspend fun deleteAutomation(id: String) = call { api.deleteAutomation(id) }
    suspend fun logAutomationExecution(id: String, status: String, summary: String) =
        call { api.logAutomationExecution(id, AutomationExecutionLogCreateDto(status, summary)) }

    suspend fun getAutomationHistory() = call { api.getAutomationHistory() }
}
