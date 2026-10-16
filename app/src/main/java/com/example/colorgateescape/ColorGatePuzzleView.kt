package com.example.colorgateescape

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator

class ColorGatePuzzleView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    private var gridSize = 3
    private val arrows = mutableListOf<Arrow>()
    private var currentLevelIndex = 0

    var onAllArrowsEscaped: ((Int) -> Unit)? = null
    var onArrowEscaped: ((Int) -> Unit)? = null
    var onUndoPerformed: ((Int) -> Unit)? = null
    var onTimeChanged: ((Int) -> Unit)? = null
    var onStarsEarned: ((Int) -> Unit)? = null

    private var moveCount = 0
    private var totalTaps = 0
    private val escapeHistory = mutableListOf<Arrow>()

    // --- Timer setup ---
    private var elapsedSeconds = 0
    private var timerRunning = false
    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (timerRunning) {
                elapsedSeconds++
                onTimeChanged?.invoke(elapsedSeconds)
                timerHandler.postDelayed(this, 1000)
            }
        }
    }

    private var topGateColor = ArrowColor.RED
    private var bottomGateColor = ArrowColor.BLUE
    private var leftGateColor = ArrowColor.GREEN
    private var rightGateColor = ArrowColor.RED

    private val gatePaint = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 20f }
    private val cellBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.LTGRAY
    }
    private val arrowPaint = Paint().apply { style = Paint.Style.FILL }

    private var boardStartX = 0f
    private var boardStartY = 0f
    private var boardCellSize = 0f

    // --- Sound setup ---
    private val soundPool: SoundPool
    private var tapSoundId = 0
    private var escapeSoundId = 0
    private var winSoundId = 0
    private var soundsLoaded = false

    // --- Vibration setup ---
    private val vibrator: Vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    // --- Settings ---
    private val settingsPrefs = context.getSharedPreferences("ColorGateEscapePrefs", Context.MODE_PRIVATE)

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) soundsLoaded = true
        }

        tapSoundId = soundPool.load(context, R.raw.tap_sound, 1)
        escapeSoundId = soundPool.load(context, R.raw.escape_sound, 1)
        winSoundId = soundPool.load(context, R.raw.win_sound, 1)

        loadLevel(currentLevelIndex)
    }

    private fun playSound(soundId: Int) {
        if (soundsLoaded && settingsPrefs.getBoolean("sound_enabled", true)) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    private fun vibrate(durationMs: Long) {
        if (!settingsPrefs.getBoolean("vibration_enabled", true)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    private fun loadLevel(index: Int) {
        val level = LevelData.levels[index]
        gridSize = level.gridSize
        arrows.clear()
        arrows.addAll(level.arrows.map { it.copy() })
        topGateColor = level.topGate
        bottomGateColor = level.bottomGate
        leftGateColor = level.leftGate
        rightGateColor = level.rightGate
        moveCount = 0
        totalTaps = 0
        escapeHistory.clear()
        resetTimer()
        invalidate()
    }

    private fun resetTimer() {
        timerHandler.removeCallbacks(timerRunnable)
        timerRunning = false
        elapsedSeconds = 0
        onTimeChanged?.invoke(elapsedSeconds)
    }

    private fun startTimer() {
        if (timerRunning) return
        timerRunning = true
        timerHandler.postDelayed(timerRunnable, 1000)
    }

    private fun stopTimer() {
        timerRunning = false
        timerHandler.removeCallbacks(timerRunnable)
    }

    fun getElapsedSeconds(): Int = elapsedSeconds

    fun getTotalTaps(): Int = totalTaps

    private fun computeStars(): Int {
        val minTaps = gridSize * gridSize
        val okThreshold = minTaps + (minTaps / 2).coerceAtLeast(1)
        return when {
            totalTaps <= minTaps -> 3
            totalTaps <= okThreshold -> 2
            else -> 1
        }
    }

    fun resetBoard() {
        loadLevel(currentLevelIndex)
    }

    fun nextLevel() {
        currentLevelIndex = (currentLevelIndex + 1) % LevelData.levels.size
        loadLevel(currentLevelIndex)
    }

    fun getCurrentLevelIndex(): Int = currentLevelIndex

    fun undoLastMove(): Boolean {
        if (escapeHistory.isEmpty()) return false

        val lastEscaped = escapeHistory.removeAt(escapeHistory.size - 1)
        lastEscaped.isEscaped = false
        lastEscaped.offsetX = 0f
        lastEscaped.offsetY = 0f
        moveCount = (moveCount - 1).coerceAtLeast(0)

        invalidate()
        onUndoPerformed?.invoke(moveCount)
        Log.d("ColorGateEscape", "Undo: brought back ${lastEscaped.color} ${lastEscaped.direction}, moves=$moveCount")
        return true
    }

    /** Call this when the view is being destroyed to free sound resources and stop the timer. */
    fun releaseSounds() {
        soundPool.release()
        stopTimer()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(Color.parseColor("#16213E")) // board background

        val size = minOf(width, height).toFloat()
        val gateThickness = size * 0.08f
        val boardSize = size - (gateThickness * 2)
        val cellSize = boardSize / gridSize

        val startX = (width - size) / 2f + gateThickness
        val startY = (height - size) / 2f + gateThickness

        boardStartX = startX
        boardStartY = startY
        boardCellSize = cellSize

        drawGates(canvas, startX, startY, boardSize, gateThickness)
        drawGrid(canvas, startX, startY, cellSize)
        drawArrows(canvas, startX, startY, cellSize)
    }

    private fun drawGates(canvas: Canvas, startX: Float, startY: Float, boardSize: Float, thickness: Float) {
        gatePaint.strokeWidth = thickness

        gatePaint.color = colorFor(topGateColor)
        canvas.drawLine(startX, startY - thickness / 2, startX + boardSize, startY - thickness / 2, gatePaint)

        gatePaint.color = colorFor(bottomGateColor)
        canvas.drawLine(startX, startY + boardSize + thickness / 2, startX + boardSize, startY + boardSize + thickness / 2, gatePaint)

        gatePaint.color = colorFor(leftGateColor)
        canvas.drawLine(startX - thickness / 2, startY, startX - thickness / 2, startY + boardSize, gatePaint)

        gatePaint.color = colorFor(rightGateColor)
        canvas.drawLine(startX + boardSize + thickness / 2, startY, startX + boardSize + thickness / 2, startY + boardSize, gatePaint)
    }

    private fun drawGrid(canvas: Canvas, startX: Float, startY: Float, cellSize: Float) {
        for (i in 0..gridSize) {
            canvas.drawLine(startX + i * cellSize, startY, startX + i * cellSize, startY + cellSize * gridSize, cellBorderPaint)
            canvas.drawLine(startX, startY + i * cellSize, startX + cellSize * gridSize, startY + i * cellSize, cellBorderPaint)
        }
    }

    private val circleBackgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#0F3460")
    }

    private fun drawArrows(canvas: Canvas, startX: Float, startY: Float, cellSize: Float) {
        for (arrow in arrows) {
            if (arrow.isEscaped) continue

            val cx = startX + arrow.col * cellSize + cellSize / 2 + arrow.offsetX
            val cy = startY + arrow.row * cellSize + cellSize / 2 + arrow.offsetY
            val r = cellSize * 0.28f
            val circleRadius = cellSize * 0.42f

            canvas.drawCircle(cx, cy, circleRadius, circleBackgroundPaint)

            arrowPaint.color = colorFor(arrow.color)
            arrowPaint.setShadowLayer(12f, 0f, 4f, Color.parseColor("#40000000"))

            val path = Path()
            when (arrow.direction) {
                ArrowDirection.UP -> {
                    path.moveTo(cx, cy - r)
                    path.quadTo(cx - r * 1.1f, cy + r * 0.6f, cx - r, cy + r)
                    path.lineTo(cx + r, cy + r)
                    path.quadTo(cx + r * 1.1f, cy + r * 0.6f, cx, cy - r)
                }
                ArrowDirection.DOWN -> {
                    path.moveTo(cx, cy + r)
                    path.quadTo(cx - r * 1.1f, cy - r * 0.6f, cx - r, cy - r)
                    path.lineTo(cx + r, cy - r)
                    path.quadTo(cx + r * 1.1f, cy - r * 0.6f, cx, cy + r)
                }
                ArrowDirection.LEFT -> {
                    path.moveTo(cx - r, cy)
                    path.quadTo(cx + r * 0.6f, cy - r * 1.1f, cx + r, cy - r)
                    path.lineTo(cx + r, cy + r)
                    path.quadTo(cx + r * 0.6f, cy + r * 1.1f, cx - r, cy)
                }
                ArrowDirection.RIGHT -> {
                    path.moveTo(cx + r, cy)
                    path.quadTo(cx - r * 0.6f, cy - r * 1.1f, cx - r, cy - r)
                    path.lineTo(cx - r, cy + r)
                    path.quadTo(cx - r * 0.6f, cy + r * 1.1f, cx + r, cy)
                }
            }
            path.close()
            canvas.drawPath(path, arrowPaint)
            arrowPaint.clearShadowLayer()
        }
    }

    private fun colorFor(c: ArrowColor): Int = when (c) {
        ArrowColor.RED -> Color.RED
        ArrowColor.BLUE -> Color.BLUE
        ArrowColor.GREEN -> Color.GREEN
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val touchX = event.x
            val touchY = event.y

            val col = ((touchX - boardStartX) / boardCellSize).toInt()
            val row = ((touchY - boardStartY) / boardCellSize).toInt()

            if (row in 0 until gridSize && col in 0 until gridSize) {
                val tappedArrow = arrows.find { it.row == row && it.col == col && !it.isEscaped && !it.isAnimating }
                if (tappedArrow != null) {
                    handleArrowTap(tappedArrow)
                } else {
                    Log.d("ColorGateEscape", "Tapped empty cell at row=$row, col=$col")
                }
            } else {
                Log.d("ColorGateEscape", "Tapped outside grid")
            }
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun canEscape(arrow: Arrow): Boolean {
        val pathClear = when (arrow.direction) {
            ArrowDirection.UP -> arrows.none {
                !it.isEscaped && it != arrow && it.col == arrow.col && it.row < arrow.row
            }
            ArrowDirection.DOWN -> arrows.none {
                !it.isEscaped && it != arrow && it.col == arrow.col && it.row > arrow.row
            }
            ArrowDirection.LEFT -> arrows.none {
                !it.isEscaped && it != arrow && it.row == arrow.row && it.col < arrow.col
            }
            ArrowDirection.RIGHT -> arrows.none {
                !it.isEscaped && it != arrow && it.row == arrow.row && it.col > arrow.col
            }
        }

        if (!pathClear) return false

        val gateColor = when (arrow.direction) {
            ArrowDirection.UP -> topGateColor
            ArrowDirection.DOWN -> bottomGateColor
            ArrowDirection.LEFT -> leftGateColor
            ArrowDirection.RIGHT -> rightGateColor
        }

        return arrow.color == gateColor
    }

    private fun handleArrowTap(arrow: Arrow) {
        startTimer()
        playSound(tapSoundId)
        totalTaps++
        val willEscape = canEscape(arrow)
        Log.d("ColorGateEscape", "Tapped arrow: color=${arrow.color}, direction=${arrow.direction}, willEscape=$willEscape")
        animateArrow(arrow, willEscape)
    }

    private fun animateArrow(arrow: Arrow, willEscape: Boolean) {
        arrow.isAnimating = true

        val travelDistance = when (arrow.direction) {
            ArrowDirection.UP, ArrowDirection.DOWN -> height.toFloat()
            ArrowDirection.LEFT, ArrowDirection.RIGHT -> width.toFloat()
        }

        val targetDistance = if (willEscape) travelDistance else boardCellSize * 0.4f

        val animator = ValueAnimator.ofFloat(0f, targetDistance)
        animator.duration = if (willEscape) 500 else 150
        animator.interpolator = if (willEscape) AccelerateInterpolator() else DecelerateInterpolator()

        if (!willEscape) {
            animator.repeatCount = 1
            animator.repeatMode = ValueAnimator.REVERSE
        }

        animator.addUpdateListener { anim ->
            val distance = anim.animatedValue as Float
            when (arrow.direction) {
                ArrowDirection.UP -> arrow.offsetY = -distance
                ArrowDirection.DOWN -> arrow.offsetY = distance
                ArrowDirection.LEFT -> arrow.offsetX = -distance
                ArrowDirection.RIGHT -> arrow.offsetX = distance
            }
            invalidate()
        }

        animator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                arrow.isAnimating = false
                if (willEscape) {
                    arrow.isEscaped = true
                    arrow.offsetX = 0f
                    arrow.offsetY = 0f
                    moveCount++
                    escapeHistory.add(arrow)
                    playSound(escapeSoundId)
                    vibrate(40)
                    Log.d("ColorGateEscape", "Arrow escaped: ${arrow.color} ${arrow.direction}, moves=$moveCount")
                    onArrowEscaped?.invoke(moveCount)
                    if (arrows.all { it.isEscaped }) {
                        stopTimer()
                        playSound(winSoundId)
                        vibrate(200)
                        onStarsEarned?.invoke(computeStars())
                        onAllArrowsEscaped?.invoke(moveCount)
                    }
                } else {
                    arrow.offsetX = 0f
                    arrow.offsetY = 0f
                    invalidate()
                    vibrate(80)
                    Log.d("ColorGateEscape", "Arrow blocked: ${arrow.color} ${arrow.direction}")
                }
            }
        })
        animator.start()
    }
}