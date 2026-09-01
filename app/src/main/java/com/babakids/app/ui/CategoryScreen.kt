package com.babakids.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.babakids.app.data.AppData
import com.babakids.app.data.CustomWordsRepository
import com.babakids.app.data.Haptics
import com.babakids.app.data.MediaStorage
import com.babakids.app.data.WordItem
import com.babakids.app.data.WordOverridesRepository
import com.babakids.app.data.withCategoryOverrides
import com.babakids.app.data.withOverrides
import com.babakids.app.ui.theme.BaBaGradients
import com.babakids.app.ui.theme.GlossyCard
import com.babakids.app.ui.theme.PremiumIconOrb
import com.babakids.app.ui.theme.WordVisual
import com.babakids.app.ui.theme.rememberAdaptiveColumns
import kotlinx.coroutines.launch

@Composable
fun CategoryScreen(
    categoryId: String,
    english: Boolean = false,
    hapticEnabled: Boolean = true,
    stars: Int = 0,
    disabledWordIds: Set<String> = emptySet(),
    myWordsSelectedIds: Set<String> = emptySet(),
    editModeEnabled: Boolean = false,
    onBack: () -> Unit = {},
    onWordClick: (WordItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val parentSettings = remember { com.babakids.app.data.ParentSettingsManager(context) }
    val customWordsRepository = remember { CustomWordsRepository(context) }
    val customWords by customWordsRepository.customWordsFlow.collectAsState(initial = emptyList())
    val overridesRepository = remember { WordOverridesRepository(context) }
    val overrides by overridesRepository.overridesFlow.collectAsState(initial = emptyMap())
    val categoryOverridesRepository = remember { com.babakids.app.data.CategoryOverridesRepository(context) }
    val categoryOverrides by categoryOverridesRepository.overridesFlow.collectAsState(initial = emptyMap())
    val currentLangCode = if (english) "en" else "ar"

    val words = remember(categoryId, customWords, english, disabledWordIds, myWordsSelectedIds, overrides) {
        val base = if (categoryId == "my_words") {
            // "My Words" always includes every parent-added custom word
            // automatically, PLUS any existing built-in word a parent has
            // explicitly picked to feature here (via Parent Mode's "Add to
            // My Words" picker) — each picked word still also appears
            // under its own normal category too, this is purely additive.
            val allBuiltIn = AppData.words + AppData.arabicLetters + AppData.englishLetters
            val picked = allBuiltIn.filter { it.id in myWordsSelectedIds }
            (customWords.filter { it.wordLanguage == currentLangCode } + picked).distinctBy { it.id }
        } else {
            AppData.wordsFor(categoryId, english) +
                customWords.filter { it.category == categoryId && it.wordLanguage == currentLangCode }
        }
        base.filterNot { it.id in disabledWordIds }.withOverrides(overrides)
    }
    val category = remember(categoryId, categoryOverrides) {
        listOf(AppData.categoryFor(categoryId)).withCategoryOverrides(categoryOverrides).first()
    }

    var editingWord by remember { mutableStateOf<WordItem?>(null) }
    var editingCategory by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(BaBaGradients.background)) {
        BackTopBar(onBack = onBack)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                text = category.displayTitle(english),
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF26354A),
                modifier = Modifier.weight(1f)
            )
            if (editModeEnabled) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.9f))
                        .clickable {
                            Haptics.vibrateTap(context, hapticEnabled)
                            editingCategory = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✏️", fontSize = 16.sp)
                }
            }
        }

        if (categoryId == "my_words" && words.isEmpty()) {
            Text(
                if (english)
                    "No words added yet — ask a parent to add one from Parent Mode!"
                else
                    "لسه مفيش كلمات مضافة — اطلب من ماما أو بابا يضيفوا واحدة من لوحة الوالدين!",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(16.dp)
            )
        }

        if (editModeEnabled) {
            GlossyCard(
                gradient = BaBaGradients.leaf,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp).height(44.dp),
                onClick = {
                    // "Save" here means: done editing for now — turns Word
                    // Edit Mode off so the pencil icons disappear until the
                    // parent switches it back on from Settings. Each
                    // word's own picture/audio/name is already saved the
                    // instant its own dialog's Save is tapped (see
                    // WordEditDialog) — this button isn't what persists
                    // those, it just closes out editing mode itself.
                    scope.launch { parentSettings.setWordEditModeEnabled(false) }
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        if (english) "✅ Save" else "✅ حفظ",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(rememberAdaptiveColumns()),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(words) { word ->
                val locked = stars < word.starsRequired
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.92f)) {
                    GlossyCard(
                        gradient = if (locked) BaBaGradients.sky else BaBaGradients.leaf,
                        modifier = Modifier.fillMaxSize(),
                        onClick = {
                            if (locked) {
                                Haptics.vibrateTap(context, hapticEnabled)
                            } else {
                                Haptics.vibrateTap(context, hapticEnabled)
                                onWordClick(word)
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (locked) {
                                Text("🔒", fontSize = 40.sp)
                                Text(
                                    "${word.starsRequired} ⭐",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            } else {
                                PremiumIconOrb(size = 82.dp) {
                                    if (word.animationStyle != "none") {
                                        SituationVisual(
                                            emoji = word.emoji,
                                            style = word.animationStyle,
                                            emojiFontSize = 50.sp,
                                            modifier = Modifier.height(64.dp).fillMaxWidth()
                                        )
                                    } else {
                                        WordVisual(
                                            word = word,
                                            emojiFontSize = 50.sp,
                                            modifier = Modifier.height(64.dp).fillMaxWidth()
                                        )
                                    }
                                }
                                Text(
                                    word.displayWord(english),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Edit-mode pencil: only on unlocked cards, so a locked
                    // word (still just a padlock, no real content) can't be
                    // "edited" before the child has even reached it.
                    if (editModeEnabled && !locked) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.9f))
                                .clickable {
                                    Haptics.vibrateTap(context, hapticEnabled)
                                    editingWord = word
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✏️", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }

    val wordBeingEdited = editingWord
    if (wordBeingEdited != null) {
        WordEditDialog(
            word = wordBeingEdited,
            english = english,
            overridesRepository = overridesRepository,
            onDismiss = { editingWord = null }
        )
    }

    if (editingCategory) {
        CategoryEditDialog(
            category = category,
            english = english,
            overridesRepository = categoryOverridesRepository,
            onDismiss = { editingCategory = false }
        )
    }
}

/**
 * Same idea as WordEditDialog, but for the section itself: rename it
 * and/or replace its icon with a real photo. Reachable via the pencil
 * next to the category title at the top of the screen, same Word Edit
 * Mode gate as word editing.
 */
@Composable
private fun CategoryEditDialog(
    category: com.babakids.app.data.Category,
    english: Boolean,
    overridesRepository: com.babakids.app.data.CategoryOverridesRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val originalName = remember(category.id, english) { category.displayTitle(english) }
    var pendingImagePath by remember { mutableStateOf(category.imagePath) }
    var pendingName by remember { mutableStateOf(originalName) }
    var imagePickError by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = MediaStorage.copyImage(context, uri)
            imagePickError = path == null
            if (path != null) pendingImagePath = path
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlossyCard(
            gradient = BaBaGradients.background,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .heightIn(max = 480.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                Text(
                    (if (english) "✏️ Edit section: " else "✏️ تعديل القسم: ") + category.displayTitle(english),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF26354A)
                )
                Spacer(Modifier.height(16.dp))

                Text(
                    if (english) "Section name" else "اسم القسم",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF26354A)
                )
                Spacer(Modifier.height(6.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = pendingName,
                    onValueChange = { pendingName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(16.dp))
                Text(
                    if (english) "Picture" else "الصورة",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF26354A)
                )
                Spacer(Modifier.height(6.dp))
                GlossyCard(
                    gradient = BaBaGradients.sky,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    onClick = { imagePickerLauncher.launch("image/*") }
                ) {
                    Text(
                        if (pendingImagePath != null) {
                            if (english) "🖼️ Change picture" else "🖼️ غيّر الصورة"
                        } else {
                            if (english) "🖼️ Choose a picture" else "🖼️ اختار صورة"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                if (imagePickError) {
                    Text(
                        if (english) "Couldn't load that picture, try another one."
                        else "الصورة دي متفتحتش، جرب صورة تانية.",
                        fontSize = 12.sp,
                        color = Color(0xFFB71C1C)
                    )
                }

                Spacer(Modifier.height(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlossyCard(
                        gradient = BaBaGradients.leaf,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        onClick = {
                            scope.launch {
                                overridesRepository.setImage(category.id, pendingImagePath)
                                // Same guard as WordEditDialog: only write a
                                // name override when it actually changed.
                                if (pendingName.trim() != originalName.trim()) {
                                    overridesRepository.setText(category.id, english, pendingName.trim().ifBlank { null })
                                }
                                // onDismiss() runs AFTER the writes finish,
                                // inside this same coroutine — dismissing
                                // first would cancel it mid-write (the same
                                // bug fixed in WordEditDialog's Save button).
                                onDismiss()
                            }
                        }
                    ) {
                        Text(
                            if (english) "✅ Save" else "✅ حفظ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    Text(
                        if (english) "↺ Reset name/picture to original"
                        else "↺ رجّع الاسم/الصورة الأصلية",
                        fontSize = 13.sp,
                        color = Color(0xFF6D4C41),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().clickable {
                            scope.launch {
                                overridesRepository.clear(category.id)
                                onDismiss()
                            }
                        }
                    )
                    Text(
                        if (english) "Cancel" else "إلغاء",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().clickable { onDismiss() }
                    )
                }
            }
        }
    }
}

/**
 * Parent-facing dialog (only reachable via the pencil icon, which itself
 * only shows when Word Edit Mode is on in Parent Mode settings): replace
 * this word's picture and/or record a new pronunciation for it. Works on
 * built-in words too — saved as a small override patch (WordOverridesRepository)
 * rather than editing the word list itself, so "reset to default" is always
 * possible and nothing about the original content is lost.
 */
@Composable
private fun WordEditDialog(
    word: WordItem,
    english: Boolean,
    overridesRepository: WordOverridesRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val originalName = remember(word.id, english) { word.displayWord(english) }
    var pendingImagePath by remember { mutableStateOf(word.imagePath) }
    var pendingAudioPath by remember { mutableStateOf(word.parentRecordingPath) }
    var pendingName by remember { mutableStateOf(originalName) }
    var isRecording by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var micPermissionExplainer by remember { mutableStateOf(false) }
    var imagePickError by remember { mutableStateOf(false) }
    var recordingError by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { mediaRecorder?.stop() }
            mediaRecorder?.release()
            previewPlayer?.release()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = MediaStorage.copyImage(context, uri)
            imagePickError = path == null
            if (path != null) pendingImagePath = path
        }
    }

    fun hasMicPermission(): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    fun startRecording() {
        recordingError = false
        val path = MediaStorage.newAudioFilePath(context)
        val result = runCatching {
            val recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(path)
                prepare()
                start()
            }
            mediaRecorder = recorder
            pendingAudioPath = path
            isRecording = true
        }
        if (result.isFailure) recordingError = true
    }

    fun stopRecording() {
        // MediaRecorder.stop() throws IllegalStateException when almost no
        // audio was captured (well under ~1 second) — a very easy thing to
        // hit if "Stop" gets tapped right after "Record". Catching that
        // here and surfacing it, instead of silently reverting to the old
        // audio, is what actually explains to the parent why nothing
        // changed rather than leaving them to guess.
        val result = runCatching {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        }
        if (result.isFailure) {
            recordingError = true
            MediaStorage.deleteIfExists(pendingAudioPath)
            pendingAudioPath = word.parentRecordingPath
        }
        mediaRecorder = null
        isRecording = false
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            micPermissionExplainer = false
            startRecording()
        } else {
            micPermissionExplainer = true
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        // Default Dialog window is sized WRAP_CONTENT by the platform, and
        // measures its content with loose/unbounded width constraints — a
        // known Compose trap where a child using Modifier.fillMaxWidth()
        // (like the card below, and the Save button inside it) can end up
        // laid out with zero effective width: it still draws its text, but
        // its actual tap target collapses to nothing, so taps on it are
        // silently swallowed. Disabling the platform default width gives
        // this dialog real, fixed constraints to measure against instead.
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlossyCard(
            gradient = BaBaGradients.background,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .heightIn(max = 560.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                Text(
                    (if (english) "✏️ Edit: " else "✏️ تعديل: ") + word.displayWord(english),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF26354A)
                )
                Spacer(Modifier.height(16.dp))

                Text(
                    if (english) "Word name" else "اسم الكلمة",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF26354A)
                )
                Spacer(Modifier.height(6.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = pendingName,
                    onValueChange = { pendingName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(
                    if (english)
                        "If you don't also record a new pronunciation below, the device's own voice will read this new spelling automatically."
                    else
                        "لو مش هتسجل نطق جديد تحت، صوت الجهاز هيقرأ الاسم الجديد ده تلقائيًا.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    if (english) "Picture" else "الصورة",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF26354A)
                )
                Spacer(Modifier.height(6.dp))
                GlossyCard(
                    gradient = BaBaGradients.sky,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    onClick = { imagePickerLauncher.launch("image/*") }
                ) {
                    Text(
                        if (pendingImagePath != null) {
                            if (english) "🖼️ Change picture" else "🖼️ غيّر الصورة"
                        } else {
                            if (english) "🖼️ Choose a picture" else "🖼️ اختار صورة"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                if (imagePickError) {
                    Text(
                        if (english) "Couldn't load that picture, try another one."
                        else "الصورة دي متفتحتش، جرب صورة تانية.",
                        fontSize = 12.sp,
                        color = Color(0xFFB71C1C)
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    if (english) "Pronunciation" else "النطق",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF26354A)
                )
                Spacer(Modifier.height(6.dp))
                GlossyCard(
                    gradient = if (isRecording) BaBaGradients.leaf else BaBaGradients.purple,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    onClick = {
                        if (isRecording) {
                            stopRecording()
                        } else if (hasMicPermission()) {
                            startRecording()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                ) {
                    Text(
                        if (isRecording) {
                            if (english) "⏹️ Stop recording" else "⏹️ وقف التسجيل"
                        } else {
                            if (english) "🎙️ Record new pronunciation" else "🎙️ سجّل نطق جديد"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                if (micPermissionExplainer) {
                    Text(
                        if (english) "Mic permission is needed to record."
                        else "محتاجين إذن المايك عشان نسجل.",
                        fontSize = 12.sp,
                        color = Color(0xFFB71C1C)
                    )
                }
                if (recordingError) {
                    Text(
                        if (english) "That recording was too short — hold Record a bit longer and try again."
                        else "التسجيل كان قصير جدًا — سجّل لفترة أطول وجرب تاني.",
                        fontSize = 12.sp,
                        color = Color(0xFFB71C1C)
                    )
                }
                if (pendingAudioPath != null && !isRecording) {
                    Spacer(Modifier.height(8.dp))
                    GlossyCard(
                        gradient = BaBaGradients.sky,
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        onClick = {
                            runCatching {
                                previewPlayer?.release()
                                val player = MediaPlayer().apply {
                                    setDataSource(pendingAudioPath)
                                    setOnCompletionListener { it.release() }
                                    prepare()
                                    start()
                                }
                                previewPlayer = player
                            }
                        }
                    ) {
                        Text(
                            if (english) "▶️ Preview" else "▶️ تسميع",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlossyCard(
                        gradient = BaBaGradients.leaf,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        onClick = {
                            // If the parent tapped Save while still
                            // recording, the .m4a file was never finalized
                            // (MediaRecorder only writes its final header on
                            // stop()) — saving that path would silently
                            // point to an unplayable file, which looked
                            // like "the old pronunciation just keeps
                            // playing". Finalize it first, always.
                            if (isRecording) stopRecording()
                            scope.launch {
                                overridesRepository.setImage(word.id, pendingImagePath)
                                overridesRepository.setAudio(word.id, pendingAudioPath)
                                // Only touch the name override if it was
                                // actually edited — writing it unconditionally
                                // (even back to the same text) would wrongly
                                // flip bypassDialectSpokenForm on for every
                                // image/audio-only edit and silently break
                                // this word's existing hand-tuned pronunciation.
                                if (pendingName.trim() != originalName.trim()) {
                                    overridesRepository.setText(word.id, english, pendingName.trim().ifBlank { null })
                                }
                                // CRITICAL: onDismiss() must run AFTER the
                                // writes above finish, not right after
                                // launching them. onDismiss() sets
                                // editingWord = null in the parent, which
                                // removes this dialog from composition —
                                // and rememberCoroutineScope()'s scope is
                                // cancelled the instant its composable
                                // leaves composition. Calling onDismiss()
                                // outside this block raced the dialog's
                                // own removal against these suspend
                                // DataStore writes, and the removal almost
                                // always won: every edit here (picture,
                                // audio, name) was silently thrown away
                                // before it ever reached disk. This is the
                                // actual reason nothing appeared to save.
                                onDismiss()
                            }
                        }
                    ) {
                        Text(
                            if (english) "✅ Save" else "✅ حفظ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    Text(
                        if (english) "↺ Reset name/picture/pronunciation to original"
                        else "↺ رجّع الاسم/الصورة/النطق الأصلي",
                        fontSize = 13.sp,
                        color = Color(0xFF6D4C41),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().clickable {
                            scope.launch {
                                overridesRepository.clear(word.id)
                                onDismiss() // see note on Save's onClick above — order matters here too
                            }
                        }
                    )
                    Text(
                        if (english) "Cancel" else "إلغاء",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().clickable { onDismiss() }
                    )
                }
            }
        }
    }
}
