package com.multibankgroup.pricetracker.domain.model

/** Domain model combining real-time price data with static metadata. */
data class Stock(
    val symbol: String,
    val companyName: String,
    val description: String,
    val currentPrice: Double,
    val previousPrice: Double,
    val priceDirection: PriceDirection,
    val lastUpdatedTimestamp: Long
)