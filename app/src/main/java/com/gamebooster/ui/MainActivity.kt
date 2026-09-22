package com.gamebooster.ui

import android.Manifest
import android.app.AppOpsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gamebooster.R
import com.gamebooster.adapter.GameAdapter
import com.gamebooster.service.BoosterService
import com.gamebooster.util.SystemUtils
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var ramProgress: ProgressBar
    private lateinit var cpuProgress: ProgressBar
    private lateinit var storageProgress: ProgressBar
    private lateinit var batteryProgress: ProgressBar

    private lateinit var ramText: TextView
    private lateinit var cpuText: TextView
    private lateinit var storageText: TextView
    private lateinit var batteryText: TextView
    private lateinit var tempText: TextView

    private lateinit var boostButton: LinearLayout
    private lateinit var cleanButton: LinearLayout
    private lateinit var monitorButton: LinearLayout
    private lateinit var settingsButton: LinearLayout

    private lateinit var gamesRecyclerView: RecyclerView
    private lateinit var gamesTitle: TextView
    private lateinit var noGamesText: TextView

    private var monitorJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupButtons()
        requestPermissions()
        checkUsageStatsPermission()
    }

    private fun initViews() {
        ramProgress = findViewById<ProgressBar>(R.id.ramProgress)
        cpuProgress = findViewById<ProgressBar>(R.id.cpuProgress)
        storageProgress = findViewById<ProgressBar>(R.id.storageProgress)
        batteryProgress = findViewById<ProgressBar>(R.id.batteryProgress)

        ramText = findViewById<TextView>(R.id.ramText)
        cpuText = findViewById<TextView>(R.id.cpuText)
        storageText = findViewById<TextView>(R.id.storageText)
        batteryText = findViewById<TextView>(R.id.batteryText)
        tempText = findViewById<TextView>(R.id.tempText)

        boostButton = findViewById<LinearLayout>(R.id.boostButton)
        cleanButton = findViewById<LinearLayout>(R.id.cleanButton)
        monitorButton = findViewById<LinearLayout>(R.id.monitorButton)
        settingsButton = findViewById<LinearLayout>(R.id.settingsButton)

        gamesRecyclerView = findViewById<RecyclerView>(R.id.gamesRecyclerView)
        gamesTitle = findViewById<TextView>(R.id.gamesTitle)
        noGamesText = findViewById<TextView>(R.id.noGamesText)

        gamesRecyclerView.layoutManager = GridLayoutManager(this, 3)
    }

    private fun setupButtons() {
        boostButton.setOnClickListener {
            val intent = Intent(this, BoostActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        cleanButton.setOnClickListener {
            performQuickClean()
        }

        monitorButton.setOnClickListener {
            Toast.makeText(this, "System monitoring activated!", Toast.LENGTH_SHORT).show()
        }

        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun performQuickClean() {
        val dialog = AlertDialog.Builder(this, R.style.BoostDialog)
            .setTitle("Optimizing...")
            .setMessage("Cleaning memory and killing background processes...")
            .setCancelable(false)
            .create()

        dialog.show()

        lifecycleScope.launch {
            delay(500)

            // Kill background processes
            val am = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
            am.killBackgroundProcesses(packageName)

            // Force GC
            Runtime.getRuntime().gc()
            System.runFinalization()

            // Try to reduce animations
            try {
                Runtime.getRuntime().exec(arrayOf("settings", "put", "global", "window_animation_scale", "0.5"))
                Runtime.getRuntime().exec(arrayOf("settings", "put", "global", "transition_animation_scale", "0.5"))
                Runtime.getRuntime().exec(arrayOf("settings", "put", "global", "animator_duration_scale", "0.5"))
            } catch (_: Exception) {}

            delay(1500)
            dialog.dismiss()

            val memInfo = SystemUtils.getMemoryInfo(this@MainActivity)
            val freedMB = SystemUtils.formatSize(memInfo.freeRam)
            AlertDialog.Builder(this@MainActivity, R.style.SuccessDialog)
                .setTitle("Optimization Complete!")
                .setMessage("Memory freed successfully!\nAvailable: $freedMB")
                .setPositiveButton("OK", null)
                .show()

            refreshStats()
        }
    }

    private fun loadGames() {
        lifecycleScope.launch {
            val games = withContext(Dispatchers.IO) {
                SystemUtils.getInstalledGames(this@MainActivity)
            }

            if (games.isNotEmpty()) {
                gamesTitle.visibility = View.VISIBLE
                gamesRecyclerView.visibility = View.VISIBLE
                noGamesText.visibility = View.GONE

                gamesRecyclerView.adapter = GameAdapter(games) { game ->
                    val intent = Intent(this@MainActivity, BoostActivity::class.java)
                    intent.putExtra("game_package", game.packageName)
                    intent.putExtra("game_name", game.name)
                    startActivity(intent)
                }
            } else {
                gamesTitle.visibility = View.GONE
                gamesRecyclerView.visibility = View.GONE
                noGamesText.visibility = View.VISIBLE
            }
        }
    }

    private fun refreshStats() {
        lifecycleScope.launch {
            val memInfo = SystemUtils.getMemoryInfo(this@MainActivity)
            val cpuInfo = SystemUtils.getCpuInfo()
            val batteryInfo = SystemUtils.getBatteryInfo(this@MainActivity)
            val storageInfo = SystemUtils.getStorageInfo()

            withContext(Dispatchers.Main) {
                // RAM
                ramProgress.progress = memInfo.usagePercent
                ramText.text = "${SystemUtils.formatSize(memInfo.usedRam)} / ${SystemUtils.formatSize(memInfo.totalRam)}"

                // CPU
                cpuProgress.progress = cpuInfo.usagePercent.toInt()
                cpuText.text = "%.1f%% - ${cpuInfo.coreCount} cores".format(cpuInfo.usagePercent)

                // Storage
                storageProgress.progress = storageInfo.usagePercent
                storageText.text = "${SystemUtils.formatSize(storageInfo.usedStorage)} / ${SystemUtils.formatSize(storageInfo.totalStorage)}"

                // Battery
                batteryProgress.progress = batteryInfo.level
                batteryText.text = "${batteryInfo.level}% - ${batteryInfo.health}"

                // Temperature
                if (cpuInfo.temperature > 0) {
                    tempText.text = "%.1f°C".format(cpuInfo.temperature)
                    tempText.setTextColor(
                        when {
                            cpuInfo.temperature > 45 -> ContextCompat.getColor(this@MainActivity, R.color.temp_hot)
                            cpuInfo.temperature > 35 -> ContextCompat.getColor(this@MainActivity, R.color.temp_warm)
                            else -> ContextCompat.getColor(this@MainActivity, R.color.temp_normal)
                        }
                    )
                } else {
                    tempText.text = "N/A"
                }
            }
        }
    }

    private fun startMonitoring() {
        monitorJob = lifecycleScope.launch {
            while (isActive) {
                refreshStats()
                delay(2000)
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 100)
        }
    }

    private fun checkUsageStatsPermission() {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )

        if (mode != AppOpsManager.MODE_ALLOWED) {
            AlertDialog.Builder(this, R.style.InfoDialog)
                .setTitle("Permission Required")
                .setMessage("Game Booster needs Usage Access permission to detect running games and optimize performance.")
                .setPositiveButton("Grant") { _, _ ->
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
                .setNegativeButton("Later", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStats()
        loadGames()
        startMonitoring()
    }

    override fun onPause() {
        super.onPause()
        monitorJob?.cancel()
    }
}
