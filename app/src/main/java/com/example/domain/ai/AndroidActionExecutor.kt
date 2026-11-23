package com.example.domain.ai

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import android.view.KeyEvent
import com.example.domain.daily.ReminderScheduler
import com.example.domain.model.DeviceInfoSnapshot
import com.example.domain.model.InstalledAppInfo
import java.util.TimeZone

data class AndroidActionResult(
    val success: Boolean,
    val message: String,
    val requiresUnlock: Boolean = false,
)

class AndroidActionExecutor(private val context: Context) {

    fun isDeviceLocked(): Boolean {
        val kg = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return kg?.isKeyguardLocked == true
    }

    fun getDeviceInfoSnapshot(): DeviceInfoSnapshot =
        DeviceInfoSnapshot(
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE ?: "Unknown",
            sdkInt = Build.VERSION.SDK_INT,
            timezoneId = TimeZone.getDefault().id,
            exactAlarmPermitted = ReminderScheduler.canScheduleExactAlarms(context),
        )

    fun getInstalledLaunchableApps(limit: Int = 24): List<InstalledAppInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
        return resolveInfos
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == context.packageName) return@mapNotNull null
                val label = info.loadLabel(pm).toString().trim()
                if (label.isBlank()) return@mapNotNull null
                InstalledAppInfo(appName = label, packageName = pkg)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
            .take(limit)
    }

    fun openDialer(phoneOrQuery: String? = null): AndroidActionResult {
        if (isDeviceLocked()) {
            return AndroidActionResult(
                success = false,
                message = "Boss, phone locked hai. Pehle device unlock kijiye before opening dialer.",
                requiresUnlock = true,
            )
        }
        val digits = phoneOrQuery?.filter { it.isDigit() || it == '+' }.orEmpty()
        val intent =
            Intent(Intent.ACTION_DIAL).apply {
                data = if (digits.isNotBlank()) Uri.parse("tel:$digits") else Uri.parse("tel:")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        return launchIntent(intent, "Opening Phone Dialer, Boss!")
    }

    fun openWhatsApp(): AndroidActionResult {
        if (isDeviceLocked()) {
            return AndroidActionResult(
                success = false,
                message = "Boss, device unlock kijiye taaki WhatsApp khol saku.",
                requiresUnlock = true,
            )
        }
        val waIntent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
        if (waIntent != null) {
            waIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return launchIntent(waIntent, "WhatsApp khol raha hoon, Boss!")
        }
        val webIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        return launchIntent(
            webIntent,
            "WhatsApp app installed nahi mila, WhatsApp Web link khol raha hoon, Boss.",
        )
    }

    /**
     * Phase 9: Opens WhatsApp or Android Share sheet ONLY after Boss explicitly reviewed and confirmed the draft.
     */
    fun openWhatsAppWithConfirmedDraft(
        phoneNumber: String,
        messageBody: String,
    ): AndroidActionResult {
        if (isDeviceLocked()) {
            return AndroidActionResult(
                success = false,
                message = "Boss, device unlock kijiye before opening WhatsApp.",
                requiresUnlock = true,
            )
        }
        val cleanedDigits = phoneNumber.filter { it.isDigit() }
        if (cleanedDigits.length >= 8) {
            val encoded = Uri.encode(messageBody)
            val uri = Uri.parse("https://wa.me/$cleanedDigits?text=$encoded")
            val waView = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return launchIntent(
                waView,
                "Confirmed! WhatsApp chat open ho raha hai—ab WhatsApp mein Send tap karein, Boss.",
            )
        }

        val waInstalled = context.packageManager.getLaunchIntentForPackage("com.whatsapp") != null
        val shareIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, messageBody)
                if (waInstalled) {
                    setPackage("com.whatsapp")
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        return if (waInstalled) {
            launchIntent(
                shareIntent,
                "Confirmed! WhatsApp share open ho raha hai—contact chunein aur Send tap karein, Boss.",
            )
        } else {
            val chooser =
                Intent.createChooser(shareIntent, "Share prepared message via").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            launchIntent(
                chooser,
                "WhatsApp installed nahi hai; Android Share sheet open kiya hai, Boss.",
            )
        }
    }

    fun adjustVolume(direction: String): AndroidActionResult {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return AndroidActionResult(false, "Audio service unavailable.")
        return runCatching {
            when (direction.lowercase()) {
                "up", "increase", "badhao" -> {
                    am.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_SHOW_UI,
                    )
                    AndroidActionResult(true, "Media volume badha diya, Boss!")
                }
                "down", "decrease", "kam" -> {
                    am.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI,
                    )
                    AndroidActionResult(true, "Media volume kam kar diya, Boss!")
                }
                "mute" -> {
                    am.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_MUTE,
                        AudioManager.FLAG_SHOW_UI,
                    )
                    AndroidActionResult(true, "Media mute kar diya, Boss!")
                }
                else -> {
                    am.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_UNMUTE,
                        AudioManager.FLAG_SHOW_UI,
                    )
                    AndroidActionResult(true, "Media unmute kar diya, Boss!")
                }
            }
        }.getOrElse {
            AndroidActionResult(false, "OS policy ne volume change block kiya: ${it.localizedMessage}")
        }
    }

    fun dispatchMediaPlayback(command: String): AndroidActionResult {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return AndroidActionResult(false, "Audio service unavailable.")
        val keyCode =
            when (command.lowercase()) {
                "play_pause", "pause", "play" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
                "previous", "prev" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
                else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            }
        return runCatching {
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            AndroidActionResult(true, "Media playback command ($command) bhej diya, Boss!")
        }.getOrElse {
            AndroidActionResult(false, "Media playback control supported nahi hua: ${it.localizedMessage}")
        }
    }

    fun openSettingsCategory(category: String): AndroidActionResult {
        val action =
            when (category.lowercase().trim()) {
                "wifi", "wi-fi" -> Settings.ACTION_WIFI_SETTINGS
                "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
                "display", "brightness", "screen" -> Settings.ACTION_DISPLAY_SETTINGS
                "sound", "volume", "audio" -> Settings.ACTION_SOUND_SETTINGS
                "apps", "app_info", "application" -> Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                else -> Settings.ACTION_SETTINGS
            }
        val intent =
            Intent(action).apply {
                if (action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        return launchIntent(intent, "Opening $category settings, Boss!")
    }

    fun openExactAlarmSettings(): AndroidActionResult {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent =
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            launchIntent(intent, "Opening Exact Alarm permission settings, Boss.")
        } else {
            openAlarm()
        }
    }

    fun openAppByQuery(rawText: String): AndroidActionResult {
        if (isDeviceLocked()) {
            return AndroidActionResult(
                success = false,
                message = "Boss, security ke liye pehle screen unlock kijiye.",
                requiresUnlock = true,
            )
        }
        val cleaned =
            rawText
                .lowercase()
                .replace("open ", "")
                .replace("launch ", "")
                .replace("kholo", "")
                .replace("karo", "")
                .replace("app", "")
                .trim()

        if (cleaned.contains("whatsapp")) return openWhatsApp()
        if (cleaned.contains("wifi") || cleaned.contains("wi-fi")) return openSettingsCategory("wifi")
        if (cleaned.contains("bluetooth")) return openSettingsCategory("bluetooth")
        if (cleaned.contains("display")) return openSettingsCategory("display")
        if (cleaned.contains("sound")) return openSettingsCategory("sound")
        if (cleaned.contains("setting")) return openSettings()

        val apps = getInstalledLaunchableApps(limit = 100)
        val matched =
            apps.firstOrNull { it.appName.lowercase() == cleaned }
                ?: apps.firstOrNull { it.appName.lowercase().contains(cleaned) && cleaned.isNotBlank() }

        return if (matched != null) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(matched.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                launchIntent(launchIntent, "${matched.appName} open kar diya, Boss!")
            } else {
                AndroidActionResult(false, "${matched.appName} launch nahi ho paya.")
            }
        } else {
            AndroidActionResult(
                false,
                "Boss, '$cleaned' naam ka koi installed app nahi mila. Activities tab se installed apps dekh sakte hain.",
            )
        }
    }

    fun openAlarm(hour: Int? = null, minute: Int? = null, message: String = "GOLU Alarm"): AndroidActionResult {
        val intent =
            if (hour != null && minute != null) {
                Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour.coerceIn(0, 23))
                    putExtra(AlarmClock.EXTRA_MINUTES, minute.coerceIn(0, 59))
                    putExtra(AlarmClock.EXTRA_MESSAGE, message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        return launchIntent(intent, "Alarm Clock open kar raha hoon, Boss!")
    }

    fun openCalendarReminder(title: String = "GOLU Reminder"): AndroidActionResult {
        val intent =
            Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        return launchIntent(intent, "Calendar reminder screen open kar raha hoon, Boss!")
    }

    fun openSettings(): AndroidActionResult = openSettingsCategory("settings")

    fun openBatterySettings(): AndroidActionResult {
        val intent =
            Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        return launchIntent(intent, "Battery details khol raha hoon, Boss!")
    }

    fun openBatteryOptimizationSettings(): AndroidActionResult {
        val intent =
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        return launchIntent(intent, "Battery optimization settings opened.")
    }

    private fun launchIntent(intent: Intent, successMessage: String): AndroidActionResult {
        return runCatching {
            context.startActivity(intent)
            AndroidActionResult(success = true, message = successMessage)
        }.getOrElse {
            AndroidActionResult(
                success = false,
                message = "Action launch nahi ho paya: ${it.localizedMessage ?: "No handler app found"}",
            )
        }
    }
}
