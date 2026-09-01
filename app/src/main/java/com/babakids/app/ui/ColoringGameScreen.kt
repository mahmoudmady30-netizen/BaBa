package com.babakids.app.ui

import android.graphics.Region as AndroidRegion
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babakids.app.audio.SmartVoiceManager
import com.babakids.app.data.ColoringCategory
import com.babakids.app.data.ColoringShape
import com.babakids.app.data.ColoringShapes
import com.babakids.app.data.Haptics
import com.babakids.app.data.ParentSettingsManager
import com.babakids.app.ui.theme.BaBaGradients
import com.babakids.app.ui.theme.GlossyCard
import kotlinx.coroutines.launch

/**
 * `nameAr` is what's shown/matched; `spokenAr` is what's actually sent to
 * TTS. They differ deliberately: undiacritized Arabic makes TTS engines
 * guess the vowels, and "برتقالي" in particular was being mispronounced.
 * The spoken forms here are fully diacritized, matching the same technique
 * already used for the built-in vocabulary in EgyptianSpokenForms.
 */
private data class PaintColor(
    val color: Color,
    val nameAr: String,
    val nameEn: String,
    val spokenAr: String
)

private val paintPalette = listOf(
    PaintColor(Color(0xFFE53935), "أحمر", "Red", "أَحْمَر"),
    PaintColor(Color(0xFFFB8C00), "برتقالي", "Orange", "بُرْتُقالِي"),
    PaintColor(Color(0xFFFDD835), "أصفر", "Yellow", "أَصْفَر"),
    PaintColor(Color(0xFF43A047), "أخضر", "Green", "أَخْضَر"),
    PaintColor(Color(0xFF1E88E5), "أزرق", "Blue", "أَزْرَق"),
    PaintColor(Color(0xFF8E24AA), "بنفسجي", "Purple", "بَنَفْسِجِي"),
    PaintColor(Color(0xFFEC407A), "وردي", "Pink", "وَرْدِي"),
    PaintColor(Color(0xFF6D4C41), "بني", "Brown", "بُنِّي")
)

/**
 * The coloring-book activity: pick a category, pick a shape, then tap
 * each region with a chosen color to fill it in — smoothly animated,
 * with the color's name spoken aloud on selection.
 */
@Composable
fun ColoringGameScreen(
    english: Boolean = false,
    arabicDialect: String = ParentSettingsManager.DIALECT_EGYPTIAN,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val smartVoice = remember { SmartVoiceManager.getInstance(context) }
    DisposableEffect(Unit) { onDispose { smartVoice.release() } }

    var category by remember { mutableStateOf<ColoringCategory?>(null) }
    var selectedShape by remember { mutableStateOf<ColoringShape?>(null) }

    val shape = selectedShape
    when {
        shape != null -> ColoringCanvasScreen(
            shape = shape,
            english = english,
            arabicDialect = arabicDialect,
            smartVoice = smartVoice,
            onBack = { selectedShape = null }
        )
        category != null -> ColoringShapePicker(
            category = category!!,
            english = english,
            onBack = { category = null },
            onPick = { selectedShape = it }
        )
        else -> ColoringCategoryPicker(english = english, onBack = onBack, onPick = { category = it })
    }
}

@Composable
private fun ColoringCategoryPicker(english: Boolean, onBack: () -> Unit, onPick: (ColoringCategory) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(onBack = onBack)
        Text(
            text = if (english) "🎨 Painter — pick a category" else "🎨 الرسام — اختار قسم",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
        val categories = listOf(
            Triple(ColoringCategory.SHAPES, "⭐", if (english) "Shapes" else "الأشكال"),
            Triple(ColoringCategory.NUMBERS, "🔢", if (english) "Numbers" else "الأرقام"),
            Triple(ColoringCategory.LETTERS, "🔤", if (english) "Letters" else "الحروف"),
            Triple(ColoringCategory.ANIMALS, "🐾", if (english) "Animals" else "الحيوانات")
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            gridItems(categories) { (cat, emoji, label) ->
                GlossyCard(
                    gradient = BaBaGradients.cycle[cat.ordinal % BaBaGradients.cycle.size],
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    onClick = { onPick(cat) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(emoji, fontSize = 48.sp)
                        Text(label, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ColoringShapePicker(
    category: ColoringCategory,
    english: Boolean,
    onBack: () -> Unit,
    onPick: (ColoringShape) -> Unit
) {
    val shapes = remember(category) { ColoringShapes.forCategory(category) }
    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(onBack = onBack)
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            gridItems(shapes) { shape ->
                GlossyCard(
                    gradient = BaBaGradients.sky,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    onClick = { onPick(shape) }
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
                        ShapePreview(shape)
                    }
                }
            }
        }
    }
}

/** A small static black-outline preview used on the shape-picker grid. */
@Composable
private fun ShapePreview(shape: ColoringShape) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val paths = remember(shape, size) {
        if (size.width > 0 && size.height > 0) shape.outline(size.width.toFloat(), size.height.toFloat()) else emptyList()
    }
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
    ) {
        paths.forEach { path ->
            drawPath(path, color = Color.White, style = Fill)
            drawPath(path, color = Color(0xFF3A3A3A), style = Stroke(width = 3.dp.toPx()))
        }
    }
}

@Composable
private fun ColoringCanvasScreen(
    shape: ColoringShape,
    english: Boolean,
    arabicDialect: String,
    smartVoice: SmartVoiceManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val regions = remember(shape, canvasSize) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            shape.outline(canvasSize.width.toFloat(), canvasSize.height.toFloat())
        } else {
            emptyList()
        }
    }
    // Each region has a target color plus a 0f->1f "how much has it been
    // painted yet" progress — the actual displayed color is computed via
    // lerp() between white and the target as progress animates, giving a
    // smooth fill-in instead of an instant color swap. Deliberately not
    // animating Color directly (Animatable<Color, ...>) — that needs a
    // Color-specific vector converter that isn't reliably available in
    // this project's exact Compose version (a real build failure this
    // avoids entirely); animating a plain Float is always safe.
    val regionTargetColors = remember(shape) { mutableStateListOf<Color>() }
    val regionProgress = remember(shape) { mutableListOf<Animatable<Float, AnimationVector1D>>() }
    LaunchedEffect(regions) {
        regionTargetColors.clear()
        regionProgress.clear()
        repeat(regions.size) {
            regionTargetColors.add(Color.White)
            regionProgress.add(Animatable(0f))
        }
    }
    var filledFlags by remember(shape) { mutableStateOf(BooleanArray(0)) }
    LaunchedEffect(regions) { filledFlags = BooleanArray(regions.size) }

    var selectedPaint by remember { mutableStateOf(paintPalette.first()) }
    var showCelebration by remember { mutableStateOf(false) }

    fun checkComplete() {
        if (filledFlags.isNotEmpty() && filledFlags.all { it }) {
            showCelebration = true
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(onBack = onBack)
        Text(
            text = if (english) shape.nameEn else shape.name,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(24.dp)
                .background(Color(0xFFFAFAFA), RoundedCornerShape(24.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(regions) {
                        detectTapGestures { offset ->
                            val hitIndex = regions.indexOfLast { path -> isPointInPath(path, offset, canvasSize) }
                            if (hitIndex >= 0 && hitIndex < regionProgress.size) {
                                regionTargetColors[hitIndex] = selectedPaint.color
                                scope.launch {
                                    regionProgress[hitIndex].snapTo(0f)
                                    regionProgress[hitIndex].animateTo(1f, tween(280))
                                }
                                if (hitIndex < filledFlags.size) {
                                    val updated = filledFlags.copyOf()
                                    updated[hitIndex] = true
                                    filledFlags = updated
                                }
                                Haptics.vibrateTap(context, true)
                                checkComplete()
                            }
                        }
                    }
            ) {
                regions.forEachIndexed { index, path ->
                    val target = regionTargetColors.getOrNull(index) ?: Color.White
                    val progress = regionProgress.getOrNull(index)?.value ?: 0f
                    val fillColor = lerp(Color.White, target, progress)
                    drawPath(path, color = fillColor, style = Fill)
                    drawPath(path, color = Color(0xFF2B2B2B), style = Stroke(width = 5.dp.toPx()))
                }
            }

            if (showCelebration) {
                ConfettiBurst(particleCount = 18, richPalette = true)
            }
        }

        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(paintPalette) { paint ->
                val isSelected = paint == selectedPaint
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 56.dp else 46.dp)
                        .shadow(if (isSelected) 8.dp else 2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(paint.color)
                        .clickable {
                            selectedPaint = paint
                            Haptics.vibrateTap(context, true)
                            smartVoice.playSmartVoice(
                                if (english) paint.nameEn else paint.spokenAr,
                                english = english,
                                dialect = arabicDialect
                            )
                        }
                )
            }
        }
    }
}

private fun isPointInPath(path: Path, offset: Offset, size: IntSize): Boolean {
    if (size.width <= 0 || size.height <= 0) return false
    val androidPath = path.asAndroidPath()
    val region = AndroidRegion()
    val clip = AndroidRegion(0, 0, size.width, size.height)
    region.setPath(androidPath, clip)
    return region.contains(offset.x.toInt(), offset.y.toInt())
}
