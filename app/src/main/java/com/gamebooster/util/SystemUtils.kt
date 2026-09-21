package com.gamebooster.util

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

object SystemUtils {

    data class MemoryInfo(
        val totalRam: Long,
        val usedRam: Long,
        val freeRam: Long,
        val usagePercent: Int,
        val availableApps: Int
    )

    data class CpuInfo(
        val usagePercent: Float,
        val coreCount: Int,
        val maxFreq: Long,
        val minFreq: Long,
        val temperature: Float
    )

    data class BatteryInfo(
        val level: Int,
        val temperature: Float,
        val isCharging: Boolean,
        val health: String,
        val voltage: Int
    )

    data class StorageInfo(
        val totalStorage: Long,
        val usedStorage: Long,
        val freeStorage: Long,
        val usagePercent: Int
    )

    fun getMemoryInfo(context: Context): MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val totalRam = memInfo.totalMem
        val freeRam = memInfo.availMem
        val usedRam = totalRam - freeRam
        val usagePercent = ((usedRam.toFloat() / totalRam) * 100).toInt()

        val runningProcesses = activityManager.runningAppProcesses?.size ?: 0

        return MemoryInfo(totalRam, usedRam, freeRam, usagePercent, runningProcesses)
    }

    fun getCpuInfo(): CpuInfo {
        val usage = getCpuUsage()
        val coreCount = Runtime.getRuntime().availableProcessors()
        val maxFreq = getCpuMaxFreq()
        val minFreq = getCpuMinFreq()
        val temperature = getCpuTemperature()

        return CpuInfo(usage, coreCount, maxFreq, minFreq, temperature)
    }

    private fun getCpuUsage(): Float {
        return try {
            val reader = RandomAccessFile("/proc/stat", "r")
            val line = reader.readLine()
            reader.close()

            val parts = line.split("\\s+".toRegex())
            val idle1 = parts[4].toLong()
            val total1 = parts.drop(1).take(7).sumOf { it.toLong() }

            Thread.sleep(100)

            val reader2 = RandomAccessFile("/proc/stat", "r")
            val line2 = reader2.readLine()
            reader2.close()

            val parts2 = line2.split("\\s+".toRegex())
            val idle2 = parts2[4].toLong()
            val total2 = parts2.drop(1).take(7).sumOf { it.toLong() }

            val idleDelta = idle2 - idle1
            val totalDelta = total2 - total1

            if (totalDelta > 0) {
                ((totalDelta - idleDelta).toFloat() / totalDelta * 100).coerceIn(0f, 100f)
            } else 0f
        } catch (e: Exception) {
            0f
        }
    }

    private fun getCpuMaxFreq(): Long {
        return try {
            val reader = RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq", "r")
            val freq = reader.readLine().trim().toLong() / 1000
            reader.close()
            freq
        } catch (e: Exception) {
            0L
        }
    }

    private fun getCpuMinFreq(): Long {
        return try {
            val reader = RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq", "r")
            val freq = reader.readLine().trim().toLong() / 1000
            reader.close()
            freq
        } catch (e: Exception) {
            0L
        }
    }

    private fun getCpuTemperature(): Float {
        val thermalFiles = listOf(
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/devices/virtual/thermal/thermal_zone0/temp"
        )
        for (file in thermalFiles) {
            try {
                val reader = RandomAccessFile(file, "r")
                val temp = reader.readLine().trim().toFloat() / 1000f
                reader.close()
                return temp
            } catch (_: Exception) {}
        }
        return -1f
    }

    fun getBatteryInfo(context: Context): BatteryInfo {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val temperature = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_TEMPERATURE) / 10f
        val isCharging = batteryManager.isCharging
        val voltage = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_VOLTAGE)

        val health = when (batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)) {
            BatteryManager.BATTERY_STATUS_GOOD -> "Good"
            BatteryManager.BATTERY_STATUS_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_STATUS_DEAD -> "Dead"
            BatteryManager.BATTERY_STATUS_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_STATUS_UNSPECIFIED_FAILURE -> "Failure"
            else -> "Unknown"
        }

        return BatteryInfo(level, temperature, isCharging, health, voltage)
    }

    fun getStorageInfo(): StorageInfo {
        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalStorage = totalBlocks * blockSize
        val freeStorage = availableBlocks * blockSize
        val usedStorage = totalStorage - freeStorage
        val usagePercent = ((usedStorage.toFloat() / totalStorage) * 100).toInt()

        return StorageInfo(totalStorage, usedStorage, freeStorage, usagePercent)
    }

    fun getInstalledGames(context: Context): List<GameInfo> {
        val packageManager = context.packageManager
        val installedPackages = packageManager.getInstalledPackages(0)

        val gameKeywords = listOf(
            "roblox", "freefire", "free.fire", "garena", "pubg", "mobile.legend",
            "mobile.legends", "mlbb", "cod", "callofduty", "fortnite", "genshin",
            "minecraft", "asphalt", "clash", "brawl", "arena", "survival",
            "racing", "shooter", "rpg", "mmo", "battle", "royale", "stumble",
            "among", "fall.guys", "codmobile", "apex", "naruto", "dragon",
            "onepiece", "fft", "ffexacts", "freefiremax"
        )

        return installedPackages.mapNotNull { packageInfo ->
            val appName = packageInfo.applicationInfo.loadLabel(packageManager).toString().lowercase()
            val packageName = packageInfo.packageName.lowercase()

            val isGame = gameKeywords.any { keyword ->
                appName.contains(keyword) || packageName.contains(keyword)
            } || (packageInfo.applicationInfo.category == 0) // CATEGORY_GAME

            if (isGame) {
                val icon = try {
                    packageInfo.applicationInfo.loadIcon(packageManager)
                } catch (e: Exception) {
                    null
                }
                GameInfo(
                    name = packageInfo.applicationInfo.loadLabel(packageManager).toString(),
                    packageName = packageInfo.packageName,
                    icon = icon,
                    lastUsed = getLastUsedTime(context, packageInfo.packageName)
                )
            } else null
        }.sortedByDescending { it.lastUsed }
    }

    private fun getLastUsedTime(context: Context, packageName: String): Long {
        return try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE)
                as? android.app.usage.UsageStatsManager ?: return 0L

            val endTime = System.currentTimeMillis()
            val beginTime = endTime - TimeUnit.DAYS.toMillis(30)

            val stats = usageStatsManager.queryUsageStats(
                android.app.usage.UsageStatsManager.INTERVAL_DAILY,
                beginTime,
                endTime
            )

            stats?.find { it.packageName == packageName }?.lastTimeUsed ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun formatSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unitIndex = 0
        while (value >= 1024 && unitIndex < units.size - 1) {
            value /= 1024
            unitIndex++
        }
        return "%.1f %s".format(value, units[unitIndex])
    }

    fun formatDuration(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}

data class GameInfo(
    val name: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable?,
    val lastUsed: Long
)
