package com.multibankgroup.pricetracker.data.model

/**
 * Static metadata about a stock symbol.
 * This is the data layer representation — the domain layer maps it
 * into its own model when combining with price data.
 */
data class StockInfo(
    val symbol: String,
    val companyName: String,
    val description: String,
    val initialPrice: Double
)