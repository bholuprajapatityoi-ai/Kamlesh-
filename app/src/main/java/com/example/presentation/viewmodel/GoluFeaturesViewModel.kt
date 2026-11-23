package com.example.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.NetworkResult
import com.example.data.local.SecurityPinVault
import com.example.data.remote.dto.*
import com.example.data.repository.GoluFeaturesRepository
import com.example.di.AppContainer
import com.example.domain.ai.AndroidActionExecutor
import com.example.domain.automation.AutomationEvaluator
import com.example.domain.daily.ReminderScheduler
import com.example.domain.model.ContactLookupResult
import com.example.domain.model.ContactMatch
import com.example.domain.model.DeviceInfoSnapshot
import com.example.domain.model.Phase7To13UiState
import com.example.domain.model.WhatsAppDraft
import com.example.domain.phone.ContactLookupHelper
import com.example.domain.voice.VoiceEngine
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GoluFeaturesViewModel(
    private val appContext: Context,
    private val repo: GoluFeaturesRepository,
    private val pinVault: SecurityPinVault,
    private val actionExecutor: AndroidActionExecutor,
    private val contactLookupHelper: ContactLookupHelper,
    private val automationEvaluator: AutomationEvaluator,
    private val voiceEngine: VoiceEngine,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            Phase7To13UiState(
                fatherFriendlyMode = pinVault.isFatherModeEnabled(),
                pinLockEnabled = pinVault.isPinLockEnabled(),
                isAppLocked = pinVault.isPinLockEnabled(),
            )
        )
    val uiState: StateFlow<Phase7To13UiState> = _uiState.asStateFlow()

    // Phase 7 — Memory
    private val _memories = MutableStateFlow<List<MemoryRecordDto>>(emptyList())
    val memories: StateFlow<List<MemoryRecordDto>> = _memories.asStateFlow()

    private val _memoryCategories =
        MutableStateFlow(
            listOf(
                "Preferences",
                "Favourite Apps",
                "Contact Nicknames",
                "Timetable",
                "Daily Routines",
                "Reminders",
                "Custom Notes",
            )
        )
    val memoryCategories: StateFlow<List<String>> = _memoryCategories.asStateFlow()

    private val _memoryAudit = MutableStateFlow<List<MemoryAuditEventDto>>(emptyList())
    val memoryAudit: StateFlow<List<MemoryAuditEventDto>> = _memoryAudit.asStateFlow()

    // Phase 8 — Phone & Contacts
    private val _phonePrefs = MutableStateFlow<PhonePreferencesDto?>(null)
    val phonePrefs: StateFlow<PhonePreferencesDto?> = _phonePrefs.asStateFlow()

    private val _contactAliases = MutableStateFlow<List<ContactAliasDto>>(emptyList())
    val contactAliases: StateFlow<List<ContactAliasDto>> = _contactAliases.asStateFlow()

    private val _contactLookupResult = MutableStateFlow<ContactLookupResult?>(null)
    val contactLookupResult: StateFlow<ContactLookupResult?> = _contactLookupResult.asStateFlow()

    private val _pendingCallMatch = MutableStateFlow<ContactMatch?>(null)
    val pendingCallMatch: StateFlow<ContactMatch?> = _pendingCallMatch.asStateFlow()

    private val _deviceInfo = MutableStateFlow(actionExecutor.getDeviceInfoSnapshot())
    val deviceInfo: StateFlow<DeviceInfoSnapshot> = _deviceInfo.asStateFlow()

    // Phase 9 — WhatsApp Assistant
    private val _messagingPrefs = MutableStateFlow<MessagingPreferencesDto?>(null)
    val messagingPrefs: StateFlow<MessagingPreferencesDto?> = _messagingPrefs.asStateFlow()

    private val _templates = MutableStateFlow<List<MessagingTemplateDto>>(emptyList())
    val templates: StateFlow<List<MessagingTemplateDto>> = _templates.asStateFlow()

    private val _whatsAppDraft = MutableStateFlow<WhatsAppDraft?>(null)
    val whatsAppDraft: StateFlow<WhatsAppDraft?> = _whatsAppDraft.asStateFlow()

    // Phase 10 — Daily Life Assistant
    private val _reminders = MutableStateFlow<List<ReminderDto>>(emptyList())
    val reminders: StateFlow<List<ReminderDto>> = _reminders.asStateFlow()

    private val _tasks = MutableStateFlow<List<TaskDto>>(emptyList())
    val tasks: StateFlow<List<TaskDto>> = _tasks.asStateFlow()

    private val _timetable = MutableStateFlow<List<TimetableDto>>(emptyList())
    val timetable: StateFlow<List<TimetableDto>> = _timetable.asStateFlow()

    private val _calendarEvents = MutableStateFlow<List<CalendarEventDto>>(emptyList())
    val calendarEvents: StateFlow<List<CalendarEventDto>> = _calendarEvents.asStateFlow()

    private val _routines = MutableStateFlow<List<DailyRoutineDto>>(emptyList())
    val routines: StateFlow<List<DailyRoutineDto>> = _routines.asStateFlow()

    private val _notificationPrefs = MutableStateFlow<NotificationPreferencesDto?>(null)
    val notificationPrefs: StateFlow<NotificationPreferencesDto?> = _notificationPrefs.asStateFlow()

    // Phase 11 — Family Mode
    private val _familyDashboard = MutableStateFlow<FamilyDashboardDto?>(null)
    val familyDashboard: StateFlow<FamilyDashboardDto?> = _familyDashboard.asStateFlow()

    private val _latestInviteCode = MutableStateFlow<String?>(null)
    val latestInviteCode: StateFlow<String?> = _latestInviteCode.asStateFlow()

    // Phase 12 — Security Dashboard
    private val _securityDashboard = MutableStateFlow<SecurityDashboardDto?>(null)
    val securityDashboard: StateFlow<SecurityDashboardDto?> = _securityDashboard.asStateFlow()

    // Phase 13 — Automation
    private val _automations = MutableStateFlow<List<AutomationDto>>(emptyList())
    val automations: StateFlow<List<AutomationDto>> = _automations.asStateFlow()

    private val _automationHistory = MutableStateFlow<List<AutomationExecutionLogDto>>(emptyList())
    val automationHistory: StateFlow<List<AutomationExecutionLogDto>> = _automationHistory.asStateFlow()

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, statusMessage = null)
    }

    private fun setLoading(loading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = loading, errorMessage = null)
    }

    private fun handleError(err: NetworkResult.Error) {
        _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = err.message)
    }

    private fun setStatus(msg: String) {
        _uiState.value = _uiState.value.copy(isLoading = false, statusMessage = msg, errorMessage = null)
    }

    // PIN & Father-Friendly Mode
    fun unlockWithPin(pin: String): Boolean {
        val ok = pinVault.verifyPin(pin)
        if (ok) {
            _uiState.value = _uiState.value.copy(isAppLocked = false, errorMessage = null)
        } else {
            _uiState.value = _uiState.value.copy(errorMessage = "Incorrect PIN. Please try again.")
        }
        return ok
    }

    fun configurePin(newPin: String) {
        if (!pinVault.setPin(newPin)) {
            _uiState.value = _uiState.value.copy(errorMessage = "PIN must be 4 to 6 digits.")
            return
        }
        _uiState.value = _uiState.value.copy(pinLockEnabled = true, statusMessage = "App PIN lock enabled.")
        viewModelScope.launch {
            repo.updateSecuritySettings(UserSecuritySettingsUpdateDto(pinLockEnabled = true))
        }
    }

    fun disablePinLock() {
        pinVault.clearPin()
        _uiState.value = _uiState.value.copy(pinLockEnabled = false, isAppLocked = false, statusMessage = "App PIN lock disabled.")
        viewModelScope.launch {
            repo.updateSecuritySettings(UserSecuritySettingsUpdateDto(pinLockEnabled = false))
        }
    }

    fun setFatherFriendlyMode(enabled: Boolean) {
        pinVault.setFatherModeEnabled(enabled)
        _uiState.value =
            _uiState.value.copy(
                fatherFriendlyMode = enabled,
                statusMessage =
                    if (enabled) "Papa-Friendly Interface enabled!"
                    else "Standard Boss Interface restored.",
            )
    }

    // Phase 7 — Personal Memory
    fun loadMemories(category: String? = null) = viewModelScope.launch {
        setLoading(true)
        when (val res = repo.listMemories(category)) {
            is NetworkResult.Success -> {
                _memories.value = res.data.items
                if (res.data.categories.isNotEmpty()) _memoryCategories.value = res.data.categories
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
            is NetworkResult.Error -> handleError(res)
        }
        when (val audit = repo.listMemoryAudit()) {
            is NetworkResult.Success -> _memoryAudit.value = audit.data
            is NetworkResult.Error -> Unit
        }
    }

    fun saveExplicitMemory(category: String, content: String, familyShared: Boolean = false) =
        viewModelScope.launch {
            setLoading(true)
            when (val res = repo.createMemory(category, content, familyShared)) {
                is NetworkResult.Success -> {
                    setStatus("Yaad rakh liya Boss! Memory saved in $category.")
                    voiceEngine.speak("Yaad rakh liya Boss! Memory save ho gayi hai.")
                    loadMemories()
                }
                is NetworkResult.Error -> handleError(res)
            }
        }

    fun updateMemory(id: String, category: String, content: String, familyShared: Boolean) =
        viewModelScope.launch {
            setLoading(true)
            when (val res = repo.updateMemory(id, category, content, familyShared)) {
                is NetworkResult.Success -> {
                    setStatus("Memory updated, Boss.")
                    loadMemories()
                }
                is NetworkResult.Error -> handleError(res)
            }
        }

    fun deleteMemory(id: String) = viewModelScope.launch {
        setLoading(true)
        when (val res = repo.deleteMemory(id)) {
            is NetworkResult.Success -> {
                setStatus("Memory deleted.")
                loadMemories()
            }
            is NetworkResult.Error -> handleError(res)
        }
    }

    fun clearAllMemories() = viewModelScope.launch {
        setLoading(true)
        when (val res = repo.clearAllMemories()) {
            is NetworkResult.Success -> {
                _memories.value = emptyList()
                setStatus(res.data.message)
            }
            is NetworkResult.Error -> handleError(res)
        }
    }

    // Phase 8 & 9 — Phone Control & WhatsApp Assistant
    fun loadPhoneAndMessaging() = viewModelScope.launch {
        _deviceInfo.value = actionExecutor.getDeviceInfoSnapshot()
        when (val p = repo.getPhonePreferences()) {
            is NetworkResult.Success -> _phonePrefs.value = p.data
            is NetworkResult.Error -> Unit
        }
        when (val a = repo.listContactAliases()) {
            is NetworkResult.Success -> _contactAliases.value = a.data
            is NetworkResult.Error -> Unit
        }
        when (val mp = repo.getMessagingPreferences()) {
            is NetworkResult.Success -> _messagingPrefs.value = mp.data
            is NetworkResult.Error -> Unit
        }
        when (val t = repo.listMessagingTemplates()) {
            is NetworkResult.Success -> _templates.value = t.data
            is NetworkResult.Error -> Unit
        }
    }

    fun updatePhonePrefs(
        confirmBeforeCall: Boolean? = null,
        allowVolume: Boolean? = null,
        allowMedia: Boolean? = null,
        allowContactLookup: Boolean? = null,
    ) = viewModelScope.launch {
        when (
            val res =
                repo.updatePhonePreferences(
                    PhonePreferencesUpdateDto(
                        confirmBeforeCall = confirmBeforeCall,
                        allowVolumeControl = allowVolume,
                        allowMediaControl = allowMedia,
                        allowContactLookup = allowContactLookup,
                    )
                )
        ) {
            is NetworkResult.Success -> {
                _phonePrefs.value = res.data
                setStatus("Phone control preferences updated.")
            }
            is NetworkResult.Error -> handleError(res)
        }
    }

    fun updateMessagingPrefs(
        defaultLanguage: String? = null,
        alwaysConfirmBeforeOpen: Boolean? = null,
        signatureText: String? = null,
    ) = viewModelScope.launch {
        when (
            val res =
                repo.updateMessagingPreferences(
                    MessagingPreferencesUpdateDto(
                        defaultLanguage = defaultLanguage,
                        alwaysConfirmBeforeOpen = alwaysConfirmBeforeOpen,
                        signatureText = signatureText,
                    )
                )
        ) {
            is NetworkResult.Success -> {
                _messagingPrefs.value = res.data
                setStatus("Messaging preferences updated.")
            }
            is NetworkResult.Error -> handleError(res)
        }
    }

    fun saveContactAlias(nickname: String, contactName: String, phoneHint: String) =
        viewModelScope.launch {
            setLoading(true)
            when (val res = repo.createContactAlias(nickname, contactName, phoneHint)) {
                is NetworkResult.Success -> {
                    setStatus("Saved nickname '$nickname' -> $contactName")
                    loadPhoneAndMessaging()
                }
                is NetworkResult.Error -> handleError(res)
            }
        }

    fun deleteContactAlias(id: String) = viewModelScope.launch {
        when (val res = repo.deleteContactAlias(id)) {
            is NetworkResult.Success -> {
                setStatus("Nickname deleted.")
                loadPhoneAndMessaging()
            }
            is NetworkResult.Error -> handleError(res)
        }
    }

    fun lookupContactForCallOrMessage(query: String) {
        val result = contactLookupHelper.lookupByQuery(query, _contactAliases.value)
        _contactLookupResult.value = result
        if (result is ContactLookupResult.Single) {
            _pendingCallMatch.value = result.match
        }
    }

    fun selectContactForCall(match: ContactMatch) {
        _pendingCallMatch.value = match
        _contactLookupResult.value = ContactLookupResult.Single(match)
    }

    fun confirmAndOpenDialer(match: ContactMatch) {
        _pendingCallMatch.value = null
        val res = actionExecutor.openDialer(match.phoneNumber)
        setStatus(res.message)
    }

    fun cancelCallConfirmation() {
        _pendingCallMatch.value = null
    }

    fun adjustVolume(dir: String) = setStatus(actionExecutor.adjustVolume(dir).message)
    fun controlMedia(cmd: String) = setStatus(actionExecutor.dispatchMediaPlayback(cmd).message)
    fun openDeviceSettings(category: String) = setStatus(actionExecutor.openSettingsCategory(category).message)
    fun openExactAlarmSettings() = setStatus(actionExecutor.openExactAlarmSettings().message)

    fun prepareWhatsAppDraft(recipientName: String, phoneNumber: String, body: String, language: String) {
        _whatsAppDraft.value = WhatsAppDraft(recipientName, phoneNumber, body, language)
    }

    fun confirmAndLaunchWhatsApp(draft: WhatsAppDraft) {
        _whatsAppDraft.value = null
        val res = actionExecutor.openWhatsAppWithConfirmedDraft(draft.phoneNumber, draft.messageBody)
        setStatus(res.message)
    }

    fun saveMessagingTemplate(title: String, language: String, body: String) = viewModelScope.launch {
        setLoading(true)
        when (val res = repo.createMessagingTemplate(title, language, body)) {
            is NetworkResult.Success -> {
                setStatus("Template '$title' saved.")
                loadPhoneAndMessaging()
            }
            is NetworkResult.Error -> handleError(res)
        }
    }

    fun deleteMessagingTemplate(id: String) = viewModelScope.launch {
        when (val res = repo.deleteMessagingTemplate(id)) {
            is NetworkResult.Success -> {
                setStatus("Template deleted.")
                loadPhoneAndMessaging()
            }
            is NetworkResult.Error -> handleError(res)
        }
    }

    // Phase 10 — Daily Life Assistant
    fun loadDailyLife() = viewModelScope.launch {
        setLoading(true)
        _deviceInfo.value = actionExecutor.getDeviceInfoSnapshot()
        when (val r = repo.listReminders()) {
            is NetworkResult.Success -> _reminders.value = r.data
            is NetworkResult.Error -> handleError(r)
        }
        when (val t = repo.listTasks()) {
            is NetworkResult.Success -> _tasks.value = t.data
            is NetworkResult.Error -> Unit
        }
        when (val tt = repo.listTimetable()) {
            is NetworkResult.Success -> _timetable.value = tt.data
            is NetworkResult.Error -> Unit
        }
        when (val ev = repo.listCalendarEvents()) {
            is NetworkResult.Success -> _calendarEvents.value = ev.data
            is NetworkResult.Error -> Unit
        }
        when (val rt = repo.listDailyRoutines()) {
            is NetworkResult.Success -> _routines.value = rt.data
            is NetworkResult.Error -> Unit
        }
        when (val np = repo.getNotificationPreferences()) {
            is NetworkResult.Success -> _notificationPrefs.value = np.data
            is NetworkResult.Error -> Unit
        }
        _uiState.value = _uiState.value.copy(isLoading = false)
    }

    fun addReminder(title: String, notes: String, delayMinutes: Long, recurrence: String) =
        viewModelScope.launch {
            setLoading(true)
            val targetIso = Instant.now().plus(delayMinutes.coerceAtLeast(1), ChronoUnit.MINUTES).toString()
            val tz = actionExecutor.getDeviceInfoSnapshot().timezoneId
            val req =
                ReminderCreateDto(
                    title = title,
                    notes = notes,
                    remindAt = targetIso,
                    timezoneName = tz,
                    recurrence = recurrence,
                    notifyEnabled = true,
                )
            ReminderScheduler.scheduleReminderWork(
                context = appContext,
                reminderId = "local_${title.hashCode()}",
                title = title,
                notes = notes,
                delayMinutes = delayMinutes,
                recurringDaily = recurrence.equals("DAILY", ignoreCase = true),
            )
            when (val res = repo.createReminder(req)) {
                is NetworkResult.Success -> {
                    setStatus("Reminder scheduled (WorkManager + PostgreSQL), Boss!")
                    loadDailyLife()
                }
                is NetworkResult.Error -> {
                    setStatus("Scheduled locally on device via WorkManager (cloud sync failed: ${res.message}).")
                }
            }
        }

    fun toggleReminderDone(reminder: ReminderDto) = viewModelScope.launch {
        when (val res = repo.updateReminder(reminder.id, !reminder.isCompleted)) {
            is NetworkResult.Success -> loadDailyLife()
            is NetworkResult.Error -> handleError(res)
        }
    }

    fun deleteReminder(id: String) = viewModelScope.launch {
        when (val res = repo.deleteReminder(id)) {
            is NetworkResult.Success -> loadDailyLife()
            is NetworkResult.Error -> handleError(res)
        }
    }

    fun addTask(title: String, priority: String) = viewModelScope.launch {
        when (val res = repo.createTask(title, priority)) {
            is NetworkResult.Success -> {
                setStatus("Task added, Boss.")
                loadDailyLife()
            }
            is NetworkResult.Error -> handleError(res)
        }
    }

    fun toggleTaskDone(task: TaskDto) = viewModelScope.launch {
        when (val res = repo.updateTask(task.id, !task.isCompleted)) {
            is NetworkResult.Success -> loadDailyLife()
            is NetworkResult.Error -> handleError(res)
        }
    }

    fun deleteTask(id: String) = viewModelScope.launch {
        when (val res = repo.deleteTask(id)) {
            is NetworkResult.Success -> loadDailyLife()
            is NetworkResult.Error -> handleError(res)
        }
    }

    fun addTimetableEntry(day: String, start: String, end: String, subject: String, location: String) =
        viewModelScope.launch {
            when (
                val res =
                    repo.createTimetableEntry(TimetableCreateDto(day, start, end, subject, location))
            ) {
                is NetworkResult.Success -> {
                    setStatus("Timetable entry added.")
                    loadDailyLife()
                }
                is NetworkResult.Error -> handleError(res)
            }
        }

    fun deleteTimetableEntry(id: String) = viewModelScope.launch {
        when (val res = repo.deleteTimetableEntry(id)) {
            is NetworkResult.Success -> loadDailyLife()
            is NetworkResult.Error -> handleError(res)
        }
    }

    fun addCalendarEvent(title: String, description: String, delayMinutes: Long) =
        viewModelScope.launch {
            val start = Instant.now().plus(delayMinutes.coerceAtLeast(15), ChronoUnit.MINUTES)
            val end = start.plus(60, ChronoUnit.MINUTES)
            val tz = actionExecutor.getDeviceInfoSnapshot().timezoneId
            when (
                val res =
                    repo.createCalendarEvent(
                        CalendarEventCreateDto(
                            title = title,
                            description = description,
                            startAt = start.toString(),
                            endAt = end.toString(),
                            timezoneName = tz,
                        )
                    )
            ) {
                is NetworkResult.Success -> {
                    setStatus("Calendar event '${res.data.title}' added.")
                    loadDailyLife()
                }
                is NetworkResult.Error -> handleError(res)
            }
        }

    fun deleteCalendarEvent(id: String) = viewModelScope.launch {
        when (val res = repo.deleteCalendarEvent(id)) {
            is NetworkResult.Success -> loadDailyLife()
            is NetworkResult.Error -> handleError(res)
        }
    }

    fun addRoutine(name: String, triggerTime: String, summaryPrompt: String) =
        viewModelScope.launch {
            when (
                val res =
                    repo.createDailyRoutine(DailyRoutineCreateDto(name, triggerTime, summaryPrompt, true))
            ) {
                is NetworkResult.Success -> {
                    setStatus("Routine '${res.data.name}' saved.")
                    loadDailyLife()
                }
                is NetworkResult.Error -> handleError(res)
            }
        }

    fun updateNotificationPrefs(
        morningGreeting: Boolean? = null,
        eveningSummary: Boolean? = null,
        remindersEnabled: Boolean? = null,
    ) = viewModelScope.launch {
        when (
            val res =
                repo.updateNotificationPreferences(
                    NotificationPreferencesUpdateDto(
                        morningGreetingEnabled = morningGreeting,
                        eveningSummaryEnabled = eveningSummary,
                        reminderNotificationsEnabled = remindersEnabled,
                    )
                )
        ) {
            is NetworkResult.Success -> {
                _notificationPrefs.value = res.data
                setStatus("Notification preferences updated.")
            }
            is NetworkResult.Error -> handleError(res)
        }
    }

    fun speakDailySummary(isEvening: Boolean) {
        val pendingTasks = _tasks.value.count { !it.isCompleted }
        val activeReminders = _reminders.value.count { !it.isCompleted }
        val msg =
            if (isEvening) {
                "Good evening Boss! Aaj ke baad aapke $pendingTasks pending tasks aur $activeReminders active reminders bache hain."
            } else {
                "Good morning Boss! Aaj aapke $activeReminders reminders aur $pendingTasks tasks scheduled hain. GOLU ready hai!"
            }
        voiceEngine.speak(msg)
        setStatus(msg)
    }

    // Phase 11 — Family Mode
    fun loadFamilyDashboard() = viewModelScope.launch {
        setLoading(true)
        when (val res = repo.getFamilyDashboard()) {
            is NetworkResult.Success -> {
                _familyDashboard.value = res.data
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
            is NetworkResult.Error -> handleError(res)
        }
    }

    fun createFamilyGroup(name: String) = viewModelScope.launch {
        setLoading(true)
        when (val res = repo.createFamilyGroup(name)) {
            is NetworkResult.Success -> {
                setStatus("Family group '${res.data.name}' created!")
                loadFamilyDashboard()
            }
            is NetworkResult.Error -> handleError(res)
        }
    }

    fun invitePapaOrMember(groupId: String, email: String, displayLabel: String, fatherMode: Boolean) =
        viewModelScope.launch {
            setLoading(true)
            val req =
                FamilyInvitationCreateDto(
                    groupId = groupId,
                    inviteeEmail = email,
                    role = if (fatherMode) "FATHER" else "MEMBER",
                    displayLabel = displayLabel,
                    fatherFriendlyMode = fatherMode,
                )
            when (val res = repo.inviteFamilyMember(req)) {
                is NetworkResult.Success -> {
                    _latestInviteCode.value = res.data.inviteCode
                    setStatus("Invitation created for $displayLabel. Share invite code securely.")
                    loadFamilyDashboard()
                }
                is NetworkResult.Error -> handleError(res)
            }
        }

    fun acceptFamilyInvite(code: String) = viewModelScope.launch {
        setLoading(true)
        when (val res = repo.acceptFamilyInvitation(code)) {
            is NetworkResult.Success -> {
                if (res.data.fatherFriendlyMode) {
                    setFatherFriendlyMode(true)
                }
                setStatus("Joined family group as ${res.data.displayLabel}!")
                loadFamilyDashboard()
            }
            is NetworkResult.Error -> handleError(res)
        }
    }

    fun addSharedReminder(groupId: String, title: String, notes: String) = viewModelScope.launch {
        val remindAt = Instant.now().plus(60, ChronoUnit.MINUTES).toString()
        when (val res = repo.createSharedReminder(SharedReminderCreateDto(groupId, title, notes, remindAt))) {
            is NetworkResult.Success -> {
                setStatus("Shared family reminder added!")
                loadFamilyDashboard()
            }
            is NetworkResult.Error -> handleError(res)
        }
    }

    fun deleteSharedReminder(id: String) = viewModelScope.launch {
        when (val res = repo.deleteSharedReminder(id)) {
            is NetworkResult.Success -> loadFamilyDashboard()
            is NetworkResult.Error -> handleError(res)
        }
    }

    fun updateFamilyMemberPrivacy(
        memberId: String,
        fatherMode: Boolean? = null,
        shareSharedReminders: Boolean? = null,
        shareFamilyRoutines: Boolean? = null,
    ) = viewModelScope.launch {
        when (
            val res =
                repo.updateFamilyMember(
                    memberId,
                    FamilyMemberUpdateDto(fatherMode, shareSharedReminders, shareFamilyRoutines),
                )
        ) {
            is NetworkResult.Success -> {
                setStatus("Family member sharing preferences updated.")
                loadFamilyDashboard()
            }
            is NetworkResult.Error -> handleError(res)
        }
    }

    // Phase 12 — Security & Privacy Dashboard
    fun loadSecurityDashboard() = viewModelScope.launch {
        setLoading(true)
        when (val res = repo.getSecurityDashboard()) {
            is NetworkResult.Success -> {
                _securityDashboard.value = res.data
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
            is NetworkResult.Error -> handleError(res)
        }
    }

    fun updateRemoteSecurityFlags(
        confirmSensitive: Boolean? = null,
        micAllowed: Boolean? = null,
        wakeWordAllowed: Boolean? = null,
    ) = viewModelScope.launch {
        val req =
            UserSecuritySettingsUpdateDto(
                confirmSensitiveActions = confirmSensitive,
                microphoneAllowed = micAllowed,
                wakeWordMonitoringAllowed = wakeWordAllowed,
            )
        when (val res = repo.updateSecuritySettings(req)) {
            is NetworkResult.Success -> {
                setStatus("Security preferences updated.")
                loadSecurityDashboard()
            }
            is NetworkResult.Error -> handleError(res)
        }
    }

    fun revokeSession(sessionId: String) = viewModelScope.launch {
        when (val res = repo.revokeSession(sessionId)) {
            is NetworkResult.Success -> {
                setStatus(res.data.message)
                loadSecurityDashboard()
            }
            is NetworkResult.Error -> handleError(res)
        }
    }

    fun wipeAllPersonalData() = viewModelScope.launch {
        setLoading(true)
        when (val res = repo.deletePersonalData()) {
            is NetworkResult.Success -> {
                _memories.value = emptyList()
                setStatus(res.data.message)
                loadSecurityDashboard()
            }
            is NetworkResult.Error -> handleError(res)
        }
    }

    // Phase 13 — Smart Automation
    fun loadAutomations() = viewModelScope.launch {
        setLoading(true)
        when (val res = repo.listAutomations()) {
            is NetworkResult.Success -> _automations.value = res.data
            is NetworkResult.Error -> handleError(res)
        }
        when (val h = repo.getAutomationHistory()) {
            is NetworkResult.Success -> _automationHistory.value = h.data
            is NetworkResult.Error -> Unit
        }
        _uiState.value = _uiState.value.copy(isLoading = false)
    }

    fun createAutomation(
        name: String,
        triggerType: String,
        triggerValue: String,
        conditionType: String?,
        conditionValue: String?,
        actionType: String,
        actionPayload: String,
        confirmedSensitive: Boolean,
    ) = viewModelScope.launch {
        setLoading(true)
        val conditions =
            if (!conditionType.isNullOrBlank() && !conditionValue.isNullOrBlank()) {
                listOf(AutomationConditionDto(conditionType, conditionValue))
            } else {
                emptyList()
            }
        val req =
            AutomationCreateDto(
                name = name,
                triggerType = triggerType,
                triggerValue = triggerValue,
                isEnabled = true,
                requiresConfirmation = actionType in listOf("OPEN_APP", "ANDROID_ACTION"),
                confirmedSensitive = confirmedSensitive,
                conditions = conditions,
                actions = listOf(AutomationActionDto(actionType, actionPayload)),
            )
        when (val res = repo.createAutomation(req)) {
            is NetworkResult.Success -> {
                setStatus("Automation '${res.data.name}' saved!")
                loadAutomations()
            }
            is NetworkResult.Error -> handleError(res)
        }
    }

    fun toggleAutomationEnabled(rule: AutomationDto) = viewModelScope.launch {
        val res =
            if (rule.isEnabled) repo.disableAutomation(rule.id)
            else repo.enableAutomation(rule.id)
        when (res) {
            is NetworkResult.Success -> loadAutomations()
            is NetworkResult.Error -> handleError(res)
        }
    }

    fun updateAutomation(
        ruleId: String,
        name: String,
        triggerType: String,
        triggerValue: String,
        actionType: String,
        actionPayload: String,
    ) = viewModelScope.launch {
        when (
            val res =
                repo.updateAutomation(
                    ruleId,
                    AutomationUpdateDto(
                        name = name,
                        triggerType = triggerType,
                        triggerValue = triggerValue,
                        requiresConfirmation = actionType in listOf("OPEN_APP", "ANDROID_ACTION"),
                        actions = listOf(AutomationActionDto(actionType, actionPayload)),
                    ),
                )
        ) {
            is NetworkResult.Success -> {
                setStatus("Automation '${res.data.name}' updated.")
                loadAutomations()
            }
            is NetworkResult.Error -> handleError(res)
        }
    }

    fun deleteAutomation(id: String) = viewModelScope.launch {
        when (val res = repo.deleteAutomation(id)) {
            is NetworkResult.Success -> loadAutomations()
            is NetworkResult.Error -> handleError(res)
        }
    }

    fun runAutomationNow(rule: AutomationDto, confirmedSensitive: Boolean = true) = viewModelScope.launch {
        val outcome = automationEvaluator.runRuleSafely(rule, confirmedSensitive)
        setStatus(outcome.summary)
        repo.logAutomationExecution(rule.id, outcome.status, outcome.summary)
        loadAutomations()
    }

    class Factory(
        private val appContext: Context,
        private val container: AppContainer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GoluFeaturesViewModel(
                appContext = appContext,
                repo = container.featuresRepository,
                pinVault = container.securityPinVault,
                actionExecutor = container.actionExecutor,
                contactLookupHelper = container.contactLookupHelper,
                automationEvaluator = container.automationEvaluator,
                voiceEngine = container.voiceEngine,
            ) as T
        }
    }
}
