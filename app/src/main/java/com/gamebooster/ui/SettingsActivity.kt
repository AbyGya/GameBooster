package com.gamebooster.ui

import android.os.Bundle
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gamebooster.R

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "⚙️ Settings"
        toolbar.setNavigationOnClickListener { finish() }
        setupSettings()
    }

    private fun setupSettings() {
        val autoBoost = findViewById<Switch>(R.id.autoBoostSwitch)
        val killApps = findViewById<Switch>(R.id.killSwitch)
        val notif = findViewById<Switch>(R.id.notificationSwitch)
        val temp = findViewById<Switch>(R.id.temperatureSwitch)
        val network = findViewById<Switch>(R.id.networkSwitch)
        val animSeek = findViewById<SeekBar>(R.id.animationSeekBar)
        val animLabel = findViewById<TextView>(R.id.animationLabel)

        autoBoost.setOnCheckedChangeListener { _, c -> Toast.makeText(this, if (c) "🔄 Auto-boost ON" else "🔄 Auto-boost OFF", Toast.LENGTH_SHORT).show() }
        killApps.setOnCheckedChangeListener { _, c -> Toast.makeText(this, if (c) "🗑️ Auto-kill ON" else "🗑️ Auto-kill OFF", Toast.LENGTH_SHORT).show() }
        notif.setOnCheckedChangeListener { _, c -> Toast.makeText(this, if (c) "🔕 Notifications ON" else "🔕 Notifications OFF", Toast.LENGTH_SHORT).show() }
        temp.setOnCheckedChangeListener { _, c -> Toast.makeText(this, if (c) "🌡️ Temp alerts ON" else "🌡️ Temp alerts OFF", Toast.LENGTH_SHORT).show() }
        network.setOnCheckedChangeListener { _, c -> Toast.makeText(this, if (c) "🌐 Network optimizer ON" else "🌐 Network optimizer OFF", Toast.LENGTH_SHORT).show() }
        animSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) {
                val scale = (p + 1) * 0.5f
                animLabel.text = "Animation Scale: ${scale}x"
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
    }
}
