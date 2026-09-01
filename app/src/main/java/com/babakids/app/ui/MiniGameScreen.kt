package com.babakids.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babakids.app.audio.SmartVoiceManager
import com.babakids.app.data.AppData
import com.babakids.app.data.CustomReward
import com.babakids.app.data.CustomRewardsRepository
import com.babakids.app.data.CustomWordsRepository
import com.babakids.app.data.Haptics
import com.babakids.app.data.ParentSettingsManager
import com.babakids.app.data.Phrases
import com.babakids.app.data.RewardManager
import com.babakids.app.data.WordItem
import com.babakids.app.ui.theme.BaBaGradients
import com.babakids.app.ui.theme.GlossyCard
import com.babakids.app.ui.theme.WordVisual
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot

/**
 * Spec §6–§10: five *genuinely different* mini-games, one picked at
 * random each time the "🎮 العب" button is used. An earlier version had
 * 10 named games, but several were the same interaction wearing a
 * different emoji (pop a bubble / open a box / hatch an egg were all
 * "tap N times"; scratch a card / wipe fog / peel a sticker were all
 * "drag to reveal") — cosmetic variety, not real variety. This keeps one
 * representative per genuinely distinct mechanic instead of padding the
 * count with skins.
 */
private enum class MiniGameMode {
    CHOOSE_CORRECT, LISTEN_CHOOSE, SCRATCH_CARD, OPEN_BOX, CATCH_STAR
}

private fun MiniGameMode.title(english: Boolean): String = when (this) {
    MiniGameMode.CHOOSE_CORRECT -> if (english) "🎮 Find the picture" else "🎮 هات الصورة الصح"
    MiniGameMode.LISTEN_CHOOSE -> if (english) "🎧 Listen & choose" else "🎧 اسمع واختار"
    MiniGameMode.SCRATCH_CARD -> if (english) "🪙 Scratch the card" else "🪙 اكشط الكرت"
    MiniGameMode.OPEN_BOX -> if (english) "🎁 Open the surprise box" else "🎁 افتح صندوق المفاجآت"
    MiniGameMode.CATCH_STAR -> if (english) "⭐ Catch it!" else "⭐ امسك الصورة!"
}

@Composable
fun MiniGameScreen(word: WordItem, reduceMotion: Boolean, hapticEnabled: Boolean = true, onExit: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Bundled VoiceTut audio (tier 1) -> local cache (tier 2) -> device
    // TTS fallback. This is what actually makes the real recorded voices
    // play during the mini-games, not just in the Parent Mode test.
    val smartVoice = remember { SmartVoiceManager.getInstance(context) }
    DisposableEffect(Unit) { onDispose { smartVoice.release() } }
    val rewardManager = remember { RewardManager(context) }
    val parentSettings = remember { ParentSettingsManager(context) }
    val customWordsRepository = remember { CustomWordsRepository(context) }
    val customRewardsRepository = remember { CustomRewardsRepository(context) }
    var customRewards by remember { mutableStateOf<List<CustomReward>>(emptyList()) }
    LaunchedEffect(Unit) {
        customRewardsRepository.rewardsFlow.collect { customRewards = it }
    }

    var childName by remember { mutableStateOf("") }
    var childGender by remember { mutableStateOf(ParentSettingsManager.GENDER_MALE) }
    var appLanguage by remember { mutableStateOf(ParentSettingsManager.LANGUAGE_AR) }
    var arabicDialect by remember { mutableStateOf(ParentSettingsManager.DIALECT_EGYPTIAN) }
    var settingsLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        parentSettings.combinedChildSettingsFlow().collect { snapshot ->
            childName = snapshot.childName
            childGender = snapshot.childGender
            appLanguage = snapshot.appLanguage
            arabicDialect = snapshot.arabicDialect
            settingsLoaded = true
        }
    }

    var customWords by remember { mutableStateOf<List<WordItem>>(emptyList()) }
    LaunchedEffect(Unit) {
        customWordsRepository.customWordsFlow.collect { customWords = it }
    }

    val english = appLanguage == ParentSettingsManager.LANGUAGE_EN
    val displayName = Phrases.displayName(childName, childGender, english)
    val displayWord = word.displayWord(english)
    // Only affects pronunciation (a diacritized Egyptian-dialect spoken
    // form when one exists) — the on-screen title still uses displayWord.
    val spokenWord = word.spokenWord(english, arabicDialect)

    val allWords = remember(customWords) { AppData.words + customWords }
    val options = remember(word.id, allWords) {
        val distractors = allWords
            .filter { it.id != word.id }
            .shuffled()
            .take(2)
        (distractors + word).shuffled()
    }

    // Motion-autonomous games (the falling star) are excluded from the
    // random pool when the parent has turned on "reduce motion" — every
    // other game only animates in response to the child's own taps/drags,
    // so those stay available.
    val availableModes = remember(reduceMotion) {
        if (reduceMotion) MiniGameMode.values().filter { it != MiniGameMode.CATCH_STAR }
        else MiniGameMode.values().toList()
    }
    val gameMode = remember(word.id) { availableModes.random() }
    val isChooseMode = gameMode == MiniGameMode.CHOOSE_CORRECT || gameMode == MiniGameMode.LISTEN_CHOOSE

    var feedback by remember { mutableStateOf<String?>(null) }
    var won by remember { mutableStateOf(false) }
    var celebrationMilestone by remember { mutableStateOf<Int?>(null) }
    var surpriseBoxCount by remember { mutableStateOf<Int?>(null) }
    var earnedReward by remember { mutableStateOf<CustomReward?>(null) }

    // SmartVoiceManager initializes its own fallback speech engine internally.

    LaunchedEffect(word.id, settingsLoaded, gameMode) {
        if (!settingsLoaded) return@LaunchedEffect
        feedback = null
        won = false
        val prompt = if (isChooseMode) {
            Phrases.findPrompts(english, arabicDialect).random().format(spokenWord) +
                (if (english) ", $displayName" else " يا $displayName")
        } else {
            if (english) "Let's discover $spokenWord, $displayName!"
            else "يلا نكتشف ال$spokenWord يا $displayName!"
        }
        smartVoice.playSmartVoice(prompt, english = english, dialect = arabicDialect)
    }

    fun onGameWon() {
        if (won) return
        won = true
        val winText = Phrases.successPhrases(childGender, english, arabicDialect).random().format(displayName)
        feedback = winText
        smartVoice.playSmartVoice(
            winText,
            english = english,
            dialect = arabicDialect,
            onPlaybackComplete = {
                // Same rule as the word-detail screen: leave automatically
                // once the encouragement finishes speaking, but only if no
                // star-milestone/reward/surprise-box celebration is about
                // to show — those overlays already have their own
                // onExit-on-dismiss, and popping the screen out from under
                // them would cut the celebration off before the child
                // gets to see it.
                scope.launch {
                    delay(700)
                    if (earnedReward == null && celebrationMilestone == null && surpriseBoxCount == null) {
                        onExit()
                    }
                }
            }
        )
        Haptics.vibrateSuccess(context, hapticEnabled)
        scope.launch {
            val newTotal = rewardManager.addStar()
            val practicedCount = rewardManager.markWordPracticed(word.id)
            val matchingReward = customRewards.firstOrNull { it.starsRequired == newTotal }
            when {
                matchingReward != null -> {
                    customRewardsRepository.markEarned(matchingReward.id)
                    earnedReward = matchingReward
                }
                newTotal % 10 == 0 -> celebrationMilestone = newTotal
                practicedCount % 5 == 0 -> {
                    rewardManager.addStar()
                    surpriseBoxCount = practicedCount
                }
            }
        }
    }

    fun onGameTryAgain() {
        val tryText = Phrases.tryAgainPhrases(childGender, english, arabicDialect).random().format(displayName)
        feedback = tryText
        smartVoice.playSmartVoice(tryText, english = english, dialect = arabicDialect)
    }

    val reward = earnedReward
    if (reward != null) {
        RewardPopupOverlay(
            rewardTitle = reward.title,
            childName = childName,
            childGender = childGender,
            english = english,
            arabicDialect = arabicDialect,
            onDismiss = { earnedReward = null; onExit() }
        )
        return
    }

    val milestone = celebrationMilestone
    if (milestone != null) {
        CelebrationOverlay(
            milestone = milestone,
            childName = childName,
            childGender = childGender,
            english = english,
            arabicDialect = arabicDialect,
            onDismiss = { celebrationMilestone = null; onExit() }
        )
        return
    }

    val surpriseCount = surpriseBoxCount
    if (surpriseCount != null) {
        SurpriseBoxOverlay(
            wordsPracticedCount = surpriseCount,
            english = english,
            onDismiss = { surpriseBoxCount = null; onExit() }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
    BackTopBar(onBack = onExit)
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (gameMode == MiniGameMode.LISTEN_CHOOSE) gameMode.title(english)
            else gameMode.title(english) + if (isChooseMode) ": $displayWord" else "",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        when (gameMode) {
            MiniGameMode.CHOOSE_CORRECT, MiniGameMode.LISTEN_CHOOSE -> {
                ChooseCorrectGame(
                    word = word,
                    options = options,
                    won = won,
                    hapticEnabled = hapticEnabled,
                    onWin = { onGameWon() },
                    onTryAgain = { onGameTryAgain() }
                )
            }
            MiniGameMode.SCRATCH_CARD -> DragRevealGame(
                word = word, coverEmoji = "🪙", coverGradient = BaBaGradients.sky,
                reduceMotion = reduceMotion, onWin = { onGameWon() }
            )
            MiniGameMode.OPEN_BOX -> TapRevealGame(
                word = word, coverEmoji = "🎁", openEmoji = "🎉", tapsNeeded = 4,
                english = english, hapticEnabled = hapticEnabled, onWin = { onGameWon() }
            )
            MiniGameMode.CATCH_STAR -> FallingCatchGame(
                word = word, hapticEnabled = hapticEnabled, onWin = { onGameWon() }
            )
        }

        Spacer(Modifier.height(28.dp))

        feedback?.let {
            Text(it, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3A3A3A))
        }

        if (won) {
            Spacer(Modifier.height(20.dp))
            GlossyCard(
                gradient = BaBaGradients.purple,
                modifier = Modifier.fillMaxWidth(),
                onClick = onExit
            ) {
                Text(
                    if (english) "✅ Done, go back" else "✅ تمام، ارجع",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
    }
}

/** Engine 1: choose the correct picture among 3 — powers 3 of the 10 named games. */
@Composable
private fun ChooseCorrectGame(
    word: WordItem,
    options: List<WordItem>,
    won: Boolean,
    hapticEnabled: Boolean,
    onWin: () -> Unit,
    onTryAgain: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        options.forEach { option ->
            GlossyCard(
                gradient = if (option.id == word.id) BaBaGradients.leaf else BaBaGradients.sky,
                modifier = Modifier.weight(1f).aspectRatio(0.85f),
                onClick = {
                    if (won) return@GlossyCard
                    Haptics.vibrateTap(context, hapticEnabled)
                    if (option.id == word.id) onWin() else onTryAgain()
                }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    WordVisual(
                        word = option,
                        emojiFontSize = 48.sp,
                        modifier = Modifier.height(64.dp).fillMaxWidth()
                    )
                }
            }
        }
    }
}

/** Engine 2: tap repeatedly to reveal the picture — powers 3 of the 10 named games. */
@Composable
private fun TapRevealGame(
    word: WordItem,
    coverEmoji: String,
    openEmoji: String,
    tapsNeeded: Int,
    english: Boolean,
    hapticEnabled: Boolean,
    onWin: () -> Unit
) {
    val context = LocalContext.current
    var taps by remember(word.id) { mutableStateOf(0) }
    val revealed = taps >= tapsNeeded

    val bounceScale by animateFloatAsState(
        targetValue = if (revealed) 1.15f else 1f + (taps.toFloat() / tapsNeeded) * 0.2f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tapRevealScale"
    )

    LaunchedEffect(revealed) { if (revealed) onWin() }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (revealed) openEmoji else coverEmoji,
            fontSize = 110.sp,
            modifier = Modifier
                .scale(bounceScale)
                .clickable(enabled = !revealed) {
                    Haptics.vibrateTap(context, hapticEnabled)
                    taps += 1
                }
        )
        Spacer(Modifier.height(16.dp))
        if (revealed) {
            WordVisual(
                word = word,
                emojiFontSize = 80.sp,
                modifier = Modifier.height(110.dp).fillMaxWidth()
            )
        } else {
            Text(
                if (english) "👆 Tap it! ($taps/$tapsNeeded)" else "👆 دوس عليها! ($taps/$tapsNeeded)",
                fontSize = 15.sp
            )
        }
    }
}

/**
 * Engine 3: drag across a cover to reveal the picture underneath — powers
 * 3 of the 10 named games (scratch card / wipe fog / peel sticker). This
 * is a simplified, honest stand-in for a literal per-pixel scratch effect
 * (which would need custom canvas erasing that's hard to get right
 * without testing on a device): cumulative drag distance fades the whole
 * cover out smoothly, which feels the same to a small child — drag until
 * it's gone — while being much more reliable.
 */
@Composable
private fun DragRevealGame(
    word: WordItem,
    coverEmoji: String,
    coverGradient: Brush,
    reduceMotion: Boolean,
    onWin: () -> Unit
) {
    var revealProgress by remember(word.id) { mutableStateOf(0f) }
    var hasWon by remember(word.id) { mutableStateOf(false) }

    LaunchedEffect(revealProgress) {
        if (revealProgress >= 0.85f && !hasWon) {
            hasWon = true
            onWin()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(28.dp))
                .pointerInput(word.id) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // kotlin.math.hypot only has a Double overload —
                        // Offset.x/.y are Float, so this needs explicit
                        // conversion or it won't compile.
                        val distance = hypot(dragAmount.x.toDouble(), dragAmount.y.toDouble()).toFloat()
                        revealProgress = (revealProgress + distance / 700f).coerceIn(0f, 1f)
                    }
                }
        ) {
            WordVisual(
                word = word,
                emojiFontSize = 100.sp,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 1f - revealProgress }
                    .background(coverGradient),
                contentAlignment = Alignment.Center
            ) {
                Text(coverEmoji, fontSize = 60.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (hasWon) "🎉" else "👆",
            fontSize = 22.sp
        )
    }
}

/** Engine 4: an image drifts down the screen — tap it anytime to catch it. */
@Composable
private fun FallingCatchGame(word: WordItem, hapticEnabled: Boolean, onWin: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    var caught by remember(word.id) { mutableStateOf(false) }
    val fallProgress = remember(word.id) { Animatable(0f) }

    LaunchedEffect(word.id) {
        fallProgress.snapTo(0f)
        fallProgress.animateTo(1f, tween(durationMillis = 4200, easing = LinearEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {
        WordVisual(
            word = word,
            emojiFontSize = 90.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(140.dp)
                .graphicsLayer { translationY = fallProgress.value * 260f * density }
                .clickable(enabled = !caught) {
                    Haptics.vibrateTap(context, hapticEnabled)
                    caught = true
                    onWin()
                }
        )
    }
}
