package com.example.colorgateescape

enum class ArrowColor {
    RED, BLUE, GREEN
}

enum class ArrowDirection {
    UP, DOWN, LEFT, RIGHT
}

data class Arrow(
    var row: Int,
    var col: Int,
    val color: ArrowColor,
    val direction: ArrowDirection,
    var isEscaped: Boolean = false,
    var isAnimating: Boolean = false,
    var offsetX: Float = 0f,
    var offsetY: Float = 0f
)