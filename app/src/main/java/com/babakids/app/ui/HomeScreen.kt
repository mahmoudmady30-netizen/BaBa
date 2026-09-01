package com.babakids.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babakids.app.audio.SmartVoiceManager
import com.babakids.app.data.AppData
import com.babakids.app.data.Category
import com.babakids.app.data.Haptics
import com.babakids.app.data.ParentSettingsManager
import com.babakids.app.data.withCategoryOverrides
import com.babakids.app.ui.theme.BaBaGradients
import com.babakids.app.ui.theme.GlossyCard
import com.babakids.app.ui.theme.PremiumIconOrb
import com.babakids.app.ui.theme.rememberAdaptiveColumns

@Composable
fun HomeScreen(
    stars: Int,
    streakDays: Int = 0,
    english: Boolean = false,
    arabicDialect: String = ParentSettingsManager.DIALECT_EGYPTIAN,
    hapticEnabled: Boolean = true,
    deviceLocked: Boolean = false,
    disabledCategories: Set<String> = emptySet(),
    newAchievementCount: Int = 0,
    newLearnedWordsCount: Int = 0,
    onLockDevice: () -> Unit = {},
    onCategoryClick: (Category) -> Unit,
    onCollectionClick: () -> Unit = {},
    onMyWordsClick: () -> Unit = {},
    onLearnedWordsClick: () -> Unit = {},
    onActivitiesClick: () -> Unit = {},
    onParentModeClick: () -> Unit
) {
    val context = LocalContext.current
    // Bundled VoiceTut audio (tier 1) -> local cache (tier 2) -> device
    // TTS fallback — same pipeline used everywhere else in the app now,
    // so category names use the real recorded voice too, not just words.
    val smartVoice = remember { SmartVoiceManager.getInstance(context) }
    DisposableEffect(Unit) { onDispose { smartVoice.release() } }
    var menuExpanded by remember { mutableStateOf(false) }
    val categoryOverridesRepository = remember { com.babakids.app.data.CategoryOverridesRepository(context) }
    val categoryOverrides by categoryOverridesRepository.overridesFlow.collectAsState(initial = emptyMap())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BaBaGradients.background)
    ) {
        // Top bar: stars + streak on the left (status displays, never
        // moved), a single consolidated menu + the lock button on the
        // right. Every other action (My Words, Learned Words, Rewards,
        // Activities, Parent Mode) lives inside that one menu now,
        // instead of five separate icons crowding the bar.
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlossyCard(
                    gradient = BaBaGradients.sun,
                    cornerRadius = 20.dp
                ) {
                    Text(
                        text = "⭐ $stars",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3A3A3A),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                if (streakDays > 1) {
                    GlossyCard(
                        gradient = BaBaGradients.orange,
                        cornerRadius = 20.dp
                    ) {
                        Text(
                            text = "🔥 $streakDays",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                // Spec: a lock button right on the home screen (not just
                // inside Parent Mode) — tapping it locks the device to the
                // app immediately and hides itself; it reappears once the
                // device is unlocked from Parent Mode, or automatically
                // whenever the app is freshly restarted (this flag isn't
                // persisted, so a cold start always shows it again).
                if (!deviceLocked) {
                    Text(
                        text = "🔒",
                        fontSize = 22.sp,
                        modifier = Modifier.clickable {
                            Haptics.vibrateTap(context, hapticEnabled)
                            onLockDevice()
                        }
                    )
                }
                // Single consolidated menu — gathers My Words, Learned
                // Words, Rewards, Activities, and Parent Settings behind
                // one clean icon instead of five crowding the bar. Only
                // stars and lock stay outside it.
                Box {
                    Text(
                        text = "☰",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            Haptics.vibrateTap(context, hapticEnabled)
                            menuExpanded = true
                        }
                    )
                    NotificationBadge(
                        count = newAchievementCount + newLearnedWordsCount,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (english) "My Words" else "كلماتي") },
                            leadingIcon = { Text("❤️", fontSize = 20.sp) },
                            onClick = {
                                menuExpanded = false
                                Haptics.vibrateTap(context, hapticEnabled)
                                onMyWordsClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (english) "Learned Words" else "الكلمات المتعلمة") },
                            leadingIcon = {
                                Box {
                                    Text("📖", fontSize = 20.sp)
                                    NotificationBadge(
                                        count = newLearnedWordsCount,
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    )
                                }
                            },
                            onClick = {
                                menuExpanded = false
                                Haptics.vibrateTap(context, hapticEnabled)
                                onLearnedWordsClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (english) "My Rewards" else "المكافآت") },
                            leadingIcon = {
                                Box {
                                    Text("🎁", fontSize = 20.sp)
                                    NotificationBadge(
                                        count = newAchievementCount,
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    )
                                }
                            },
                            onClick = {
                                menuExpanded = false
                                onCollectionClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (english) "My Games" else "ألعابي") },
                            leadingIcon = { Text("🎮", fontSize = 20.sp) },
                            onClick = {
                                menuExpanded = false
                                Haptics.vibrateTap(context, hapticEnabled)
                                onActivitiesClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (english) "Parent Settings" else "إعدادات الوالدين") },
                            leadingIcon = { Text("⚙️", fontSize = 20.sp) },
                            onClick = {
                                menuExpanded = false
                                Haptics.vibrateTap(context, hapticEnabled)
                                onParentModeClick()
                            }
                        )
                    }
                }
            }
        }

        GlossyCard(
            gradient = BaBaGradients.sky,
            cornerRadius = 30.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(118.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 20.dp, end = 108.dp), // leaves clear room for the icon orb on the right, so text never runs under it
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    if (english) "Let's learn and play!" else "يلا نتعلم ونلعب!",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    if (english) "Choose a world and start your adventure" else "اختار عالم وابدأ مغامرتك",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            PremiumIconOrb(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 18.dp),
                size = 82.dp
            ) { Text("🚀", fontSize = 42.sp, modifier = Modifier.graphicsLayer { rotationZ = -8f }) }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(rememberAdaptiveColumns()),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val visibleCategories = AppData.categories
                .withCategoryOverrides(categoryOverrides)
                .filterNot { it.id in disabledCategories }
            itemsIndexed(visibleCategories) { index, category ->
                val gradient = BaBaGradients.cycle[index % BaBaGradients.cycle.size]
                CategoryCard(
                    category = category,
                    english = english,
                    gradient = gradient,
                    onClick = {
                        Haptics.vibrateTap(context, hapticEnabled)
                        // A little interactive voice cue on tap — says the
                        // category's name, both delightful and reinforces
                        // vocabulary before the category screen opens.
                        smartVoice.playSmartVoice(
                            category.displayTitle(english),
                            english = english,
                            dialect = arabicDialect
                        )
                        onCategoryClick(category)
                    }
                )
            }
        }
    }
}

/** A small red count badge, used for the menu icon and its items. */
@Composable
private fun NotificationBadge(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    Box(
        modifier = modifier
            .offset(x = 6.dp, y = (-6).dp)
            .size(20.dp)
            .clip(CircleShape)
            .background(Color(0xFFE53935)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 9) "9+" else count.toString(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun CategoryCard(
    category: Category,
    english: Boolean,
    gradient: androidx.compose.ui.graphics.Brush,
    onClick: () -> Unit
) {
    GlossyCard(
        gradient = gradient,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.92f),
        cornerRadius = 30.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PremiumIconOrb(size = 82.dp) {
                if (category.imagePath != null) {
                    val bitmap = remember(category.imagePath) {
                        runCatching { android.graphics.BitmapFactory.decodeFile(category.imagePath) }.getOrNull()
                    }
                    if (bitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = category.displayTitle(english),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            category.emoji,
                            fontSize = 46.sp,
                            modifier = Modifier.graphicsLayer { rotationX = 5f; rotationY = -5f; cameraDistance = 18f * density }
                        )
                    }
                } else {
                    Text(
                        category.emoji,
                        fontSize = 46.sp,
                        modifier = Modifier.graphicsLayer { rotationX = 5f; rotationY = -5f; cameraDistance = 18f * density }
                    )
                }
            }
            Text(
                category.displayTitle(english),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}
