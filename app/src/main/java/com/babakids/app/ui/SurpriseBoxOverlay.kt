package com.babakids.app.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
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
import com.babakids.app.data.ArcadeSounds

/**
 * A quick, delightful "found a treasure" moment every 5th distinct word
 * practiced — separate from the bigger 10-star celebration, this rewards
 * trying new words specifically. Tap to dismiss. Styled to match the
 * arcade/voxel opening (dark background, voxel-block bonus-star tile,
 * pixel font, coin sound on open) for a consistent theme across the app.
 */
@Composable
fun SurpriseBoxOverlay(wordsPracticedCount: Int, english: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sounds = remember { ArcadeSounds() }
    DisposableEffect(Unit) { onDispose { sounds.release() } }

    var opened by remember { mutableStateOf(false) }
    val chestScale by animateFloatAsState(
        targetValue = if (opened) 1.3f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chestScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0221))
            .clickable {
                if (!opened) {
                    opened = true
                    sounds.playCoin()
                } else {
                    onDismiss()
                }
            }
    ) {
        if (opened) {
            ConfettiBurst(particleCount = 14, richPalette = true)
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
                if (opened) "🎁✨" else "🎁",
                fontSize = 90.sp,
                modifier = Modifier.scale(chestScale)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                if (english) "SURPRISE FOUND!" else "لقيت مفاجأة!",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF00E5FF)
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                VoxelCube(letter = "+1", color = Color(0xFFFFB300), cubeSize = 50.dp, fontSize = 18.sp)
                VoxelCube(letter = "★", color = Color(0xFFFFB300), cubeSize = 50.dp, fontSize = 22.sp)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                if (english) "$wordsPracticedCount words tried so far!"
                else "جربت $wordsPracticedCount كلمة لحد دلوقتي!",
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White
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
