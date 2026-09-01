package com.babakids.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babakids.app.audio.SmartVoiceManager
import com.babakids.app.data.ArcadeSounds
import com.babakids.app.data.Phrases
import kotlinx.coroutines.launch

/**
 * Spec §5: a real celebration every time the child reaches a multiple of
 * 10 stars (10, 20, 30...), not just a number ticking up. Tap anywhere to
 * dismiss and keep going. Speaks the celebration line out loud too.
 *
 * Styled to match the arcade/voxel opening: dark CRT-style background,
 * the star count shown as gold voxel-block digits (a score-counter feel),
 * monospace pixel font, a blinking "tap to continue" prompt, and a real
 * synthesized level-up jingle (see ArcadeSounds) — the same visual/audio
 * language as the splash screen, not a separate one-off look.
 */
@Composable
fun CelebrationOverlay(
    milestone: Int,
    childName: String,
    childGender: String,
    english: Boolean,
    arabicDialect: String = com.babakids.app.data.ParentSettingsManager.DIALECT_EGYPTIAN,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val smartVoice = remember { SmartVoiceManager.getInstance(context) }
    val sounds = remember { ArcadeSounds() }
    DisposableEffect(Unit) {
        onDispose {
            sounds.release()
            smartVoice.release()
        }
    }
    val displayName = Phrases.displayName(childName, childGender, english)
    val isBigMilestone = milestone % 50 == 0

    LaunchedEffect(milestone) {
        launch { sounds.playLevelUpJingle() }
        val line = if (english) {
            if (isBigMilestone) "Wow $displayName! $milestone stars! You're a true champion!"
            else "Wow $displayName! You collected $milestone stars!"
        } else {
            if (isBigMilestone) "واو يا $displayName! جمعت $milestone نجمة! إنت بطل حقيقي!"
            else "واو يا $displayName! أنت ممتاز! جمعت $milestone نجمة!"
        }
        // Uses the offline-first Smart Voice system (bundled pre-recorded
        // audio -> local cache -> device TTS fallback) — see
        // com.babakids.app.audio.SmartVoiceManager for the full pipeline.
        smartVoice.playSmartVoice(line, english = english, dialect = arabicDialect)
    }

    // A bouncy scale-in entrance (overshoots past 1x then settles) — the
    // "pop" that makes the reveal itself feel premium, not just the loop
    // that runs after it settles.
    val entranceScale = remember { Animatable(0.3f) }
    LaunchedEffect(milestone) {
        entranceScale.snapTo(0.3f)
        entranceScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 260f))
    }

    val transition = rememberInfiniteTransition(label = "celebration")

    // Drives the bouncing star row (after the entrance settles).
    val bounceState = transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable<Float>(
            animation = tween(450),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    val bounce = bounceState.value

    // Drives the pseudo-3D tilt on the title text.
    val tiltState = transition.animateFloat(
        initialValue = -22f,
        targetValue = 22f,
        animationSpec = infiniteRepeatable<Float>(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tilt"
    )
    val tilt = tiltState.value

    val density = LocalDensity.current.density

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0221))
            .clickable { onDismiss() }
    ) {
        ConfettiBurst(particleCount = if (isBigMilestone) 22 else 14, richPalette = isBigMilestone)
        ShineSweep()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .graphicsLayer {
                    scaleX = entranceScale.value
                    scaleY = entranceScale.value
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val rowEmojis = if (isBigMilestone) {
                    listOf("🏆", "🎉", "⭐", "🎊", "⭐", "🎉", "🏆")
                } else {
                    listOf("🎉", "⭐", "🎊", "⭐", "🎉")
                }
                rowEmojis.forEach { emoji ->
                    Text(emoji, fontSize = if (isBigMilestone) 44.sp else 40.sp, modifier = Modifier.scale(bounce))
                }
            }

            Spacer(Modifier.height(20.dp))

            // The star count as a gold voxel-block score display — the
            // "arcade score counter" fused with the "voxel block" look.
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                milestone.toString().forEach { digit ->
                    VoxelCube(
                        letter = digit.toString(),
                        color = Color(0xFFFFB300),
                        cubeSize = 46.dp,
                        fontSize = 24.sp
                    )
                }
                VoxelCube(letter = "★", color = Color(0xFFFFB300), cubeSize = 46.dp, fontSize = 22.sp)
            }

            Spacer(Modifier.height(20.dp))

            Text(
                Phrases.celebrationTitle(displayName, english, arabicDialect),
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF00E5FF),
                modifier = Modifier.graphicsLayer {
                    rotationY = tilt
                    cameraDistance = 12f * density
                }
            )
            Spacer(Modifier.height(10.dp))
            Text(
                Phrases.celebrationSubtitle(milestone, english),
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )
            if (isBigMilestone) {
                Spacer(Modifier.height(10.dp))
                Text(
                    if (english) "🏆 TRUE CHAMPION 🏆" else "🏆 بطل حقيقي 🏆",
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFB300)
                )
            }

            Spacer(Modifier.height(30.dp))
            ArcadeBlinkText(
                if (english) "TAP TO CONTINUE" else "دوس عشان تكمل",
                fontSize = 14.sp
            )
        }
    }
}
