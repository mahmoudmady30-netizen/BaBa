package com.babakids.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Base pastel palette from the spec: sky blue, yellow, light green,
// orange, light pink, light purple.
val SkyBlue = Color(0xFF8ED8F8)
val SunYellow = Color(0xFFFFE082)
val LeafGreen = Color(0xFFB9F6CA)
val Orange = Color(0xFFFFCC80)
val Pink = Color(0xFFF8BBD0)
val Purple = Color(0xFFD1C4E9)
val Background = Color(0xFFFFF6EC)
val TextDark = Color(0xFF3A3A3A)

// Deeper companions used only to build gradients — this is what gives the
// "glossy / premium" depth instead of flat pastel fills, while the light
// end of every gradient stays inside the original pastel palette above.
private val SkyBlueDeep = Color(0xFF4FC3F7)
private val SunYellowDeep = Color(0xFFFFC107)
private val LeafGreenDeep = Color(0xFF66BB6A)
private val OrangeDeep = Color(0xFFFFA451)
private val PinkDeep = Color(0xFFF06292)
private val PurpleDeep = Color(0xFF9575CD)

object BaBaGradients {
    val sky = Brush.linearGradient(listOf(SkyBlue, SkyBlueDeep))
    val sun = Brush.linearGradient(listOf(SunYellow, SunYellowDeep))
    val leaf = Brush.linearGradient(listOf(LeafGreen, LeafGreenDeep))
    val orange = Brush.linearGradient(listOf(Orange, OrangeDeep))
    val pink = Brush.linearGradient(listOf(Pink, PinkDeep))
    val purple = Brush.linearGradient(listOf(Purple, PurpleDeep))
    val background = Brush.verticalGradient(
        listOf(
            Color(0xFFF8FCFF),
            Color(0xFFF5F8FF),
            Color(0xFFFFF7ED)
        )
    )

    /** Cycles through the palette so a list of cards doesn't look monotone. */
    val cycle = listOf(sky, sun, leaf, orange, pink, purple)
}

private val BaBaColorScheme = lightColorScheme(
    primary = SkyBlueDeep,
    secondary = SunYellowDeep,
    tertiary = PinkDeep,
    background = Background,
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = TextDark,
    onSurface = TextDark
)

private val BaBaShapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp)
)

private val BaBaTypography = Typography(
    headlineMedium = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        letterSpacing = 0.2.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    )
)

@Composable
fun BaBaKidsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BaBaColorScheme,
        shapes = BaBaShapes,
        typography = BaBaTypography,
        content = content
    )
}
