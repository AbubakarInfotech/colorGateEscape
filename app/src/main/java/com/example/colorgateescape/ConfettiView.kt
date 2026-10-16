package com.example.colorgateescape

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class ConfettiView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private data class Particle(
        var x: Float,
        var y: Float,
        var speedY: Float,
        var speedX: Float,
        var size: Float,
        var color: Int,
        var rotation: Float,
        var rotationSpeed: Float
    )

    private val particles = mutableListOf<Particle>()
    private val paint = Paint().apply { style = Paint.Style.FILL }
    private var animator: ValueAnimator? = null

    private val confettiColors = listOf(
        Color.parseColor("#E94560"),
        Color.parseColor("#FFD700"),
        Color.parseColor("#00D9FF"),
        Color.RED,
        Color.GREEN,
        Color.parseColor("#FF69B4")
    )

    /** Call this to start a confetti burst covering the current view size. */
    fun start() {
        if (width == 0 || height == 0) return // view not measured yet, skip

        particles.clear()
        repeat(80) {
            particles.add(
                Particle(
                    x = Random.nextFloat() * width,
                    y = -Random.nextFloat() * height * 0.5f, // start above the screen
                    speedY = Random.nextFloat() * 8f + 6f,
                    speedX = Random.nextFloat() * 4f - 2f,
                    size = Random.nextFloat() * 16f + 8f,
                    color = confettiColors.random(),
                    rotation = Random.nextFloat() * 360f,
                    rotationSpeed = Random.nextFloat() * 10f - 5f
                )
            )
        }

        visibility = VISIBLE

        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2500
            addUpdateListener {
                updateParticles()
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    visibility = INVISIBLE
                    particles.clear()
                }
            })
            start()
        }
    }

    private fun updateParticles() {
        for (p in particles) {
            p.y += p.speedY
            p.x += p.speedX
            p.rotation += p.rotationSpeed
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (p in particles) {
            paint.color = p.color
            canvas.save()
            canvas.translate(p.x, p.y)
            canvas.rotate(p.rotation)
            canvas.drawRect(-p.size / 2, -p.size / 4, p.size / 2, p.size / 4, paint)
            canvas.restore()
        }
    }

    fun stop() {
        animator?.cancel()
        particles.clear()
        visibility = INVISIBLE
    }
}