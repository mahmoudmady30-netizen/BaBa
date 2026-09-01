package com.babakids.app.data

import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.graphics.Rect as AndroidRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import kotlin.math.cos
import kotlin.math.sin

enum class ColoringCategory { NUMBERS, LETTERS, SHAPES, ANIMALS }

/**
 * One coloring-book shape. `outline` builds the actual fillable Path at
 * the canvas's real pixel size (so it's crisp at any screen density),
 * either as a single region (the whole shape fills at once) or as
 * several regions a child colors separately (e.g. a cat's ears vs body).
 */
data class ColoringShape(
    val id: String,
    val name: String,
    val nameEn: String,
    val category: ColoringCategory,
    val outline: (widthPx: Float, heightPx: Float) -> List<Path>
)

object ColoringShapes {

    /**
     * Real glyph outlines (not a blocky approximation) via Android's own
     * text-to-path extraction — the same technique font-rendering tools
     * use internally. Works for both Arabic and Latin characters/digits.
     */
    private fun glyphOutline(text: String): (Float, Float) -> List<Path> = { w, h ->
        val androidPath = AndroidPath()
        val paint = AndroidPaint().apply {
            isAntiAlias = true
            textSize = h * 0.85f
            textAlign = AndroidPaint.Align.CENTER
        }
        val bounds = AndroidRect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val x = w / 2f
        val y = h / 2f - bounds.exactCenterY()
        paint.getTextPath(text, 0, text.length, x, y, androidPath)
        listOf(androidPath.asComposePath())
    }

    private fun regularPolygon(sides: Int, rotationDeg: Float = -90f): (Float, Float) -> List<Path> = { w, h ->
        val cx = w / 2f
        val cy = h / 2f
        val radius = minOf(w, h) / 2f * 0.92f
        val path = Path()
        for (i in 0 until sides) {
            val angle = Math.toRadians((rotationDeg + i * 360f / sides).toDouble())
            val x = cx + radius * cos(angle).toFloat()
            val y = cy + radius * sin(angle).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        listOf(path)
    }

    private fun star(points: Int = 5): (Float, Float) -> List<Path> = { w, h ->
        val cx = w / 2f
        val cy = h / 2f
        val outerR = minOf(w, h) / 2f * 0.92f
        val innerR = outerR * 0.42f
        val path = Path()
        val total = points * 2
        for (i in 0 until total) {
            val angle = Math.toRadians((-90 + i * 360.0 / total))
            val r = if (i % 2 == 0) outerR else innerR
            val x = cx + r * cos(angle).toFloat()
            val y = cy + r * sin(angle).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        listOf(path)
    }

    private fun heart(): (Float, Float) -> List<Path> = { w, h ->
        val path = Path()
        val cx = w / 2f
        path.moveTo(cx, h * 0.88f)
        path.cubicTo(w * -0.05f, h * 0.55f, w * 0.08f, h * 0.05f, cx, h * 0.32f)
        path.cubicTo(w * 0.92f, h * 0.05f, w * 1.05f, h * 0.55f, cx, h * 0.88f)
        path.close()
        listOf(path)
    }

    private fun circleShape(): (Float, Float) -> List<Path> = { w, h ->
        val path = Path()
        path.addOval(androidx.compose.ui.geometry.Rect(w * 0.04f, h * 0.04f, w * 0.96f, h * 0.96f))
        listOf(path)
    }

    // --- Simple animals built from basic primitives, each region separately fillable ---

    private fun catOutline(): (Float, Float) -> List<Path> = { w, h ->
        val head = Path().apply { addOval(androidx.compose.ui.geometry.Rect(w * 0.25f, h * 0.15f, w * 0.75f, h * 0.62f)) }
        val earLeft = Path().apply {
            moveTo(w * 0.28f, h * 0.28f); lineTo(w * 0.18f, h * 0.05f); lineTo(w * 0.42f, h * 0.20f); close()
        }
        val earRight = Path().apply {
            moveTo(w * 0.72f, h * 0.28f); lineTo(w * 0.82f, h * 0.05f); lineTo(w * 0.58f, h * 0.20f); close()
        }
        val body = Path().apply { addOval(androidx.compose.ui.geometry.Rect(w * 0.22f, h * 0.55f, w * 0.78f, h * 0.95f)) }
        listOf(body, head, earLeft, earRight)
    }

    private fun fishOutline(): (Float, Float) -> List<Path> = { w, h ->
        val body = Path().apply { addOval(androidx.compose.ui.geometry.Rect(w * 0.08f, h * 0.25f, w * 0.72f, h * 0.75f)) }
        val tail = Path().apply {
            moveTo(w * 0.70f, h * 0.5f); lineTo(w * 0.95f, h * 0.2f); lineTo(w * 0.95f, h * 0.8f); close()
        }
        listOf(body, tail)
    }

    private fun birdOutline(): (Float, Float) -> List<Path> = { w, h ->
        val body = Path().apply { addOval(androidx.compose.ui.geometry.Rect(w * 0.15f, h * 0.35f, w * 0.85f, h * 0.90f)) }
        val head = Path().apply { addOval(androidx.compose.ui.geometry.Rect(w * 0.45f, h * 0.05f, w * 0.90f, h * 0.45f)) }
        val beak = Path().apply {
            moveTo(w * 0.90f, h * 0.22f); lineTo(w * 1.02f, h * 0.28f); lineTo(w * 0.90f, h * 0.34f); close()
        }
        listOf(body, head, beak)
    }

    private fun turtleOutline(): (Float, Float) -> List<Path> = { w, h ->
        val shell = Path().apply { addOval(androidx.compose.ui.geometry.Rect(w * 0.18f, h * 0.20f, w * 0.82f, h * 0.80f)) }
        val head = Path().apply { addOval(androidx.compose.ui.geometry.Rect(w * 0.72f, h * 0.38f, w * 0.98f, h * 0.62f)) }
        val legFrontLeft = Path().apply { addOval(androidx.compose.ui.geometry.Rect(w * 0.05f, h * 0.15f, w * 0.28f, h * 0.35f)) }
        val legBackLeft = Path().apply { addOval(androidx.compose.ui.geometry.Rect(w * 0.05f, h * 0.65f, w * 0.28f, h * 0.85f)) }
        listOf(shell, head, legFrontLeft, legBackLeft)
    }

    val shapes: List<ColoringShape> = buildList {
        // --- Geometric shapes ---
        add(ColoringShape("shape_circle", "دايرة", "Circle", ColoringCategory.SHAPES, circleShape()))
        add(ColoringShape("shape_square", "مربع", "Square", ColoringCategory.SHAPES, regularPolygon(4, -45f)))
        add(ColoringShape("shape_triangle", "مثلث", "Triangle", ColoringCategory.SHAPES, regularPolygon(3)))
        add(ColoringShape("shape_star", "نجمة", "Star", ColoringCategory.SHAPES, star(5)))
        add(ColoringShape("shape_heart", "قلب", "Heart", ColoringCategory.SHAPES, heart()))
        add(ColoringShape("shape_hexagon", "سداسي", "Hexagon", ColoringCategory.SHAPES, regularPolygon(6)))
        add(ColoringShape("shape_pentagon", "خماسي", "Pentagon", ColoringCategory.SHAPES, regularPolygon(5)))

        // --- Numbers 0-9, real digit outlines ---
        val numberNames = listOf(
            "صفر" to "Zero", "واحد" to "One", "اتنين" to "Two", "تلاتة" to "Three", "أربعة" to "Four",
            "خمسة" to "Five", "ستة" to "Six", "سبعة" to "Seven", "تمانية" to "Eight", "تسعة" to "Nine"
        )
        numberNames.forEachIndexed { digit, (ar, en) ->
            add(ColoringShape("number_$digit", ar, en, ColoringCategory.NUMBERS, glyphOutline(digit.toString())))
        }

        // --- Letters — a representative set from each alphabet ---
        val arabicLetters = listOf("أ", "ب", "ت", "ج", "د", "ر", "س", "ل", "م", "ن")
        arabicLetters.forEach { letter ->
            add(ColoringShape("letter_ar_$letter", letter, letter, ColoringCategory.LETTERS, glyphOutline(letter)))
        }
        ('A'..'J').forEach { letter ->
            add(
                ColoringShape(
                    "letter_en_$letter",
                    letter.toString(),
                    letter.toString(),
                    ColoringCategory.LETTERS,
                    glyphOutline(letter.toString())
                )
            )
        }

        // --- Simple animals, built from basic shapes ---
        add(ColoringShape("animal_cat", "قطة", "Cat", ColoringCategory.ANIMALS, catOutline()))
        add(ColoringShape("animal_fish", "سمكة", "Fish", ColoringCategory.ANIMALS, fishOutline()))
        add(ColoringShape("animal_bird", "عصفور", "Bird", ColoringCategory.ANIMALS, birdOutline()))
        add(ColoringShape("animal_turtle", "سلحفاة", "Turtle", ColoringCategory.ANIMALS, turtleOutline()))
    }

    fun forCategory(category: ColoringCategory): List<ColoringShape> = shapes.filter { it.category == category }
}
