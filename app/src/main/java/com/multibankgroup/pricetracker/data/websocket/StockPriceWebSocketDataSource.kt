package com.multibankgroup.pricetracker.data.websocket

import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber

private const val CLOSE_NORMAL = 1000
private const val CLOSE_REASON = "Client closed connection"

/**
 * Single concern: manages the raw WebSocket connection.
 * Bridges [WebSocketListener] callbacks into a [Flow] via [callbackFlow].
 * [WebSocketFactory] is injected so tests can provide a fake.
 */
class StockPriceWebSocketDataSource @Inject constructor(
    private val webSocketFactory: WebSocketFactory
) : StockPriceDataSource {
    /**
     * Reference to the active WebSocket connection.
     * Stored at class level so [send] can be called from outside the [callbackFlow].
     * Nulled out on close to prevent sends to a dead connection.
     */
    private var webSocket: WebSocket? = null

    /**
     * Establish a WebSocket connection and observe events as a [Flow].
     *
     * The flow is active as long as it is collected. When the collector cancels
     * (e.g., repository scope is cancelled), [awaitClose] triggers cleanup.
     *
     * Events are internal to the data layer — the repository maps them into
     * [StockData] and [ConnectionStatus].
     *
     * @param url The WebSocket server URL (e.g., wss://ws.postman-echo.com/raw)
     */
    override fun observeConnection(url: String): Flow<WebSocketEvent> = callbackFlow {
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.d("WebSocket connected")
                trySend(WebSocketEvent.Connected)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Timber.d("WebSocket message received: ${text.take(100)}")
                trySend(WebSocketEvent.MessageReceived(text))
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("WebSocket closing: code=$code reason=$reason")
                trySend(WebSocketEvent.Disconnected(code, reason))
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("WebSocket closed: code=$code reason=$reason")
                trySend(WebSocketEvent.Disconnected(code, reason))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.e(t, "WebSocket failure")
                trySend(WebSocketEvent.Error(t))
            }
        }

        webSocket = webSocketFactory.create(url, listener)

        awaitClose {
            Timber.d("Flow cancelled — closing WebSocket")
            closeConnection()
        }
    }

    /**
     * Send a raw text message through the active WebSocket connection.
     *
     * @return true if the message was enqueued successfully, false if the
     *         connection is not active or the send failed.
     */
    override fun send(message: String): Boolean {
        return webSocket?.send(message) ?: run {
            Timber.w("Attempted to send on a null WebSocket")
            false
        }
    }

    /**
     * Gracefully close the WebSocket connection.
     * Called automatically when the [observeConnection] flow is cancelled,
     * and can also be called explicitly.
     */
    private fun closeConnection() {
        webSocket?.close(CLOSE_NORMAL, CLOSE_REASON)
        webSocket = null
    }
}