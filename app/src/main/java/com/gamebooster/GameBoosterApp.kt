package com.gamebooster

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class GameBoosterApp : Application() {
    companion object {
        const val CHANNEL_BOOST = "boost_channel"
        const val CHANNEL_MONITOR = "monitor_channel"
        const val NOTIFICATION_BOOST = 1001
        const val NOTIFICATION_MONITOR = 1002
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val boostChannel = NotificationChannel(CHANNEL_BOOST, "⚡ Game Boost", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows game boost status"
                setShowBadge(false)
            }
            val monitorChannel = NotificationChannel(CHANNEL_MONITOR, "📊 System Monitor", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows system monitoring stats"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(boostChannel)
            manager.createNotificationChannel(monitorChannel)
        }
    }
}
