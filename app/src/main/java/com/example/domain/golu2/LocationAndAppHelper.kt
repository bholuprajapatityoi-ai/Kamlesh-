package com.example.domain.golu2

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.example.domain.model.InstalledAppInfo
import com.google.android.gms.location.LocationServices
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

data class DeviceCoordinates(
    val latitude: Double,
    val longitude: Double,
)

object LocationAndAppHelper {

    fun queryLauncherApps(context: Context): List<InstalledAppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        return resolved
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                val label = info.loadLabel(pm)?.toString()?.trim().orEmpty().ifBlank { pkg }
                InstalledAppInfo(appName = label, packageName = pkg)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
    }

    fun launchPackage(context: Context, packageName: String): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return true
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fine =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val coarse =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownCoordinates(context: Context): DeviceCoordinates? {
        if (!hasLocationPermission(context)) return null
        val fused =
            runCatching {
                suspendCancellableCoroutine<DeviceCoordinates?> { cont ->
                    LocationServices.getFusedLocationProviderClient(context)
                        .lastLocation
                        .addOnSuccessListener { loc ->
                            if (cont.isActive) {
                                cont.resume(loc?.let { DeviceCoordinates(it.latitude, it.longitude) })
                            }
                        }
                        .addOnFailureListener {
                            if (cont.isActive) cont.resume(null)
                        }
                }
            }.getOrNull()
        if (fused != null) return fused

        return runCatching {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            val netLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val chosen = gpsLoc ?: netLoc
            chosen?.let { DeviceCoordinates(it.latitude, it.longitude) }
        }.getOrNull()
    }
}
