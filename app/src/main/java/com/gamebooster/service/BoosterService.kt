package com.gamebooster.service

import android.app.ActivityManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.app.usage.UsageStatsManager
import androidx.core.app.NotificationCompat
import com.gamebooster.GameBoosterApp
import com.gamebooster.R
import com.gamebooster.ui.MainActivity
import com.gamebooster.util.SystemUtils
import kotlinx.coroutines.*

class BoosterService : Service() {

    companion object {
        const val ACTION_BOOST = "com.gamebooster.ACTION_BOOST"
        const val ACTION_STOP_BOOST = "com.gamebooster.ACTION_STOP_BOOST"
        const val EXTRA_GAME_PACKAGE = "game_package"

        private var isBoosting = false
        fun isRunning() = isBoosting
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var boostJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_BOOST -> {
                val gamePackage = intent.getStringExtra(EXTRA_GAME_PACKAGE)
                startBoost(gamePackage)
            }
            ACTION_STOP_BOOST -> {
                stopBoost()
            }
        }
        return START_STICKY
    }

    private fun startBoost(gamePackage: String?) {
        if (isBoosting) return
        isBoosting = true

        startForeground(GameBoosterApp.NOTIFICATION_BOOST, createBoostNotification("Optimizing..."))

        boostJob = scope.launch {
            // Step 1: Kill background processes
            killBackgroundProcesses()
            delay(500)

            // Step 2: Clean memory
            cleanMemory()
            delay(500)

            // Step 3: Optimize system settings
            optimizeSystem()

            // Step 4: Launch game
            if (gamePackage != null) {
                delay(1000)
                launchGame(gamePackage)
            }

            // Step 5: Continue monitoring
            startContinuousBoost()
        }
    }

    private fun killBackgroundProcesses() {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val beginTime = endTime - 60 * 1000

            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, beginTime, endTime
            )

            val currentPackage = getCurrentForegroundPackage()

            stats?.filter {
                it.lastTimeUsed < System.currentTimeMillis() - 30000 &&
                it.packageName != currentPackage &&
                it.packageName != packageName
            }?.forEach { stat ->
                try {
                    activityManager.killBackgroundProcesses(stat.packageName)
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            // Fallback: kill common background processes
            val commonBackground = listOf(
                "com.google.android.gms", "com.google.android.apps.messaging",
                "com.android.chrome", "com.google.android.googlequicksearchbox",
                "com.samsung.android.app.spage", "com.sec.android.app.sbrowser"
            )
            commonBackground.forEach { pkg ->
                try {
                    activityManager.killBackgroundProcesses(pkg)
                } catch (_: Exception) {}
            }
        }
    }

    private fun cleanMemory() {
        Runtime.getRuntime().gc()
        System.runFinalization()
    }

    private fun optimizeSystem() {
        // Reduce animation scale if possible
        try {
            Runtime.getRuntime().exec(arrayOf("settings", "put", "global", "window_animation_scale", "0.5"))
            Runtime.getRuntime().exec(arrayOf("settings", "put", "global", "transition_animation_scale", "0.5"))
            Runtime.getRuntime().exec(arrayOf("settings", "put", "global", "animator_duration_scale", "0.5"))
        } catch (_: Exception) {}
    }

    private fun launchGame(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun startContinuousBoost() {
        scope.launch {
            while (isActive && isBoosting) {
                cleanMemory()
                delay(5000) // Clean every 5 seconds
            }
        }
    }

    private fun getCurrentForegroundPackage(): String? {
        return try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val beginTime = endTime - 1000

            usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, beginTime, endTime
            )?.maxByOrNull { it.lastTimeUsed }?.packageName
        } catch (_: Exception) {
            null
        }
    }

    private fun createBoostNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, GameBoosterApp.CHANNEL_BOOST)
            .setContentTitle("Game Booster Active")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_boost)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun stopBoost() {
        isBoosting = false
        boostJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        isBoosting = false
        scope.cancel()
    }
}
