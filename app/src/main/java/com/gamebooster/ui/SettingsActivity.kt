package com.gamebooster.ui

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
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

        setupToolbar()
        setupSettings()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupSettings() {
        val autoBoostSwitch = findViewById<Switch>(R.id.autoBoostSwitch)
        val notificationSwitch = findViewById<Switch>(R.id.notificationSwitch)
        val temperatureSwitch = findViewById<Switch>(R.id.temperatureSwitch)
        val animationSeekBar = findViewById<SeekBar>(R.id.animationSeekBar)
        val animationLabel = findViewById<TextView>(R.id.animationLabel)

        autoBoostSwitch.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(this,
                if (isChecked) "Auto-boost enabled" else "Auto-boost disabled",
                Toast.LENGTH_SHORT
            ).show()
        }

        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(this,
                if (isChecked) "Notifications enabled" else "Notifications disabled",
                Toast.LENGTH_SHORT
            ).show()
        }

        temperatureSwitch.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(this,
                if (isChecked) "Temperature alerts enabled" else "Temperature alerts disabled",
                Toast.LENGTH_SHORT
            ).show()
        }

        animationSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val scale = (progress + 1) * 0.5f
                animationLabel.text = "Animation Scale: ${scale}x"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
}
