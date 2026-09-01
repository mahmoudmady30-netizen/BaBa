package com.babakids.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread

/**
 * Records the child's voice *at the same time* as speech recognition runs.
 *
 * Why AudioRecord instead of MediaRecorder: MediaRecorder takes a heavier,
 * more exclusive hold on the microphone (it owns a full encoder pipeline),
 * which is what made it interfere with SpeechRecognizer when both ran
 * together. AudioRecord is the low-level raw-PCM API — it's what the
 * recognizer itself is built on.
 *
 * Audio source: MIC, not VOICE_RECOGNITION. Real-world reports (almost no
 * saved clips) point to the earlier choice — both this recorder and
 * Android's own SpeechRecognizer opening the *same* tuned source
 * (VOICE_RECOGNITION) — as the likely cause: when two clients request the
 * identical source, many devices' audio HAL treats the second one as a
 * duplicate/competing claim and either refuses it outright or silently
 * hands it a muted stream (start() still reports success, but every frame
 * is silence). Requesting the plain MIC source instead asks for a
 * genuinely different logical input, which is far more likely to be mixed
 * in by AudioFlinger rather than arbitrated away.
 *
 * Honest limitation: Android does not formally guarantee that two
 * microphone consumers can capture simultaneously — behaviour depends on
 * the device and OS version. If the system refuses the second capture,
 * start() simply returns false and recognition proceeds untouched. And
 * even when start() succeeds, the stream can still come back silent on a
 * given device — hasSignal() below checks for that after the fact instead
 * of trusting start()'s return value alone, so a dead-air clip is never
 * saved as if it were a real recording. Either way, recognition accuracy
 * is never sacrificed for the recording.
 */
class ConcurrentVoiceRecorder {

    private var recorder: AudioRecord? = null
    private var recordingThread: Thread? = null
    @Volatile private var isRecording = false
    private val pcmBuffer = ByteArrayOutputStream()

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    // Below this average absolute sample amplitude (out of a possible
    // 32767 for 16-bit PCM), a clip is treated as silence/noise-floor
    // rather than an actually-spoken word — this is what catches the
    // "start() succeeded but the OS muted the stream" case above.
    private val silenceThreshold = 180

    @SuppressLint("MissingPermission") // caller checks RECORD_AUDIO first
    fun start(): Boolean {
        return runCatching {
            val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            if (minBuffer <= 0) return false

            val bufferSize = minBuffer * 2
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord.release()
                return false
            }

            pcmBuffer.reset()
            audioRecord.startRecording()
            if (audioRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord.release()
                return false
            }

            recorder = audioRecord
            isRecording = true
            recordingThread = thread(start = true, isDaemon = true) {
                val chunk = ByteArray(bufferSize)
                while (isRecording) {
                    val read = runCatching { audioRecord.read(chunk, 0, chunk.size) }.getOrDefault(-1)
                    if (read > 0) {
                        synchronized(pcmBuffer) { pcmBuffer.write(chunk, 0, read) }
                    } else if (read < 0) {
                        break
                    }
                }
            }
            true
        }.getOrDefault(false)
    }

    /**
     * Stops capture and, if `keep` is true and the captured audio actually
     * contains a real signal (not just silence — see hasSignal()), writes
     * it to `outputPath` as a playable WAV file. Returns the path on
     * success, or null if nothing usable was recorded (or keep=false).
     */
    fun stopAndSave(outputPath: String, keep: Boolean): String? {
        isRecording = false
        runCatching { recordingThread?.join(500) }
        recordingThread = null
        runCatching {
            recorder?.stop()
            recorder?.release()
        }
        recorder = null

        if (!keep) return null
        val pcm = synchronized(pcmBuffer) { pcmBuffer.toByteArray() }
        if (pcm.isEmpty() || !hasSignal(pcm)) return null

        return runCatching {
            File(outputPath).writeBytes(wrapPcmAsWav(pcm))
            outputPath
        }.getOrNull()
    }

    /** True if the captured PCM has a real voice-level signal, not just silence/noise floor. */
    private fun hasSignal(pcm: ByteArray): Boolean {
        if (pcm.size < 4) return false
        var sum = 0L
        var sampleCount = 0
        var i = 0
        while (i + 1 < pcm.size) {
            val sample = ((pcm[i + 1].toInt() shl 8) or (pcm[i].toInt() and 0xFF)).toShort()
            sum += kotlin.math.abs(sample.toInt())
            sampleCount++
            i += 2
        }
        if (sampleCount == 0) return false
        return (sum / sampleCount) >= silenceThreshold
    }

    fun cancel() {
        isRecording = false
        runCatching { recordingThread?.join(300) }
        recordingThread = null
        runCatching {
            recorder?.stop()
            recorder?.release()
        }
        recorder = null
        synchronized(pcmBuffer) { pcmBuffer.reset() }
    }

    /** Adds a standard 44-byte WAV header so MediaPlayer can play the raw PCM back. */
    private fun wrapPcmAsWav(pcm: ByteArray): ByteArray {
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(36 + pcm.size)
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16)
        header.putShort(1)
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort(blockAlign.toShort())
        header.putShort(bitsPerSample.toShort())
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(pcm.size)
        return header.array() + pcm
    }
}
