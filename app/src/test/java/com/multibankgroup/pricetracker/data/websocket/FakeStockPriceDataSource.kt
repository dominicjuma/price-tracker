package com.multibankgroup.pricetracker.data.websocket

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow

/** Fake [StockPriceDataSource] backed by a [Channel]. No real network. */
class FakeStockPriceDataSource : StockPriceDataSource {

    private val eventChannel = Channel<WebSocketEvent>(Channel.UNLIMITED)

    val sentMessages = mutableListOf<String>()
    var isConnected = false
        private set

    override fun observeConnection(url: String): Flow<WebSocketEvent> {
        isConnected = true
        return eventChannel.consumeAsFlow()
    }

    override fun send(message: String): Boolean {
        if (!isConnected) return false
        sentMessages.add(message)
        return true
    }

    // ── Test control ─────────────────────────────────────────────────

    suspend fun emit(event: WebSocketEvent) {
        eventChannel.send(event)
    }

    suspend fun simulateConnected() {
        emit(WebSocketEvent.Connected)
    }

    suspend fun echoLastSentMessage() {
        val lastMessage = sentMessages.lastOrNull() ?: return
        emit(WebSocketEvent.MessageReceived(lastMessage))
    }

    suspend fun simulateError(throwable: Throwable = RuntimeException("Connection lost")) {
        isConnected = false
        emit(WebSocketEvent.Error(throwable))
    }

    suspend fun simulateDisconnect(code: Int = 1000, reason: String = "Normal closure") {
        isConnected = false
        emit(WebSocketEvent.Disconnected(code, reason))
    }

    fun close() {
        isConnected = false
        eventChannel.close()
    }
}