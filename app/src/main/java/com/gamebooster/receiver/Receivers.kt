package com.gamebooster.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Auto-start monitoring on boot if enabled
        }
    }
}

class BoostReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra("package")
        if (packageName != null) {
            val serviceIntent = Intent(context, com.gamebooster.service.BoosterService::class.java).apply {
                action = com.gamebooster.service.BoosterService.ACTION_BOOST
                putExtra(com.gamebooster.service.BoosterService.EXTRA_GAME_PACKAGE, packageName)
            }
            context.startForegroundService(serviceIntent)
        }
    }
}
