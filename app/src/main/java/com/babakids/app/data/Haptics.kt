package com.babakids.app.data

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * Physical feedback in addition to sound and visuals — research on
 * early-childhood apps consistently shows this increases engagement.
 * Both functions are no-ops if the parent has turned haptics off (see
 * ParentSettingsManager.hapticFeedbackEnabledFlow) — callers pass the
 * current setting in rather than Haptics reading it itself, to avoid a
 * DataStore read on every single tap.
 */
object Haptics {
    /** A short, pleasant buzz on a correct answer — never used for a wrong answer, so it never reads as a penalty. */
    fun vibrateSuccess(context: Context, enabled: Boolean = true) {
        if (!enabled) return
        vibrate(context, durationMs = 70)
    }

    /** A lighter, quicker tick on any tap/selection — general tactile feedback for browsing and choosing. */
    fun vibrateTap(context: Context, enabled: Boolean = true) {
        if (!enabled) return
        vibrate(context, durationMs = 25)
    }

    private fun vibrate(context: Context, durationMs: Long) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        }
    }
}
