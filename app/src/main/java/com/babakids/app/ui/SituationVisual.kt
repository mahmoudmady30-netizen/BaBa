package com.babakids.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A real, continuously looping animation for a "situation" word's icon —
 * not a static emoji. Four reusable animation styles, chosen per word via
 * WordItem.animationStyle, applied contextually (sleep/tired get the
 * "breathing" style, eat/drink get "bounce", play/happy get "wiggle",
 * scared/hurt get "shake"). This is a Compose-native animated icon, not
 * an imported GIF/Lottie file (no such asset exists in this project).
 */
@Composable
fun SituationVisual(emoji: String, style: String, emojiFontSize: TextUnit, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "situation_$style")

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (style) {
            "sleep" -> {
                // A gentle "breathing" scale pulse, like a sleeping child's
                // chest rising and falling, plus a small floating "Zzz".
                val breathe = transition.animateFloat(
                    initialValue = 0.94f,
                    targetValue = 1.06f,
                    animationSpec = infiniteRepeatable<Float>(
                        animation = tween(1100),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "breathe"
                )
                val zFloat = transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable<Float>(
                        animation = tween(1800),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "zFloat"
                )
                Text(
                    emoji,
                    fontSize = emojiFontSize,
                    modifier = Modifier.graphicsLayer {
                        scaleX = breathe.value
                        scaleY = breathe.value
                    }
                )
                Text(
                    "💤",
                    fontSize = 20.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-4).dp)
                        .graphicsLayer {
                            translationY = -zFloat.value * 14f
                            alpha = 1f - zFloat.value
                        }
                )
            }

            "bounce" -> {
                // A cheerful up-down hop, like chewing or an excited jump.
                val bounce = transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable<Float>(
                        animation = tween(420),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bounce"
                )
                Text(
                    emoji,
                    fontSize = emojiFontSize,
                    modifier = Modifier.graphicsLayer {
                        translationY = -bounce.value * 12f
                        val squash = 1f - bounce.value * 0.08f
                        scaleX = 1f + bounce.value * 0.06f
                        scaleY = squash
                    }
                )
            }

            "wiggle" -> {
                // A playful side-to-side rock, like an excited/happy sway.
                val wiggle = transition.animateFloat(
                    initialValue = -12f,
                    targetValue = 12f,
                    animationSpec = infiniteRepeatable<Float>(
                        animation = tween(380),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "wiggle"
                )
                Text(
                    emoji,
                    fontSize = emojiFontSize,
                    modifier = Modifier.graphicsLayer { rotationZ = wiggle.value }
                )
            }

            "shake" -> {
                // A quick nervous/urgent tremble.
                val shake = transition.animateFloat(
                    initialValue = -6f,
                    targetValue = 6f,
                    animationSpec = infiniteRepeatable<Float>(
                        animation = tween(90),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "shake"
                )
                Text(
                    emoji,
                    fontSize = emojiFontSize,
                    modifier = Modifier.graphicsLayer { translationX = shake.value }
                )
            }

            else -> {
                Text(emoji, fontSize = emojiFontSize)
            }
        }
    }
}
