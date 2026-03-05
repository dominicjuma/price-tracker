package com.multibankgroup.pricetracker.feature.shared_ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colors for stock price direction indicators.
 *
 * Material 3's built-in color roles don't have "positive/negative" semantics,
 * so we extend the theme with a custom [CompositionLocal].
 */
@Immutable
data class StockColors(
    val up: Color,
    val down: Color,
    val upContainer: Color,
    val downContainer: Color,
    val upFlash: Color,
    val downFlash: Color
)

val DarkStockColors = StockColors(
    up = DarkStockUp,
    down = DarkStockDown,
    upContainer = DarkStockUpContainer,
    downContainer = DarkStockDownContainer,
    upFlash = DarkStockUpFlash,
    downFlash = DarkStockDownFlash
)

val LightStockColors = StockColors(
    up = LightStockUp,
    down = LightStockDown,
    upContainer = LightStockUpContainer,
    downContainer = LightStockDownContainer,
    upFlash = LightStockUpFlash,
    downFlash = LightStockDownFlash
)

val LocalStockColors = staticCompositionLocalOf {
    StockColors(
        up = Color.Unspecified,
        down = Color.Unspecified,
        upContainer = Color.Unspecified,
        downContainer = Color.Unspecified,
        upFlash = Color.Unspecified,
        downFlash = Color.Unspecified
    )
}