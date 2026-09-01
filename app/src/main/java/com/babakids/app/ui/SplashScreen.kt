package com.babakids.app.ui

import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.babakids.app.audio.SmartVoiceManager
import com.babakids.app.data.ArcadeSounds
import com.babakids.app.data.ParentSettingsManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Opening sequence — starts directly on the voxel/blocky "BaBa" logo
 * reveal (bevelled letter cubes dropping into place with a confetti
 * burst) against a bright Mario-style sky-blue background. No running
 * character and no coin-collecting beat anymore — "BaBa" is the opening
 * itself, not something you wait through an intro to reach.
 *
 * Path priority, same as always:
 * 1) `assets/opening.mp4`, if present, plays as a real rendered video.
 * 2) Otherwise, this Compose-native sequence.
 *
 * For a brand-new install, onboarding (language, child's name, gender,
 * age group) now happens as a direct continuation of this same screen —
 * same background, no separate navigation — right after the reveal
 * animation finishes, instead of jumping to a completely different
 * screen. Reduce-motion and returning users get a short version; tap
 * anywhere skips straight past the animation (never past onboarding
 * itself, which still needs an actual answer).
 *
 * Honest limitation: this is 2D property animation on hand-drawn shapes,
 * not a real game engine or a sprite with actual walk-cycle frames.
 */
private const val OPENING_VIDEO_ASSET = "opening.mp4"

/** The Mario-style bright sky-blue used throughout the opening + onboarding continuation. */
private val SkyBlue = Color(0xFF64B5F6)
private val SkyBlueLight = Color(0xFF90CAF9)
private val InkText = Color(0xFF1A237E)

@Composable
fun SplashScreen(onFinished: () -> Unit, reduceMotion: Boolean = false) {
    val context = LocalContext.current

    val hasVideo = remember {
        runCatching { context.assets.open(OPENING_VIDEO_ASSET).close(); true }.getOrDefault(false)
    }
    var videoFailed by remember { mutableStateOf(false) }

    if (hasVideo && !videoFailed) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(Uri.parse("file:///android_asset/$OPENING_VIDEO_ASSET"))
                    setOnPreparedListener { mediaPlayer ->
                        mediaPlayer.isLooping = false
                        start()
                    }
                    setOnCompletionListener { onFinished() }
                    setOnErrorListener { _, _, _ ->
                        videoFailed = true
                        true
                    }
                }
            }
        )
        return
    }

    ArcadeVoxelOpening(onFinished = onFinished, reduceMotion = reduceMotion)
}

private const val PHASE_LOGO = 3
private const val PHASE_READY = 4
private const val PHASE_ONBOARDING = 5
private const val PHASE_ZOOM = 6

@Composable
private fun ArcadeVoxelOpening(onFinished: () -> Unit, reduceMotion: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Bundled VoiceTut audio (tier 1) -> local cache (tier 2) -> device
    // TTS fallback — "بابا" is one of the bundled phrases, so the opening
    // uses the real recorded voice too when it's available.
    val smartVoice = remember { SmartVoiceManager.getInstance(context) }
    val parentSettings = remember { ParentSettingsManager(context) }
    val sounds = remember { ArcadeSounds() }
    DisposableEffect(Unit) {
        onDispose {
            sounds.release()
            smartVoice.release()
        }
    }

    var english by remember { mutableStateOf(false) }
    var isReturningUser by remember { mutableStateOf(false) }
    var settingsLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        parentSettings.appLanguageFlow.collect { english = it == ParentSettingsManager.LANGUAGE_EN }
    }
    LaunchedEffect(Unit) {
        parentSettings.hasOnboardedFlow.collect {
            isReturningUser = it
            settingsLoaded = true
        }
    }

    var phase by remember { mutableStateOf(PHASE_LOGO) }
    var finishedOnce by remember { mutableStateOf(false) }
    fun finishOnce() {
        if (!finishedOnce) {
            finishedOnce = true
            onFinished()
        }
    }

    val zoomOutScale = remember { Animatable(1f) }
    val zoomOutAlpha = remember { Animatable(1f) }

    fun zoomAndFinish() {
        scope.launch {
            phase = PHASE_ZOOM
            launch { zoomOutScale.animateTo(1.35f, tween(500, easing = LinearEasing)) }
            zoomOutAlpha.animateTo(0f, tween(450, easing = LinearEasing))
            finishOnce()
        }
    }

    var animationJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(settingsLoaded) {
        if (!settingsLoaded) return@LaunchedEffect

        // "BaBa" is the opening now — no boot screen, no running character,
        // no coins. Just the logo reveal, straight away.
        val job = launch {
            phase = PHASE_LOGO
            smartVoice.playSmartVoice("بابا", english = english, pitch = 1.4f)
            if (!reduceMotion) {
                launch { sounds.playLevelUpJingle() }
            }
            delay(
                when {
                    reduceMotion -> 1200L
                    isReturningUser -> 1500L
                    else -> 3200L // longer now: the tagline appears ~1s in, so it needs time on screen
                }
            )

            if (!reduceMotion && !isReturningUser) {
                phase = PHASE_READY
                delay(1500)
            }
        }
        animationJob = job
        // join() returns whether the animation finished naturally OR was
        // cancelled by a tap (see the Box's clickable below) — either way,
        // what happens next is the same: a returning user goes straight
        // home, a first-time user always sees onboarding. Tapping to skip
        // the animation used to call finishOnce() directly, which is
        // exactly the bug where it "goes straight into the app without
        // finishing" and onboarding never showed for a new user — tapping
        // now only ever skips the *animation*, never onboarding itself.
        job.join()

        if (isReturningUser) {
            zoomAndFinish()
        } else {
            // First-time setup happens right here, same screen, same
            // background — the onboarding UI's own "Start" button is what
            // eventually calls zoomAndFinish(), not a fixed delay.
            phase = PHASE_ONBOARDING
        }
    }

    // The whole opening — logo letters, HUD, ground tiles, world layout —
    // is forced to LTR regardless of the app's language setting. Without
    // this, when the app language is Arabic the entire screen (including
    // Splash) runs under RTL layout direction, and Compose's Row reverses
    // multi-child ordering under RTL — which is exactly why the "BaBa"
    // logo letters sometimes rendered as "aBaB": the brand wordmark and
    // this screen's visual layout should never depend on app language.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyBlue)
            .then(
                // Entirely omitted (not just disabled) during onboarding —
                // a disabled clickable still participates in pointer-input
                // hit-testing, which is one more thing that could get in
                // the way of the text field underneath reliably gaining
                // focus/receiving taps.
                if (phase != PHASE_ONBOARDING) {
                    Modifier.clickable(
                        onClick = {
                            // Cancels the animation coroutine early — the code
                            // right after job.join() above then decides correctly
                            // whether to show onboarding or go home, instead of
                            // this handler blindly finishing straight to Home.
                            animationJob?.cancel()
                        }
                    )
                } else {
                    Modifier
                }
            )
            .graphicsLayer {
                scaleX = zoomOutScale.value
                scaleY = zoomOutScale.value
                alpha = zoomOutAlpha.value
            }
    ) {
        when (phase) {
            PHASE_LOGO -> VoxelLogoScene(english = english, reduceMotion = reduceMotion)
            PHASE_READY -> ReadyScene(english = english)
            PHASE_ONBOARDING -> OnboardingContinuation(
                initialLanguageEnglish = english,
                parentSettings = parentSettings,
                onComplete = { zoomAndFinish() }
            )
            else -> {}
        }
    }
    }
}


/**
 * "BaBa" built from bevelled voxel/cube letters that drop into place one
 * at a time, celebrated with a confetti burst as they land. The two "Ba"
 * syllables get a slightly wider gap between them than the letters
 * within each syllable, so it reads as "Ba · Ba" rather than one solid
 * "BaBa" block.
 */
@Composable
private fun VoxelLogoScene(english: Boolean, reduceMotion: Boolean) {
    val letters = listOf("B", "a", "B", "a")
    val cubeColors = listOf(Color(0xFFEF5350), Color(0xFFFFB300), Color(0xFF42A5F5), Color(0xFFAB47BC))
    val drops = remember { letters.map { Animatable(-260f) } }
    var subtitleVisible by remember { mutableStateOf(false) }
    var taglineVisible by remember { mutableStateOf(false) }
    var showCoins by remember { mutableStateOf(false) }

    // Slow continuous push-in on the whole scene — the single cheapest
    // trick that reads as "cinematic camera" rather than a static title
    // card. Runs for the whole scene, not as a one-shot.
    val cameraPush = remember { Animatable(if (reduceMotion) 1f else 0.88f) }
    // Soft glow behind the logo that breathes in and out, so the letters
    // sit in light instead of on a flat colour.
    val glowTransition = rememberInfiniteTransition(label = "logoGlow")
    val glowScale = glowTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable<Float>(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoGlowScale"
    )

    LaunchedEffect(Unit) {
        if (!reduceMotion) {
            launch { cameraPush.animateTo(1.06f, tween(4200, easing = LinearEasing)) }
        }
        drops.forEachIndexed { index, anim ->
            launch {
                delay(index * 160L)
                anim.animateTo(0f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = 250f))
            }
        }
        delay(letters.size * 160L + 300L)
        showCoins = true
        delay(200L)
        subtitleVisible = true
        delay(500L)
        taglineVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = cameraPush.value
                scaleY = cameraPush.value
            }
    ) {
        if (showCoins && !reduceMotion) {
            ConfettiBurst(particleCount = 14, richPalette = true)
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Radial glow pool behind the letters.
                if (!reduceMotion) {
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .graphicsLayer {
                                scaleX = glowScale.value
                                scaleY = glowScale.value
                            }
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.55f),
                                        Color.White.copy(alpha = 0.12f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
                Row {
                    letters.forEachIndexed { index, letter ->
                        // A slightly wider gap right before the second "B" —
                        // the start of the second "Ba" syllable — than the
                        // 4dp used between letters within the same syllable.
                        val startGap = if (index == 2) 14.dp else 4.dp
                        VoxelCube(
                            letter = letter,
                            color = cubeColors[index],
                            modifier = Modifier
                                .padding(start = startGap, end = 4.dp)
                                .graphicsLayer { translationY = drops[index].value }
                        )
                    }
                }
            }
            if (subtitleVisible) {
                Spacer(Modifier.height(14.dp))
                Text(
                    if (english) "App for Kids" else "تطبيق للأطفال",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = InkText
                )
            }
            if (taglineVisible) {
                Spacer(Modifier.height(10.dp))
                // The "Learn • Play • Grow" tagline from the storyboard,
                // each word in its own colour like the reference art.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (english) "Learn" else "اتعلم",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFF9800)
                    )
                    Text(" • ", fontSize = 18.sp, color = InkText)
                    Text(
                        if (english) "Play" else "العب",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2196F3)
                    )
                    Text(" • ", fontSize = 18.sp, color = InkText)
                    Text(
                        if (english) "Grow" else "اكبر",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

/** "Ready Player" / "Press Start" style prompt before onboarding or entering the app. */
@Composable
private fun ReadyScene(english: Boolean) {
    val blinkTransition = rememberInfiniteTransition(label = "readyBlink")
    val blinkAlpha = blinkTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable<Float>(
            animation = tween(300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "readyBlinkAlpha"
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (english) "🎮 READY PLAYER ONE! 🎮" else "🎮 خلّيك جاهز! 🎮",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            color = InkText
        )
        Spacer(Modifier.height(14.dp))
        Text(
            if (english) "TAP TO START" else "دوس تبدأ",
            fontSize = 16.sp,
            fontFamily = FontFamily.Monospace,
            color = InkText,
            modifier = Modifier.graphicsLayer { alpha = blinkAlpha.value }
        )
    }
}

/**
 * First-run setup (language, child's name, gender, age group) rendered
 * as a direct continuation of the opening — same sky background, no
 * separate screen/navigation. Only calls onComplete() once every step is
 * answered and settings are saved.
 */
@Composable
private fun OnboardingContinuation(
    initialLanguageEnglish: Boolean,
    parentSettings: ParentSettingsManager,
    onComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(0) }
    var languageEnglish by remember { mutableStateOf(initialLanguageEnglish) }
    var childName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(ParentSettingsManager.GENDER_MALE) }

    val english = languageEnglish

    // The parent screen forces LTR everywhere (needed to keep the "BaBa"
    // logo letters in a fixed order) — but that same forcing breaks
    // Arabic text INPUT specifically: editing RTL text inside a text
    // field under a mismatched forced-LTR layout direction is a known
    // category of text-field bugs (cursor/selection behaving as if
    // nothing is being typed). This step is actual data entry, not a
    // fixed brand wordmark, so it restores the correct direction for
    // whichever language the parent just chose.
    CompositionLocalProvider(
        LocalLayoutDirection provides if (languageEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl
    ) {
    Column(
        modifier = Modifier.fillMaxSize().imePadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("👋🧸", fontSize = 56.sp)
        Spacer(Modifier.height(20.dp))

        when (step) {
            0 -> {
                Text(
                    if (english) "Choose a language" else "اختار اللغة",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkText
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OnboardingChoiceButton(
                        label = "العربية",
                        selected = !languageEnglish,
                        onClick = { languageEnglish = false }
                    )
                    OnboardingChoiceButton(
                        label = "English",
                        selected = languageEnglish,
                        onClick = { languageEnglish = true }
                    )
                }
            }
            1 -> {
                Text(
                    if (english) "What's the child's name?" else "اسم الطفل إيه؟",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkText
                )
                Spacer(Modifier.height(20.dp))
                // A proper card container instead of a bare text field —
                // matches the visual weight of the other steps' choice
                // buttons, and fillMaxWidth() + centered textStyle
                // together are what actually keeps the typed name
                // centered (a bare-width field was the real cause of it
                // looking off-center before).
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(20.dp))
                        .background(Color.White, RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("✏️", fontSize = 32.sp)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = childName,
                            onValueChange = { childName = it },
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            singleLine = true,
                            label = {
                                Text(
                                    if (english) "Child's name" else "اسم الطفل",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            2 -> {
                Text(
                    if (english) "Boy or girl?" else "ولد ولا بنت؟",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkText
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OnboardingChoiceButton(
                        label = if (english) "Boy 👦" else "ولد 👦",
                        selected = gender == ParentSettingsManager.GENDER_MALE,
                        onClick = { gender = ParentSettingsManager.GENDER_MALE }
                    )
                    OnboardingChoiceButton(
                        label = if (english) "Girl 👧" else "بنت 👧",
                        selected = gender == ParentSettingsManager.GENDER_FEMALE,
                        onClick = { gender = ParentSettingsManager.GENDER_FEMALE }
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        val canAdvance = when (step) {
            1 -> childName.trim().isNotEmpty()
            else -> true
        }

        Button(
            onClick = {
                if (step < 2) {
                    step += 1
                } else {
                    scope.launch {
                        parentSettings.setAppLanguage(
                            if (languageEnglish) ParentSettingsManager.LANGUAGE_EN else ParentSettingsManager.LANGUAGE_AR
                        )
                        parentSettings.setChildName(childName.trim())
                        parentSettings.setChildGender(gender)
                        parentSettings.setOnboarded(true)
                        onComplete()
                    }
                }
            },
            enabled = canAdvance,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(
                when {
                    step < 2 -> if (english) "Next" else "التالي"
                    else -> if (english) "Start! 🎉" else "يلا نبدأ! 🎉"
                },
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
    }
}

@Composable
private fun OnboardingChoiceButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) InkText else SkyBlueLight)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
