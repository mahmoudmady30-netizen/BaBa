package com.babakids.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babakids.app.audio.SmartVoiceManager
import com.babakids.app.data.CustomRewardsRepository
import com.babakids.app.data.Haptics
import com.babakids.app.data.LearnedWordsRepository
import com.babakids.app.data.ParentSettingsManager
import com.babakids.app.data.Phrases
import com.babakids.app.data.RewardManager
import com.babakids.app.data.WordItem
import com.babakids.app.speech.SpeechHelper
import com.babakids.app.ui.theme.BaBaGradients
import com.babakids.app.ui.theme.GlossyCard
import com.babakids.app.ui.theme.Pink
import com.babakids.app.ui.theme.WordVisual
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun WordDetailScreen(
    word: WordItem,
    reduceMotion: Boolean = false,
    hapticEnabled: Boolean = true,
    onBack: () -> Unit = {},
    onPlayGame: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density

    val speechHelper = remember { SpeechHelper(context) }
    // Bundled VoiceTut audio (tier 1) -> local cache (tier 2) -> device
    // TTS fallback (speechHelper above, still used directly for speech
    // *recognition* via listenAndCompare, which has no bundled-audio
    // equivalent). This is what actually makes the real recorded voices
    // play during normal word-tapping, not just in the Parent Mode test.
    val smartVoice = remember { SmartVoiceManager.getInstance(context) }
    DisposableEffect(Unit) {
        onDispose {
            smartVoice.release()
        }
    }
    val rewardManager = remember { RewardManager(context) }
    val parentSettings = remember { ParentSettingsManager(context) }
    val learnedWordsRepository = remember { LearnedWordsRepository(context) }
    val customRewardsRepository = remember { CustomRewardsRepository(context) }
    var customRewards by remember { mutableStateOf<List<com.babakids.app.data.CustomReward>>(emptyList()) }
    LaunchedEffect(Unit) {
        customRewardsRepository.rewardsFlow.collect { customRewards = it }
    }

    var childName by remember { mutableStateOf("") }
    var childGender by remember { mutableStateOf(ParentSettingsManager.GENDER_MALE) }
    var appLanguage by remember { mutableStateOf(ParentSettingsManager.LANGUAGE_AR) }
    var arabicDialect by remember { mutableStateOf(ParentSettingsManager.DIALECT_EGYPTIAN) }
    // Bug fix: previously each of these settings was collected by its own
    // independent LaunchedEffect, each starting from a default value. The
    // very first auto-play (below) could fire before the *real* language
    // value had actually arrived from disk, so it spoke Arabic once even
    // right after switching to English. Reading all four together and
    // gating the auto-play on `settingsLoaded` closes that race.
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

    val english = appLanguage == ParentSettingsManager.LANGUAGE_EN
    val displayName = Phrases.displayName(childName, childGender, english)
    val displayWord = word.displayWord(english)
    // Only affects pronunciation (a diacritized Egyptian-dialect spoken
    // form when one exists) — the on-screen text stays displayWord.
    val spokenWord = word.spokenWord(english, arabicDialect)

    var feedback by remember { mutableStateOf<String?>(null) }
    var heardText by remember { mutableStateOf<String?>(null) }
    var isListening by remember { mutableStateOf(false) }
    // Neither the word nor the mic can be tapped while the app itself is
    // talking — besides feeling more responsive/orderly, this avoids the
    // mic starting to listen while the speaker is still playing the word,
    // which would otherwise pick up the app's own voice as background
    // noise and hurt recognition accuracy.
    var isSpeaking by remember { mutableStateOf(false) }
    var micPermissionExplainer by remember { mutableStateOf(false) }
    var celebrationMilestone by remember { mutableStateOf<Int?>(null) }
    var surpriseBoxCount by remember { mutableStateOf<Int?>(null) }
    var earnedReward by remember { mutableStateOf<com.babakids.app.data.CustomReward?>(null) }

    // Released automatically if the screen is left mid-playback.
    var activePlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(Unit) {
        onDispose { activePlayer?.release() }
    }

    // --- Idle "alive" animation: gentle float + slow 3D tilt, always running. ---
    val idleTransition = rememberInfiniteTransition(label = "wordIdle")
    val idleBobState = idleTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable<Float>(
            animation = tween(if (reduceMotion) 1 else 1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idleBob"
    )
    val idleTiltState = idleTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable<Float>(
            animation = tween(if (reduceMotion) 1 else 2400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idleTilt"
    )
    val idleBob = if (reduceMotion) 0f else idleBobState.value
    val idleTilt = if (reduceMotion) 0f else idleTiltState.value

    // --- Tap reaction: a short, category-flavored "personality" animation
    // — fruits/food give a giggly squash-and-stretch "laugh", animals hop
    // and wiggle like they're running/dancing, everything else gets a
    // friendly bouncy pop. Built with Animatable instead of a fixed
    // AnimationSpec because it's a one-shot triggered sequence, not a
    // continuous loop.
    val reactionScale = remember { Animatable(1f) }
    val reactionRotationZ = remember { Animatable(0f) }

    suspend fun playReaction() {
        if (reduceMotion) return
        when (word.category) {
            "food" -> {
                repeat(3) {
                    reactionScale.animateTo(1.18f, tween(110))
                    reactionScale.animateTo(0.9f, tween(110))
                }
                reactionScale.animateTo(1f, tween(140))
            }
            "animals" -> {
                repeat(4) { step ->
                    reactionRotationZ.animateTo(if (step % 2 == 0) 14f else -14f, tween(120))
                    reactionScale.animateTo(1.12f, tween(120))
                }
                reactionRotationZ.animateTo(0f, tween(150))
                reactionScale.animateTo(1f, tween(150))
            }
            else -> {
                reactionScale.animateTo(1.22f, tween(150))
                reactionScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
        }
    }

    fun handleResult(matched: Boolean, heard: String?, recordingPath: String?) {
        isListening = false
        heardText = heard
        if (matched) {
            val text = Phrases.successPhrases(childGender, english, arabicDialect).random().format(displayName)
            feedback = text
            smartVoice.playSmartVoice(
                text,
                english = english,
                dialect = arabicDialect,
                onPlaybackComplete = {
                    // Leave the word screen right after the encouragement
                    // finishes speaking — but NOT if a star milestone,
                    // custom reward, or surprise box just got triggered
                    // below: those are full-screen celebrations the child
                    // should actually get to see and dismiss themselves,
                    // and popping the screen out from under them would cut
                    // that off. Only auto-exit on an ordinary correct
                    // answer with none of those active.
                    scope.launch {
                        kotlinx.coroutines.delay(700)
                        if (earnedReward == null && celebrationMilestone == null && surpriseBoxCount == null) {
                            onBack()
                        }
                    }
                }
            )
            scope.launch { playReaction() }
            Haptics.vibrateSuccess(context, hapticEnabled)
            // Saving to Learned Words runs on a background dispatcher —
            // this used to run synchronously right here, blocking the
            // success voice/haptics above until it finished, which is
            // what caused the perceptible slowness on every correct answer.
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                learnedWordsRepository.recordCorrectPronunciation(
                    wordId = word.id,
                    word = displayWord,
                    emoji = word.emoji,
                    imagePath = word.imagePath,
                    language = if (english) "en" else "ar",
                    recordingPath = recordingPath
                )
            }
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
                        rewardManager.addStar() // the surprise box's bonus star
                        surpriseBoxCount = practicedCount
                    }
                }
            }
        } else {
            // Wrong answer — no clip was ever recorded now, nothing to discard.
            val text = Phrases.tryAgainPhrases(childGender, english, arabicDialect).random().format(displayName)
            feedback = text
            smartVoice.playSmartVoice(text, english = english, dialect = arabicDialect)
        }
    }

    /**
     * Recognizes what the child said — no separate voice-clip recording
     * attempt anymore.
     *
     * History: many versions in a row tried to also save a clip of the
     * child's own voice alongside recognition — first with MediaRecorder
     * (too exclusive a hold on the mic, hurt recognition accuracy), then
     * via SpeechRecognizer's onBufferReceived (dead since Android 4.0),
     * then a dedicated AudioRecord sharing VOICE_RECOGNITION with the
     * recognizer, then that same approach on the plain MIC source instead.
     * None of it held up reliably in real use — every attempt at capturing
     * a second, simultaneous stream from the mic came back either silent
     * or altogether refused, device-dependent and unpredictable. That's a
     * real Android platform limitation for this exact scenario (two mic
     * consumers, one process), not a bug worth chasing a sixth time.
     *
     * Per the decision to prioritize reliability: a correct answer is
     * still recognized, still spoken back with encouragement, still
     * counted as a star and saved to "Learned Words" — exactly as before —
     * it just never carries a saved audio clip of the child's own voice.
     * That was already how every *other* success path in the app worked;
     * this just makes the "Try saying it" mic button behave the same way
     * instead of being the one path that intermittently failed.
     */
    fun beginListening() {
        isListening = true
        heardText = null
        scope.launch { rewardManager.addAttempt() }

        speechHelper.listenAndCompare(displayWord, english = english) { matched, heard ->
            handleResult(matched, heard, recordingPath = null)
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            micPermissionExplainer = false
            beginListening()
        } else {
            micPermissionExplainer = true
        }
    }

    fun hasMicPermission(): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(Unit) { speechHelper.init() }

    fun playWord() {
        isSpeaking = true
        scope.launch { playReaction() }
        val recordingPath = word.parentRecordingPath
        if (recordingPath != null) {
            // Parent's own voice takes priority over TTS when available (spec §14).
            runCatching {
                activePlayer?.release()
                val player = MediaPlayer().apply {
                    setDataSource(recordingPath)
                    setOnCompletionListener {
                        it.release()
                        isSpeaking = false
                    }
                    prepare()
                    start()
                }
                activePlayer = player
            }.onFailure {
                smartVoice.playSmartVoice(
                    spokenWord,
                    english = english,
                    dialect = arabicDialect,
                    onPlaybackComplete = { isSpeaking = false }
                )
            }
        } else {
            smartVoice.playSmartVoice(
                spokenWord,
                english = english,
                dialect = arabicDialect,
                onPlaybackComplete = { isSpeaking = false }
            )
        }
    }

    LaunchedEffect(word.id, settingsLoaded) {
        if (!settingsLoaded) return@LaunchedEffect
        feedback = null
        heardText = null
        micPermissionExplainer = false
        // Spec setting: "Auto-speak words on open" — when off, the word
        // still speaks normally on tap, it just doesn't announce itself
        // the moment the screen appears.
        val autoSpeak = runCatching { parentSettings.autoSpeakFlow.first() }.getOrDefault(true)
        if (autoSpeak) playWord()
    }

    val reward = earnedReward
    if (reward != null) {
        RewardPopupOverlay(
            rewardTitle = reward.title,
            childName = childName,
            childGender = childGender,
            english = english,
            arabicDialect = arabicDialect,
            onDismiss = { earnedReward = null }
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
            onDismiss = { celebrationMilestone = null }
        )
        return
    }

    val surpriseCount = surpriseBoxCount
    if (surpriseCount != null) {
        SurpriseBoxOverlay(
            wordsPracticedCount = surpriseCount,
            english = english,
            onDismiss = { surpriseBoxCount = null }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
    BackTopBar(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .clickable(enabled = !isSpeaking && !isListening) {
                Haptics.vibrateTap(context, hapticEnabled)
                playWord()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Frameless "3D" picture: a soft glow disc behind the picture
        // (stands in for a drop shadow / depth cue since there's no real
        // 3D renderer here) plus a continuous gentle float + tilt, and the
        // tap-triggered personality reaction on top.
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.55f), Color.Transparent)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .graphicsLayer {
                        translationY = idleBob * 6f * density
                        rotationY = idleTilt
                        rotationZ = reactionRotationZ.value
                        val combinedScale = (1f + idleBob * 0.02f) * reactionScale.value
                        scaleX = combinedScale
                        scaleY = combinedScale
                        cameraDistance = 12f * density
                    }
            ) {
                if (word.animationStyle != "none") {
                    SituationVisual(
                        emoji = word.emoji,
                        style = word.animationStyle,
                        emojiFontSize = 110.sp,
                        modifier = Modifier.size(180.dp)
                    )
                } else {
                    WordVisual(
                        word = word,
                        emojiFontSize = 110.sp,
                        modifier = Modifier.size(180.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            displayWord,
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF3A3A3A)
        )

        Spacer(Modifier.height(12.dp))
        Text(
            if (english) "👆 Tap the picture to hear it again" else "👆 اضغط على الصورة عشان تسمع الكلمة تاني",
            fontSize = 14.sp
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (hasMicPermission()) {
                    beginListening()
                } else {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            enabled = !isSpeaking && !isListening,
            colors = ButtonDefaults.buttonColors(containerColor = Pink),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.height(64.dp).fillMaxWidth()
        ) {
            Text(
                if (isListening) {
                    if (english) "🎤 Listening..." else "🎤 بسمعك..."
                } else {
                    if (english) "🎤 Try saying it" else "🎤 جرب تقولها"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))

        GlossyCard(
            gradient = BaBaGradients.purple,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            onClick = onPlayGame
        ) {
            Text(
                if (english) "🎮 Play" else "🎮 العب",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        if (micPermissionExplainer) {
            Spacer(Modifier.height(12.dp))
            Text(
                if (english) "We need mic access so BaBa can hear you 🎤"
                else "محتاجين إذن المايك عشان BaBa يقدر يسمعك 🎤 — دوس على الزرار وسمح للتطبيق.",
                fontSize = 13.sp,
                color = Color(0xFFB71C1C)
            )
        }

        feedback?.let {
            Spacer(Modifier.height(20.dp))
            Text(it, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }

        // What the app actually heard, written back to the child — not
        // just a small parent-diagnostics note anymore. Shown as a
        // friendly speech-bubble style line so the interaction feels like
        // "BaBa is listening and telling you what it caught", which
        // matters most on devices/attempts where the mic didn't pick up a
        // clean clip to save: the child still gets to see (and re-hear,
        // via feedback above) that they were heard.
        heardText?.let {
            Spacer(Modifier.height(10.dp))
            GlossyCard(
                gradient = BaBaGradients.sky,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    (if (english) "🗨️ I heard: " else "🗨️ سمعتك تقول: ") + it,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
    }
}
