package com.multibankgroup.pricetracker.data.repository

import com.multibankgroup.pricetracker.data.model.DataError
import com.multibankgroup.pricetracker.data.model.StockData
import com.multibankgroup.pricetracker.data.websocket.ConnectionStatus
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** Single source of truth for real-time stock price data. */
interface StockPriceRepository {

    /** Observable stream of all stock prices, keyed by symbol. */
    val stockPrices: StateFlow<Map<String, StockData>>

    /** Observable connection status for the UI's connection indicator. */
    val connectionStatus: StateFlow<ConnectionStatus>

    /** Whether the price feed (send loop) is currently active. */
    val isFeedActive: StateFlow<Boolean>

    /** Whether the device currently has internet connectivity. */
    val isOnline: StateFlow<Boolean>

    /**
     * One-shot error events for the UI to display.
     * Uses [SharedFlow] (not StateFlow) because errors are events, not state —
     * they should be shown once and dismissed, not replayed on recomposition.
     */
    val errors: SharedFlow<DataError>

    /** Start the WebSocket connection. Idempotent. */
    fun connect()

    /** Toggle the price feed on or off. */
    fun toggleFeed()

    /** Disconnect the WebSocket and stop all operations. */
    fun disconnect()
}