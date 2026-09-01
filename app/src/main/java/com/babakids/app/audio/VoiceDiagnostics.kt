package com.babakids.app.audio

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Records what happened on the last playSmartVoice() attempt — which
 * tier handled it (bundled audio / cache / fallback) and why. Every
 * previous "voice isn't working" report in this project turned out to
 * have a real, findable cause once we could actually see what the app
 * was doing — this makes that visible directly in Parent Mode instead of
 * requiring `adb logcat` access. Also logs the same messages to Logcat
 * (tag "BaBaVoice") for anyone who does have `adb` handy.
 */
object VoiceDiagnostics {
    var lastAttempt: String? by mutableStateOf(null)
        private set

    fun record(message: String) {
        lastAttempt = message
        Log.d("BaBaVoice", message)
    }
}
