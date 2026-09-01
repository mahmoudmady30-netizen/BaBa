package com.babakids.app.ui

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babakids.app.data.LearnedWord
import com.babakids.app.data.LearnedWordsRepository
import com.babakids.app.ui.theme.BaBaGradients
import com.babakids.app.ui.theme.GlossyCard
import kotlinx.coroutines.launch

private val reactionEmojiChoices = listOf("🌟", "🏆", "❤️", "👏", "🎉", "😍", "💪", "🔥")

/**
 * Every word the child has said correctly, split by language (the app's
 * current language at view time), newest first — spec: word + picture +
 * date/time + the child's own recording, one entry per word (later
 * correct attempts just refresh the timestamp). Icon-only entry point
 * from Home, like "My Words".
 *
 * parentControlsEnabled: only true when reached from Parent Mode — adds a
 * delete button and an emoji-reaction picker per word. Off (the normal
 * child-facing default) hides both, since a child tapping around here
 * shouldn't be able to erase their own progress.
 */
@Composable
fun LearnedWordsScreen(english: Boolean = false, parentControlsEnabled: Boolean = false, onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val repository = remember { LearnedWordsRepository(context) }
    val scope = rememberCoroutineScope()
    val allWords by repository.learnedWordsFlow.collectAsState(initial = emptyList())
    val languageCode = if (english) "en" else "ar"
    val words = allWords
        .filter { it.language == languageCode }
        .sortedByDescending { it.lastPracticedAt }

    val player = remember { AudioPlayerHolder() }
    DisposableEffect(Unit) { onDispose { player.release() } }

    var pendingDelete by remember { mutableStateOf<LearnedWord?>(null) }
    var emojiPickerFor by remember { mutableStateOf<LearnedWord?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(onBack = onBack)
        Text(
            text = if (english) "📖 Learned Words" else "📖 الكلمات المتعلمة",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
        if (parentControlsEnabled) {
            Text(
                if (english) "Tap 🗑️ to remove a word, or the emoji to add a reaction."
                else "دوس 🗑️ عشان تشيل كلمة، أو على الإيموجي عشان تضيف تفاعل.",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (words.isEmpty()) {
            Text(
                if (english)
                    "No words yet — this fills in automatically once the child pronounces a word correctly."
                else
                    "لسه مفيش كلمات — هتتملى تلقائيًا أول ما الطفل ينطق كلمة صح.",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(16.dp)
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(words, key = { it.wordId }) { learned ->
                LearnedWordCard(
                    learned = learned,
                    parentControlsEnabled = parentControlsEnabled,
                    onPlayRecording = { path -> player.play(path) },
                    onDeleteClick = { pendingDelete = learned },
                    onReactionClick = { emojiPickerFor = learned }
                )
            }
        }
    }

    pendingDelete?.let { toDelete ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(if (english) "Remove this word?" else "شيل الكلمة دي؟") },
            text = {
                Text(
                    if (english)
                        "\"${toDelete.word}\" will be removed from Learned Words. This can't be undone."
                    else
                        "\"${toDelete.word}\" هتتشال من الكلمات المتعلمة. الإجراء ده مش قابل للتراجع."
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    scope.launch { repository.deleteWord(toDelete.wordId, toDelete.language) }
                    pendingDelete = null
                }) {
                    Text(if (english) "Remove" else "شيل", color = Color(0xFFFF5C5C))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingDelete = null }) {
                    Text(if (english) "Cancel" else "إلغاء")
                }
            }
        )
    }

    emojiPickerFor?.let { target ->
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { emojiPickerFor = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            GlossyCard(
                gradient = BaBaGradients.sky,
                modifier = Modifier.fillMaxWidth(0.9f).padding(vertical = 24.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        if (english) "Add a reaction" else "ضيف تفاعل",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.padding(top = 10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(reactionEmojiChoices) { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.25f))
                                    .clickable {
                                        scope.launch {
                                            repository.setReactionEmoji(target.wordId, target.language, emoji)
                                        }
                                        emojiPickerFor = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 22.sp)
                            }
                        }
                    }
                    if (target.reactionEmoji != null) {
                        Text(
                            if (english) "Remove reaction" else "شيل التفاعل",
                            fontSize = 13.sp,
                            color = Color.White,
                            modifier = Modifier.padding(top = 14.dp).clickable {
                                scope.launch { repository.setReactionEmoji(target.wordId, target.language, null) }
                                emojiPickerFor = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LearnedWordCard(
    learned: LearnedWord,
    parentControlsEnabled: Boolean,
    onPlayRecording: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onReactionClick: () -> Unit
) {
    GlossyCard(
        gradient = BaBaGradients.leaf,
        modifier = Modifier.fillMaxWidth(),
        onClick = { learned.recordingPath?.let(onPlayRecording) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (learned.imagePath != null) {
                val bitmap = remember(learned.imagePath) {
                    runCatching { BitmapFactory.decodeFile(learned.imagePath) }.getOrNull()
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = learned.word,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                } else {
                    Text(learned.emoji, fontSize = 40.sp)
                }
            } else {
                Text(learned.emoji, fontSize = 40.sp)
            }
            Spacer(Modifier.padding(horizontal = 6.dp))
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(learned.word, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    learned.reactionEmoji?.let {
                        Spacer(Modifier.padding(horizontal = 3.dp))
                        Text(it, fontSize = 18.sp)
                    }
                }
                Text(
                    formatRewardTimestamp(learned.lastPracticedAt),
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            if (learned.recordingPath != null) {
                Text("▶️", fontSize = 22.sp)
            }
            if (parentControlsEnabled) {
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text(
                    "😀",
                    fontSize = 20.sp,
                    modifier = Modifier.clickable { onReactionClick() }.padding(6.dp)
                )
                Text(
                    "🗑️",
                    fontSize = 20.sp,
                    modifier = Modifier.clickable { onDeleteClick() }.padding(6.dp)
                )
            }
        }
    }
}

/** A tiny MediaPlayer wrapper for tapping a card to hear the child's own recording. */
private class AudioPlayerHolder {
    private var player: MediaPlayer? = null

    fun play(path: String) {
        runCatching {
            player?.release()
            player = MediaPlayer().apply {
                setDataSource(path)
                setOnCompletionListener { it.release() }
                prepare()
                start()
            }
        }
    }

    fun release() {
        runCatching { player?.release() }
        player = null
    }
}
