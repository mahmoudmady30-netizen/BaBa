package com.babakids.app.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babakids.app.audio.SmartVoiceManager
import com.babakids.app.data.ArcadeSounds
import com.babakids.app.data.ParentSettingsManager
import com.babakids.app.data.Phrases
import kotlinx.coroutines.launch

/**
 * A real-world reward a parent set up ("ice cream!", "trip to the park!")
 * just got unlocked — this popup announces it by name, spoken aloud too,
 * instead of the child just seeing a generic sticker appear silently.
 * Styled to match the arcade/voxel opening (dark background, voxel-block
 * "PRIZE" tile, pixel font, level-up jingle) for a consistent theme.
 */
@Composable
fun RewardPopupOverlay(
    rewardTitle: String,
    childName: String,
    childGender: String,
    english: Boolean,
    arabicDialect: String = ParentSettingsManager.DIALECT_EGYPTIAN,
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

    var opened by remember { mutableStateOf(false) }
    val giftScale by animateFloatAsState(
        targetValue = if (opened) 1.25f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "giftScale"
    )

    LaunchedEffect(rewardTitle) {
        launch { sounds.playLevelUpJingle() }
        val line = if (english) {
            "Congratulations $displayName! You earned: $rewardTitle!"
        } else {
            "مبروك يا $displayName! كسبت: $rewardTitle!"
        }
        // This exact phrase (name + reward text) is a perfect candidate
        // for bundled pre-recorded audio or the local cache — once a real
        // offline generation tier or bundled clip exists for it, repeated
        // rewards with the same name+text combo play instantly.
        smartVoice.playSmartVoice(line, english = english, dialect = arabicDialect)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0221))
            .clickable {
                if (!opened) opened = true else onDismiss()
            }
    ) {
        if (opened) {
            ConfettiBurst(particleCount = 18, richPalette = true)
        }
        ShineSweep()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                if (opened) "🎁🎉" else "🎁",
                fontSize = 96.sp,
                modifier = Modifier.scale(giftScale)
            )
            Spacer(Modifier.height(18.dp))
            VoxelCube(
                letter = if (english) "!" else "★",
                color = Color(0xFFAB47BC),
                cubeSize = 52.dp,
                fontSize = 26.sp
            )
            Spacer(Modifier.height(18.dp))
            Text(
                if (english) "CONGRATULATIONS $displayName!" else "مبروك يا $displayName!",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF00E5FF)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                if (english) "YOU EARNED:" else "كسبت:",
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                rewardTitle,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFFFB300)
            )
            Spacer(Modifier.height(30.dp))
            ArcadeBlinkText(
                if (opened) {
                    if (english) "TAP TO CONTINUE" else "دوس عشان تكمل"
                } else {
                    if (english) "TAP TO OPEN" else "دوس تفتحها"
                },
                fontSize = 14.sp
            )
        }
    }
}
