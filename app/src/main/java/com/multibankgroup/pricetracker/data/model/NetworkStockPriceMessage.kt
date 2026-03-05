package com.multibankgroup.pricetracker.data.model

import kotlinx.serialization.Serializable

/**
 * Wire format for stock price messages sent to and echoed from the WebSocket server.
 * This is the raw data layer representation — only used within the data layer.
 *
 * We serialize all symbol prices in a single message per tick to minimize
 * WebSocket sends (1 message per 2-second interval, not 25).
 */
@Serializable
data class NetworkStockPriceMessage(
    val prices: List<NetworkSymbolPrice>
)

@Serializable
data class NetworkSymbolPrice(
    val symbol: String,
    val price: Double,
    val timestamp: Long
)