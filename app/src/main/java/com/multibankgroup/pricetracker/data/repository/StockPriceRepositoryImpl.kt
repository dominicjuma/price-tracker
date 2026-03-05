package com.multibankgroup.pricetracker.data.repository

import com.multibankgroup.pricetracker.core.connectivity.ConnectivityObserver
import com.multibankgroup.pricetracker.core.util.Clock
import com.multibankgroup.pricetracker.data.model.DataError
import com.multibankgroup.pricetracker.data.model.NetworkStockPriceMessage
import com.multibankgroup.pricetracker.data.model.NetworkSymbolPrice
import com.multibankgroup.pricetracker.data.model.StockData
import com.multibankgroup.pricetracker.data.websocket.ConnectionStatus
import com.multibankgroup.pricetracker.data.websocket.StockPriceDataSource
import com.multibankgroup.pricetracker.data.websocket.WebSocketEvent
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber

private const val WEBSOCKET_URL = "wss://ws.postman-echo.com/raw"
private const val PRICE_UPDATE_INTERVAL_MS = 2_000L
private const val INITIAL_RETRY_DELAY_MS = 1_000L
private const val MAX_RETRY_DELAY_MS = 30_000L
private const val RETRY_BACKOFF_MULTIPLIER = 2.0
private const val MAX_PRICE_DELTA_PERCENT = 0.02
private const val MIN_STOCK_PRICE = 1.0

/**
 * Prices update ONLY on echo receipt, not on send — server-authoritative.
 * Reconnects with exponential backoff. Waits for connectivity before retrying.
 */
@Singleton
class StockPriceRepositoryImpl @Inject constructor(
    private val dataSource: StockPriceDataSource,
    private val metadataRepository: StockMetadataRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val externalScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val json: Json,
    private val clock: Clock
) : StockPriceRepository {

    private val _stockPrices = MutableStateFlow<Map<String, StockData>>(emptyMap())
    override val stockPrices: StateFlow<Map<String, StockData>> = _stockPrices.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    override val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _isFeedActive = MutableStateFlow(true)
    override val isFeedActive: StateFlow<Boolean> = _isFeedActive.asStateFlow()

    override val isOnline: StateFlow<Boolean> get() = connectivityObserver.isOnline

    private val _errors = MutableSharedFlow<DataError>(extraBufferCapacity = 1)
    override val errors: SharedFlow<DataError> = _errors

    private var tickerJob: Job? = null
    private var connectionJob: Job? = null
    private var isStarted = false

    override fun connect() {
        if (isStarted) return
        isStarted = true

        seedInitialPrices()
        connectionJob = externalScope.launch {
            connectWithRetry()
        }
    }

    /** Pauses/resumes the ticker without disconnecting the WebSocket. */
    override fun toggleFeed() {
        val newActive = !_isFeedActive.value
        _isFeedActive.update { newActive }

        if (newActive) startTicker() else stopTicker()
    }

    override fun disconnect() {
        stopTicker()
        connectionJob?.cancel()
        connectionJob = null
        isStarted = false
        _connectionStatus.update { ConnectionStatus.DISCONNECTED }
    }

    // ── Connection with exponential backoff ───────────────────────────

    private suspend fun connectWithRetry() {
        var retryDelay = INITIAL_RETRY_DELAY_MS

        while (externalScope.isActive) {
            _connectionStatus.update { ConnectionStatus.CONNECTING }

            try {
                dataSource.observeConnection(WEBSOCKET_URL).collect { event ->
                    when (event) {
                        is WebSocketEvent.Connected -> {
                            retryDelay = INITIAL_RETRY_DELAY_MS
                            _connectionStatus.update { ConnectionStatus.CONNECTED }
                            if (_isFeedActive.value) startTicker()
                        }

                        is WebSocketEvent.MessageReceived -> {
                            onEchoReceived(event.text)
                        }

                        is WebSocketEvent.Disconnected -> {
                            _connectionStatus.update { ConnectionStatus.DISCONNECTED }
                            stopTicker()
                        }

                        is WebSocketEvent.Error -> {
                            Timber.e(event.throwable, "WebSocket error")
                            _connectionStatus.update { ConnectionStatus.DISCONNECTED }
                            _errors.tryEmit(DataError.ConnectionLost(event.throwable))
                            stopTicker()
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "WebSocket collection failed")
                _connectionStatus.update { ConnectionStatus.DISCONNECTED }
                _errors.tryEmit(DataError.ConnectionFailed(e))
                stopTicker()
            }

            // Wait for connectivity before burning through retries
            if (!connectivityObserver.isOnline.value) {
                _errors.tryEmit(DataError.NoInternet)
                connectivityObserver.isOnline.first { it }
                retryDelay = INITIAL_RETRY_DELAY_MS
                continue
            }

            delay(retryDelay)
            retryDelay = min(
                (retryDelay * RETRY_BACKOFF_MULTIPLIER).toLong(),
                MAX_RETRY_DELAY_MS
            )
        }
    }

    // ── Price generation ─────────────────────────────────────────────

    /** Seeds prices from metadata so the UI has data before the first echo. */
    private fun seedInitialPrices() {
        val initial = metadataRepository.getAllStockInfo().associate { info ->
            info.symbol to StockData(
                symbol = info.symbol,
                currentPrice = info.initialPrice,
                previousPrice = info.initialPrice,
                lastUpdatedTimestamp = clock.now()
            )
        }
        _stockPrices.update { initial }
    }

    private fun startTicker() {
        stopTicker()
        tickerJob = externalScope.launch {
            while (isActive) {
                delay(PRICE_UPDATE_INTERVAL_MS)
                sendPriceUpdate()
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    /** Batches all 25 symbols into one message per tick. Runs on [ioDispatcher]. */
    private suspend fun sendPriceUpdate() = withContext(ioDispatcher) {
        try {
            val currentPrices = _stockPrices.value
            val symbols = metadataRepository.getAllSymbols()
            val now = clock.now()

            val priceUpdates = symbols.map { symbol ->
                val currentPrice = currentPrices[symbol]?.currentPrice
                    ?: metadataRepository.getStockInfo(symbol).initialPrice
                val newPrice = generateNewPrice(currentPrice)
                NetworkSymbolPrice(
                    symbol = symbol,
                    price = newPrice,
                    timestamp = now
                )
            }

            val message = NetworkStockPriceMessage(prices = priceUpdates)
            val jsonString = json.encodeToString(message)
            dataSource.send(jsonString)
        } catch (e: Exception) {
            Timber.e(e, "Failed to serialize/send price update")
            _errors.tryEmit(DataError.SerializationFailed(e))
        }
    }

    private fun generateNewPrice(currentPrice: Double): Double {
        val maxDelta = currentPrice * MAX_PRICE_DELTA_PERCENT
        val delta = Random.nextDouble(-maxDelta, maxDelta)
        val newPrice = max(currentPrice + delta, MIN_STOCK_PRICE)
        return Math.round(newPrice * 100.0) / 100.0
    }

    // ── Echo processing ──────────────────────────────────────────────

    /**
     * ONLY place prices are updated — on echo receipt, never on send.
     * Runs on [ioDispatcher] for JSON deserialization.
     */
    private suspend fun onEchoReceived(text: String) = withContext(ioDispatcher) {
        try {
            val message = json.decodeFromString<NetworkStockPriceMessage>(text)

            _stockPrices.update { currentMap ->
                val mutableMap = currentMap.toMutableMap()

                for (symbolPrice in message.prices) {
                    val existing = mutableMap[symbolPrice.symbol]
                    mutableMap[symbolPrice.symbol] = StockData(
                        symbol = symbolPrice.symbol,
                        currentPrice = symbolPrice.price,
                        previousPrice = existing?.currentPrice ?: symbolPrice.price,
                        lastUpdatedTimestamp = symbolPrice.timestamp
                    )
                }

                mutableMap.toMap()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse echoed price message")
            _errors.tryEmit(DataError.ParseFailed(e))
        }
    }
}