package com.example.domain.automation

import android.content.Context
import com.example.core.DeviceTelemetry
import com.example.data.remote.dto.AutomationDto
import com.example.domain.ai.AndroidActionExecutor
import com.example.domain.daily.ReminderScheduler
import com.example.domain.voice.VoiceEngine

data class AutomationRunOutcome(
    val executed: Boolean,
    val status: String,
    val summary: String,
    val requiresConfirmation: Boolean = false,
)

class AutomationEvaluator(
    private val context: Context,
    private val actionExecutor: AndroidActionExecutor,
    private val voiceEngine: VoiceEngine,
) {
    fun runRuleSafely(
        rule: AutomationDto,
        confirmedSensitive: Boolean = false,
    ): AutomationRunOutcome {
        if (!rule.isEnabled) {
            return AutomationRunOutcome(
                executed = false,
                status = "SKIPPED",
                summary = "Automation '${rule.name}' is paused/disabled.",
            )
        }
        if (rule.requiresConfirmation && !confirmedSensitive) {
            return AutomationRunOutcome(
                executed = false,
                status = "CONFIRMATION_REQUIRED",
                summary = "Boss, '${rule.name}' performs a device action. Please confirm execution.",
                requiresConfirmation = true,
            )
        }

        // Check conditions
        val battery = DeviceTelemetry.getBatteryTelemetry(context)
        for (cond in rule.conditions) {
            if (cond.conditionType.equals("DEVICE_STATE", ignoreCase = true) &&
                cond.conditionValue.contains("BATTERY_LOW", ignoreCase = true) &&
                battery.levelPercent > 25
            ) {
                return AutomationRunOutcome(
                    executed = false,
                    status = "CONDITION_NOT_MET",
                    summary = "Condition not met: battery is ${battery.levelPercent}% (>25%).",
                )
            }
        }

        if (rule.actions.isEmpty()) {
            return AutomationRunOutcome(
                executed = true,
                status = "EXECUTED",
                summary = "Rule '${rule.name}' triggered (no actions attached).",
            )
        }

        val summaries = mutableListOf<String>()
        for (action in rule.actions) {
            when (action.actionType.uppercase()) {
                "SPEAK_MESSAGE" -> {
                    val msg = action.actionPayload.ifBlank { "Hello Boss! Automation active." }
                    voiceEngine.speak(msg)
                    summaries.add("Spoke: $msg")
                }
                "SHOW_NOTIFICATION" -> {
                    val msg = action.actionPayload.ifBlank { rule.name }
                    ReminderScheduler.postReminderNotification(
                        context = context,
                        notifId = kotlin.math.abs(rule.id.hashCode()).coerceAtLeast(8000),
                        title = "GOLU Automation: ${rule.name}",
                        body = msg,
                    )
                    summaries.add("Notification shown")
                }
                "OPEN_APP" -> {
                    val res = actionExecutor.openAppByQuery(action.actionPayload)
                    summaries.add(res.message)
                }
                "READ_TIMETABLE" -> {
                    val msg = action.actionPayload.ifBlank { "Boss, aapka daily timetable check karne ka samay ho gaya hai." }
                    voiceEngine.speak(msg)
                    summaries.add("Timetable summary announced")
                }
                "CREATE_REMINDER" -> {
                    ReminderScheduler.scheduleReminderWork(
                        context = context,
                        reminderId = rule.id,
                        title = action.actionPayload.ifBlank { rule.name },
                        notes = "Created by GOLU Automation",
                        delayMinutes = 1,
                    )
                    summaries.add("Scheduled reminder notification")
                }
                "ANDROID_ACTION" -> {
                    val res = actionExecutor.openSettingsCategory(action.actionPayload)
                    summaries.add(res.message)
                }
                else -> summaries.add("Ignored unsupported action ${action.actionType}")
            }
        }

        return AutomationRunOutcome(
            executed = true,
            status = "EXECUTED",
            summary = summaries.joinToString(" | ").take(250),
        )
    }
}
