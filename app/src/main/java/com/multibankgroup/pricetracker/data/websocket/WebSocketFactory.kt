package com.multibankgroup.pricetracker.data.websocket

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener

private const val PING_INTERVAL_SECONDS = 30L

/**
 * Factory that creates OkHttp [WebSocket] instances.
 *
 * Isolating WebSocket construction behind a factory enables:
 * - Injecting a fake factory in unit tests that returns a mock WebSocket
 * - Swapping the OkHttp configuration without touching the data source
 */
class WebSocketFactory @Inject constructor() {

    fun create(url: String, listener: WebSocketListener): WebSocket {
        val client = OkHttpClient.Builder()
            .pingInterval(PING_INTERVAL_SECONDS, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(url)
            .build()

        return client.newWebSocket(request, listener)
    }
}