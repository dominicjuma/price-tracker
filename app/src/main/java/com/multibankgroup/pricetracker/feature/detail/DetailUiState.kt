package com.multibankgroup.pricetracker.feature.detail

import androidx.compose.runtime.Immutable
import com.multibankgroup.pricetracker.domain.model.PriceDirection
import com.multibankgroup.pricetracker.feature.shared_ui.model.UiError

/** Detail screen state. [isLoading] covers the gap before first price emission. */
@Immutable
data class DetailUiState(
    val symbol: String = "",
    val companyName: String = "",
    val description: String = "",
    val currentPrice: Double = 0.0,
    val previousPrice: Double = 0.0,
    val priceDirection: PriceDirection = PriceDirection.NONE,
    val lastUpdatedTimestamp: Long = 0L,
    val isLoading: Boolean = true,
    val error: UiError? = null
)