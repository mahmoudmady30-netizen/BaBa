package com.babakids.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private data class ConfettiParticle(
    val angleDegrees: Float,
    val emoji: String,
    val phase: Float,
    val sizeSp: Int
)

/**
 * A richer, more premium confetti burst than a plain fixed-emoji ring —
 * varied particle sizes, a bit of spin, and (optionally) a wider "gold and
 * gems" emoji set for reward-style moments. Shared across
 * CelebrationOverlay, SurpriseBoxOverlay, and RewardPopupOverlay so every
 * big moment in the app feels consistently premium.
 */
@Composable
fun ConfettiBurst(particleCount: Int = 16, richPalette: Boolean = false) {
    val density = LocalDensity.current.density
    val emojis = if (richPalette) {
        listOf("🎉", "⭐", "✨", "🏆", "💰", "🎊", "💎")
    } else {
        listOf("✨", "🎆", "🎇", "⭐", "💥")
    }
    val particles = remember(particleCount, richPalette) {
        (0 until particleCount).map { index ->
            ConfettiParticle(
                angleDegrees = index * (360f / particleCount),
                emoji = emojis[index % emojis.size],
                phase = (index % 4) * 0.25f,
                sizeSp = 20 + (index % 3) * 6
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "confetti")
    val burstState = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable<Float>(
            animation = tween(1700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confettiBurst"
    )
    val burstProgress = burstState.value

    Box(modifier = Modifier.fillMaxSize()) {
        particles.forEach { particle ->
            val progress = (burstProgress + particle.phase) % 1f
            val radiusPx = progress * 280f * density
            val angleRad = Math.toRadians(particle.angleDegrees.toDouble())
            val x = (cos(angleRad) * radiusPx).roundToInt()
            val y = (sin(angleRad) * radiusPx).roundToInt()
            val alpha = (1f - progress).coerceIn(0f, 1f)
            Text(
                particle.emoji,
                fontSize = particle.sizeSp.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset(x, y) }
                    .graphicsLayer {
                        this.alpha = alpha
                        val scale = 0.6f + progress * 0.7f
                        scaleX = scale
                        scaleY = scale
                        rotationZ = progress * 180f
                    }
            )
        }
    }
}

/**
 * A soft diagonal light band that sweeps across the whole screen on a
 * loop — the classic "premium/gold shimmer" touch used in reward-heavy
 * apps. Purely decorative, sits above the background but doesn't block
 * taps (no pointer handling attached).
 */
@Composable
fun ShineSweep() {
    val density = LocalDensity.current.density
    val transition = rememberInfiniteTransition(label = "shine")
    val sweepState = transition.animateFloat(
        initialValue = -400f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable<Float>(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shineSweep"
    )

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(90.dp)
            .graphicsLayer {
                translationX = sweepState.value * density
                rotationZ = 20f
                alpha = 0.18f
            }
            .background(Color.White)
    )
}
