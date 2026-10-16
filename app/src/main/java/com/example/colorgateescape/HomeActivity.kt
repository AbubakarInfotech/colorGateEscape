package com.example.colorgateescape

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        findViewById<android.widget.Button>(R.id.playButton).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        findViewById<android.widget.Button>(R.id.levelSelectButton).setOnClickListener {
            startActivity(Intent(this, LevelSelectActivity::class.java))
        }

        findViewById<android.widget.Button>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<android.widget.Button>(R.id.leaderboardButton).setOnClickListener {
            Toast.makeText(this, "Leaderboard coming soon", Toast.LENGTH_SHORT).show()
        }
    }
}