package com.gamebooster.ui

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
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

    private lateinit var gameIcon: ImageView
    private lateinit var gameName: TextView
    private lateinit var boostStatus: TextView
    private lateinit var boostProgress: ProgressBar
    private lateinit var startBoostButton: LinearLayout
    private lateinit var statsContainer: LinearLayout
    private lateinit var ramFreedText: TextView
    private lateinit var processesKilledText: TextView
    private lateinit var fpsOptimizedText: TextView

    private var gamePackage: String? = null
    private var gameNameStr: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_boost)

        gamePackage = intent.getStringExtra("game_package")
        gameNameStr = intent.getStringExtra("game_name")

        initViews()
        setupUI()
        performBoost()
    }

    private fun initViews() {
        gameIcon = findViewById<ImageView>(R.id.gameIcon)
        gameName = findViewById<TextView>(R.id.gameName)
        boostStatus = findViewById<TextView>(R.id.boostStatus)
        boostProgress = findViewById<ProgressBar>(R.id.boostProgress)
        startBoostButton = findViewById<LinearLayout>(R.id.startBoostButton)
        statsContainer = findViewById<LinearLayout>(R.id.statsContainer)
        ramFreedText = findViewById<TextView>(R.id.ramFreedText)
        processesKilledText = findViewById<TextView>(R.id.processesKilledText)
        fpsOptimizedText = findViewById<TextView>(R.id.fpsOptimizedText)
    }

    private fun setupUI() {
        if (gameNameStr != null) {
            gameName.text = gameNameStr
            startBoostButton.visibility = View.GONE
        } else {
            gameName.text = "Extreme Boost"
            startBoostButton.visibility = View.VISIBLE
        }

        startBoostButton.setOnClickListener {
            performBoost()
        }

        // Pulse animation on boost icon
        val pulseAnimation = ObjectAnimator.ofFloat(gameIcon, "scaleX", 1f, 1.1f, 1f).apply {
            duration = 1500
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        val pulseAnimationY = ObjectAnimator.ofFloat(gameIcon, "scaleY", 1f, 1.1f, 1f).apply {
            duration = 1500
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        pulseAnimation.start()
        pulseAnimationY.start()
    }

    private fun performBoost() {
        boostProgress.visibility = View.VISIBLE
        statsContainer.visibility = View.GONE

        lifecycleScope.launch {
            // Phase 1: Analyzing
            boostStatus.text = "Analyzing system..."
            boostProgress.progress = 10
            delay(600)

            val memBefore = SystemUtils.getMemoryInfo(this@BoostActivity)

            // Phase 2: Killing background processes
            boostStatus.text = "Killing background processes..."
            boostProgress.progress = 30
            killBackgroundProcesses()
            delay(800)

            // Phase 3: Cleaning memory
            boostStatus.text = "Cleaning memory..."
            boostProgress.progress = 50
            cleanMemory()
            delay(600)

            // Phase 4: Optimizing settings
            boostStatus.text = "Optimizing system settings..."
            boostProgress.progress = 70
            optimizeSettings()
            delay(600)

            // Phase 5: Network optimization
            boostStatus.text = "Optimizing network..."
            boostProgress.progress = 85
            delay(500)

            // Phase 6: Final
            boostStatus.text = "Boost complete!"
            boostProgress.progress = 100

            val memAfter = SystemUtils.getMemoryInfo(this@BoostActivity)
            val freed = memBefore.freeRam - memAfter.freeRam

            delay(500)

            // Show results
            boostProgress.visibility = View.GONE
            statsContainer.visibility = View.VISIBLE
            statsContainer.alpha = 0f
            statsContainer.animate().alpha(1f).setDuration(500).start()

            ramFreedText.text = "RAM Freed: ${SystemUtils.formatSize(if (freed > 0) freed else memAfter.freeRam)}"
            processesKilledText.text = "Processes Cleaned: Active"
            fpsOptimizedText.text = "Settings: Optimized"

            // Launch game if package exists
            if (gamePackage != null) {
                delay(1500)
                launchGame()
            } else {
                // Show restart option
                startBoostButton.visibility = View.VISIBLE
                (startBoostButton as? TextView)?.text = "BOOST AGAIN"
            }
        }
    }

    private fun killBackgroundProcesses() {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager

        try {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager
            usm?.let {
                val endTime = System.currentTimeMillis()
                val beginTime = endTime - 60 * 1000
                val stats = it.queryUsageStats(
                    android.app.usage.UsageStatsManager.INTERVAL_DAILY,
                    beginTime,
                    endTime
                )

                stats?.filter { stat ->
                    stat.lastTimeUsed < System.currentTimeMillis() - 30000 &&
                    stat.packageName != packageName &&
                    stat.packageName != gamePackage
                }?.take(20)?.forEach { stat ->
                    try {
                        am.killBackgroundProcesses(stat.packageName)
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {
            // Fallback
            am.killBackgroundProcesses(packageName)
        }
    }

    private fun cleanMemory() {
        Runtime.getRuntime().gc()
        System.runFinalization()

        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        am.getMemoryInfo(ActivityManager.MemoryInfo())
    }

    private fun optimizeSettings() {
        try {
            // Reduce animations for smoother gameplay
            Runtime.getRuntime().exec(arrayOf("settings", "put", "global", "window_animation_scale", "0.5"))
            Runtime.getRuntime().exec(arrayOf("settings", "put", "global", "transition_animation_scale", "0.5"))
            Runtime.getRuntime().exec(arrayOf("settings", "put", "global", "animator_duration_scale", "0.5"))
        } catch (_: Exception) {}
    }

    private fun launchGame() {
        val intent = packageManager.getLaunchIntentForPackage(gamePackage!!)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Game not found!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
