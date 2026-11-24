package com.example.domain.golu2

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CapturedDeviceNotification(
    val key: String,
    val packageName: String,
    val title: String,
    val body: String,
    val postedAt: Long,
)

/**
 * Feature 4: Smart Notification Intelligence Listener.
 * Operates ONLY if user explicitly grants Notification Access in Android Settings.
 * Buffers recent items in memory locally; never uploads without user consent.
 */
class GoluNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val item = sbn ?: return
        if (item.packageName == packageName) return
        val extras = item.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (title.isBlank() && text.isBlank()) return

        val entry =
            CapturedDeviceNotification(
                key = item.key,
                packageName = item.packageName,
                title = title.ifBlank { item.packageName }.take(180),
                body = text.take(500),
                postedAt = item.postTime,
            )
        val updated = (listOf(entry) + _recentNotifications.value.filterNot { it.key == entry.key }).take(25)
        _recentNotifications.value = updated
    }

    companion object {
        private val _recentNotifications =
            MutableStateFlow<List<CapturedDeviceNotification>>(emptyList())
        val recentNotifications: StateFlow<List<CapturedDeviceNotification>> =
            _recentNotifications.asStateFlow()

        fun isNotificationAccessGranted(context: Context): Boolean {
            val flat =
                Settings.Secure.getString(
                    context.contentResolver,
                    "enabled_notification_listeners",
                ) ?: return false
            val component = ComponentName(context, GoluNotificationListenerService::class.java).flattenToString()
            return flat.split(":").any { it.equals(component, ignoreCase = true) }
        }

        fun clearBuffer() {
            _recentNotifications.value = emptyList()
        }
    }
}
