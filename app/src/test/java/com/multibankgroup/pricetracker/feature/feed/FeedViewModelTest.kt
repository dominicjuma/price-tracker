package com.multibankgroup.pricetracker.feature.feed

import app.cash.turbine.test
import com.multibankgroup.pricetracker.data.model.StockData
import com.multibankgroup.pricetracker.data.repository.FakeStockPriceRepository
import com.multibankgroup.pricetracker.data.repository.StockMetadataRepositoryImpl
import com.multibankgroup.pricetracker.data.websocket.ConnectionStatus
import com.multibankgroup.pricetracker.domain.ObserveStocksUseCase
import com.multibankgroup.pricetracker.domain.model.PriceDirection
import com.multibankgroup.pricetracker.feature.shared_ui.model.UiError
import com.multibankgroup.pricetracker.test.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeStockPriceRepository
    private lateinit var metadataRepository: StockMetadataRepositoryImpl
    private lateinit var useCase: ObserveStocksUseCase
    private lateinit var viewModel: FeedViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeStockPriceRepository()
        metadataRepository = StockMetadataRepositoryImpl()
        useCase = ObserveStocksUseCase(
            stockPriceRepository = fakeRepository,
            stockMetadataRepository = metadataRepository
        )
        viewModel = FeedViewModel(
            observeStocksUseCase = useCase,
            stockPriceRepository = fakeRepository
        )
    }

    @Test
    fun `connect is called on ViewModel creation`() {
        assertTrue(fakeRepository.connectCalled)
    }

    @Test
    fun `initial UI state has empty stocks, disconnected, feed active`() = runTest {
        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial.stocks.isEmpty())
            assertFalse(initial.isConnected)
            assertTrue(initial.isFeedActive)
            assertTrue(initial.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stocks are sorted by current price descending`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            fakeRepository.setPrices(
                mapOf(
                    "AAPL" to stockData("AAPL", currentPrice = 100.00),
                    "GOOG" to stockData("GOOG", currentPrice = 300.00),
                    "MSFT" to stockData("MSFT", currentPrice = 200.00)
                )
            )

            val state = awaitItem()
            assertEquals(3, state.stocks.size)
            assertEquals("GOOG", state.stocks[0].symbol)
            assertEquals("MSFT", state.stocks[1].symbol)
            assertEquals("AAPL", state.stocks[2].symbol)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sorting updates when prices change`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            fakeRepository.setPrices(
                mapOf(
                    "AAPL" to stockData("AAPL", currentPrice = 100.00),
                    "GOOG" to stockData("GOOG", currentPrice = 200.00)
                )
            )
            assertEquals("GOOG", awaitItem().stocks[0].symbol)

            fakeRepository.setPrices(
                mapOf(
                    "AAPL" to stockData("AAPL", currentPrice = 500.00),
                    "GOOG" to stockData("GOOG", currentPrice = 200.00)
                )
            )
            assertEquals("AAPL", awaitItem().stocks[0].symbol)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isConnected is true when status is CONNECTED`() = runTest {
        viewModel.uiState.test {
            skipItems(1)
            fakeRepository.setConnectionStatus(ConnectionStatus.CONNECTED)
            assertTrue(awaitItem().isConnected)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isConnected is false when status is DISCONNECTED`() = runTest {
        viewModel.uiState.test {
            skipItems(1)
            fakeRepository.setConnectionStatus(ConnectionStatus.CONNECTED)
            assertTrue(awaitItem().isConnected)
            fakeRepository.setConnectionStatus(ConnectionStatus.DISCONNECTED)
            assertFalse(awaitItem().isConnected)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isConnected is false when status is CONNECTING`() = runTest {
        viewModel.uiState.test {
            skipItems(1)
            fakeRepository.setConnectionStatus(ConnectionStatus.CONNECTED)
            assertTrue(awaitItem().isConnected)
            fakeRepository.setConnectionStatus(ConnectionStatus.CONNECTING)
            assertFalse(awaitItem().isConnected)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onToggleFeed flips isFeedActive through UDF cycle`() = runTest {
        viewModel.uiState.test {
            assertTrue(awaitItem().isFeedActive)
            viewModel.onToggleFeed()
            assertFalse(awaitItem().isFeedActive)
            viewModel.onToggleFeed()
            assertTrue(awaitItem().isFeedActive)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Stock maps to StockDisplayItem with correct fields`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            fakeRepository.setPrices(
                mapOf(
                    "AAPL" to StockData(
                        symbol = "AAPL",
                        currentPrice = 200.00,
                        previousPrice = 180.00,
                        lastUpdatedTimestamp = 5000L
                    )
                )
            )

            val item = awaitItem().stocks.first()
            assertEquals("AAPL", item.symbol)
            assertEquals("Apple Inc.", item.companyName)
            assertEquals(200.00, item.currentPrice, 0.001)
            assertEquals(180.00, item.previousPrice, 0.001)
            assertEquals(PriceDirection.UP, item.priceDirection)
            assertEquals(5000L, item.lastUpdatedTimestamp)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `direction is computed correctly in display items`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            fakeRepository.setPrices(
                mapOf(
                    "AAPL" to stockData("AAPL", currentPrice = 200.00, previousPrice = 180.00),
                    "GOOG" to stockData("GOOG", currentPrice = 130.00, previousPrice = 141.80),
                    "MSFT" to stockData("MSFT", currentPrice = 415.30, previousPrice = 415.30)
                )
            )

            val items = awaitItem().stocks.associateBy { it.symbol }
            assertEquals(PriceDirection.UP, items["AAPL"]!!.priceDirection)
            assertEquals(PriceDirection.DOWN, items["GOOG"]!!.priceDirection)
            assertEquals(PriceDirection.NONE, items["MSFT"]!!.priceDirection)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isLoading is false when stocks are available`() = runTest {
        viewModel.uiState.test {
            skipItems(1)
            fakeRepository.setPrices(
                mapOf("AAPL" to stockData("AAPL", currentPrice = 180.00))
            )
            assertFalse(awaitItem().isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onDismissError clears error from state`() = runTest {
        viewModel.uiState.test {
            skipItems(1)
            fakeRepository.emitError(com.multibankgroup.pricetracker.data.model.DataError.NoInternet)
            assertEquals(UiError.NO_INTERNET, awaitItem().error)
            viewModel.onDismissError()
            assertEquals(null, awaitItem().error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun stockData(
        symbol: String,
        currentPrice: Double,
        previousPrice: Double = currentPrice,
        timestamp: Long = 1L
    ) = StockData(
        symbol = symbol,
        currentPrice = currentPrice,
        previousPrice = previousPrice,
        lastUpdatedTimestamp = timestamp
    )
}