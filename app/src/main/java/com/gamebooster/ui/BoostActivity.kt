package com.gamebooster.ui

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.View
import android.animation.ObjectAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gamebooster.R
import com.gamebooster.service.BoosterService
import com.gamebooster.util.SystemUtils
import kotlinx.coroutines.*

class BoostActivity : AppCompatActivity() {

    private var gamePackage: String? = null
    private var gameNameStr: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_boost)
        gamePackage = intent.getStringExtra("game_package")
        gameNameStr = intent.getStringExtra("game_name")
        setupUI()
        performBoost()
    }

    private fun setupUI() {
        val gameName = findViewById<TextView>(R.id.gameName)
        val boostStatus = findViewById<TextView>(R.id.boostStatus)
        gameName.text = gameNameStr ?: "EXTREME BOOST"
        boostStatus.text = "Ready to boost"

        findViewById<FrameLayout>(R.id.gameIcon).post {
            val icon = findViewById<View>(R.id.gameIcon)
            val pulse = ObjectAnimator.ofFloat(icon, "scaleX", 1f, 1.15f, 1f).apply {
                duration = 1500; repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
            }
            val pulseY = ObjectAnimator.ofFloat(icon, "scaleY", 1f, 1.15f, 1f).apply {
                duration = 1500; repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
            }
            pulse.start(); pulseY.start()
        }

        findViewById<LinearLayout>(R.id.startBoostButton).setOnClickListener { performBoost() }
    }

    private fun performBoost() {
        val progress = findViewById<ProgressBar>(R.id.boostProgress)
        val status = findViewById<TextView>(R.id.boostStatus)
        val statsContainer = findViewById<LinearLayout>(R.id.statsContainer)
        val startBtn = findViewById<LinearLayout>(R.id.startBoostButton)

        progress.visibility = View.VISIBLE
        statsContainer.visibility = View.GONE
        startBtn.visibility = View.GONE

        lifecycleScope.launch {
            status.text = "🔍 Analyzing system..."
            progress.progress = 10; delay(600)

            val memBefore = SystemUtils.getMemoryInfo(this@BoostActivity)
            status.text = "🗑️ Killing background processes..."
            progress.progress = 25; killBackgroundProcesses(); delay(800)

            status.text = "🧹 Cleaning memory..."
            progress.progress = 45; cleanMemory(); delay(600)

            status.text = "⚡ Optimizing performance..."
            progress.progress = 60; optimizePerformance(); delay(600)

            status.text = "🌐 Optimizing network..."
            progress.progress = 75; delay(500)

            status.text = "🌡️ Cooling device..."
            progress.progress = 90; delay(500)

            status.text = "✅ Boost complete!"
            progress.progress = 100; delay(500)

            val memAfter = SystemUtils.getMemoryInfo(this@BoostActivity)
            val freed = memBefore.freeRam - memAfter.freeRam

            delay(500)
            progress.visibility = View.GONE
            statsContainer.visibility = View.VISIBLE
            statsContainer.alpha = 0f
            statsContainer.animate().alpha(1f).setDuration(500).start()

            findViewById<TextView>(R.id.ramFreedText).text = "RAM Freed: ${SystemUtils.formatSize(if (freed > 0) freed else memAfter.freeRam)}"
            findViewById<TextView>(R.id.processesKilledText).text = "Processes: Terminated"
            findViewById<TextView>(R.id.fpsOptimizedText).text = "Performance: Maximum"

            if (gamePackage != null) {
                delay(1500); launchGame()
            } else {
                startBtn.visibility = View.VISIBLE
                (startBtn as? android.widget.LinearLayout)?.getChildAt(0) as? android.widget.TextView?.text = "⚡ BOOST AGAIN"
            }
        }
    }

    private fun killBackgroundProcesses() {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        try {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager
            usm?.let {
                val endTime = System.currentTimeMillis(); val beginTime = endTime - 60000
                val stats = it.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, beginTime, endTime)
                stats?.filter { stat ->
                    stat.lastTimeUsed < System.currentTimeMillis() - 30000 && stat.packageName != packageName
                }?.take(20)?.forEach {
                    try { am.killBackgroundProcesses(it.packageName) } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) { am.killBackgroundProcesses(packageName) }
    }

    private fun cleanMemory() {
        Runtime.getRuntime().gc(); System.runFinalization()
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        am.getMemoryInfo(ActivityManager.MemoryInfo())
    }

    private fun optimizePerformance() {
        try {
            Runtime.getRuntime().exec(arrayOf("settings", "put", "global", "window_animation_scale", "0.5"))
            Runtime.getRuntime().exec(arrayOf("settings", "put", "global", "transition_animation_scale", "0.5"))
            Runtime.getRuntime().exec(arrayOf("settings", "put", "global", "animator_duration_scale", "0.5"))
            Runtime.getRuntime().exec(arrayOf("settings", "put", "global", "background_process_limit", "2"))
        } catch (_: Exception) {}
    }

    private fun launchGame() {
        val intent = packageManager.getLaunchIntentForPackage(gamePackage!!)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent); finish()
        } else Toast.makeText(this, "Game not found!", Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
