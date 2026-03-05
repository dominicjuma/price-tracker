package com.multibankgroup.pricetracker.data.repository

import com.multibankgroup.pricetracker.core.connectivity.FakeConnectivityObserver
import com.multibankgroup.pricetracker.core.util.FakeClock
import com.multibankgroup.pricetracker.data.model.NetworkStockPriceMessage
import com.multibankgroup.pricetracker.data.model.NetworkSymbolPrice
import com.multibankgroup.pricetracker.data.websocket.ConnectionStatus
import com.multibankgroup.pricetracker.data.websocket.FakeStockPriceDataSource
import com.multibankgroup.pricetracker.data.websocket.WebSocketEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [StockPriceRepository].
 *
 * Uses [FakeStockPriceDataSource] (preferred over mocks per Android testing docs)
 * and [TestScope] to control time (the repository uses 2-second ticker delays).
 *
 * The real [StockMetadataRepository] and [Json] are used because they have
 * no external dependencies — they're just static data and a lightweight serializer.
 *
 * ## What's tested
 * 1. Initial prices are seeded on connect
 * 2. Prices update ONLY on echo receipt (server-authoritative)
 * 3. previousPrice tracks correctly for direction computation
 * 4. toggleFeed pauses/resumes the ticker
 * 5. Connection status reflects WebSocket events
 * 6. connect() is idempotent
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StockPriceRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fakeDataSource: FakeStockPriceDataSource
    private lateinit var fakeConnectivity: FakeConnectivityObserver
    private lateinit var metadataRepository: StockMetadataRepositoryImpl
    private lateinit var fakeClock: FakeClock
    private lateinit var json: Json
    private lateinit var repository: StockPriceRepositoryImpl

    @Before
    fun setUp() {
        fakeDataSource = FakeStockPriceDataSource()
        fakeConnectivity = FakeConnectivityObserver(initialOnline = true)
        metadataRepository = StockMetadataRepositoryImpl()
        fakeClock = FakeClock(currentTime = 1_000L)
        json = Json { ignoreUnknownKeys = true; isLenient = true }

        repository = StockPriceRepositoryImpl(
            dataSource = fakeDataSource,
            metadataRepository = metadataRepository,
            connectivityObserver = fakeConnectivity,
            externalScope = testScope.backgroundScope,
            ioDispatcher = testDispatcher,
            json = json,
            clock = fakeClock
        )
    }

    @After
    fun tearDown() {
        repository.disconnect()
        fakeDataSource.close()
    }

    // ──────────────────────────────────────────────────────────────────
    // 1. Seeding
    // ──────────────────────────────────────────────────────────────────

    @Test
    fun `connect seeds initial prices for all 25 symbols`() = testScope.runTest {
        repository.connect()

        val prices = repository.stockPrices.value
        val allSymbols = metadataRepository.getAllSymbols()

        assertEquals(25, prices.size)
        allSymbols.forEach { symbol ->
            assertTrue("Missing symbol: $symbol", prices.containsKey(symbol))
        }
    }

    @Test
    fun `seeded prices match metadata initial prices`() = testScope.runTest {
        repository.connect()

        val prices = repository.stockPrices.value
        metadataRepository.getAllStockInfo().forEach { info ->
            val stockData = prices[info.symbol]!!
            assertEquals(info.initialPrice, stockData.currentPrice, 0.001)
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // 2. Echo-only updates (server-authoritative)
    // ──────────────────────────────────────────────────────────────────

    @Test
    fun `prices do NOT update on send, only on echo receipt`() = testScope.runTest {
        repository.connect()
        fakeDataSource.simulateConnected()

        val pricesBeforeTick = repository.stockPrices.value.toMap()

        // Advance past one tick — repository sends a message but no echo yet
        advanceTimeBy(2_500)

        assertTrue("Repository should have sent a message", fakeDataSource.sentMessages.isNotEmpty())

        // Prices should still be the seeded values (no echo received)
        val pricesAfterSend = repository.stockPrices.value
        pricesBeforeTick.forEach { (symbol, before) ->
            assertEquals(
                "Price for $symbol should not change without echo",
                before.currentPrice,
                pricesAfterSend[symbol]!!.currentPrice,
                0.001
            )
        }
    }

    @Test
    fun `prices update when echo is received`() = testScope.runTest {
        repository.connect()
        fakeDataSource.simulateConnected()

        // Advance past one tick so the repository sends a price update
        advanceTimeBy(2_500)

        // Simulate the server echoing the message back
        fakeDataSource.echoLastSentMessage()

        // At least some prices should have changed from seeded values
        val updatedPrices = repository.stockPrices.value
        val initialPrices = metadataRepository.getAllStockInfo().associate {
            it.symbol to it.initialPrice
        }

        val anyChanged = updatedPrices.any { (symbol, data) ->
            data.currentPrice != initialPrices[symbol]
        }
        assertTrue("At least one price should differ after echo", anyChanged)
    }

    @Test
    fun `echo with known prices updates correctly`() = testScope.runTest {
        repository.connect()
        fakeDataSource.simulateConnected()

        // Manually craft an echo message with known prices
        val testMessage = NetworkStockPriceMessage(
            prices = listOf(
                NetworkSymbolPrice("AAPL", 200.00, fakeClock.now()),
                NetworkSymbolPrice("GOOG", 150.00, fakeClock.now())
            )
        )
        fakeDataSource.emit(
            WebSocketEvent.MessageReceived(
                json.encodeToString(testMessage)
            )
        )

        val prices = repository.stockPrices.value
        assertEquals(200.00, prices["AAPL"]!!.currentPrice, 0.001)
        assertEquals(150.00, prices["GOOG"]!!.currentPrice, 0.001)
    }

    // ──────────────────────────────────────────────────────────────────
    // 3. previousPrice tracking
    // ──────────────────────────────────────────────────────────────────

    @Test
    fun `previousPrice tracks the prior current price after echo`() = testScope.runTest {
        repository.connect()
        fakeDataSource.simulateConnected()

        val now = fakeClock.now()

        // First echo: AAPL = 200.00
        fakeDataSource.emit(
            WebSocketEvent.MessageReceived(
                json.encodeToString(
                    NetworkStockPriceMessage(listOf(NetworkSymbolPrice("AAPL", 200.00, now)))
                )
            )
        )

        assertEquals(200.00, repository.stockPrices.value["AAPL"]!!.currentPrice, 0.001)

        // Second echo: AAPL = 210.00
        fakeDataSource.emit(
            WebSocketEvent.MessageReceived(
                json.encodeToString(
                    NetworkStockPriceMessage(listOf(NetworkSymbolPrice("AAPL", 210.00, now + 2000)))
                )
            )
        )

        val aapl = repository.stockPrices.value["AAPL"]!!
        assertEquals(210.00, aapl.currentPrice, 0.001)
        assertEquals(200.00, aapl.previousPrice, 0.001)
    }

    // ──────────────────────────────────────────────────────────────────
    // 4. Feed toggle
    // ──────────────────────────────────────────────────────────────────

    @Test
    fun `toggleFeed pauses the ticker - no new messages sent`() = testScope.runTest {
        repository.connect()
        fakeDataSource.simulateConnected()

        // Let one tick fire
        advanceTimeBy(2_500)
        val messagesAfterFirstTick = fakeDataSource.sentMessages.size

        // Pause the feed
        repository.toggleFeed()
        assertEquals(false, repository.isFeedActive.value)

        // Clear sent messages and advance time — no new messages should appear
        fakeDataSource.sentMessages.clear()
        advanceTimeBy(5_000)

        assertEquals(
            "No messages should be sent while feed is paused",
            0,
            fakeDataSource.sentMessages.size
        )
    }

    @Test
    fun `toggleFeed resumes the ticker - messages sent again`() = testScope.runTest {
        repository.connect()
        fakeDataSource.simulateConnected()

        // Pause
        repository.toggleFeed()
        advanceTimeBy(5_000)

        // Resume
        repository.toggleFeed()
        assertEquals(true, repository.isFeedActive.value)

        fakeDataSource.sentMessages.clear()
        advanceTimeBy(2_500)

        assertTrue(
            "Messages should be sent after resuming feed",
            fakeDataSource.sentMessages.isNotEmpty()
        )
    }

    // ──────────────────────────────────────────────────────────────────
    // 5. Connection status
    // ──────────────────────────────────────────────────────────────────

    @Test
    fun `connection status is CONNECTING before WebSocket opens`() = testScope.runTest {
        repository.connect()

        // The repository calls connectWithRetry which sets CONNECTING before
        // the fake emits any event
        assertEquals(ConnectionStatus.CONNECTING, repository.connectionStatus.value)
    }

    @Test
    fun `connection status is CONNECTED after WebSocket opens`() = testScope.runTest {
        repository.connect()
        fakeDataSource.simulateConnected()

        assertEquals(ConnectionStatus.CONNECTED, repository.connectionStatus.value)
    }

    @Test
    fun `connection status is DISCONNECTED after error`() = testScope.runTest {
        repository.connect()
        fakeDataSource.simulateConnected()
        fakeDataSource.simulateError()

        assertEquals(ConnectionStatus.DISCONNECTED, repository.connectionStatus.value)
    }

    // ──────────────────────────────────────────────────────────────────
    // 6. Idempotent connect
    // ──────────────────────────────────────────────────────────────────

    @Test
    fun `calling connect twice does not create duplicate connections`() = testScope.runTest {
        repository.connect()
        repository.connect() // second call should be no-op

        val prices = repository.stockPrices.value
        assertEquals(25, prices.size) // still 25, not 50
    }

    // ──────────────────────────────────────────────────────────────────
    // 7. Sent message format
    // ──────────────────────────────────────────────────────────────────

    @Test
    fun `sent messages are valid NetworkStockPriceMessage JSON with 25 symbols`() = testScope.runTest {
        repository.connect()
        fakeDataSource.simulateConnected()

        advanceTimeBy(2_500)

        assertTrue(fakeDataSource.sentMessages.isNotEmpty())

        val message = json.decodeFromString<NetworkStockPriceMessage>(fakeDataSource.sentMessages.first())
        assertEquals(25, message.prices.size)

        val symbols = message.prices.map { it.symbol }.toSet()
        metadataRepository.getAllSymbols().forEach { symbol ->
            assertTrue("Sent message missing symbol: $symbol", symbols.contains(symbol))
        }
    }
}