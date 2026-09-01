package com.babakids.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babakids.app.data.AppData
import com.babakids.app.data.Haptics
import com.babakids.app.ui.theme.BaBaGradients
import com.babakids.app.ui.theme.GlossyCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class MemoryCard(
    val key: Int,
    val pairId: String,
    val emoji: String,
    val label: String,
    var isFaceUp: Boolean = false,
    var isMatched: Boolean = false
)

/**
 * "لعبة الذاكرة" — classic pairs-matching, built from real vocabulary
 * emojis so it doubles as word recognition practice, not just a generic
 * memory game. A fresh, shuffled set of pairs is drawn every round — no
 * repeated pairs within a single round, since each round only ever picks
 * each source word once (see wordsForRound below).
 */
@Composable
fun MemoryMatchGameScreen(english: Boolean = false, reduceMotion: Boolean = false, hapticEnabled: Boolean = true, onBack: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    // Pool: any built-in word with a plain emoji (skip letters/numbers,
    // which are less visually distinct for a matching game).
    val pool = remember {
        AppData.words.filter { it.emoji.isNotBlank() && it.category != "letters_ar" && it.category != "letters_en" }
    }

    var roundSeed by remember { mutableStateOf(0) }
    var cards by remember(roundSeed) {
        mutableStateOf(buildRound(pool, pairCount = 6, english = english))
    }
    var firstPick by remember(roundSeed) { mutableStateOf<Int?>(null) }
    var isChecking by remember(roundSeed) { mutableStateOf(false) }
    var moves by remember(roundSeed) { mutableStateOf(0) }
    val allMatched = cards.isNotEmpty() && cards.all { it.isMatched }

    fun onCardTap(key: Int) {
        if (isChecking) return
        val index = cards.indexOfFirst { it.key == key }
        if (index == -1 || cards[index].isFaceUp || cards[index].isMatched) return

        Haptics.vibrateTap(context, hapticEnabled)
        cards = cards.toMutableList().also { it[index] = it[index].copy(isFaceUp = true) }

        val first = firstPick
        if (first == null) {
            firstPick = key
            return
        }

        moves++
        val firstIndex = cards.indexOfFirst { it.key == first }
        isChecking = true
        scope.launch {
            delay(500)
            val matched = cards[firstIndex].pairId == cards[index].pairId
            cards = cards.toMutableList().also { list ->
                if (matched) {
                    list[firstIndex] = list[firstIndex].copy(isMatched = true)
                    list[index] = list[index].copy(isMatched = true)
                } else {
                    list[firstIndex] = list[firstIndex].copy(isFaceUp = false)
                    list[index] = list[index].copy(isFaceUp = false)
                }
            }
            if (matched) Haptics.vibrateSuccess(context, hapticEnabled)
            firstPick = null
            isChecking = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(onBack = onBack)
        Text(
            text = if (english) "🧠 Memory Match" else "🧠 لعبة الذاكرة",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            text = if (english) "Find the matching pairs!" else "لاقي كل زوج متشابه!",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (allMatched) {
            GlossyCard(
                gradient = BaBaGradients.leaf,
                modifier = Modifier.fillMaxWidth().padding(16.dp).aspectRatio(2.6f),
                onClick = { roundSeed++ }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(if (english) "🎉 You did it!" else "🎉 برافو عليك!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(if (english) "Tap to play again" else "دوس تلعب تاني", fontSize = 14.sp, color = Color.White)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(cards, key = { it.key }) { card ->
                    GlossyCard(
                        gradient = if (card.isFaceUp || card.isMatched) BaBaGradients.sky else BaBaGradients.purple,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        onClick = { onCardTap(card.key) }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (card.isFaceUp || card.isMatched) {
                                Text(card.emoji, fontSize = 34.sp)
                            } else {
                                Text("❓", fontSize = 28.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildRound(pool: List<com.babakids.app.data.WordItem>, pairCount: Int, english: Boolean): List<MemoryCard> {
    // Distinct words only, so no pair repeats within a round.
    val chosen = pool.shuffled().distinctBy { it.emoji }.take(pairCount)
    val cards = chosen.flatMapIndexed { index, word ->
        listOf(
            MemoryCard(key = index * 2, pairId = word.id, emoji = word.emoji, label = word.displayWord(english)),
            MemoryCard(key = index * 2 + 1, pairId = word.id, emoji = word.emoji, label = word.displayWord(english))
        )
    }
    return cards.shuffled()
}
