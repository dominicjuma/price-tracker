package com.multibankgroup.pricetracker.feature.shared_ui.model

import androidx.compose.runtime.Immutable
import com.multibankgroup.pricetracker.domain.model.PriceDirection

/** UI model for a feed row. [lastUpdatedTimestamp] keys the flash animation. */
@Immutable
data class StockDisplayItem(
    val symbol: String,
    val companyName: String,
    val currentPrice: Double,
    val previousPrice: Double,
    val priceDirection: PriceDirection,
    val lastUpdatedTimestamp: Long
)