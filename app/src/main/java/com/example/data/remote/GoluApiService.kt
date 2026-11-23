package com.example.data.remote

import com.example.data.remote.dto.*
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface GoluApiService {

    // Phase 1 — System & Health
    @GET("api/v1/health")
    suspend fun getHealth(): Response<HealthResponseDto>

    @GET("api/v1/health/database")
    suspend fun getDatabaseHealth(): Response<DatabaseHealthResponseDto>

    @GET("api/v1/status")
    suspend fun getStatus(): SystemStatusResponseDto

    @GET("api/v1/config")
    suspend fun getConfig(): SystemConfigResponseDto

    // Phase 3 — Authentication
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): AuthResponseDto

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): AuthResponseDto

    @POST("api/v1/auth/logout")
    suspend fun logout(): MessageResponseDto

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshRequestDto): AuthResponseDto

    @POST("api/v1/auth/refresh")
    fun refreshTokenSync(@Body request: RefreshRequestDto): Call<AuthResponseDto>

    @POST("api/v1/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequestDto): MessageResponseDto

    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequestDto): MessageResponseDto

    @POST("api/v1/auth/verify-email/request")
    suspend fun requestEmailVerification(): MessageResponseDto

    @POST("api/v1/auth/verify-email")
    suspend fun verifyEmail(@Body request: VerifyEmailConfirmRequestDto): MessageResponseDto

    @GET("api/v1/auth/me")
    suspend fun getCurrentUser(): UserDto

    // Phase 4 & 5 — Voice, Assistant & Wake-Word Settings
    @GET("api/v1/voice/settings")
    suspend fun getVoiceSettings(): VoiceSettingsDto

    @PUT("api/v1/voice/settings")
    suspend fun updateVoiceSettings(@Body request: VoiceSettingsUpdateDto): VoiceSettingsDto

    @GET("api/v1/assistant/settings")
    suspend fun getAssistantSettings(): AssistantSettingsDto

    @PUT("api/v1/assistant/settings")
    suspend fun updateAssistantSettings(@Body request: AssistantSettingsUpdateDto): AssistantSettingsDto

    @GET("api/v1/assistant/wake-word/settings")
    suspend fun getWakeWordSettings(): WakeWordSettingsDto

    @PUT("api/v1/assistant/wake-word/settings")
    suspend fun updateWakeWordSettings(@Body request: WakeWordSettingsUpdateDto): WakeWordSettingsDto

    // Phase 6 — Hybrid AI Brain
    @POST("api/v1/ai/chat")
    suspend fun chat(@Body request: AiChatRequestDto): AiChatResponseDto

    @POST("api/v1/ai/intent")
    suspend fun classifyIntent(@Body request: AiIntentRequestDto): AiIntentResponseDto

    @GET("api/v1/ai/status")
    suspend fun getAiStatus(): AiStatusResponseDto

    @POST("api/v1/ai/context/clear")
    suspend fun clearAiContext(@Body request: ClearContextRequestDto): MessageResponseDto

    // Devices & Notifications
    @GET("api/v1/devices")
    suspend fun listDevices(): List<DeviceDto>

    @POST("api/v1/devices")
    suspend fun registerDevice(@Body request: DeviceRegisterRequestDto): DeviceDto

    @DELETE("api/v1/devices/{id}")
    suspend fun deleteDevice(@Path("id") id: String): MessageResponseDto

    @GET("api/v1/notifications")
    suspend fun listNotifications(): List<NotificationRecordDto>

    @POST("api/v1/notifications")
    suspend fun createNotification(@Body request: NotificationCreateRequestDto): NotificationRecordDto

    @POST("api/v1/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): NotificationRecordDto

    // Phase 7 — Smart Personal Memory
    @POST("api/v1/memory")
    suspend fun createMemory(@Body request: MemoryCreateRequestDto): MemoryRecordDto

    @GET("api/v1/memory")
    suspend fun listMemories(@Query("category") category: String? = null): MemoryListResponseDto

    @PUT("api/v1/memory/{id}")
    suspend fun updateMemory(
        @Path("id") id: String,
        @Body request: MemoryUpdateRequestDto,
    ): MemoryRecordDto

    @DELETE("api/v1/memory/{id}")
    suspend fun deleteMemory(@Path("id") id: String): MessageResponseDto

    @DELETE("api/v1/memory")
    suspend fun clearAllMemories(): MessageResponseDto

    @GET("api/v1/memory/audit")
    suspend fun listMemoryAudit(): List<MemoryAuditEventDto>

    // Phase 8 — Phone Preferences & Contact Aliases
    @GET("api/v1/phone/preferences")
    suspend fun getPhonePreferences(): PhonePreferencesDto

    @PUT("api/v1/phone/preferences")
    suspend fun updatePhonePreferences(@Body request: PhonePreferencesUpdateDto): PhonePreferencesDto

    @GET("api/v1/phone/contact-aliases")
    suspend fun listContactAliases(): List<ContactAliasDto>

    @POST("api/v1/phone/contact-aliases")
    suspend fun createContactAlias(@Body request: ContactAliasCreateDto): ContactAliasDto

    @DELETE("api/v1/phone/contact-aliases/{id}")
    suspend fun deleteContactAlias(@Path("id") id: String): MessageResponseDto

    // Phase 9 — WhatsApp Messaging Preferences & Templates
    @GET("api/v1/messaging/preferences")
    suspend fun getMessagingPreferences(): MessagingPreferencesDto

    @PUT("api/v1/messaging/preferences")
    suspend fun updateMessagingPreferences(
        @Body request: MessagingPreferencesUpdateDto,
    ): MessagingPreferencesDto

    @GET("api/v1/messaging/templates")
    suspend fun listMessagingTemplates(): List<MessagingTemplateDto>

    @POST("api/v1/messaging/templates")
    suspend fun createMessagingTemplate(@Body request: MessagingTemplateCreateDto): MessagingTemplateDto

    @DELETE("api/v1/messaging/templates/{id}")
    suspend fun deleteMessagingTemplate(@Path("id") id: String): MessageResponseDto

    // Phase 10 — Daily Life Assistant
    @GET("api/v1/reminders")
    suspend fun listReminders(): List<ReminderDto>

    @POST("api/v1/reminders")
    suspend fun createReminder(@Body request: ReminderCreateDto): ReminderDto

    @PUT("api/v1/reminders/{id}")
    suspend fun updateReminder(
        @Path("id") id: String,
        @Body request: ReminderUpdateDto,
    ): ReminderDto

    @DELETE("api/v1/reminders/{id}")
    suspend fun deleteReminder(@Path("id") id: String): MessageResponseDto

    @GET("api/v1/tasks")
    suspend fun listTasks(): List<TaskDto>

    @POST("api/v1/tasks")
    suspend fun createTask(@Body request: TaskCreateDto): TaskDto

    @PUT("api/v1/tasks/{id}")
    suspend fun updateTask(@Path("id") id: String, @Body request: TaskUpdateDto): TaskDto

    @DELETE("api/v1/tasks/{id}")
    suspend fun deleteTask(@Path("id") id: String): MessageResponseDto

    @GET("api/v1/timetable")
    suspend fun listTimetable(): List<TimetableDto>

    @POST("api/v1/timetable")
    suspend fun createTimetableEntry(@Body request: TimetableCreateDto): TimetableDto

    @DELETE("api/v1/timetable/{id}")
    suspend fun deleteTimetableEntry(@Path("id") id: String): MessageResponseDto

    @GET("api/v1/calendar/events")
    suspend fun listCalendarEvents(): List<CalendarEventDto>

    @POST("api/v1/calendar/events")
    suspend fun createCalendarEvent(@Body request: CalendarEventCreateDto): CalendarEventDto

    @DELETE("api/v1/calendar/events/{id}")
    suspend fun deleteCalendarEvent(@Path("id") id: String): MessageResponseDto

    @GET("api/v1/daily-life/routines")
    suspend fun listDailyRoutines(): List<DailyRoutineDto>

    @POST("api/v1/daily-life/routines")
    suspend fun createDailyRoutine(@Body request: DailyRoutineCreateDto): DailyRoutineDto

    @GET("api/v1/daily-life/notifications")
    suspend fun getNotificationPreferences(): NotificationPreferencesDto

    @PUT("api/v1/daily-life/notifications")
    suspend fun updateNotificationPreferences(
        @Body request: NotificationPreferencesUpdateDto,
    ): NotificationPreferencesDto

    // Phase 11 — Family Mode
    @GET("api/v1/family/dashboard")
    suspend fun getFamilyDashboard(): FamilyDashboardDto

    @POST("api/v1/family/groups")
    suspend fun createFamilyGroup(@Body request: FamilyGroupCreateDto): FamilyGroupDto

    @POST("api/v1/family/invitations")
    suspend fun createFamilyInvitation(@Body request: FamilyInvitationCreateDto): FamilyInvitationDto

    @POST("api/v1/family/invitations/accept")
    suspend fun acceptFamilyInvitation(@Body request: FamilyInvitationAcceptDto): FamilyMemberDto

    @PUT("api/v1/family/members/{id}")
    suspend fun updateFamilyMember(
        @Path("id") id: String,
        @Body request: FamilyMemberUpdateDto,
    ): FamilyMemberDto

    @POST("api/v1/family/shared-reminders")
    suspend fun createSharedReminder(@Body request: SharedReminderCreateDto): SharedReminderDto

    @DELETE("api/v1/family/shared-reminders/{id}")
    suspend fun deleteSharedReminder(@Path("id") id: String): MessageResponseDto

    // Phase 12 — Security & Privacy Dashboard
    @GET("api/v1/security/dashboard")
    suspend fun getSecurityDashboard(): SecurityDashboardDto

    @PUT("api/v1/security/settings")
    suspend fun updateSecuritySettings(@Body request: UserSecuritySettingsUpdateDto): UserSecuritySettingsDto

    @POST("api/v1/security/sessions/{id}/revoke")
    suspend fun revokeSecuritySession(@Path("id") id: String): MessageResponseDto

    @DELETE("api/v1/security/personal-data")
    suspend fun deletePersonalData(): MessageResponseDto

    // Phase 13 — Smart Automation
    @GET("api/v1/automations")
    suspend fun listAutomations(): List<AutomationDto>

    @POST("api/v1/automations")
    suspend fun createAutomation(@Body request: AutomationCreateDto): AutomationDto

    @PUT("api/v1/automations/{id}")
    suspend fun updateAutomation(
        @Path("id") id: String,
        @Body request: AutomationUpdateDto,
    ): AutomationDto

    @POST("api/v1/automations/{id}/enable")
    suspend fun enableAutomation(@Path("id") id: String): AutomationDto

    @POST("api/v1/automations/{id}/disable")
    suspend fun disableAutomation(@Path("id") id: String): AutomationDto

    @DELETE("api/v1/automations/{id}")
    suspend fun deleteAutomation(@Path("id") id: String): MessageResponseDto

    @POST("api/v1/automations/{id}/logs")
    suspend fun logAutomationExecution(
        @Path("id") id: String,
        @Body request: AutomationExecutionLogCreateDto,
    ): AutomationExecutionLogDto

    @GET("api/v1/automations/history")
    suspend fun getAutomationHistory(): List<AutomationExecutionLogDto>
}
