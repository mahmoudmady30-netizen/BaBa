package com.babakids.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * Both pictures and voice recordings a parent adds need to live somewhere
 * the app controls (not a content:// Uri that can be revoked) so they keep
 * working across app restarts. Everything goes under the app's private
 * files directory, which needs no extra storage permission on modern
 * Android.
 */
object MediaStorage {

    private fun imagesDir(context: Context): File =
        File(context.filesDir, "word_images").apply { mkdirs() }

    private fun audioDir(context: Context): File =
        File(context.filesDir, "word_audio").apply { mkdirs() }

    /**
     * Copies a picked gallery image into private storage; returns the saved
     * file's path.
     *
     * IMPORTANT: this downsamples to a max dimension instead of copying the
     * original bytes. A raw copy of a modern phone photo (often 3000px+ on
     * a side) makes BitmapFactory.decodeFile fail or return null on plenty
     * of devices when the app later tries to display it — which silently
     * falls back to the emoji placeholder. That's exactly the "image shows
     * as a placeholder instead of the real picture" bug: it looks like the
     * picker didn't work, but really the saved file was just too heavy to
     * decode back.
     *
     * The photo is also composed onto a soft rounded card matching the
     * app's palette, cropped to a clean centered square. This is NOT real
     * background removal (that needs an ML segmentation model or a paid
     * cloud API, neither of which this project has) — the photo's own
     * background stays as-is, but every picture now gets the same
     * consistent, polished framing instead of a random raw crop.
     */
    fun copyImage(context: Context, sourceUri: Uri): String? {
        return try {
            val destination = File(imagesDir(context), "${UUID.randomUUID()}.png")
            val bitmap = loadDownsampledBitmap(context, sourceUri, maxDimension = 800)
                ?: return null
            val presented = presentOnCard(bitmap)
            destination.outputStream().use { output ->
                presented.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            bitmap.recycle()
            presented.recycle()
            destination.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /** Center-crops to a square and draws it onto a soft rounded pastel card. */
    private fun presentOnCard(source: Bitmap): Bitmap {
        val size = 720
        val inset = 36
        val outerRadius = 64f
        val innerRadius = 44f

        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFF6EC") // matches the app's warm background tone
        }
        canvas.drawRoundRect(
            RectF(0f, 0f, size.toFloat(), size.toFloat()),
            outerRadius,
            outerRadius,
            backgroundPaint
        )

        val photoSize = size - inset * 2
        val srcMin = minOf(source.width, source.height)
        val srcLeft = (source.width - srcMin) / 2
        val srcTop = (source.height - srcMin) / 2
        val srcRect = Rect(srcLeft, srcTop, srcLeft + srcMin, srcTop + srcMin)
        val destRect = RectF(
            inset.toFloat(),
            inset.toFloat(),
            (inset + photoSize).toFloat(),
            (inset + photoSize).toFloat()
        )

        val clipPath = Path().apply {
            addRoundRect(destRect, innerRadius, innerRadius, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)
        canvas.drawBitmap(source, srcRect, destRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        canvas.restore()

        return output
    }

    private fun loadDownsampledBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // decodeStream() ALWAYS returns null in bounds-only mode — it just
        // populates outWidth/outHeight as a side effect. Don't treat that
        // null as failure (the previous version of this function did
        // exactly that via `?: return null` here, which made every single
        // photo pick silently fail).
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, boundsOptions)
        }

        var sampleSize = 1
        var width = boundsOptions.outWidth
        var height = boundsOptions.outHeight
        if (width <= 0 || height <= 0) return null
        while (width / 2 >= maxDimension || height / 2 >= maxDimension) {
            width /= 2
            height /= 2
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        }
    }

    /** A fresh file path to record a new parent voice clip into. */
    fun newAudioFilePath(context: Context): String =
        File(audioDir(context), "${UUID.randomUUID()}.m4a").absolutePath

    fun deleteIfExists(path: String?) {
        if (path == null) return
        runCatching { File(path).delete() }
    }
}
