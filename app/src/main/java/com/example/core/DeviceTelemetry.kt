package com.example.core

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BatteryTelemetry(
    val levelPercent: Int,
    val isCharging: Boolean,
)

object DeviceTelemetry {
    fun getBatteryTelemetry(context: Context): BatteryTelemetry {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val percent =
            if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else 100
        val charging =
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        return BatteryTelemetry(levelPercent = percent, isCharging = charging)
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun formattedTime(now: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(now))

    fun formattedDate(now: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(now))
}
