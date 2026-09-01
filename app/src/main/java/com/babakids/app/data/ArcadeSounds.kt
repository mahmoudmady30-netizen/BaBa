package com.babakids.app.data

import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.delay

/**
 * Simple synthesized retro "blip" sounds using Android's built-in
 * ToneGenerator — genuine, audible tones (not a fake/silent stand-in),
 * but simple synthesized beeps rather than a produced/mixed sound
 * effect, since no audio asset files are available in this project.
 * ToneGenerator needs no permissions and no bundled assets.
 */
class ArcadeSounds {
    private val generator: ToneGenerator? =
        runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }.getOrNull()

    fun playCoin() {
        runCatching { generator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 110) }
    }

    fun playBoot() {
        runCatching { generator?.startTone(ToneGenerator.TONE_PROP_ACK, 160) }
    }

    /** A quick ascending 4-note jingle for the logo reveal, built from DTMF tones at rising pitches. */
    suspend fun playLevelUpJingle() {
        val tones = listOf(
            ToneGenerator.TONE_DTMF_1,
            ToneGenerator.TONE_DTMF_3,
            ToneGenerator.TONE_DTMF_5,
            ToneGenerator.TONE_DTMF_8
        )
        tones.forEach { tone ->
            runCatching { generator?.startTone(tone, 130) }
            delay(150)
        }
    }

    fun release() {
        runCatching { generator?.release() }
    }
}
