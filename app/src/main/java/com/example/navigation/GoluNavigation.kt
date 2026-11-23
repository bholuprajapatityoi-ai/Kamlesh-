package com.example.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.presentation.components.PinUnlockOverlay
import com.example.presentation.screens.ActivitiesScreen
import com.example.presentation.screens.AssistantScreen
import com.example.presentation.screens.AutomationScreen
import com.example.presentation.screens.ConversationScreen
import com.example.presentation.screens.DailyLifeScreen
import com.example.presentation.screens.FamilyModeScreen
import com.example.presentation.screens.GuideAndDocsScreen
import com.example.presentation.screens.HomeScreen
import com.example.presentation.screens.MemoryManagementScreen
import com.example.presentation.screens.PhoneWhatsAppScreen
import com.example.presentation.screens.ProfileScreen
import com.example.presentation.screens.SecurityDashboardScreen
import com.example.presentation.screens.SettingsScreen
import com.example.presentation.viewmodel.GoluFeaturesViewModel
import com.example.presentation.viewmodel.GoluViewModel

sealed class GoluRoute(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    data object Home : GoluRoute("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    data object Assistant : GoluRoute("assistant", "Assistant", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome)
    data object Conversation : GoluRoute("conversation", "Chat", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat)
    data object Activities : GoluRoute("activities", "Activity", Icons.Filled.History, Icons.Outlined.History)
    data object Settings : GoluRoute("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    data object Profile : GoluRoute("profile", "Profile", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)

    companion object {
        val all = listOf(Home, Assistant, Conversation, Activities, Settings, Profile)
        const val MEMORY = "memory"
        const val PHONE_WHATSAPP = "phone_whatsapp"
        const val DAILY_LIFE = "daily_life"
        const val FAMILY = "family"
        const val SECURITY = "security"
        const val AUTOMATION = "automation"
        const val GUIDE = "guide"
    }
}

@Composable
fun GoluMainShell(
    viewModel: GoluViewModel,
    featuresViewModel: GoluFeaturesViewModel,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val authFeedback by viewModel.authFeedback.collectAsStateWithLifecycle()
    val diagnostics by viewModel.diagnostics.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val wakeWordState by viewModel.wakeWordState.collectAsStateWithLifecycle()
    val wakeWordNote by viewModel.wakeWordNote.collectAsStateWithLifecycle()
    val audioLevel by viewModel.audioLevel.collectAsStateWithLifecycle()
    val partialTranscript by viewModel.partialTranscript.collectAsStateWithLifecycle()
    val voiceError by viewModel.voiceError.collectAsStateWithLifecycle()
    val battery by viewModel.battery.collectAsStateWithLifecycle()
    val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()
    val currentDate by viewModel.currentDate.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val isThinking by viewModel.isThinking.collectAsStateWithLifecycle()
    val actionToast by viewModel.actionToast.collectAsStateWithLifecycle()

    // Phase 7-13 Feature States
    val featuresUiState by featuresViewModel.uiState.collectAsStateWithLifecycle()
    val memories by featuresViewModel.memories.collectAsStateWithLifecycle()
    val memoryCategories by featuresViewModel.memoryCategories.collectAsStateWithLifecycle()
    val memoryAudit by featuresViewModel.memoryAudit.collectAsStateWithLifecycle()
    val deviceInfo by featuresViewModel.deviceInfo.collectAsStateWithLifecycle()
    val phonePrefs by featuresViewModel.phonePrefs.collectAsStateWithLifecycle()
    val contactAliases by featuresViewModel.contactAliases.collectAsStateWithLifecycle()
    val contactLookup by featuresViewModel.contactLookupResult.collectAsStateWithLifecycle()
    val pendingCallMatch by featuresViewModel.pendingCallMatch.collectAsStateWithLifecycle()
    val messagingPrefs by featuresViewModel.messagingPrefs.collectAsStateWithLifecycle()
    val templates by featuresViewModel.templates.collectAsStateWithLifecycle()
    val whatsAppDraft by featuresViewModel.whatsAppDraft.collectAsStateWithLifecycle()
    val reminders by featuresViewModel.reminders.collectAsStateWithLifecycle()
    val tasks by featuresViewModel.tasks.collectAsStateWithLifecycle()
    val timetable by featuresViewModel.timetable.collectAsStateWithLifecycle()
    val calendarEvents by featuresViewModel.calendarEvents.collectAsStateWithLifecycle()
    val routines by featuresViewModel.routines.collectAsStateWithLifecycle()
    val notificationPrefs by featuresViewModel.notificationPrefs.collectAsStateWithLifecycle()
    val familyDashboard by featuresViewModel.familyDashboard.collectAsStateWithLifecycle()
    val latestInviteCode by featuresViewModel.latestInviteCode.collectAsStateWithLifecycle()
    val securityDashboard by featuresViewModel.securityDashboard.collectAsStateWithLifecycle()
    val automations by featuresViewModel.automations.collectAsStateWithLifecycle()
    val automationHistory by featuresViewModel.automationHistory.collectAsStateWithLifecycle()

    LaunchedEffect(actionToast) {
        actionToast?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionToast()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateSecondary(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar {
                            GoluRoute.all.forEach { dest ->
                                val selected =
                                    currentDestination?.hierarchy?.any { it.route == dest.route } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector =
                                                if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label,
                                        )
                                    },
                                    label = { Text(dest.label, maxLines = 1) },
                                    modifier = Modifier.testTag("nav_${dest.route}"),
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    if (useRail) {
                        NavigationRail {
                            GoluRoute.all.forEach { dest ->
                                val selected =
                                    currentDestination?.hierarchy?.any { it.route == dest.route } == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTo(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector =
                                                if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label,
                                        )
                                    },
                                    label = { Text(dest.label) },
                                    modifier = Modifier.testTag("rail_${dest.route}"),
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = GoluRoute.Home.route,
                        modifier = Modifier.weight(1f).fillMaxSize(),
                    ) {
                        composable(GoluRoute.Home.route) {
                            HomeScreen(
                                settings = settings,
                                authState = authState,
                                diagnostics = diagnostics,
                                voiceState = voiceState,
                                wakeWordState = wakeWordState,
                                audioLevel = audioLevel,
                                partialTranscript = partialTranscript,
                                voiceError = voiceError,
                                battery = battery,
                                currentTime = currentTime,
                                currentDate = currentDate,
                                onMicClick = { viewModel.startVoiceListening() },
                                onStopVoice = viewModel::stopSpeaking,
                                onPauseVoice = viewModel::pauseSpeaking,
                                onResumeVoice = viewModel::resumeSpeaking,
                                onQuickAction = viewModel::runQuickAction,
                                onOpenApps = { navigateTo(GoluRoute.Activities.route) },
                                onNavigateToSettings = { navigateTo(GoluRoute.Settings.route) },
                                onNavigateToConversation = { navigateTo(GoluRoute.Conversation.route) },
                                onNavigateToProfile = { navigateTo(GoluRoute.Profile.route) },
                                onRetryConnection = viewModel::retryBackendConnection,
                                fatherFriendlyMode = featuresUiState.fatherFriendlyMode,
                                onOpenModule = ::navigateSecondary,
                            )
                        }

                        composable(GoluRoute.Assistant.route) {
                            AssistantScreen(
                                settings = settings,
                                voiceState = voiceState,
                                wakeWordState = wakeWordState,
                                wakeWordNote = wakeWordNote,
                                audioLevel = audioLevel,
                                partialTranscript = partialTranscript,
                                voiceError = voiceError,
                                onStartListening = { viewModel.startVoiceListening() },
                                onStopListening = viewModel::stopVoiceListening,
                                onPauseSpeaking = viewModel::pauseSpeaking,
                                onResumeSpeaking = viewModel::resumeSpeaking,
                                onStopSpeaking = viewModel::stopSpeaking,
                                onToggleWakeWord = viewModel::toggleWakeWord,
                                onOpenBatteryOptimization = viewModel::openBatteryOptimizationSettings,
                                onRunSamplePrompt = { cmd ->
                                    viewModel.submitPrompt(cmd, speakResponse = true)
                                    navigateTo(GoluRoute.Conversation.route)
                                },
                                onOpenModule = ::navigateSecondary,
                            )
                        }

                        composable(GoluRoute.Conversation.route) {
                            ConversationScreen(
                                messages = messages,
                                isThinking = isThinking,
                                voiceState = voiceState,
                                onSendPrompt = { viewModel.submitPrompt(it, speakResponse = true) },
                                onConfirmSensitive = {
                                    viewModel.submitPrompt(it, speakResponse = true, confirmedSensitive = true)
                                },
                                onConfirmSaveMemory = { cat, content ->
                                    featuresViewModel.saveExplicitMemory(cat, content)
                                },
                                onOpenWhatsAppDraft = { recipient, draft ->
                                    featuresViewModel.prepareWhatsAppDraft(recipient, "", draft, settings.language.label)
                                    navigateSecondary(GoluRoute.PHONE_WHATSAPP)
                                },
                                onConfirmCallTarget = { target ->
                                    viewModel.submitPrompt(target, speakResponse = true, confirmedSensitive = true)
                                },
                                onVoiceInput = { viewModel.startVoiceListening() },
                                onStopVoice = {
                                    viewModel.stopVoiceListening()
                                    viewModel.stopSpeaking()
                                },
                                onRetryLast = viewModel::retryLastPrompt,
                                onClearContext = viewModel::clearConversationContext,
                            )
                        }

                        composable(GoluRoute.Activities.route) {
                            ActivitiesScreen(
                                activities = activities,
                                installedApps = installedApps,
                                onLaunchApp = viewModel::launchInstalledApp,
                                onRefreshApps = viewModel::refreshInstalledApps,
                            )
                        }

                        composable(GoluRoute.Settings.route) {
                            SettingsScreen(
                                settings = settings,
                                diagnostics = diagnostics,
                                onDarkThemeChange = viewModel::setDarkTheme,
                                onVoiceGenderChange = viewModel::setVoiceGender,
                                onSpeechSpeedChange = viewModel::setSpeechSpeed,
                                onLanguageChange = viewModel::setLanguage,
                                onGreetingChange = viewModel::setGreeting,
                                onResponseStyleChange = viewModel::setResponseStyle,
                                onWakeWordChange = viewModel::toggleWakeWord,
                                onKeepContextChange = viewModel::setKeepSessionContext,
                                onRedactLogsChange = viewModel::setPrivacyRedactLogs,
                                onRetryConnection = viewModel::retryBackendConnection,
                                onOpenBatteryOptimization = viewModel::openBatteryOptimizationSettings,
                            )
                        }

                        composable(GoluRoute.Profile.route) {
                            ProfileScreen(
                                authState = authState,
                                feedback = authFeedback,
                                diagnostics = diagnostics,
                                onLogin = viewModel::login,
                                onRegister = viewModel::register,
                                onForgotPassword = viewModel::requestPasswordReset,
                                onResetPassword = viewModel::confirmPasswordReset,
                                onRestoreSession = viewModel::restoreSession,
                                onLogout = viewModel::logout,
                                onClearFeedback = viewModel::clearAuthFeedback,
                                onRetryConnection = viewModel::retryBackendConnection,
                            )
                        }

                        // Phase 7-15 Secondary Screens
                        composable(GoluRoute.MEMORY) {
                            MemoryManagementScreen(
                                uiState = featuresUiState,
                                memories = memories,
                                categories = memoryCategories,
                                auditEvents = memoryAudit,
                                onRefresh = { cat -> featuresViewModel.loadMemories(cat) },
                                onCreateMemory = featuresViewModel::saveExplicitMemory,
                                onUpdateMemory = featuresViewModel::updateMemory,
                                onDeleteMemory = featuresViewModel::deleteMemory,
                                onClearAll = featuresViewModel::clearAllMemories,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable(GoluRoute.PHONE_WHATSAPP) {
                            PhoneWhatsAppScreen(
                                uiState = featuresUiState,
                                deviceInfo = deviceInfo,
                                phonePrefs = phonePrefs,
                                aliases = contactAliases,
                                lookupResult = contactLookup,
                                pendingCallMatch = pendingCallMatch,
                                messagingPrefs = messagingPrefs,
                                templates = templates,
                                activeDraft = whatsAppDraft,
                                onLoad = featuresViewModel::loadPhoneAndMessaging,
                                onVolumeAdjust = featuresViewModel::adjustVolume,
                                onMediaControl = featuresViewModel::controlMedia,
                                onOpenSettingsCategory = featuresViewModel::openDeviceSettings,
                                onSaveAlias = featuresViewModel::saveContactAlias,
                                onDeleteAlias = featuresViewModel::deleteContactAlias,
                                onLookupContact = featuresViewModel::lookupContactForCallOrMessage,
                                onSelectContact = featuresViewModel::selectContactForCall,
                                onConfirmCall = featuresViewModel::confirmAndOpenDialer,
                                onCancelCall = featuresViewModel::cancelCallConfirmation,
                                onUpdatePhonePrefs = featuresViewModel::updatePhonePrefs,
                                onUpdateMessagingPrefs = { lang, confirm ->
                                    featuresViewModel.updateMessagingPrefs(lang, confirm, null)
                                },
                                onPrepareWhatsAppDraft = featuresViewModel::prepareWhatsAppDraft,
                                onConfirmLaunchWhatsApp = featuresViewModel::confirmAndLaunchWhatsApp,
                                onSaveTemplate = featuresViewModel::saveMessagingTemplate,
                                onDeleteTemplate = featuresViewModel::deleteMessagingTemplate,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable(GoluRoute.DAILY_LIFE) {
                            DailyLifeScreen(
                                uiState = featuresUiState,
                                deviceInfo = deviceInfo,
                                reminders = reminders,
                                tasks = tasks,
                                timetable = timetable,
                                events = calendarEvents,
                                routines = routines,
                                notificationPrefs = notificationPrefs,
                                onRefresh = featuresViewModel::loadDailyLife,
                                onAddReminder = featuresViewModel::addReminder,
                                onToggleReminder = featuresViewModel::toggleReminderDone,
                                onDeleteReminder = featuresViewModel::deleteReminder,
                                onAddTask = featuresViewModel::addTask,
                                onToggleTask = featuresViewModel::toggleTaskDone,
                                onDeleteTask = featuresViewModel::deleteTask,
                                onAddTimetable = featuresViewModel::addTimetableEntry,
                                onDeleteTimetable = featuresViewModel::deleteTimetableEntry,
                                onAddCalendarEvent = featuresViewModel::addCalendarEvent,
                                onDeleteCalendarEvent = featuresViewModel::deleteCalendarEvent,
                                onAddRoutine = featuresViewModel::addRoutine,
                                onUpdateNotificationPrefs = featuresViewModel::updateNotificationPrefs,
                                onSpeakSummary = featuresViewModel::speakDailySummary,
                                onOpenExactAlarmSettings = featuresViewModel::openExactAlarmSettings,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable(GoluRoute.FAMILY) {
                            FamilyModeScreen(
                                uiState = featuresUiState,
                                dashboard = familyDashboard,
                                latestInviteCode = latestInviteCode,
                                onRefresh = featuresViewModel::loadFamilyDashboard,
                                onToggleFatherMode = featuresViewModel::setFatherFriendlyMode,
                                onCreateGroup = featuresViewModel::createFamilyGroup,
                                onInviteMember = featuresViewModel::invitePapaOrMember,
                                onAcceptInvite = featuresViewModel::acceptFamilyInvite,
                                onUpdateMemberPrivacy = { id, father, rem ->
                                    featuresViewModel.updateFamilyMemberPrivacy(id, father, rem, null)
                                },
                                onAddSharedReminder = featuresViewModel::addSharedReminder,
                                onDeleteSharedReminder = featuresViewModel::deleteSharedReminder,
                                onFatherTalkToGolu = { viewModel.startVoiceListening() },
                                onFatherQuickCall = { viewModel.runQuickAction("Call") },
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable(GoluRoute.SECURITY) {
                            SecurityDashboardScreen(
                                uiState = featuresUiState,
                                dashboard = securityDashboard,
                                onRefresh = featuresViewModel::loadSecurityDashboard,
                                onSetPin = featuresViewModel::configurePin,
                                onDisablePin = featuresViewModel::disablePinLock,
                                onUpdateRemoteFlags = featuresViewModel::updateRemoteSecurityFlags,
                                onRevokeSession = featuresViewModel::revokeSession,
                                onWipePersonalData = featuresViewModel::wipeAllPersonalData,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable(GoluRoute.AUTOMATION) {
                            AutomationScreen(
                                uiState = featuresUiState,
                                rules = automations,
                                history = automationHistory,
                                onRefresh = featuresViewModel::loadAutomations,
                                onCreateRule = featuresViewModel::createAutomation,
                                onUpdateRule = featuresViewModel::updateAutomation,
                                onToggleEnabled = featuresViewModel::toggleAutomationEnabled,
                                onRunNow = featuresViewModel::runAutomationNow,
                                onDeleteRule = featuresViewModel::deleteAutomation,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable(GoluRoute.GUIDE) {
                            GuideAndDocsScreen(
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }

        if (featuresUiState.isAppLocked) {
            PinUnlockOverlay(
                errorMessage = featuresUiState.errorMessage,
                onUnlock = featuresViewModel::unlockWithPin,
            )
        }
    }
}
