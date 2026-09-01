package com.babakids.app.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale
import kotlin.math.max

/**
 * Wraps Android's built-in TextToSpeech and SpeechRecognizer so the rest of
 * the app just calls speak(word) / listenAndCompare(word, onResult).
 *
 * Locale can be switched per call (english = true/false) to support the
 * app-language setting. When Arabic, `dialect` picks the TTS voice:
 * Egyptian (ar-EG) or Modern Standard/Fusha (plain "ar", which steers most
 * engines toward a standard-Arabic voice instead of a specific dialect).
 * Recognition always uses the generic "ar" tag regardless of dialect —
 * some devices flatly reject ar-EG for *recognition* (not just TTS) with
 * "language not supported", so recognition intentionally stays broad.
 */
class SpeechHelper(private val context: Context) {

    companion object {
        const val DIALECT_EGYPTIAN = "eg"
        const val DIALECT_FUSHA = "fusha"

        val AR_EGYPTIAN_TTS_LOCALE: Locale = Locale("ar", "EG")
        val AR_FUSHA_TTS_LOCALE: Locale = Locale("ar")
        val AR_RECOGNITION_LOCALE: Locale = Locale("ar")
        val EN_LOCALE: Locale = Locale.US

        /**
         * English pronunciation should reflect the device's own configured
         * language/locale (en-GB, en-AU, en-IN...) rather than a fixed
         * US accent — this is deliberately independent of anything stored
         * per-word in WordItem (wordEn is just the displayed/spoken text,
         * never a locale). Falls back to US English only when the device
         * itself isn't set to any English variant, since the TTS engine
         * still needs *some* valid English locale to work with.
         */
        fun deviceEnglishLocale(): Locale {
            val deviceLocale = Locale.getDefault()
            return if (deviceLocale.language.equals("en", ignoreCase = true)) deviceLocale else EN_LOCALE
        }
    }

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingUtterance: SpeechRequest? = null
    private var currentOnDone: (() -> Unit)? = null
    private data class SpeechRequest(
        val text: String,
        val slow: Boolean,
        val english: Boolean,
        val dialect: String,
        val pitch: Float
    )

    fun init(onReady: () -> Unit = {}) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}

                    override fun onDone(utteranceId: String?) {
                        currentOnDone?.invoke()
                        currentOnDone = null
                    }

                    @Deprecated("Deprecated in Java, still required to override")
                    override fun onError(utteranceId: String?) {
                        currentOnDone?.invoke()
                        currentOnDone = null
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        currentOnDone?.invoke()
                        currentOnDone = null
                    }
                })
                pendingUtterance?.let { speakInternal(it.text, it.slow, it.english, it.dialect, it.pitch) }
                pendingUtterance = null
                onReady()
            }
        }
    }

    /**
     * Speaks a word or sentence slowly and clearly, as the spec requires.
     * If the TTS engine hasn't finished starting up yet (very likely the
     * very first time a screen opens and immediately tries to speak), the
     * utterance is queued and spoken automatically the moment it's ready,
     * instead of being silently dropped.
     *
     * `pitch` raises/lowers the voice — used on the splash screen for a
     * brighter, more playful/childlike tone. It's a pitch-shift of
     * Android's standard TTS voice, not a real recorded child's voice
     * (no such audio asset exists in this project).
     *
     * `onDone` fires once this utterance finishes playing (or fails) —
     * used to gate "wait until speaking is done" interactions, like not
     * letting the mic start listening while the word is still being
     * spoken (which would otherwise pick up the app's own voice as
     * background noise and hurt recognition accuracy).
     */
    fun speak(
        text: String,
        slow: Boolean = true,
        english: Boolean = false,
        dialect: String = DIALECT_EGYPTIAN,
        pitch: Float = 1f,
        onDone: () -> Unit = {}
    ) {
        if (!ttsReady) {
            pendingUtterance = SpeechRequest(text, slow, english, dialect, pitch)
            currentOnDone = onDone
            return
        }
        currentOnDone = onDone
        speakInternal(text, slow, english, dialect, pitch)
    }

    private fun speakInternal(text: String, slow: Boolean, english: Boolean, dialect: String, pitch: Float) {
        val locale = when {
            english -> deviceEnglishLocale()
            dialect == DIALECT_FUSHA -> AR_FUSHA_TTS_LOCALE
            else -> AR_EGYPTIAN_TTS_LOCALE
        }
        tts?.language = locale
        if (!english && dialect != DIALECT_FUSHA) {
            selectBestEgyptianVoiceIfAvailable()
        }
        tts?.setSpeechRate(if (slow) 0.85f else 1.0f)
        tts?.setPitch(pitch)
        // Strip emoji before handing text to the TTS engine — several
        // engines try to literally announce emoji ("star", "party
        // popper"...) instead of silently skipping them, which sounds like
        // the app is reading out random words after every phrase.
        val spokenText = stripEmojiForSpeech(text)
        if (spokenText.isBlank()) return
        tts?.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, "baba_utterance")
    }

    private var cachedEgyptianVoiceSearched = false
    private var cachedEgyptianVoice: android.speech.tts.Voice? = null

    /**
     * Best-effort search through whatever voices the device's TTS engine
     * actually has installed, looking for one that specifically identifies
     * as Egyptian Arabic rather than the generic default Arabic voice
     * `tts.language = Locale("ar","EG")` picks on its own — on some
     * devices/engines that default ignores the country code entirely and
     * just uses one generic Arabic voice regardless. If a better match
     * exists among the installed voices, this switches to it every time
     * (setting `tts.language` can silently reset a previously chosen
     * voice back to that language's default, so re-applying it on every
     * utterance is necessary, not just once); if no such voice exists on
     * this device, there's genuinely nothing more that can be done from
     * app code — the accent is baked into the voice data the OS ships,
     * not something an app can synthesize.
     */
    private fun selectBestEgyptianVoiceIfAvailable() {
        val engine = tts ?: return
        if (!cachedEgyptianVoiceSearched) {
            cachedEgyptianVoiceSearched = true
            val voices = runCatching { engine.voices }.getOrNull()
            cachedEgyptianVoice = voices?.firstOrNull { voice ->
                voice.locale.language == "ar" &&
                    (voice.locale.country.equals("EG", ignoreCase = true) ||
                        voice.name.contains("EG", ignoreCase = true) ||
                        voice.name.contains("egypt", ignoreCase = true))
            }
        }
        cachedEgyptianVoice?.let { voice -> runCatching { engine.voice = voice } }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }

    /**
     * Listens for the child's attempt and reports whether it roughly
     * matched. This is deliberately generous ("best effort", not exact
     * matching) — a child's pronunciation is never going to come back as a
     * clean transcript, so this checks every candidate transcript the
     * recognizer offers, in both directions (contains / is contained by),
     * and also accepts a "close enough" fuzzy match (small edit distance)
     * so near-misses in transcription still count as success. The spec is
     * explicit that the child must never feel like they failed for a
     * reasonable attempt.
     *
     * Deliberately does NOT try to capture audio. A previous version
     * attempted to save the child's recording via onBufferReceived — that
     * callback has effectively not been delivered by Android since 4.0
     * (ICS), so it always produced empty audio and was silently useless.
     * Recording is now handled separately and *sequentially* by the
     * caller (record first, then recognize), so the two never compete for
     * the microphone — see WordDetailScreen.beginListening().
     */
    fun listenAndCompare(
        expectedWord: String,
        english: Boolean = false,
        onResult: (matched: Boolean, heard: String?) -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onResult(false, null)
            return
        }

        val locale = if (english) deviceEnglishLocale() else AR_RECOGNITION_LOCALE
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toString())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale.toString())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val heard = matches?.firstOrNull()
                val expectedNormalized = normalize(expectedWord)
                val matched = matches?.any { candidate ->
                    isCloseEnough(normalize(candidate), expectedNormalized)
                } ?: false
                onResult(matched, heard)
                recognizer.destroy()
            }

            override fun onError(error: Int) {
                onResult(false, null)
                recognizer.destroy()
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer.startListening(intent)
    }

    private fun isCloseEnough(candidate: String, expected: String): Boolean {
        if (candidate.isBlank() || expected.isBlank()) return false
        if (candidate == expected) return true
        // Exact containment (not fuzzy) — handles the recognizer picking
        // up extra words around the target ("قول أحمر" still contains
        // "أحمر" exactly), which is a reliable signal regardless of
        // length. This is different from the fuzzy check below, which is
        // where short-word false positives like "أحمد"/"أحمر" came from.
        if (candidate.contains(expected) || expected.contains(candidate)) return true

        val distance = levenshtein(candidate, expected)
        val longestLength = max(candidate.length, expected.length)
        val similarity = 1.0 - (distance.toDouble() / longestLength.toDouble())

        // Short words need a much stricter threshold than long ones — a
        // single swapped letter in a 4-letter word (e.g. "أحمد"/"أحمر",
        // "احمد"/"احمر" once normalized — Ahmed vs. red) is a completely
        // different word despite being 75% similar by edit distance, but
        // the exact same one-letter swap in a 9+ letter word barely
        // changes the string at all. A single fixed percentage can't be
        // right for both, so the bar rises for shorter words.
        val requiredSimilarity = when {
            longestLength <= 3 -> 1.0
            longestLength <= 5 -> 0.85
            longestLength <= 8 -> 0.65
            else -> 0.45
        }
        return similarity >= requiredSimilarity
    }

    private fun levenshtein(a: String, b: String): Int {
        val costs = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var previousDiagonal = costs[0]
            costs[0] = i
            for (j in 1..b.length) {
                val previousDiagonalSaved = costs[j]
                costs[j] = if (a[i - 1] == b[j - 1]) {
                    previousDiagonal
                } else {
                    1 + minOf(previousDiagonal, costs[j], costs[j - 1])
                }
                previousDiagonal = previousDiagonalSaved
            }
        }
        return costs[b.length]
    }

    /**
     * Strips Arabic diacritics, tatweel, and normalizes common alef/yeh/teh
     * marbuta variants so "انا" vs "أنا" or "ماء" vs "ماية" style spelling
     * differences from the recognizer don't count as a mismatch.
     */
    private fun normalize(text: String): String {
        val diacritics = Regex("[\\u064B-\\u065F\\u0670\\u0640]")
        return text
            .replace(diacritics, "")
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ى', 'ي')
            .replace('ة', 'ه')
            .replace(" ", "")
            .trim()
    }

    /**
     * Removes emoji / pictographic symbols (kept on-screen elsewhere, just
     * not sent to the TTS engine) plus variation selectors and zero-width
     * joiners used to combine them. Iterates by code point since most
     * emoji live outside the Basic Multilingual Plane and can't be matched
     * with simple \\uXXXX regex escapes.
     */
    private fun stripEmojiForSpeech(text: String): String {
        val builder = StringBuilder()
        var i = 0
        while (i < text.length) {
            val codePoint = text.codePointAt(i)
            val charCount = Character.charCount(codePoint)
            val isEmojiish = codePoint in 0x1F000..0x1FFFF ||
                codePoint in 0x2600..0x27BF ||
                codePoint in 0x2190..0x21FF ||
                codePoint in 0x2300..0x23FF ||
                codePoint in 0x2B00..0x2BFF ||
                codePoint == 0xFE0F ||
                codePoint == 0x200D ||
                codePoint in 0x1F1E6..0x1F1FF
            if (!isEmojiish) {
                builder.appendCodePoint(codePoint)
            }
            i += charCount
        }
        return builder.toString().trim()
    }
}
