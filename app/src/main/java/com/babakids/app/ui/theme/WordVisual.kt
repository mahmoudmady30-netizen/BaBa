package com.babakids.app.ui.theme

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.TextUnit
import com.babakids.app.data.WordItem

/**
 * Renders `word.imagePath` (a real photo a parent uploaded) if it exists
 * and is still readable on disk; otherwise falls back to the emoji
 * placeholder so nothing ever shows blank. Content is always centered
 * within whatever size `modifier` gives it.
 */
@Composable
fun WordVisual(word: WordItem, emojiFontSize: TextUnit, modifier: Modifier = Modifier) {
    val bitmap = remember(word.imagePath) {
        word.imagePath?.let { path ->
            runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = word.word,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(text = word.emoji, fontSize = emojiFontSize)
        }
    }
}
