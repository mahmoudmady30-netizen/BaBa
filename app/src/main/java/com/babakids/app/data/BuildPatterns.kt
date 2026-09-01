package com.babakids.app.data

import androidx.compose.ui.graphics.Color

data class BuildPattern(
    val id: String,
    val name: String,
    val nameEn: String,
    val emoji: String,
    val grid: List<List<Color?>> // row-major, null = empty cell
)

object BuildPatterns {
    private val red = Color(0xFFE53935)
    private val brown = Color(0xFF6D4C41)
    private val green = Color(0xFF43A047)
    private val yellow = Color(0xFFFDD835)
    private val blue = Color(0xFF1E88E5)
    private val pink = Color(0xFFEC407A)
    private val orange = Color(0xFFFB8C00)
    private val purple = Color(0xFF8E24AA)

    private val n: Color? = null

    val patterns: List<BuildPattern> = listOf(
        BuildPattern(
            "heart", "قلب", "Heart", "❤️",
            listOf(
                listOf(n, red, red, n, red, red, n),
                listOf(red, red, red, red, red, red, red),
                listOf(red, red, red, red, red, red, red),
                listOf(n, red, red, red, red, red, n),
                listOf(n, n, red, red, red, n, n),
                listOf(n, n, n, red, n, n, n)
            )
        ),
        BuildPattern(
            "house", "بيت", "House", "🏠",
            listOf(
                listOf(n, n, n, red, n, n, n),
                listOf(n, n, red, red, red, n, n),
                listOf(n, red, red, red, red, red, n),
                listOf(brown, brown, brown, brown, brown, brown, brown),
                listOf(brown, yellow, brown, brown, brown, blue, brown),
                listOf(brown, yellow, brown, brown, brown, blue, brown)
            )
        ),
        BuildPattern(
            "tree", "شجرة", "Tree", "🌳",
            listOf(
                listOf(n, n, green, green, green, n, n),
                listOf(n, green, green, green, green, green, n),
                listOf(green, green, green, green, green, green, green),
                listOf(n, green, green, green, green, green, n),
                listOf(n, n, n, brown, n, n, n),
                listOf(n, n, n, brown, n, n, n)
            )
        ),
        BuildPattern(
            "star", "نجمة", "Star", "⭐",
            listOf(
                listOf(n, n, n, yellow, n, n, n),
                listOf(n, n, yellow, yellow, yellow, n, n),
                listOf(yellow, yellow, yellow, yellow, yellow, yellow, yellow),
                listOf(n, yellow, yellow, yellow, yellow, yellow, n),
                listOf(n, yellow, yellow, n, yellow, yellow, n),
                listOf(yellow, n, n, n, n, n, yellow)
            )
        ),
        BuildPattern(
            "flower", "زهرة", "Flower", "🌸",
            listOf(
                listOf(n, pink, n, n, n, pink, n),
                listOf(pink, pink, pink, yellow, pink, pink, pink),
                listOf(n, pink, yellow, yellow, yellow, pink, n),
                listOf(pink, pink, pink, yellow, pink, pink, pink),
                listOf(n, pink, n, green, n, pink, n),
                listOf(n, n, n, green, n, n, n)
            )
        ),
        BuildPattern(
            "boat", "مركب", "Boat", "⛵",
            listOf(
                listOf(n, n, n, blue, n, n, n),
                listOf(n, n, n, blue, orange, n, n),
                listOf(n, n, purple, blue, orange, orange, n),
                listOf(n, n, purple, blue, n, n, n),
                listOf(brown, brown, brown, brown, brown, brown, brown),
                listOf(n, blue, blue, blue, blue, blue, n)
            )
        )
    )
}
