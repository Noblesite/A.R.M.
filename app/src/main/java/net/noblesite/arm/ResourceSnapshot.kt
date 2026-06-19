package net.noblesite.arm

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.BatteryManager

data class ResourceSnapshot(
    val capturedAtMillis: Long,
    val availableMemoryBytes: Long,
    val totalMemoryBytes: Long,
    val lowMemory: Boolean,
    val batteryPercent: Int?,
    val batteryCharging: Boolean?,
    val totalRxBytes: Long?,
    val totalTxBytes: Long?
)

class ResourceSnapshotCollector(
    private val context: Context,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {
    fun collect(capturedAtMillis: Long = currentTimeMillis()): ResourceSnapshot {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val batteryManager = context.getSystemService(BatteryManager::class.java)
        val batteryPercent = batteryManager
            .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            .takeIf { it >= 0 }
        val chargingStatus = context
            .registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val batteryCharging = when (chargingStatus) {
            BatteryManager.BATTERY_STATUS_CHARGING,
            BatteryManager.BATTERY_STATUS_FULL -> true
            BatteryManager.BATTERY_STATUS_DISCHARGING,
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> false
            else -> null
        }

        return ResourceSnapshot(
            capturedAtMillis = capturedAtMillis,
            availableMemoryBytes = memoryInfo.availMem,
            totalMemoryBytes = memoryInfo.totalMem,
            lowMemory = memoryInfo.lowMemory,
            batteryPercent = batteryPercent,
            batteryCharging = batteryCharging,
            totalRxBytes = TrafficStats.getTotalRxBytes().knownTrafficBytesOrNull(),
            totalTxBytes = TrafficStats.getTotalTxBytes().knownTrafficBytesOrNull()
        )
    }
}

fun Long.knownTrafficBytesOrNull(): Long? {
    return takeUnless { it == TrafficStats.UNSUPPORTED.toLong() }
}

fun formatBytes(bytes: Long): String {
    val sign = if (bytes < 0L) "-" else ""
    val absoluteBytes = kotlin.math.abs(bytes)
    return when {
        absoluteBytes >= 1024L * 1024L * 1024L -> {
            "$sign${absoluteBytes / (1024L * 1024L * 1024L)} GB"
        }

        absoluteBytes >= 1024L * 1024L -> {
            "$sign${absoluteBytes / (1024L * 1024L)} MB"
        }

        absoluteBytes >= 1024L -> {
            "$sign${absoluteBytes / 1024L} KB"
        }

        else -> "$sign$absoluteBytes B"
    }
}

fun formatOptionalBytes(bytes: Long?): String {
    return bytes?.let(::formatBytes) ?: "Unavailable"
}

fun formatBatteryPercent(percent: Int?): String {
    return percent?.let { "$it%" } ?: "Unavailable"
}
