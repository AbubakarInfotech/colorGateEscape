package com.example.colorgateescape

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.colorgateescape.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var autoNextTimer: CountDownTimer? = null
    private val AUTO_NEXT_SECONDS = 6

    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("ColorGateEscapePrefs", Context.MODE_PRIVATE)

        updateBestScoreDisplay()
        binding.moveCountText.text = "Moves: 0"
        binding.timerText.text = "Time: 0s"

        binding.puzzleView.onArrowEscaped = { currentMoves ->
            binding.moveCountText.text = "Moves: $currentMoves"
        }

        binding.puzzleView.onUndoPerformed = { currentMoves ->
            binding.moveCountText.text = "Moves: $currentMoves"
        }

        binding.puzzleView.onTimeChanged = { seconds ->
            binding.timerText.text = "Time: ${seconds}s"
        }

        binding.puzzleView.onAllArrowsEscaped = { totalMoves ->
            binding.finalStatsText.text = "Time: ${binding.puzzleView.getElapsedSeconds()}s   •   Moves: $totalMoves"
            binding.levelCompleteCard.visibility = View.VISIBLE
            binding.confettiView.start()
            saveBestScoreIfBetter(totalMoves)
            updateBestScoreDisplay()
            startAutoNextCountdown()
        }

        binding.puzzleView.onStarsEarned = { stars ->
            binding.starsText.text = "★".repeat(stars) + "☆".repeat(3 - stars)
            saveBestStarsIfBetter(stars)
        }

        binding.undoButton.setOnClickListener {
            binding.puzzleView.undoLastMove()
        }

        binding.resetButton.setOnClickListener {
            cancelAutoNext()
            binding.puzzleView.resetBoard()
            binding.levelCompleteCard.visibility = View.GONE
            binding.moveCountText.text = "Moves: 0"
        }

        binding.nextButton.setOnClickListener {
            cancelAutoNext()
            goToNextLevel()
        }

        binding.winText.setOnClickListener {
            cancelAutoNext()
            goToNextLevel()
        }
    }

    private fun goToNextLevel() {
        binding.puzzleView.nextLevel()
        binding.levelCompleteCard.visibility = View.GONE
        binding.moveCountText.text = "Moves: 0"
        updateBestScoreDisplay()
    }

    private fun bestScoreKey(): String {
        val levelIndex = binding.puzzleView.getCurrentLevelIndex()
        return "best_score_level_$levelIndex"
    }

    private fun saveBestScoreIfBetter(newScore: Int) {
        val key = bestScoreKey()
        val currentBest = prefs.getInt(key, Int.MAX_VALUE)
        if (newScore < currentBest) {
            prefs.edit().putInt(key, newScore).apply()
        }
    }

    private fun saveBestStarsIfBetter(newStars: Int) {
        val levelIndex = binding.puzzleView.getCurrentLevelIndex()
        val key = "best_stars_level_$levelIndex"
        val currentBest = prefs.getInt(key, 0)
        if (newStars > currentBest) {
            prefs.edit().putInt(key, newStars).apply()
        }
    }

    private fun updateBestScoreDisplay() {
        val key = bestScoreKey()
        val best = prefs.getInt(key, -1)
        binding.bestScoreText.text = if (best == -1) "Best: --" else "Best: $best"
    }

    private fun startAutoNextCountdown() {
        cancelAutoNext()
        binding.countdownText.visibility = View.VISIBLE
        binding.countdownText.text = "Next in $AUTO_NEXT_SECONDS..."

        val timer = object : CountDownTimer(AUTO_NEXT_SECONDS * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = ((millisUntilFinished + 999) / 1000).toInt()
                binding.countdownText.text = "Next in $secondsLeft..."
            }

            override fun onFinish() {
                goToNextLevel()
            }
        }
        autoNextTimer = timer
        timer.start()
    }

    private fun cancelAutoNext() {
        autoNextTimer?.cancel()
        autoNextTimer = null
        binding.countdownText.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelAutoNext()
        binding.puzzleView.releaseSounds()
    }
}