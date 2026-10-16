package com.example.colorgateescape

data class Level(
    val arrows: List<Arrow>,
    val topGate: ArrowColor,
    val bottomGate: ArrowColor,
    val leftGate: ArrowColor,
    val rightGate: ArrowColor,
    val gridSize: Int = 3
)

object LevelData {

    /**
     * Easy pattern: each column moves fully UP or fully DOWN (alternating).
     * Used for the first couple of levels to teach the mechanic.
     */
    private fun generateEasyLevel(size: Int, levelIndex: Int): Level {
        val colors = listOf(ArrowColor.RED, ArrowColor.BLUE, ArrowColor.GREEN)
        val topColor = colors[levelIndex % 3]
        val bottomColor = colors[(levelIndex + 1) % 3]
        val leftColor = colors[(levelIndex + 2) % 3]
        val rightColor = colors[(levelIndex + 2) % 3]

        val arrows = mutableListOf<Arrow>()
        for (col in 0 until size) {
            val goingUp = col % 2 == 0
            val color = if (goingUp) topColor else bottomColor
            val direction = if (goingUp) ArrowDirection.UP else ArrowDirection.DOWN
            for (row in 0 until size) {
                arrows.add(Arrow(row, col, color, direction))
            }
        }

        return Level(
            arrows = arrows,
            topGate = topColor,
            bottomGate = bottomColor,
            leftGate = leftColor,
            rightGate = rightColor,
            gridSize = size
        )
    }

    /**
     * Ring/onion pattern: each cell points toward its nearest edge.
     * The "tieBreakOrder" controls which edge wins when a cell is equally
     * close to two edges — rotating this per level changes which cells
     * get grouped together, so the solve order genuinely differs level to level,
     * not just "bigger board, same trick."
     */
    private fun generateRingLevel(size: Int, levelIndex: Int, tieBreakRotation: Int): Level {
        val colors = listOf(ArrowColor.RED, ArrowColor.BLUE, ArrowColor.GREEN)
        val topColor = colors[levelIndex % 3]
        val bottomColor = colors[(levelIndex + 1) % 3]
        val leftColor = colors[(levelIndex + 2) % 3]
        val rightColor = colors[(levelIndex + 3) % 3]

        val arrows = mutableListOf<Arrow>()
        val last = size - 1

        // The order in which we check edges when distances tie.
        // Rotating this per level changes which direction "wins" ties,
        // reshuffling which cells belong to which peel-group.
        val edgeCheckOrder = when (tieBreakRotation % 4) {
            0 -> listOf(ArrowDirection.UP, ArrowDirection.DOWN, ArrowDirection.LEFT, ArrowDirection.RIGHT)
            1 -> listOf(ArrowDirection.LEFT, ArrowDirection.UP, ArrowDirection.RIGHT, ArrowDirection.DOWN)
            2 -> listOf(ArrowDirection.DOWN, ArrowDirection.RIGHT, ArrowDirection.UP, ArrowDirection.LEFT)
            else -> listOf(ArrowDirection.RIGHT, ArrowDirection.DOWN, ArrowDirection.LEFT, ArrowDirection.UP)
        }

        for (row in 0 until size) {
            for (col in 0 until size) {
                val distances = mapOf(
                    ArrowDirection.UP to row,
                    ArrowDirection.DOWN to (last - row),
                    ArrowDirection.LEFT to col,
                    ArrowDirection.RIGHT to (last - col)
                )

                var bestDirection = edgeCheckOrder[0]
                var bestDist = distances.getValue(bestDirection)
                for (dir in edgeCheckOrder) {
                    val d = distances.getValue(dir)
                    if (d < bestDist) {
                        bestDist = d
                        bestDirection = dir
                    }
                }

                val color = when (bestDirection) {
                    ArrowDirection.UP -> topColor
                    ArrowDirection.DOWN -> bottomColor
                    ArrowDirection.LEFT -> leftColor
                    ArrowDirection.RIGHT -> rightColor
                }

                arrows.add(Arrow(row, col, color, bestDirection))
            }
        }

        return Level(
            arrows = arrows,
            topGate = topColor,
            bottomGate = bottomColor,
            leftGate = leftColor,
            rightGate = rightColor,
            gridSize = size
        )
    }

    // Level 1 (size 3) & Level 2 (size 4): easy, teaches the mechanic.
    // Level 3 through Level 10 (size 5 to size 12): ring pattern, growing bigger,
    // with rotating tie-break so each level's solve order feels genuinely different.
    val levels: List<Level> = buildList {
        add(generateEasyLevel(size = 3, levelIndex = 0))
        add(generateEasyLevel(size = 4, levelIndex = 1))
        for (i in 0 until 8) {
            val size = i + 5 // sizes 5..12
            add(generateRingLevel(size = size, levelIndex = i + 2, tieBreakRotation = i))
        }
    }
}