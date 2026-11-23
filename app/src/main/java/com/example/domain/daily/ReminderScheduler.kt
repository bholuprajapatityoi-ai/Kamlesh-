package com.example.domain.daily

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import java.util.concurrent.TimeUnit

class ReminderWorker(
    private val appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: "GOLU Reminder"
        val notes = inputData.getString(KEY_NOTES) ?: "Boss, aapka scheduled reminder!"
        val notifId = inputData.getInt(KEY_NOTIF_ID, 7100)
        ReminderScheduler.postReminderNotification(appContext, notifId, title, notes)
        return Result.success()
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_NOTES = "notes"
        const val KEY_NOTIF_ID = "notif_id"
    }
}

object ReminderScheduler {
    const val CHANNEL_ID = "golu_daily_reminders"

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return am.canScheduleExactAlarms()
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch =
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.reminder_channel_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Reminders, daily routines, and scheduled GOLU notifications"
                }
            nm.createNotificationChannel(ch)
        }
    }

    fun scheduleReminderWork(
        context: Context,
        reminderId: String,
        title: String,
        notes: String,
        delayMinutes: Long,
        recurringDaily: Boolean = false,
    ) {
        val notifId = reminderId.hashCode().let { if (it == 0) 7101 else kotlin.math.abs(it) }
        val input =
            Data.Builder()
                .putString(ReminderWorker.KEY_TITLE, title)
                .putString(ReminderWorker.KEY_NOTES, notes.ifBlank { "GOLU Boss Reminder" })
                .putInt(ReminderWorker.KEY_NOTIF_ID, notifId)
                .build()

        val wm = WorkManager.getInstance(context.applicationContext)
        if (recurringDaily) {
            val req =
                PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(delayMinutes.coerceAtLeast(1), TimeUnit.MINUTES)
                    .setInputData(input)
                    .build()
            wm.enqueueUniquePeriodicWork("reminder_$reminderId", ExistingPeriodicWorkPolicy.UPDATE, req)
        } else {
            val req =
                OneTimeWorkRequestBuilder<ReminderWorker>()
                    .setInitialDelay(delayMinutes.coerceAtLeast(0), TimeUnit.MINUTES)
                    .setInputData(input)
                    .build()
            wm.enqueueUniqueWork("reminder_$reminderId", ExistingWorkPolicy.REPLACE, req)
        }
    }

    fun postReminderNotification(
        context: Context,
        notifId: Int,
        title: String,
        body: String,
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannel(context)

        val openIntent =
            PendingIntent.getActivity(
                context,
                notifId,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val doneIntent =
            PendingIntent.getBroadcast(
                context,
                notifId + 1,
                Intent(context, ReminderActionReceiver::class.java).apply {
                    action = ReminderActionReceiver.ACTION_MARK_DONE
                    putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notifId)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val snoozeIntent =
            PendingIntent.getBroadcast(
                context,
                notifId + 2,
                Intent(context, ReminderActionReceiver::class.java).apply {
                    action = ReminderActionReceiver.ACTION_SNOOZE_10M
                    putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notifId)
                    putExtra(ReminderActionReceiver.EXTRA_TITLE, title)
                    putExtra(ReminderActionReceiver.EXTRA_NOTES, body)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notif =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openIntent)
                .addAction(0, "Done", doneIntent)
                .addAction(0, "Snooze 10m", snoozeIntent)
                .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notifId, notif)
    }
}
