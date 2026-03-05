package com.multibankgroup.pricetracker.feature.feed

import androidx.compose.runtime.Immutable
import com.multibankgroup.pricetracker.feature.shared_ui.model.StockDisplayItem
import com.multibankgroup.pricetracker.feature.shared_ui.model.UiError

/** Feed screen state. @Immutable guarantees recomposition skipping. */
@Immutable
data class FeedUiState(
    val stocks: List<StockDisplayItem> = emptyList(),
    val isConnected: Boolean = false,
    val isFeedActive: Boolean = true,
    val isOnline: Boolean = true,
    val error: UiError? = null
)