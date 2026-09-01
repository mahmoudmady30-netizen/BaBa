package com.babakids.app.audio

import com.babakids.app.speech.SpeechHelper

/**
 * The offline safety net. The spec asks for a bundle of pre-recorded
 * natural-voice phrases ("برافو!", "ممتاز!"...) that always work without
 * internet. This project has no real recorded voice files to bundle —
 * only Android's built-in TTS engine (SpeechHelper), which is already
 * integrated, proven throughout this app, and genuinely works fully
 * offline. So that's what backs this tier honestly, rather than
 * pretending to ship "pre-recorded" audio that doesn't exist.
 */
class FallbackAudioManager(private val speechHelper: SpeechHelper) {
    fun speak(text: String, english: Boolean, dialect: String, pitch: Float = 1f, onDone: () -> Unit = {}) {
        speechHelper.speak(text, english = english, dialect = dialect, pitch = pitch, onDone = onDone)
    }
}
