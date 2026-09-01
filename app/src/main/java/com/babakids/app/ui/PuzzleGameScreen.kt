package com.babakids.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babakids.app.data.AppData
import com.babakids.app.data.Haptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class LetterTile(val key: Int, val char: Char, var placedInSlot: Int? = null)

/**
 * "ألغاز" — a word-spelling puzzle. A real word from the app's own
 * vocabulary (any category, shapes included) is shown as a picture
 * (its emoji/letter), its letters are scrambled into a tray, and the
 * child taps them in the correct order to spell it into the empty slots
 * above. Tapping a filled slot sends that letter back to the tray, so
 * mistakes are easy to undo.
 *
 * Replaces an earlier picture-slicing jigsaw design that turned out
 * confusing/unreliable in practice (cropping an emoji glyph into 9
 * abstract-looking tiles gave the child no real visual cue for how they
 * fit together). Spelling a word letter-by-letter is mechanically much
 * simpler — plain text tiles, no custom cropping — and it stays true to
 * this app's actual purpose: matching sounds/letters to words.
 */
@Composable
fun PuzzleGameScreen(english: Boolean = false, reduceMotion: Boolean = false, hapticEnabled: Boolean = true, onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Real words only, 3-6 letters — long enough to be a real puzzle,
    // short enough that the tray isn't overwhelming for a young child.
    val pool = remember {
        AppData.words.filter {
            it.category != "letters_ar" && it.category != "letters_en" && it.category != "letters" &&
                it.word.length in 3..6
        }
    }

    var roundSeed by remember { mutableStateOf(0) }
    val targetWord = remember(roundSeed) { pool.random() }
    val targetLetters = remember(roundSeed) { targetWord.word.toList() }

    var tiles by remember(roundSeed) {
        mutableStateOf(
            targetLetters.mapIndexed { index, c -> LetterTile(key = index, char = c) }.shuffled()
        )
    }
    var slots by remember(roundSeed) { mutableStateOf(List<Int?>(targetLetters.size) { null }) } // slot -> tile key
    var solved by remember(roundSeed) { mutableStateOf(false) }
    var wrongFlash by remember(roundSeed) { mutableStateOf(false) }

    fun checkSolved() {
        if (slots.any { it == null }) return
        val spelled = slots.map { key -> tiles.first { it.key == key }.char }
        if (spelled == targetLetters) {
            solved = true
            Haptics.vibrateSuccess(context, hapticEnabled)
            scope.launch {
                delay(1400)
                roundSeed++
            }
        } else {
            wrongFlash = true
            scope.launch {
                delay(650)
                // Send every tile back to the tray so the child can try again.
                slots = List<Int?>(targetLetters.size) { null }
                wrongFlash = false
            }
        }
    }

    fun onTrayTileTap(tileKey: Int) {
        if (solved) return
        val firstEmptySlot = slots.indexOfFirst { it == null }
        if (firstEmptySlot == -1) return
        slots = slots.toMutableList().also { it[firstEmptySlot] = tileKey }
        Haptics.vibrateTap(context, hapticEnabled)
        checkSolved()
    }

    fun onSlotTap(slotIndex: Int) {
        if (solved) return
        if (slots[slotIndex] == null) return
        slots = slots.toMutableList().also { it[slotIndex] = null }
        Haptics.vibrateTap(context, hapticEnabled)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        BackTopBar(onBack = onBack)
        Text(
            text = if (english) "🧩 Puzzle" else "🧩 ألغاز",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            text = when {
                solved -> if (english) "🎉 You spelled it!" else "🎉 كتبتها صح!"
                wrongFlash -> if (english) "Not quite — try again!" else "لسه مش كده — جرب تاني!"
                else -> if (english) "Tap the letters in order to spell the word" else "دوس على الحروف بالترتيب عشان تكتب الكلمة"
            },
            fontSize = 14.sp,
            color = if (wrongFlash) Color(0xFFD32F2F) else Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // The picture being spelled — a real visual hint, not just letters in a void.
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .aspectRatio(2.4f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFEFEFF5)),
            contentAlignment = Alignment.Center
        ) {
            Text(targetWord.emoji, fontSize = 56.sp)
        }

        // Empty slots to fill, in word order.
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(slots.size) { slotIndex ->
                val tileKey = slots[slotIndex]
                val letterChar = tileKey?.let { key -> tiles.first { it.key == key }.char }
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (letterChar != null) Color(0xFFB3E5FC) else Color(0xFFE0E0E0))
                        .clickable { onSlotTap(slotIndex) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(letterChar?.toString() ?: "", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text(
            if (english) "Letters" else "الحروف",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // The scrambled letter tray — only letters not currently placed in a slot are shown here.
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(tiles, key = { it.key }) { tile ->
                val isPlaced = tile.key in slots
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isPlaced) Color.Transparent else Color(0xFFFFD54F))
                        .clickable(enabled = !isPlaced) { onTrayTileTap(tile.key) },
                    contentAlignment = Alignment.Center
                ) {
                    if (!isPlaced) {
                        Text(tile.char.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
