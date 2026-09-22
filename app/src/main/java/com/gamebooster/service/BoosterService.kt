package com.gamebooster.service

import android.app.ActivityManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
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

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_BOOST -> startBoost(intent.getStringExtra(EXTRA_GAME_PACKAGE))
            ACTION_STOP_BOOST -> stopBoost()
        }
        return START_STICKY
    }

    private fun startBoost(gamePackage: String?) {
        if (isBoosting) return
        isBoosting = true
        startForeground(GameBoosterApp.NOTIFICATION_BOOST, createNotification("⚡ Boosting..."))

        boostJob = scope.launch {
            killBackgroundProcesses()
            delay(500)
            cleanMemory()
            delay(500)
            optimizePerformance()
            if (gamePackage != null) { delay(1000); launchGame(gamePackage) }
            while (isActive && isBoosting) { cleanMemory(); delay(5000) }
        }
    }

    private fun killBackgroundProcesses() {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        am.killBackgroundProcesses(packageName)
        try {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager
            usm?.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, System.currentTimeMillis() - 60000, System.currentTimeMillis())
                ?.filter { it.lastTimeUsed < System.currentTimeMillis() - 30000 && it.packageName != packageName }
                ?.take(20)?.forEach { try { am.killBackgroundProcesses(it.packageName) } catch (_: Exception) {} }
        } catch (_: Exception) {}
    }

    private fun cleanMemory() { Runtime.getRuntime().gc(); System.runFinalization() }

    private fun optimizePerformance() {
        try {
            Runtime.getRuntime().exec(arrayOf("settings", "put", "global", "window_animation_scale", "0.5"))
            Runtime.getRuntime().exec(arrayOf("settings", "put", "global", "transition_animation_scale", "0.5"))
            Runtime.getRuntime().exec(arrayOf("settings", "put", "global", "animator_duration_scale", "0.5"))
            Runtime.getRuntime().exec(arrayOf("settings", "put", "global", "background_process_limit", "2"))
            Runtime.getRuntime().exec(arrayOf("settings", "put", "global", "screen_off_timeout", "600000"))
        } catch (_: Exception) {}
    }

    private fun launchGame(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun createNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, GameBoosterApp.CHANNEL_BOOST)
            .setContentTitle("⚡ Game Booster")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_boost)
            .setContentIntent(pi)
            .setOngoing(true).setSilent(true).build()
    }

    private fun stopBoost() {
        isBoosting = false; boostJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy(); isBoosting = false; scope.cancel()
    }
}
