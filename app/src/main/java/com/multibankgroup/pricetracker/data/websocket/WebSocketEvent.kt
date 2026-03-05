package com.multibankgroup.pricetracker.data.websocket

/** Raw WebSocket events, internal to the data layer. Repository maps these into [StockData] and [ConnectionStatus]. */
sealed interface WebSocketEvent {
    data object Connected : WebSocketEvent
    /** The echoed price data. */
    data class MessageReceived(val text: String) : WebSocketEvent
    data class Disconnected(val code: Int, val reason: String) : WebSocketEvent
    data class Error(val throwable: Throwable) : WebSocketEvent
}

/** WebSocket connection state exposed to the UI for the connection indicator. */
enum class ConnectionStatus { CONNECTING, CONNECTED, DISCONNECTED }