package com.babakids.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babakids.app.ui.theme.BaBaGradients
import com.babakids.app.ui.theme.GlossyCard

data class GameActivity(val id: String, val emoji: String, val titleAr: String, val titleEn: String)

/** The full catalog of mini-games — Parent Mode's game-visibility toggle filters this same list, so add a new game here and the parent control picks it up automatically. */
val allGameActivities = listOf(
    GameActivity("coloring", "🎨", "الرسام", "Painter"),
    GameActivity("blocks", "🧊", "المكعبات الملونة", "Colored Blocks"),
    GameActivity("memory", "🧠", "لعبة الذاكرة", "Memory Match"),
    GameActivity("puzzle", "🧩", "ألغاز", "Puzzle")
)

/** "ألعابي" (My Games) — a hub of mini-games inspired by the main categories (coloring, building, matching), not vocabulary practice. */
@Composable
fun ActivitiesScreen(
    english: Boolean = false,
    disabledActivityIds: Set<String> = emptySet(),
    pinnedActivityId: String? = null,
    onBack: () -> Unit = {},
    onActivityClick: (String) -> Unit
) {
    val visible = allGameActivities
        .filterNot { it.id in disabledActivityIds }
        .sortedByDescending { it.id == pinnedActivityId }

    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(onBack = onBack)
        Text(
            text = if (english) "🎮 My Games" else "🎮 ألعابي",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(visible) { activity ->
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                    GlossyCard(
                        gradient = BaBaGradients.orange,
                        modifier = Modifier.fillMaxSize(),
                        onClick = { onActivityClick(activity.id) }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(activity.emoji, fontSize = 52.sp)
                            Text(
                                if (english) activity.titleEn else activity.titleAr,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                    if (activity.id == pinnedActivityId) {
                        Text(
                            "🎮📌",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }
        }
    }
}
