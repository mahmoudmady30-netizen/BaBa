package com.babakids.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A single bevelled "voxel/block" tile with a letter on it — light strip
 * top-left, dark strip bottom-right, like a lit block face. Shared between
 * the splash screen's logo build and the arcade-style celebration
 * overlays so both use the same visual language.
 */
@Composable
fun VoxelCube(
    letter: String,
    color: Color,
    cubeSize: Dp = 64.dp,
    fontSize: TextUnit = 32.sp,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(cubeSize).background(color)) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .height(7.dp)
                .background(Color.White.copy(alpha = 0.35f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth()
                .height(7.dp)
                .background(Color.Black.copy(alpha = 0.25f))
        )
        Text(
            letter,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

/** Blinking retro pixel-font text — the classic "PRESS START" attract-mode look. */
@Composable
fun ArcadeBlinkText(text: String, fontSize: TextUnit = 16.sp, color: Color = Color.White, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "arcadeBlink")
    val alpha = transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable<Float>(
            animation = tween(300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arcadeBlinkAlpha"
    )
    Text(
        text,
        fontSize = fontSize,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier.graphicsLayer { this.alpha = alpha.value }
    )
}
