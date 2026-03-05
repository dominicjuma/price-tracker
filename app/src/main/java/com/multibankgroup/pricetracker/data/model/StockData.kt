package com.multibankgroup.pricetracker.data.model

/**
 * Internal data layer representation of a stock's price state.
 * Tracks both current and previous price to enable direction calculation
 * in the domain layer.
 *
 * This model is exposed by [StockPriceRepository] and consumed by the domain layer.
 * It is immutable — new state is created on each price update.
 */
data class StockData(
    val symbol: String,
    val currentPrice: Double,
    val previousPrice: Double,
    val lastUpdatedTimestamp: Long
)