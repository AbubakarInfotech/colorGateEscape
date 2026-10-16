package com.example.colorgateescape

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import androidx.appcompat.app.AppCompatActivity

class LevelSelectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_level_select)

        val grid = findViewById<GridLayout>(R.id.levelGrid)

        for (i in LevelData.levels.indices) {
            val button = Button(this)
            button.text = "${i + 1}"
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(i % 4, 1f)
                rowSpec = GridLayout.spec(i / 4)
                setMargins(12, 12, 12, 12)
            }
            button.layoutParams = params
            button.setOnClickListener {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("level_index", i)
                startActivity(intent)
            }
            grid.addView(button)
        }
    }
}