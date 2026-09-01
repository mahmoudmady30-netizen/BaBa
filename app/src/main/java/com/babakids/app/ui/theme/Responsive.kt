package com.babakids.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Adaptive column count for the app's card grids (categories, words,
 * sentence-builder tiles). Reads the *current* screen width, which
 * updates correctly on rotation and on tablets — this is what makes
 * landscape/tablet layouts use more columns instead of stretching two
 * huge cards across a wide screen.
 *
 * Rough breakpoints follow Material's compact/medium/expanded window
 * size classes (600dp / 840dp).
 */
@Composable
fun rememberAdaptiveColumns(baseColumns: Int = 2): Int {
    val widthDp = LocalConfiguration.current.screenWidthDp
    val extraColumns = when {
        widthDp < 600 -> 0
        widthDp < 840 -> 1
        widthDp < 1200 -> 2
        else -> 3
    }
    return baseColumns + extraColumns
}
