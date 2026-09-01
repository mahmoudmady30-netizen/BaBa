package com.babakids.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** One synchronized read of every setting that affects how the child-facing screens speak and greet. */
data class ChildSettingsSnapshot(
    val childName: String,
    val childGender: String,
    val appLanguage: String,
    val arabicDialect: String
)

/**
 * Spec §13 (daily usage duration, changeable parent PIN, child's name) and
 * §19 (accessibility — "reduce motion from parent settings if it bothers
 * the child").
 *
 * Days are tracked as a simple epoch-day Long (millis / a day) instead of
 * java.time, since this project's minSdk (24) is below java.time's
 * unbundled minimum (26) and pulling in desugaring just for this isn't
 * worth it.
 */
class ParentSettingsManager(private val context: Context) {

    companion object {
        const val DEFAULT_PIN = "1234"
        const val GENDER_MALE = "male"
        const val GENDER_FEMALE = "female"
        const val LANGUAGE_AR = "ar"
        const val LANGUAGE_EN = "en"
        const val DIALECT_EGYPTIAN = "eg"
        const val DIALECT_FUSHA = "fusha"
    }

    private val dailyLimitKey = intPreferencesKey("daily_limit_minutes")
    private val reduceMotionKey = booleanPreferencesKey("reduce_motion")
    private val homeAnimationKey = booleanPreferencesKey("home_animation_enabled")
    private val hapticFeedbackKey = booleanPreferencesKey("haptic_feedback_enabled")
    private val voiceEnabledKey = booleanPreferencesKey("voice_enabled")
    private val voiceVolumeKey = floatPreferencesKey("voice_volume")
    private val autoSpeakKey = booleanPreferencesKey("auto_speak_enabled")
    private val minutesUsedTodayKey = intPreferencesKey("minutes_used_today")
    private val lastUsageDayKey = longPreferencesKey("last_usage_day")
    private val childNameKey = stringPreferencesKey("child_name")
    private val parentPinKey = stringPreferencesKey("parent_pin")
    private val childGenderKey = stringPreferencesKey("child_gender")
    private val appLanguageKey = stringPreferencesKey("app_language")
    private val arabicDialectKey = stringPreferencesKey("arabic_dialect")
    private val onboardedKey = booleanPreferencesKey("has_onboarded")
    private val childAgeGroupKey = stringPreferencesKey("child_age_group")
    private val disabledCategoriesKey = stringSetPreferencesKey("disabled_categories")
    private val disabledActivityIdsKey = stringSetPreferencesKey("disabled_activity_ids")
    private val pinnedActivityIdKey = stringPreferencesKey("pinned_activity_id")
    private val disabledWordIdsKey = stringSetPreferencesKey("disabled_word_ids")
    private val myWordsSelectedIdsKey = stringSetPreferencesKey("my_words_selected_ids")
    private val lastSeenAchievementCountKey = intPreferencesKey("last_seen_achievement_count")
    private val lastSeenLearnedWordsCountKey = intPreferencesKey("last_seen_learned_words_count")
    private val wordEditModeKey = booleanPreferencesKey("word_edit_mode_enabled")

    private val defaultDailyLimit = 60 // minutes; 0 means "no limit"
    private fun currentDay(): Long = System.currentTimeMillis() / (24L * 60 * 60 * 1000)

    val dailyLimitMinutesFlow: Flow<Int> =
        context.appDataStore.data.map { prefs -> prefs[dailyLimitKey] ?: defaultDailyLimit }

    val reduceMotionFlow: Flow<Boolean> =
        context.appDataStore.data.map { prefs -> prefs[reduceMotionKey] ?: false }

    /** Separate from reduceMotion — a focused on/off switch for just the home screen's idle float animation. */
    val homeAnimationEnabledFlow: Flow<Boolean> =
        context.appDataStore.data.map { prefs -> prefs[homeAnimationKey] ?: true }

    /**
     * When on, every unlocked word card shows a small ✏️ pencil the parent
     * can tap to replace that word's picture and/or record a new
     * pronunciation for it — for built-in words too, not just custom ones.
     * Off by default so a curious child tapping around doesn't accidentally
     * open an edit dialog.
     */
    val wordEditModeFlow: Flow<Boolean> =
        context.appDataStore.data.map { prefs -> prefs[wordEditModeKey] ?: false }

    /** Physical vibration feedback on taps and correct answers — on by default, parent can turn it off. */
    val hapticFeedbackEnabledFlow: Flow<Boolean> =
        context.appDataStore.data.map { prefs -> prefs[hapticFeedbackKey] ?: true }

    /** Master on/off switch for all spoken audio — on by default. */
    val voiceEnabledFlow: Flow<Boolean> =
        context.appDataStore.data.map { prefs -> prefs[voiceEnabledKey] ?: true }

    /** 0f (silent) to 1f (full) — applied to bundled/cached audio playback. */
    val voiceVolumeFlow: Flow<Float> =
        context.appDataStore.data.map { prefs -> prefs[voiceVolumeKey] ?: 1f }

    /** Whether word/category screens should speak automatically on open, vs. only on tap. */
    val autoSpeakFlow: Flow<Boolean> =
        context.appDataStore.data.map { prefs -> prefs[autoSpeakKey] ?: true }

    /** Empty string means "no name set yet" — callers fall back to a generic greeting. */
    val childNameFlow: Flow<String> =
        context.appDataStore.data.map { prefs -> prefs[childNameKey] ?: "" }

    val parentPinFlow: Flow<String> =
        context.appDataStore.data.map { prefs -> prefs[parentPinKey] ?: DEFAULT_PIN }

    /** GENDER_MALE or GENDER_FEMALE — used for the fallback address ("بطل"/"بطلة") when no name is set. */
    val childGenderFlow: Flow<String> =
        context.appDataStore.data.map { prefs -> prefs[childGenderKey] ?: GENDER_MALE }

    /**
     * LANGUAGE_AR or LANGUAGE_EN — affects word content, category titles,
     * and speech locale. Defaults to the *device's* current language for
     * a brand-new install (before anything has been explicitly chosen),
     * instead of always defaulting to Arabic regardless of the phone's
     * own language setting.
     */
    val appLanguageFlow: Flow<String> =
        context.appDataStore.data.map { prefs -> prefs[appLanguageKey] ?: deviceDefaultLanguage() }

    /** DIALECT_EGYPTIAN or DIALECT_FUSHA — only relevant when appLanguage is Arabic. */
    val arabicDialectFlow: Flow<String> =
        context.appDataStore.data.map { prefs -> prefs[arabicDialectKey] ?: DIALECT_EGYPTIAN }

    /** False until the first-run onboarding (name/language/gender) has been completed once. */
    val hasOnboardedFlow: Flow<Boolean> =
        context.appDataStore.data.map { prefs -> prefs[onboardedKey] ?: false }

    /** A simple age group ("2-3", "4-5", "6+") collected once during onboarding, empty until set. */
    val childAgeGroupFlow: Flow<String> =
        context.appDataStore.data.map { prefs -> prefs[childAgeGroupKey] ?: "" }

    /**
     * Category IDs the parent has turned OFF from the child's home screen
     * — empty by default (everything visible). "So the child doesn't get
     * distracted": a parent can narrow the app down to just a few topics.
     */
    val disabledCategoriesFlow: Flow<Set<String>> =
        context.appDataStore.data.map { prefs -> prefs[disabledCategoriesKey] ?: emptySet() }

    /** Games a parent has hidden from "ألعابي" (My Games) — same pattern as disabledCategories but for the mini-games hub. */
    val disabledActivityIdsFlow: Flow<Set<String>> =
        context.appDataStore.data.map { prefs -> prefs[disabledActivityIdsKey] ?: emptySet() }

    /** The one game, if any, a parent has pinned to the front of "ألعابي". */
    val pinnedActivityIdFlow: Flow<String?> =
        context.appDataStore.data.map { prefs -> prefs[pinnedActivityIdKey] }

    /** Individual word IDs hidden from the child regardless of category — finer-grained than disabling a whole category. */
    val disabledWordIdsFlow: Flow<Set<String>> =
        context.appDataStore.data.map { prefs -> prefs[disabledWordIdsKey] ?: emptySet() }

    /**
     * Existing built-in (or custom) word IDs a parent has explicitly
     * picked to also feature in the "My Words" category — separate from
     * custom words, which are always auto-included there. Purely
     * additive: a picked word still appears under its normal category too.
     */
    val myWordsSelectedIdsFlow: Flow<Set<String>> =
        context.appDataStore.data.map { prefs -> prefs[myWordsSelectedIdsKey] ?: emptySet() }

    /** Emits minutes used today, automatically treating a stale stored day as 0. */
    val minutesUsedTodayFlow: Flow<Int> =
        context.appDataStore.data.map { prefs ->
            val storedDay = prefs[lastUsageDayKey] ?: -1L
            if (storedDay == currentDay()) prefs[minutesUsedTodayKey] ?: 0 else 0
        }

    /**
     * One synchronized snapshot of everything that affects child-screen
     * speech/greeting. Screens that auto-speak on first composition (e.g.
     * WordDetailScreen) should wait for this to emit at least once before
     * speaking — reading the 4 flows separately race against each
     * composable's own default initial state, which is how "switched to
     * English, but the very first word still speaks Arabic" happened: the
     * auto-play fired before the language flow's real value had arrived.
     */
    fun combinedChildSettingsFlow(): Flow<ChildSettingsSnapshot> =
        combine(childNameFlow, childGenderFlow, appLanguageFlow, arabicDialectFlow) { name, gender, lang, dialect ->
            ChildSettingsSnapshot(name, gender, lang, dialect)
        }

    suspend fun setDailyLimitMinutes(minutes: Int) {
        context.appDataStore.edit { prefs -> prefs[dailyLimitKey] = minutes }
    }

    suspend fun setReduceMotion(enabled: Boolean) {
        context.appDataStore.edit { prefs -> prefs[reduceMotionKey] = enabled }
    }

    suspend fun setHomeAnimationEnabled(enabled: Boolean) {
        context.appDataStore.edit { prefs -> prefs[homeAnimationKey] = enabled }
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.appDataStore.edit { prefs -> prefs[hapticFeedbackKey] = enabled }
    }

    suspend fun setWordEditModeEnabled(enabled: Boolean) {
        context.appDataStore.edit { prefs -> prefs[wordEditModeKey] = enabled }
    }

    suspend fun setVoiceEnabled(enabled: Boolean) {
        context.appDataStore.edit { prefs -> prefs[voiceEnabledKey] = enabled }
    }

    suspend fun setVoiceVolume(volume: Float) {
        context.appDataStore.edit { prefs -> prefs[voiceVolumeKey] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setAutoSpeak(enabled: Boolean) {
        context.appDataStore.edit { prefs -> prefs[autoSpeakKey] = enabled }
    }

    suspend fun setChildName(name: String) {
        context.appDataStore.edit { prefs -> prefs[childNameKey] = name.trim() }
    }

    suspend fun setParentPin(newPin: String) {
        context.appDataStore.edit { prefs -> prefs[parentPinKey] = newPin }
    }

    suspend fun setChildGender(gender: String) {
        context.appDataStore.edit { prefs -> prefs[childGenderKey] = gender }
    }

    suspend fun setAppLanguage(language: String) {
        context.appDataStore.edit { prefs -> prefs[appLanguageKey] = language }
    }

    suspend fun setArabicDialect(dialect: String) {
        context.appDataStore.edit { prefs -> prefs[arabicDialectKey] = dialect }
    }

    suspend fun setOnboarded(completed: Boolean = true) {
        context.appDataStore.edit { prefs -> prefs[onboardedKey] = completed }
    }

    suspend fun setChildAgeGroup(ageGroup: String) {
        context.appDataStore.edit { prefs -> prefs[childAgeGroupKey] = ageGroup }
    }

    suspend fun setCategoryEnabled(categoryId: String, enabled: Boolean) {
        context.appDataStore.edit { prefs ->
            val current = prefs[disabledCategoriesKey] ?: emptySet()
            prefs[disabledCategoriesKey] = if (enabled) current - categoryId else current + categoryId
        }
    }

    suspend fun setActivityEnabled(activityId: String, enabled: Boolean) {
        context.appDataStore.edit { prefs ->
            val current = prefs[disabledActivityIdsKey] ?: emptySet()
            prefs[disabledActivityIdsKey] = if (enabled) current - activityId else current + activityId
        }
    }

    /** Pass null to unpin. Pinning a hidden game has no visible effect until it's shown again. */
    suspend fun setPinnedActivity(activityId: String?) {
        context.appDataStore.edit { prefs ->
            if (activityId == null) prefs.remove(pinnedActivityIdKey) else prefs[pinnedActivityIdKey] = activityId
        }
    }

    suspend fun setWordEnabled(wordId: String, enabled: Boolean) {
        context.appDataStore.edit { prefs ->
            val current = prefs[disabledWordIdsKey] ?: emptySet()
            prefs[disabledWordIdsKey] = if (enabled) current - wordId else current + wordId
        }
    }

    suspend fun setWordInMyWords(wordId: String, included: Boolean) {
        context.appDataStore.edit { prefs ->
            val current = prefs[myWordsSelectedIdsKey] ?: emptySet()
            prefs[myWordsSelectedIdsKey] = if (included) current + wordId else current - wordId
        }
    }

    /**
     * How many total achievements (unlocked stickers + earned real-world
     * rewards) had been seen the last time the child opened the Rewards
     * screen — used to compute a "new achievement" badge count on the
     * gift icon without needing a separate per-achievement seen/unseen
     * flag for each one.
     */
    val lastSeenAchievementCountFlow: Flow<Int> =
        context.appDataStore.data.map { prefs -> prefs[lastSeenAchievementCountKey] ?: 0 }

    suspend fun markAchievementsSeen(currentCount: Int) {
        context.appDataStore.edit { prefs -> prefs[lastSeenAchievementCountKey] = currentCount }
    }

    /** Same "new since last seen" pattern as achievements, for the Learned Words badge. */
    val lastSeenLearnedWordsCountFlow: Flow<Int> =
        context.appDataStore.data.map { prefs -> prefs[lastSeenLearnedWordsCountKey] ?: 0 }

    suspend fun markLearnedWordsSeen(currentCount: Int) {
        context.appDataStore.edit { prefs -> prefs[lastSeenLearnedWordsCountKey] = currentCount }
    }

    /**
     * Reads the device's current system language synchronously (no
     * DataStore involved) — used both as appLanguageFlow's fallback and
     * directly by the splash screen's onboarding continuation, which
     * picks a default language before anything is persisted yet.
     */
    fun deviceDefaultLanguage(): String {
        val locale = context.resources.configuration.locales[0]
        return if (locale.language.equals("en", ignoreCase = true)) LANGUAGE_EN else LANGUAGE_AR
    }

    /** Call roughly once per minute while the app is in the foreground. */
    suspend fun addUsageMinute() {
        context.appDataStore.edit { prefs ->
            val storedDay = prefs[lastUsageDayKey] ?: -1L
            val today = currentDay()
            val minutesSoFar = if (storedDay == today) prefs[minutesUsedTodayKey] ?: 0 else 0
            prefs[lastUsageDayKey] = today
            prefs[minutesUsedTodayKey] = minutesSoFar + 1
        }
    }
}
