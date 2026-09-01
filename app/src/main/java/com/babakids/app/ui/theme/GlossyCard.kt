package com.babakids.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Premium layered card used throughout BaBa.
 *
 * The card intentionally has three visual layers: a deep shadow, a bright
 * beveled surface and a moving-looking glass highlight. This gives the UI a
 * soft toy / 3D dashboard feel instead of the usual flat Material cards.
 */
@Composable
fun GlossyCard(
    gradient: Brush,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .shadow(18.dp, shape, clip = false, ambientColor = Color(0x33000000), spotColor = Color(0x55000000))
            .clip(shape)
            .background(gradient)
            .border(1.dp, Color.White.copy(alpha = 0.72f), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        content()

        // Beveled top light.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.White.copy(alpha = 0.38f),
                            0.18f to Color.White.copy(alpha = 0.12f),
                            0.52f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.08f)
                        )
                    )
                )
        )
        // A subtle diagonal reflection makes the surface feel like polished
        // plastic rather than a flat gradient.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.14f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.05f)
                        )
                    )
                )
        )
    }
}
