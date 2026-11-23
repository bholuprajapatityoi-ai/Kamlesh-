package com.example.domain.ai

import android.content.Context
import com.example.core.DeviceTelemetry
import com.example.domain.model.GoluLanguage
import com.example.domain.model.GoluSettings
import com.example.domain.model.IntentType
import com.example.domain.model.ResponseStyle
import com.example.domain.model.RoutingTarget

data class OfflineCommandResponse(
    val reply: String,
    val target: RoutingTarget,
    val handled: Boolean,
    val requiresConfirmation: Boolean = false,
    val pendingMemoryContent: String? = null,
    val pendingMemoryCategory: String? = null,
    val pendingCallTarget: String? = null,
    val pendingWhatsAppRecipient: String? = null,
    val pendingWhatsAppDraft: String? = null,
)

/**
 * Phase 6-10 On-Device Offline Command Engine.
 * Handles time, date, battery, greetings, app launching, settings, volume/media,
 * alarms, reminders, memory save proposals, and WhatsApp compose drafts locally.
 */
class OfflineCommandEngine(
    private val context: Context,
    private val actionExecutor: AndroidActionExecutor,
) {
    fun tryExecute(
        text: String,
        intentType: IntentType,
        settings: GoluSettings,
        confirmedSensitive: Boolean = false,
    ): OfflineCommandResponse {
        val playful = settings.responseStyle == ResponseStyle.PLAYFUL_BOSS
        val hindi = settings.language == GoluLanguage.HINDI
        val english = settings.language == GoluLanguage.ENGLISH
        val t = text.lowercase().trim()

        return when (intentType) {
            IntentType.MEMORY_SAVE -> {
                val extracted =
                    text.replace(Regex("(?i)(golu[, ]*|ye yaad rakhna|yaad rakhna|remember this|remember that)"), "")
                        .trim(' ', ':', ',', '-', '.')
                if (extracted.length < 4) {
                    OfflineCommandResponse(
                        reply = "Boss, kya yaad rakhna hai? Kripya saaf-saaf batayein (jaise 'GOLU, ye yaad rakhna meri fav app Spotify hai').",
                        target = RoutingTarget.OFFLINE_ENGINE,
                        handled = true,
                    )
                } else {
                    val category =
                        when {
                            extracted.contains("app", ignoreCase = true) -> "Favourite Apps"
                            extracted.contains("papa", ignoreCase = true) ||
                                extracted.contains("mummy", ignoreCase = true) ||
                                extracted.contains("number", ignoreCase = true) -> "Contact Nicknames"
                            extracted.contains("timetable", ignoreCase = true) ||
                                extracted.contains("class", ignoreCase = true) -> "Timetable"
                            extracted.contains("routine", ignoreCase = true) -> "Daily Routines"
                            extracted.contains("language", ignoreCase = true) -> "Preferences"
                            else -> "Custom Notes"
                        }
                    OfflineCommandResponse(
                        reply = "Boss, kya aap confirm karte hain ki main yeh Personal Memory ($category) mein save karoon: \"$extracted\"?",
                        target = RoutingTarget.OFFLINE_ENGINE,
                        handled = true,
                        requiresConfirmation = true,
                        pendingMemoryContent = extracted,
                        pendingMemoryCategory = category,
                    )
                }
            }

            IntentType.GREETING -> {
                val reply =
                    when {
                        hindi -> "नमस्ते बॉस! गोलू बिल्कुल तैयार है, बताइए आज क्या धमाल करना है?"
                        english -> "Hello Boss! GOLU is powered up and ready. What's our mission?"
                        playful -> "Hello! Boss, GOLU ready hai! Boliye kya hukum hai mere aaka?"
                        else -> "Hello Boss, GOLU ready hai. Bataiye kya madad karoon?"
                    }
                OfflineCommandResponse(reply, RoutingTarget.OFFLINE_ENGINE, true)
            }

            IntentType.TIME_QUERY -> {
                val time = DeviceTelemetry.formattedTime()
                val reply =
                    when {
                        hindi -> "बॉस, अभी समय $time हुआ है।"
                        english -> "Boss, the current time is $time."
                        else -> "Boss, abhi time $time hua hai. Ekdum sahi waqt hai kaam shuru karne ka!"
                    }
                OfflineCommandResponse(reply, RoutingTarget.OFFLINE_ENGINE, true)
            }

            IntentType.DATE_QUERY -> {
                val date = DeviceTelemetry.formattedDate()
                val reply =
                    when {
                        hindi -> "बॉस, आज की तारीख $date है।"
                        english -> "Boss, today's date is $date."
                        else -> "Boss, aaj ki date $date hai!"
                    }
                OfflineCommandResponse(reply, RoutingTarget.OFFLINE_ENGINE, true)
            }

            IntentType.BATTERY_QUERY -> {
                val battery = DeviceTelemetry.getBatteryTelemetry(context)
                val chargeNote = if (battery.isCharging) "aur phone charge ho raha hai ⚡" else "aur charger disconnected hai"
                val reply =
                    when {
                        hindi -> "बॉस, बैटरी ${battery.levelPercent}% है।"
                        english ->
                            "Boss, battery is at ${battery.levelPercent}%${if (battery.isCharging) " and charging." else "."}"
                        else ->
                            "Boss, battery ${battery.levelPercent}% hai $chargeNote. ${
                                if (battery.levelPercent < 20) "Thoda juice pilaiye phone ko!" else "Aaram se chalega!"
                            }"
                    }
                OfflineCommandResponse(reply, RoutingTarget.ANDROID_ACTION, true)
            }

            IntentType.PHONE_CONTROL -> {
                val res =
                    when {
                        t.contains("wifi") || t.contains("wi-fi") -> actionExecutor.openSettingsCategory("wifi")
                        t.contains("bluetooth") -> actionExecutor.openSettingsCategory("bluetooth")
                        t.contains("display") -> actionExecutor.openSettingsCategory("display")
                        t.contains("sound setting") -> actionExecutor.openSettingsCategory("sound")
                        t.contains("volume") && (t.contains("up") || t.contains("badhao") || t.contains("increase")) ->
                            actionExecutor.adjustVolume("up")
                        t.contains("volume") && (t.contains("down") || t.contains("kam") || t.contains("decrease")) ->
                            actionExecutor.adjustVolume("down")
                        t.contains("unmute") -> actionExecutor.adjustVolume("unmute")
                        t.contains("mute") -> actionExecutor.adjustVolume("mute")
                        t.contains("next song") -> actionExecutor.dispatchMediaPlayback("next")
                        t.contains("previous song") -> actionExecutor.dispatchMediaPlayback("previous")
                        t.contains("pause music") || t.contains("play music") ->
                            actionExecutor.dispatchMediaPlayback("play_pause")
                        t.contains("phone info") -> {
                            val info = actionExecutor.getDeviceInfoSnapshot()
                            AndroidActionResult(
                                true,
                                "Boss, device ${info.manufacturer} ${info.model}, Android ${info.androidVersion} (SDK ${info.sdkInt}), Timezone ${info.timezoneId}.",
                            )
                        }
                        else -> actionExecutor.openSettings()
                    }
                OfflineCommandResponse(res.message, RoutingTarget.ANDROID_ACTION, true)
            }

            IntentType.WHATSAPP_ACTION -> {
                if (t == "open whatsapp" || t == "whatsapp kholo" || t == "whatsapp") {
                    val res = actionExecutor.openWhatsApp()
                    return OfflineCommandResponse(res.message, RoutingTarget.ANDROID_ACTION, true)
                }
                val recipient =
                    when {
                        t.contains("papa") -> "Papa"
                        t.contains("mummy") -> "Mummy"
                        t.contains("brother") || t.contains("bhai") -> "Brother"
                        t.contains("sister") || t.contains("didi") -> "Sister"
                        else -> "Contact"
                    }
                val draft =
                    when (settings.language) {
                        GoluLanguage.HINDI -> "नमस्ते $recipient, मैं ठीक हूँ। कृपया बताएं।"
                        GoluLanguage.ENGLISH -> "Hello $recipient, hope you are doing well!"
                        GoluLanguage.HINGLISH -> "Namaste $recipient, main pahunch gaya hoon / sab theek hai."
                    }
                OfflineCommandResponse(
                    reply = "Boss, maine $recipient ke liye WhatsApp draft tayyar kiya hai: \"$draft\". Confirm karein ya Phone & WhatsApp tab mein edit karke open karein.",
                    target = RoutingTarget.ANDROID_ACTION,
                    handled = true,
                    requiresConfirmation = true,
                    pendingWhatsAppRecipient = recipient,
                    pendingWhatsAppDraft = draft,
                )
            }

            IntentType.CALL_CONTACT -> {
                if (!confirmedSensitive) {
                    OfflineCommandResponse(
                        reply = "Boss, call lagane se pehle confirm kijiye: \"$text\"?",
                        target = RoutingTarget.ANDROID_ACTION,
                        handled = true,
                        requiresConfirmation = true,
                        pendingCallTarget = text,
                    )
                } else {
                    val res = actionExecutor.openDialer(text)
                    OfflineCommandResponse(res.message, RoutingTarget.ANDROID_ACTION, true)
                }
            }

            IntentType.OPEN_APP -> {
                val res = actionExecutor.openAppByQuery(text)
                OfflineCommandResponse(res.message, RoutingTarget.ANDROID_ACTION, true)
            }

            IntentType.SET_ALARM -> {
                val timeMatch = Regex("(\\d{1,2})[:.](\\d{2})").find(text)
                val hour = timeMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
                val min = timeMatch?.groupValues?.getOrNull(2)?.toIntOrNull()
                val res = actionExecutor.openAlarm(hour, min)
                OfflineCommandResponse(res.message, RoutingTarget.ANDROID_ACTION, true)
            }

            IntentType.SET_REMINDER -> {
                val cleaned =
                    text.replace(Regex("(?i)(set reminder|remind me|yaad dilana|calendar)"), "")
                        .trim()
                        .ifBlank { "GOLU Boss Reminder" }
                val res = actionExecutor.openCalendarReminder(cleaned)
                OfflineCommandResponse(res.message, RoutingTarget.ANDROID_ACTION, true)
            }

            IntentType.OPEN_SETTINGS -> {
                val res = actionExecutor.openSettings()
                OfflineCommandResponse(res.message, RoutingTarget.ANDROID_ACTION, true)
            }

            else -> OfflineCommandResponse("", RoutingTarget.ONLINE_AI_BACKEND, false)
        }
    }
}
