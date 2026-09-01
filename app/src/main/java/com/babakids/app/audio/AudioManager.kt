package com.babakids.app.audio

import android.content.Context
import android.media.MediaPlayer

/**
 * Plays cached/bundled audio files and controls playback/volume — the
 * "AudioManager" role from the spec. Named distinctly from
 * android.media.AudioManager (a different class entirely, in a different
 * package) to avoid confusion even though there's no actual naming
 * collision as long as a single file doesn't need both unqualified.
 *
 * Also implements "prevent overlapping speech": starting a new clip
 * always stops/releases whatever was playing first (see playFile/
 * playAsset — both release the previous MediaPlayer before starting).
 */
class AudioManager {
    private var mediaPlayer: MediaPlayer? = null

    /** Plays a file from ordinary app storage (e.g. the runtime cache directory). */
    fun playFile(path: String, volume: Float = 1f, onComplete: () -> Unit = {}, onFailure: () -> Unit = {}) {
        val result = runCatching {
            releaseCurrent()
            val player = MediaPlayer().apply {
                setDataSource(path)
                setVolume(volume, volume)
                setOnCompletionListener {
                    it.release()
                    onComplete()
                }
                setOnErrorListener { mp, _, _ ->
                    mp.release()
                    onFailure()
                    true
                }
                prepare()
                start()
            }
            mediaPlayer = player
        }
        if (result.isFailure) onFailure()
    }

    /** Plays a file bundled in the app's assets/ folder (e.g. "audio/words/word_001.wav"). */
    fun playAsset(context: Context, assetPath: String, volume: Float = 1f, onComplete: () -> Unit = {}, onFailure: () -> Unit = {}) {
        val result = runCatching {
            releaseCurrent()
            val descriptor = context.assets.openFd(assetPath)
            val player = MediaPlayer().apply {
                setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
                setVolume(volume, volume)
                setOnCompletionListener {
                    it.release()
                    onComplete()
                }
                setOnErrorListener { mp, _, _ ->
                    mp.release()
                    onFailure()
                    true
                }
                prepare()
                start()
            }
            descriptor.close()
            mediaPlayer = player
        }
        if (result.isFailure) onFailure()
    }

    /** Stops and releases whatever is currently playing — call before starting new speech to avoid overlap. */
    fun stopCurrentSpeech() {
        releaseCurrent()
    }

    private fun releaseCurrent() {
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
    }

    fun stop() = stopCurrentSpeech()

    fun release() = stopCurrentSpeech()
}
