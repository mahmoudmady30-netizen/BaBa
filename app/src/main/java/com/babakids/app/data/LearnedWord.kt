package com.babakids.app.data

/**
 * A word the child has successfully pronounced at least once. Added the
 * first time they get it right; on every later correct attempt of the
 * same word, only lastPracticedAt is refreshed — never a duplicate entry.
 */
data class LearnedWord(
    val wordId: String,
    val word: String,
    val emoji: String,
    val imagePath: String?,
    val language: String, // "ar" or "en" — matches WordItem.wordLanguage
    val firstLearnedAt: Long,
    val lastPracticedAt: Long,
    val recordingPath: String?,
    // A parent-added celebratory emoji shown next to this word (e.g. "🌟"),
    // separate from `emoji` above which is the word's own picture/icon.
    val reactionEmoji: String? = null
)
