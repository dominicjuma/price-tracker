package com.multibankgroup.pricetracker.data.websocket

import kotlinx.coroutines.flow.Flow

/**
 * Abstracts the raw WebSocket connection so tests inject a [FakeStockPriceDataSource]
 * with full control over events — no real network needed.
 */
interface StockPriceDataSource {

    /**
     * Establish a connection and observe lifecycle events + messages as a [Flow].
     * The flow is active as long as it is collected.
     */
    fun observeConnection(url: String): Flow<WebSocketEvent>

    /**
     * Send a raw text message through the active connection.
     * @return true if the message was enqueued, false otherwise.
     */
    fun send(message: String): Boolean
}