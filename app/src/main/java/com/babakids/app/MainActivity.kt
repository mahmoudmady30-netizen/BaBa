package com.babakids.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.babakids.app.data.AppData
import com.babakids.app.data.CustomWordsRepository
import com.babakids.app.data.LearnedWordsRepository
import com.babakids.app.data.ParentSettingsManager
import com.babakids.app.data.RewardManager
import com.babakids.app.data.withOverrides
import com.babakids.app.ui.CategoryScreen
import com.babakids.app.ui.HomeScreen
import com.babakids.app.ui.ActivitiesScreen
import com.babakids.app.ui.BlockBuildingGameScreen
import com.babakids.app.ui.ColoringGameScreen
import com.babakids.app.ui.LearnedWordsScreen
import com.babakids.app.ui.MemoryMatchGameScreen
import com.babakids.app.ui.MiniGameScreen
import com.babakids.app.ui.ParentModeScreen
import com.babakids.app.ui.PuzzleGameScreen
import com.babakids.app.ui.SplashScreen
import com.babakids.app.ui.StickerCollectionScreen
import com.babakids.app.ui.WordDetailScreen
import com.babakids.app.ui.theme.BaBaGradients
import com.babakids.app.ui.theme.BaBaKidsTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaBaKidsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BaBaKidsApp()
                }
            }
        }
    }
}

/**
 * Repeatedly tapping the on-screen back button fast (very easy for a
 * child to do) could fire popBackStack() more than once before the first
 * navigation transition settles, which can pop past the start
 * destination and leave nothing left to render — a blank white screen.
 * Guarding on the current entry's lifecycle state (only pop once it's
 * actually RESUMED, i.e. the previous transition has finished) is the
 * standard fix for this exact class of bug.
 */
private fun NavHostController.safePop() {
    if (currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true) {
        popBackStack()
    }
}

private object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val CATEGORY = "category/{categoryId}"
    const val WORD = "word/{wordId}"
    const val MINI_GAME = "mini_game/{wordId}"
    const val PARENT_MODE = "parent_mode"
    const val COLLECTION = "collection"
    const val LEARNED_WORDS = "learned_words"
    const val LEARNED_WORDS_MANAGE = "learned_words_manage"
    const val ACTIVITIES = "activities"
    const val COLORING_GAME = "coloring_game"
    const val BLOCK_GAME = "block_game"
    const val MEMORY_GAME = "memory_game"
    const val PUZZLE_GAME = "puzzle_game"

    fun category(id: String) = "category/$id"
    fun word(id: String) = "word/$id"
    fun miniGame(id: String) = "mini_game/$id"
}

@Composable
private fun BaBaKidsApp() {
    val navController: NavHostController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    val rewardManager = remember { RewardManager(context) }
    val parentSettings = remember { ParentSettingsManager(context) }
    val customWordsRepository = remember { CustomWordsRepository(context) }
    val customRewardsRepository = remember { com.babakids.app.data.CustomRewardsRepository(context) }
    val learnedWordsRepository = remember { LearnedWordsRepository(context) }
    val wordOverridesRepository = remember { com.babakids.app.data.WordOverridesRepository(context) }

    val wordOverrides by wordOverridesRepository.overridesFlow.collectAsState(initial = emptyMap())
    val wordEditModeEnabled by parentSettings.wordEditModeFlow.collectAsState(initial = false)

    val stars by rewardManager.starsFlow.collectAsState(initial = 0)
    val streakDays by rewardManager.streakDaysFlow.collectAsState(initial = 0)
    val reduceMotion by parentSettings.reduceMotionFlow.collectAsState(initial = false)
    val hapticEnabled by parentSettings.hapticFeedbackEnabledFlow.collectAsState(initial = true)
    val dailyLimit by parentSettings.dailyLimitMinutesFlow.collectAsState(initial = 60)
    val minutesUsedToday by parentSettings.minutesUsedTodayFlow.collectAsState(initial = 0)
    val customWords by customWordsRepository.customWordsFlow.collectAsState(initial = emptyList())
    val customRewards by customRewardsRepository.rewardsFlow.collectAsState(initial = emptyList())
    val disabledCategories by parentSettings.disabledCategoriesFlow.collectAsState(initial = emptySet())
    val disabledActivityIds by parentSettings.disabledActivityIdsFlow.collectAsState(initial = emptySet())
    val pinnedActivityId by parentSettings.pinnedActivityIdFlow.collectAsState(initial = null)
    val disabledWordIds by parentSettings.disabledWordIdsFlow.collectAsState(initial = emptySet())
    val myWordsSelectedIds by parentSettings.myWordsSelectedIdsFlow.collectAsState(initial = emptySet())
    val lastSeenAchievementCount by parentSettings.lastSeenAchievementCountFlow.collectAsState(initial = 0)
    val currentAchievementCount = com.babakids.app.data.StickerCollection.unlockedFor(stars).size +
        customRewards.count { it.earnedAt != null }
    val newAchievementCount = (currentAchievementCount - lastSeenAchievementCount).coerceAtLeast(0)
    val learnedWords by learnedWordsRepository.learnedWordsFlow.collectAsState(initial = emptyList())
    val lastSeenLearnedWordsCount by parentSettings.lastSeenLearnedWordsCountFlow.collectAsState(initial = 0)
    val currentLearnedWordsCount = learnedWords.size
    val newLearnedWordsCount = (currentLearnedWordsCount - lastSeenLearnedWordsCount).coerceAtLeast(0)
    val appLanguage by parentSettings.appLanguageFlow.collectAsState(
        initial = ParentSettingsManager.LANGUAGE_AR
    )
    val english = appLanguage == ParentSettingsManager.LANGUAGE_EN
    val arabicDialect by parentSettings.arabicDialectFlow.collectAsState(
        initial = ParentSettingsManager.DIALECT_EGYPTIAN
    )

    // Streak: counted once per app session (cold start), not per
    // recomposition — a simple, honest "did they open the app today" check.
    LaunchedEffect(Unit) { rewardManager.recordDailyVisit() }

    // Shared between Home (lock button) and Parent Mode (unlock button) so
    // both reflect the same real state — locking from Home immediately
    // hides its own button, and unlocking from Parent Mode immediately
    // brings it back. Not persisted: a fresh app process always starts
    // unlocked, which is also the documented way to reset it.
    var deviceLocked by remember { mutableStateOf(false) }

    // Spec §13: daily usage duration. Ticks once a minute while the app is
    // in the foreground. This is a simple v1 (no background tracking).
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            parentSettings.addUsageMinute()
        }
    }

    val timeIsUp = dailyLimit > 0 && minutesUsedToday >= dailyLimit
    var parentOverride by remember { mutableStateOf(false) }
    var showParentGate by remember { mutableStateOf(false) }

    // Bug fix: parentOverride used to stay true for the rest of the app's
    // process lifetime once a parent opened Parent Mode to get past the
    // lock screen — so the lock never came back, even the next day once a
    // fresh daily limit was reached again. It should only cover the
    // *current* lockout: the moment timeIsUp genuinely goes back to false
    // (a new day starts and minutesUsedToday resets), the override is
    // cleared, so the next time the limit is hit the lock screen requires
    // a fresh parent unlock again, exactly like the first time.
    LaunchedEffect(timeIsUp) {
        if (!timeIsUp) parentOverride = false
    }

    // Root-cause fix for "language/gender selection looks reversed": Row
    // ordering (and the whole RTL/LTR layout) was following the *phone's*
    // system language, not the language chosen inside the app. If the
    // phone's OS is set to English but the app is set to Arabic, Compose
    // laid things out left-to-right while the text itself is Arabic —
    // that mismatch is what looked "backwards". This ties layout
    // direction to the in-app choice instead, everywhere, always.
    CompositionLocalProvider(
        LocalLayoutDirection provides if (english) LayoutDirection.Ltr else LayoutDirection.Rtl
    ) {
        if (timeIsUp && !parentOverride) {
            TimeUpScreen(onOpenParentMode = { parentOverride = true })
        } else {
            BaBaKidsNavHost(
                navController = navController,
                context = context,
                stars = stars,
                streakDays = streakDays,
                english = english,
                arabicDialect = arabicDialect,
                reduceMotion = reduceMotion,
                hapticEnabled = hapticEnabled,
                customWords = customWords,
                disabledCategories = disabledCategories,
                disabledActivityIds = disabledActivityIds,
                pinnedActivityId = pinnedActivityId,
                disabledWordIds = disabledWordIds,
                myWordsSelectedIds = myWordsSelectedIds,
                wordOverrides = wordOverrides,
                wordEditModeEnabled = wordEditModeEnabled,
                newAchievementCount = newAchievementCount,
                currentAchievementCount = currentAchievementCount,
                onMarkAchievementsSeen = {
                    coroutineScope.launch { parentSettings.markAchievementsSeen(currentAchievementCount) }
                },
                onOpenParentGate = { showParentGate = true },
                newLearnedWordsCount = newLearnedWordsCount,
                currentLearnedWordsCount = currentLearnedWordsCount,
                onMarkLearnedWordsSeen = {
                    coroutineScope.launch { parentSettings.markLearnedWordsSeen(currentLearnedWordsCount) }
                },
                deviceLocked = deviceLocked,
                onDeviceLockedChange = { deviceLocked = it }
            )
        }

        if (showParentGate) {
            com.babakids.app.ui.ParentGateDialog(
                onSuccess = {
                    showParentGate = false
                    navController.navigate(Routes.PARENT_MODE)
                },
                onDismiss = { showParentGate = false }
            )
        }
    }
}

@Composable
private fun BaBaKidsNavHost(
    navController: NavHostController,
    context: android.content.Context,
    stars: Int,
    streakDays: Int,
    english: Boolean,
    arabicDialect: String,
    reduceMotion: Boolean,
    hapticEnabled: Boolean,
    customWords: List<com.babakids.app.data.WordItem>,
    disabledCategories: Set<String>,
    disabledActivityIds: Set<String>,
    pinnedActivityId: String?,
    disabledWordIds: Set<String>,
    myWordsSelectedIds: Set<String>,
    wordOverrides: Map<String, com.babakids.app.data.WordOverride>,
    wordEditModeEnabled: Boolean,
    newAchievementCount: Int,
    currentAchievementCount: Int,
    onMarkAchievementsSeen: () -> Unit,
    onOpenParentGate: () -> Unit,
    newLearnedWordsCount: Int,
    currentLearnedWordsCount: Int,
    onMarkLearnedWordsSeen: () -> Unit,
    deviceLocked: Boolean,
    onDeviceLockedChange: (Boolean) -> Unit
) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(
                reduceMotion = reduceMotion,
                onFinished = {
                    // Onboarding (language/name/gender/age) now happens as
                    // part of SplashScreen itself for first-time users —
                    // by the time onFinished() fires, it has already
                    // called setOnboarded(true) if needed, so this can
                    // always go straight to Home with no separate route
                    // or extra flag check (which used to race against the
                    // DataStore write completing).
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                stars = stars,
                streakDays = streakDays,
                english = english,
                arabicDialect = arabicDialect,
                hapticEnabled = hapticEnabled,
                deviceLocked = deviceLocked,
                disabledCategories = disabledCategories,
                newAchievementCount = newAchievementCount,
                newLearnedWordsCount = newLearnedWordsCount,
                onLockDevice = {
                    runCatching { (context as? android.app.Activity)?.startLockTask() }
                    onDeviceLockedChange(true)
                },
                onCategoryClick = { category -> navController.navigate(Routes.category(category.id)) },
                onCollectionClick = {
                    onMarkAchievementsSeen()
                    navController.navigate(Routes.COLLECTION)
                },
                onMyWordsClick = { navController.navigate(Routes.category("my_words")) },
                onLearnedWordsClick = {
                    onMarkLearnedWordsSeen()
                    navController.navigate(Routes.LEARNED_WORDS)
                },
                onActivitiesClick = { navController.navigate(Routes.ACTIVITIES) },
                onParentModeClick = { onOpenParentGate() }
            )
        }

        composable(Routes.LEARNED_WORDS) {
            LearnedWordsScreen(
                english = english,
                onBack = { navController.safePop() }
            )
        }

        composable(Routes.LEARNED_WORDS_MANAGE) {
            LearnedWordsScreen(
                english = english,
                parentControlsEnabled = true,
                onBack = { navController.safePop() }
            )
        }

        composable(Routes.ACTIVITIES) {
            ActivitiesScreen(
                english = english,
                disabledActivityIds = disabledActivityIds,
                pinnedActivityId = pinnedActivityId,
                onBack = { navController.safePop() },
                onActivityClick = { activityId ->
                    when (activityId) {
                        "coloring" -> navController.navigate(Routes.COLORING_GAME)
                        "blocks" -> navController.navigate(Routes.BLOCK_GAME)
                        "memory" -> navController.navigate(Routes.MEMORY_GAME)
                        "puzzle" -> navController.navigate(Routes.PUZZLE_GAME)
                    }
                }
            )
        }

        composable(Routes.COLORING_GAME) {
            ColoringGameScreen(
                english = english,
                arabicDialect = arabicDialect,
                onBack = { navController.safePop() }
            )
        }

        composable(Routes.BLOCK_GAME) {
            BlockBuildingGameScreen(
                english = english,
                onBack = { navController.safePop() }
            )
        }

        composable(Routes.MEMORY_GAME) {
            MemoryMatchGameScreen(
                english = english,
                reduceMotion = reduceMotion,
                hapticEnabled = hapticEnabled,
                onBack = { navController.safePop() }
            )
        }

        composable(Routes.PUZZLE_GAME) {
            PuzzleGameScreen(
                english = english,
                reduceMotion = reduceMotion,
                hapticEnabled = hapticEnabled,
                onBack = { navController.safePop() }
            )
        }

        composable(Routes.COLLECTION) {
            StickerCollectionScreen(
                stars = stars,
                english = english,
                onBack = { navController.safePop() }
            )
        }

        composable(Routes.CATEGORY) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: return@composable
            CategoryScreen(
                categoryId = categoryId,
                english = english,
                hapticEnabled = hapticEnabled,
                stars = stars,
                disabledWordIds = disabledWordIds,
                myWordsSelectedIds = myWordsSelectedIds,
                editModeEnabled = wordEditModeEnabled,
                onBack = { navController.safePop() },
                onWordClick = { word -> navController.navigate(Routes.word(word.id)) }
            )
        }

        composable(Routes.WORD) { backStackEntry ->
            val wordId = backStackEntry.arguments?.getString("wordId") ?: return@composable
            val word = (AppData.words + AppData.arabicLetters + AppData.englishLetters + customWords)
                .withOverrides(wordOverrides)
                .firstOrNull { it.id == wordId } ?: return@composable
            WordDetailScreen(
                word = word,
                reduceMotion = reduceMotion,
                hapticEnabled = hapticEnabled,
                onBack = { navController.safePop() },
                onPlayGame = { navController.navigate(Routes.miniGame(word.id)) }
            )
        }

        composable(Routes.MINI_GAME) { backStackEntry ->
            val wordId = backStackEntry.arguments?.getString("wordId") ?: return@composable
            val word = (AppData.words + AppData.arabicLetters + AppData.englishLetters + customWords)
                .withOverrides(wordOverrides)
                .firstOrNull { it.id == wordId } ?: return@composable
            MiniGameScreen(
                word = word,
                reduceMotion = reduceMotion,
                hapticEnabled = hapticEnabled,
                onExit = { navController.safePop() }
            )
        }

        composable(Routes.PARENT_MODE) {
            ParentModeScreen(
                onBack = { navController.safePop() },
                deviceLocked = deviceLocked,
                onDeviceLockedChange = onDeviceLockedChange,
                onManageLearnedWords = { navController.navigate(Routes.LEARNED_WORDS_MANAGE) }
            )
        }
    }
}

@Composable
private fun TimeUpScreen(onOpenParentMode: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BaBaGradients.purple)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("😴🧸", fontSize = 72.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "خلص وقت اللعب النهارده!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color.White
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "تعالى بكرة نكمل مع BaBa 💛",
            fontSize = 16.sp,
            color = androidx.compose.ui.graphics.Color.White
        )
        Spacer(Modifier.height(32.dp))
        Text(
            "(للوالدين: تقدروا تغيروا مدة الاستخدام من إعدادات الوالدين)",
            fontSize = 12.sp,
            color = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier.padding(top = 24.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "⚙️ فتح إعدادات الوالدين",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier
                .padding(12.dp)
                .clickable(onClick = onOpenParentMode)
        )
    }
}
