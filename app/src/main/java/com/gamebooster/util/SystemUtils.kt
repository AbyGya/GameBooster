package com.gamebooster.util

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

object SystemUtils {

    data class MemoryInfo(
        val totalRam: Long, val usedRam: Long, val freeRam: Long,
        val usagePercent: Int, val availableApps: Int
    )

    data class CpuInfo(
        val usagePercent: Float, val coreCount: Int, val maxFreq: Long,
        val minFreq: Long, val temperature: Float
    )

    data class BatteryInfo(
        val level: Int, val temperature: Float, val isCharging: Boolean,
        val health: String, val voltage: Int
    )

    data class StorageInfo(
        val totalStorage: Long, val usedStorage: Long, val freeStorage: Long, val usagePercent: Int
    )

    data class PerformanceData(
        val memoryInfo: MemoryInfo, val cpuInfo: CpuInfo,
        val batteryInfo: BatteryInfo, val storageInfo: StorageInfo,
        val gameModeActive: Boolean, val networkOptimized: Boolean
    )

    fun getMemoryInfo(context: Context): MemoryInfo {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val totalRam = memInfo.totalMem
        val freeRam = memInfo.availMem
        val usedRam = totalRam - freeRam
        val usagePercent = ((usedRam.toFloat() / totalRam) * 100).toInt()
        val runningProcesses = am.runningAppProcesses?.size ?: 0
        return MemoryInfo(totalRam, usedRam, freeRam, usagePercent, runningProcesses)
    }

    fun getCpuInfo(): CpuInfo {
        return CpuInfo(getCpuUsage(), Runtime.getRuntime().availableProcessors(), getCpuMaxFreq(), getCpuMinFreq(), getCpuTemperature())
    }

    private fun getCpuUsage(): Float {
        return try {
            val r = RandomAccessFile("/proc/stat", "r")
            val line = r.readLine(); r.close()
            val p = line.split("\\s+".toRegex())
            val idle1 = p[4].toLong(); val total1 = p.drop(1).take(7).sumOf { it.toLong() }
            Thread.sleep(100)
            val r2 = RandomAccessFile("/proc/stat", "r")
            val line2 = r2.readLine(); r2.close()
            val p2 = line2.split("\\s+".toRegex())
            val idle2 = p2[4].toLong(); val total2 = p2.drop(1).take(7).sumOf { it.toLong() }
            val idleDelta = idle2 - idle1; val totalDelta = total2 - total1
            if (totalDelta > 0) ((totalDelta - idleDelta).toFloat() / totalDelta * 100).coerceIn(0f, 100f) else 0f
        } catch (e: Exception) { 0f }
    }

    private fun getCpuMaxFreq(): Long {
        return try { RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq", "r").use { it.readLine().trim().toLong() / 1000 } } catch (e: Exception) { 0L }
    }

    private fun getCpuMinFreq(): Long {
        return try { RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq", "r").use { it.readLine().trim().toLong() / 1000 } } catch (e: Exception) { 0L }
    }

    private fun getCpuTemperature(): Float {
        val files = listOf("/sys/class/thermal/thermal_zone0/temp", "/sys/devices/virtual/thermal/thermal_zone0/temp")
        for (f in files) try { RandomAccessFile(f, "r").use { return it.readLine().trim().toFloat() / 1000f } } catch (_: Exception) {}
        return -1f
    }

    fun getBatteryInfo(context: Context): BatteryInfo {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val temperature = try { bm.getIntProperty(2) / 10f } catch (_: Exception) { -1f }
        val isCharging = bm.isCharging
        val voltage = try { bm.getIntProperty(3) } catch (_: Exception) { 0 }
        val health = try {
            when (bm.getIntProperty(1)) { 1 -> "Good"; 2 -> "Overheat"; 3 -> "Dead"; 4 -> "Over Voltage"; 5 -> "Failure"; else -> "Unknown" }
        } catch (_: Exception) { "Unknown" }
        return BatteryInfo(level, temperature, isCharging, health, voltage)
    }

    fun getStorageInfo(): StorageInfo {
        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong; val totalBlocks = stat.blockCountLong; val availableBlocks = stat.availableBlocksLong
        val totalStorage = totalBlocks * blockSize; val freeStorage = availableBlocks * blockSize; val usedStorage = totalStorage - freeStorage
        val usagePercent = ((usedStorage.toFloat() / totalStorage) * 100).toInt()
        return StorageInfo(totalStorage, usedStorage, freeStorage, usagePercent)
    }

    fun getInstalledGames(context: Context): List<GameInfo> {
        val pm = context.packageManager
        val installed = pm.getInstalledPackages(0)
        val keywords = listOf("roblox", "freefire", "free.fire", "garena", "pubg", "mobile.legend",
            "mlbb", "cod", "callofduty", "fortnite", "genshin", "minecraft", "asphalt",
            "clash", "brawl", "arena", "survival", "racing", "shooter", "rpg", "mmo", "battle",
            "royale", "stumble", "among", "fall.guys", "codmobile", "apex", "naruto", "dragon",
            "onepiece", "fft", "ffexacts", "freefiremax"
        )
        return installed.mapNotNull { pkg ->
            val appName = pkg.applicationInfo.loadLabel(pm).toString().lowercase()
            val pkgName = pkg.packageName.lowercase()
            val isGame = keywords.any { appName.contains(it) || pkgName.contains(it) }
            if (isGame) {
                GameInfo(pkg.applicationInfo.loadLabel(pm).toString(), pkg.packageName, pkg.applicationInfo.loadIcon(pm), getLastUsedTime(context, pkg.packageName))
            } else null
        }.sortedByDescending { it.lastUsed }
    }

    private fun getLastUsedTime(context: Context, packageName: String): Long {
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager ?: return 0L
            val stats = usm.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30), System.currentTimeMillis())
            stats?.find { it.packageName == packageName }?.lastTimeUsed ?: 0L
        } catch (e: Exception) { 0L }
    }

    fun formatSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble(); var idx = 0
        while (value >= 1024 && idx < units.size - 1) { value /= 1024; idx++ }
        return "%.1f %s".format(value, units[idx])
    }

    fun formatDuration(millis: Long): String {
        val h = TimeUnit.MILLISECONDS.toHours(millis); val m = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    fun getTotalApps(context: Context): Int {
        val pm = context.packageManager
        return pm.getInstalledPackages(PackageManager.GET_META_DATA).size
    }

    fun getRunningApps(context: Context): Int {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.runningAppProcesses?.size ?: 0
    }

    suspend fun forcePerformanceMode(context: Context): Boolean {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                Runtime.getRuntime().exec(arrayOf("settings", "put", "global", "screen_off_timeout", "600000"))
                Runtime.getRuntime().exec(arrayOf("settings", "put", "global", "window_animation_scale", "0.5"))
                Runtime.getRuntime().exec(arrayOf("settings", "put", "global", "transition_animation_scale", "0.5"))
                Runtime.getRuntime().exec(arrayOf("settings", "put", "global", "animator_duration_scale", "0.5"))
                true
            } catch (e: Exception) { false }
        }
    }

    suspend fun optimizeNetwork(context: Context): Boolean {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                Runtime.getRuntime().exec(arrayOf("settings", "put", "global", "private_dns_mode", "off"))
                true
            } catch (e: Exception) { false }
        }
    }
}

data class GameInfo(
    val name: String, val packageName: String, val icon: android.graphics.drawable.Drawable?, val lastUsed: Long
)
