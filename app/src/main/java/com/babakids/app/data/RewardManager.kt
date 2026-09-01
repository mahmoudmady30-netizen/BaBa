package com.babakids.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Tracks the child's star count, practice attempts, distinct words
 * practiced (for the surprise-box milestone), and the daily play streak.
 * This is the seam where "AI Features" (per-word mastery, daily plans,
 * etc. from the spec) would plug in later without touching the UI layer.
 */
class RewardManager(private val context: Context) {

    private val starsKey = intPreferencesKey("stars_total")
    private val attemptsKey = intPreferencesKey("attempts_total")
    private val practicedWordIdsKey = stringSetPreferencesKey("practiced_word_ids")
    private val lastPlayDayKey = longPreferencesKey("last_play_day")
    private val streakDaysKey = intPreferencesKey("streak_days")

    private fun currentDay(): Long = System.currentTimeMillis() / (24L * 60 * 60 * 1000)

    val starsFlow: Flow<Int> =
        context.appDataStore.data.map { prefs -> prefs[starsKey] ?: 0 }

    val attemptsFlow: Flow<Int> =
        context.appDataStore.data.map { prefs -> prefs[attemptsKey] ?: 0 }

    /** Count of distinct words the child has successfully practiced at least once. */
    val practicedWordsCountFlow: Flow<Int> =
        context.appDataStore.data.map { prefs -> prefs[practicedWordIdsKey]?.size ?: 0 }

    val streakDaysFlow: Flow<Int> =
        context.appDataStore.data.map { prefs -> prefs[streakDaysKey] ?: 0 }

    /** Returns the new total after incrementing, so callers can detect milestones (every 10). */
    suspend fun addStar(): Int {
        var newTotal = 0
        context.appDataStore.edit { prefs ->
            newTotal = (prefs[starsKey] ?: 0) + 1
            prefs[starsKey] = newTotal
        }
        return newTotal
    }

    suspend fun addAttempt() {
        context.appDataStore.edit { prefs ->
            prefs[attemptsKey] = (prefs[attemptsKey] ?: 0) + 1
        }
    }

    /**
     * Records that a word was successfully practiced. Returns the new
     * distinct-word count so the caller can trigger the surprise box every
     * 5th *new* word — trying new words is what's rewarded, not repeating
     * a favorite one.
     */
    suspend fun markWordPracticed(wordId: String): Int {
        var newCount = 0
        context.appDataStore.edit { prefs ->
            val updated = (prefs[practicedWordIdsKey] ?: emptySet()) + wordId
            prefs[practicedWordIdsKey] = updated
            newCount = updated.size
        }
        return newCount
    }

    /**
     * Call once per app session (e.g. when Home first appears). Increments
     * the streak if the app was also opened yesterday, keeps it unchanged
     * if already counted today, or resets to 1 after a gap.
     */
    suspend fun recordDailyVisit(): Int {
        var newStreak = 0
        context.appDataStore.edit { prefs ->
            val today = currentDay()
            val lastDay = prefs[lastPlayDayKey] ?: -1L
            val currentStreak = prefs[streakDaysKey] ?: 0
            newStreak = when (today) {
                lastDay -> currentStreak
                lastDay + 1 -> currentStreak + 1
                else -> 1
            }
            prefs[lastPlayDayKey] = today
            prefs[streakDaysKey] = newStreak
        }
        return newStreak
    }
}
