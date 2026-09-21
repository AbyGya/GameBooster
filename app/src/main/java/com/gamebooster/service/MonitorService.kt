package com.gamebooster.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.gamebooster.GameBoosterApp
import com.gamebooster.R
import com.gamebooster.ui.MainActivity
import kotlinx.coroutines.*

class MonitorService : Service() {

    companion object {
        private var isMonitoring = false
        fun isRunning() = isMonitoring
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isMonitoring = true
        startForeground(GameBoosterApp.NOTIFICATION_MONITOR, createMonitorNotification("Monitoring..."))
        return START_STICKY
    }

    private fun createMonitorNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, GameBoosterApp.CHANNEL_MONITOR)
            .setContentTitle("System Monitor")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_monitor)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isMonitoring = false
        scope.cancel()
    }
}
