package com.multibankgroup.pricetracker.data.repository

import com.multibankgroup.pricetracker.data.model.DataError
import com.multibankgroup.pricetracker.data.model.StockData
import com.multibankgroup.pricetracker.data.websocket.ConnectionStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Fake implementation of [StockPriceRepository] for unit testing use cases
 * and ViewModels.
 *
 * Exposes [MutableStateFlow]s so tests can push state changes directly
 * without needing a WebSocket, ticker, or coroutine scope.
 */
class FakeStockPriceRepository : StockPriceRepository {

    private val _stockPrices = MutableStateFlow<Map<String, StockData>>(emptyMap())
    override val stockPrices: StateFlow<Map<String, StockData>> = _stockPrices.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    override val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _isFeedActive = MutableStateFlow(true)
    override val isFeedActive: StateFlow<Boolean> = _isFeedActive.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _errors = MutableSharedFlow<DataError>(extraBufferCapacity = 1)
    override val errors: SharedFlow<DataError> = _errors.asSharedFlow()

    var connectCalled = false
        private set
    var disconnectCalled = false
        private set

    override fun connect() {
        connectCalled = true
    }

    override fun toggleFeed() {
        _isFeedActive.update { !it }
    }

    override fun disconnect() {
        disconnectCalled = true
    }

    // ──────────────────────────────────────────────────────────────────
    // Test control methods
    // ──────────────────────────────────────────────────────────────────

    fun setPrices(prices: Map<String, StockData>) {
        _stockPrices.value = prices
    }

    fun setConnectionStatus(status: ConnectionStatus) {
        _connectionStatus.value = status
    }

    fun setOnline(online: Boolean) {
        _isOnline.value = online
    }

    fun emitError(error: DataError) {
        _errors.tryEmit(error)
    }
}