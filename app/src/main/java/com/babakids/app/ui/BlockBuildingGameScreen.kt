package com.babakids.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babakids.app.data.BuildPattern
import com.babakids.app.data.BuildPatterns
import com.babakids.app.data.Haptics
import com.babakids.app.ui.theme.BaBaGradients
import com.babakids.app.ui.theme.GlossyCard
import kotlinx.coroutines.launch

private val blockPalette = listOf(
    Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFDD835), Color(0xFF43A047),
    Color(0xFF1E88E5), Color(0xFF8E24AA), Color(0xFFEC407A), Color(0xFF6D4C41)
)

/**
 * The colored-blocks activity: pick a target pattern, then place colored
 * blocks on an empty grid to build a match — same bevelled-cube visual
 * language as the splash screen's logo, so the blocks read as real
 * chunky 3D-ish pieces rather than flat colored squares.
 */
@Composable
fun BlockBuildingGameScreen(english: Boolean = false, onBack: () -> Unit = {}) {
    var selectedPattern by remember { mutableStateOf<BuildPattern?>(null) }
    val pattern = selectedPattern
    if (pattern != null) {
        BlockBuildScreen(pattern = pattern, english = english, onBack = { selectedPattern = null })
    } else {
        BlockPatternPicker(english = english, onBack = onBack, onPick = { selectedPattern = it })
    }
}

@Composable
private fun BlockPatternPicker(english: Boolean, onBack: () -> Unit, onPick: (BuildPattern) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(onBack = onBack)
        Text(
            text = if (english) "🧊 Colored Blocks — pick a shape to build" else "🧊 المكعبات الملونة — اختار شكل تبنيه",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            gridItems(BuildPatterns.patterns) { pattern ->
                GlossyCard(
                    gradient = BaBaGradients.purple,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    onClick = { onPick(pattern) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(pattern.emoji, fontSize = 44.sp)
                        Text(
                            if (english) pattern.nameEn else pattern.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockBuildScreen(pattern: BuildPattern, english: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val rows = pattern.grid.size
    val cols = pattern.grid.firstOrNull()?.size ?: 0

    // One animated color per cell — starts empty (null/transparent),
    // smoothly pops in with the chosen color when tapped.
    val cellColors = remember(pattern) {
        List(rows) { r -> List(cols) { c -> mutableStateOf<Color?>(null) } }
    }
    val cellScales = remember(pattern) {
        List(rows) { r -> List(cols) { c -> Animatable(1f) } }
    }
    var selectedColor by remember(pattern) { mutableStateOf(blockPalette.first()) }
    var isEraser by remember(pattern) { mutableStateOf(false) }
    var showCelebration by remember(pattern) { mutableStateOf(false) }

    fun checkComplete() {
        val matches = (0 until rows).all { r ->
            (0 until cols).all { c -> cellColors[r][c].value == pattern.grid[r][c] }
        }
        if (matches) showCelebration = true
    }

    fun tapCell(r: Int, c: Int) {
        val newColor = if (isEraser) null else selectedColor
        cellColors[r][c].value = newColor
        Haptics.vibrateTap(context, true)
        scope.launch {
            cellScales[r][c].animateTo(1.15f, tween(90))
            cellScales[r][c].animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        checkComplete()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(onBack = onBack)
        Text(
            text = if (english) "Build: ${pattern.nameEn}" else "ابني: ${pattern.name}",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))

        // Small target preview — what the child is trying to match.
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                Column {
                    pattern.grid.forEach { row ->
                        Row {
                            row.forEach { cellColor ->
                                if (cellColor != null) {
                                    VoxelCube(letter = "", color = cellColor, cubeSize = 14.dp, fontSize = 1.sp)
                                } else {
                                    Box(modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // The interactive build grid — starts empty, fills in as the
        // child taps with the currently selected color.
        Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp), contentAlignment = Alignment.Center) {
            Column {
                for (r in 0 until rows) {
                    Row {
                        for (c in 0 until cols) {
                            val color = cellColors[r][c].value
                            val scaleValue = cellScales[r][c].value
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .padding(2.dp)
                                    .scale(scaleValue)
                                    .clickable { tapCell(r, c) }
                            ) {
                                if (color != null) {
                                    VoxelCube(
                                        letter = "",
                                        color = color,
                                        cubeSize = 34.dp,
                                        fontSize = 1.sp,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showCelebration) {
                ConfettiBurst(particleCount = 20, richPalette = true)
            }
        }

        // Eraser — pulled out as its own clearly-labeled button above the
        // color row, not one more small circle mixed in among the paint
        // colors, so it's unmistakable and easy for a child to find.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .shadow(if (isEraser) 8.dp else 3.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isEraser) Color(0xFFBDBDBD) else Color(0xFFEEEEEE))
                    .clickable { isEraser = true }
                    .padding(horizontal = 22.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🧹", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (english) "Eraser" else "مسح",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF3A3A3A)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(blockPalette) { color ->
                val isSelected = !isEraser && color == selectedColor
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 52.dp else 44.dp)
                        .shadow(if (isSelected) 8.dp else 2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(color)
                        .clickable {
                            selectedColor = color
                            isEraser = false
                        }
                )
            }
        }
    }
}
