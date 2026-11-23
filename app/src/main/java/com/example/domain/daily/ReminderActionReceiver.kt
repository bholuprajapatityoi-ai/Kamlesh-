package com.example.domain.daily

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "GOLU Reminder"
        val notes = intent.getStringExtra(EXTRA_NOTES) ?: ""

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (notificationId != 0) {
            nm?.cancel(notificationId)
        }

        if (intent.action == ACTION_SNOOZE_10M) {
            val data =
                Data.Builder()
                    .putString(ReminderWorker.KEY_TITLE, title)
                    .putString(ReminderWorker.KEY_NOTES, notes)
                    .putInt(ReminderWorker.KEY_NOTIF_ID, notificationId.takeIf { it != 0 } ?: 7101)
                    .build()
            val req =
                OneTimeWorkRequestBuilder<ReminderWorker>()
                    .setInitialDelay(10, TimeUnit.MINUTES)
                    .setInputData(data)
                    .build()
            WorkManager.getInstance(context.applicationContext).enqueue(req)
        }
    }

    companion object {
        const val ACTION_MARK_DONE = "com.example.action.REMINDER_DONE"
        const val ACTION_SNOOZE_10M = "com.example.action.REMINDER_SNOOZE_10M"
        const val EXTRA_NOTIFICATION_ID = "extra_notif_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_NOTES = "extra_notes"
    }
}
