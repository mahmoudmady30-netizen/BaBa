package com.babakids.app.audio

import com.babakids.app.data.AppData
import com.babakids.app.data.ParentSettingsManager
import com.babakids.app.data.Phrases

/**
 * The full set of phrases this app can speak, used by
 * OfflineAudioPreGenerator to pre-fill the local cache in one pass.
 * Vocabulary comes directly from AppData (the same single source of
 * truth the rest of the app uses), so this list can never drift out of
 * sync with the real word list.
 *
 * Coverage and an honest limit: every vocabulary word (Arabic + English)
 * and every "أنا عايز" situation phrase has NO variable content, so
 * pre-generating them here means every future request for that exact
 * text is an instant cache hit, permanently. The generic encouragement
 * phrases below use the fallback address ("بطل"/"بطلة") — that matches
 * real runtime speech ONLY for a child with no name set. Once a parent
 * sets a real name, encouragement phrases embed that name and become
 * different text with a different cache key — those still work
 * perfectly, they just can't be bulk-pre-generated ahead of time without
 * already knowing the name, so the very first time each named phrase is
 * used it goes through live synthesis once, then is cached from then on.
 */
object PhraseLibrary {
    data class Entry(val text: String, val english: Boolean, val dialect: String)

    private const val EGYPTIAN = ParentSettingsManager.DIALECT_EGYPTIAN

    fun allEntries(): List<Entry> {
        val entries = mutableListOf<Entry>()

        // Every word's spoken form — Egyptian Arabic and English.
        val allWords = AppData.words + AppData.arabicLetters + AppData.englishLetters
        allWords.forEach { word ->
            entries += Entry(word.spokenWord(false, EGYPTIAN), false, EGYPTIAN)
            entries += Entry(word.displayWord(true), true, EGYPTIAN)
        }

        // Generic (name-free) encouragement phrases — matches real speech
        // for a child with no name set; see class doc for the limit on
        // named children.
        val maleAddress = Phrases.fallbackAddress(ParentSettingsManager.GENDER_MALE, false)
        val femaleAddress = Phrases.fallbackAddress(ParentSettingsManager.GENDER_FEMALE, false)
        Phrases.successPhrases(ParentSettingsManager.GENDER_MALE, false, EGYPTIAN).forEach {
            entries += Entry(it.format(maleAddress), false, EGYPTIAN)
        }
        Phrases.successPhrases(ParentSettingsManager.GENDER_FEMALE, false, EGYPTIAN).forEach {
            entries += Entry(it.format(femaleAddress), false, EGYPTIAN)
        }
        Phrases.tryAgainPhrases(ParentSettingsManager.GENDER_MALE, false, EGYPTIAN).forEach {
            entries += Entry(it.format(maleAddress), false, EGYPTIAN)
        }
        Phrases.tryAgainPhrases(ParentSettingsManager.GENDER_FEMALE, false, EGYPTIAN).forEach {
            entries += Entry(it.format(femaleAddress), false, EGYPTIAN)
        }

        entries += Entry("بابا", false, EGYPTIAN)

        return entries.distinctBy { "${it.text}|${it.english}|${it.dialect}" }
    }
}
