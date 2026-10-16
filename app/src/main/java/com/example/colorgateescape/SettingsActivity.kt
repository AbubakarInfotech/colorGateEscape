package com.example.colorgateescape

import android.content.Context
import android.os.Bundle
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences("ColorGateEscapePrefs", Context.MODE_PRIVATE)

        val soundSwitch = findViewById<Switch>(R.id.soundSwitch)
        val vibrationSwitch = findViewById<Switch>(R.id.vibrationSwitch)

        soundSwitch.isChecked = prefs.getBoolean("sound_enabled", true)
        vibrationSwitch.isChecked = prefs.getBoolean("vibration_enabled", true)

        soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("sound_enabled", isChecked).apply()
        }

        vibrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("vibration_enabled", isChecked).apply()
        }

        findViewById<android.widget.Button>(R.id.backButton).setOnClickListener {
            finish()
        }
    }
}