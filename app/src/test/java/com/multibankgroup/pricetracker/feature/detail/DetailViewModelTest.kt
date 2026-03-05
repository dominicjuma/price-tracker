package com.multibankgroup.pricetracker.feature.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.multibankgroup.pricetracker.data.model.StockData
import com.multibankgroup.pricetracker.data.repository.FakeStockPriceRepository
import com.multibankgroup.pricetracker.data.repository.StockMetadataRepositoryImpl
import com.multibankgroup.pricetracker.domain.ObserveStocksUseCase
import com.multibankgroup.pricetracker.domain.model.PriceDirection
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
class DetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeStockPriceRepository
    private lateinit var metadataRepository: StockMetadataRepositoryImpl
    private lateinit var useCase: ObserveStocksUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeStockPriceRepository()
        metadataRepository = StockMetadataRepositoryImpl()
        useCase = ObserveStocksUseCase(
            stockPriceRepository = fakeRepository,
            stockMetadataRepository = metadataRepository
        )
    }

    private fun createViewModel(symbol: String): DetailViewModel {
        return DetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("symbol" to symbol)),
            observeStocksUseCase = useCase,
            stockPriceRepository = fakeRepository
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `missing symbol in SavedStateHandle throws`() {
        DetailViewModel(
            savedStateHandle = SavedStateHandle(),
            observeStocksUseCase = useCase,
            stockPriceRepository = fakeRepository
        )
    }

    @Test
    fun `initial state has symbol from SavedStateHandle and isLoading true`() = runTest {
        val viewModel = createViewModel("AAPL")
        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals("AAPL", initial.symbol)
            assertTrue(initial.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isLoading becomes false when stock data arrives`() = runTest {
        fakeRepository.setPrices(mapOf("AAPL" to stockData("AAPL", current = 180.00)))
        val viewModel = createViewModel("AAPL")
        viewModel.uiState.test {
            assertFalse(awaitItem().isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isLoading stays true when symbol not in price map`() = runTest {
        fakeRepository.setPrices(mapOf("GOOG" to stockData("GOOG", current = 140.00)))
        val viewModel = createViewModel("AAPL")
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("AAPL", state.symbol)
            assertTrue(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `only shows data for the requested symbol`() = runTest {
        fakeRepository.setPrices(
            mapOf(
                "AAPL" to stockData("AAPL", current = 180.00),
                "GOOG" to stockData("GOOG", current = 140.00),
                "MSFT" to stockData("MSFT", current = 400.00)
            )
        )
        val viewModel = createViewModel("GOOG")
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("GOOG", state.symbol)
            assertEquals(140.00, state.currentPrice, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `maps all Stock fields to DetailUiState correctly`() = runTest {
        fakeRepository.setPrices(
            mapOf(
                "AAPL" to StockData(
                    symbol = "AAPL",
                    currentPrice = 200.00,
                    previousPrice = 178.50,
                    lastUpdatedTimestamp = 9000L
                )
            )
        )
        val viewModel = createViewModel("AAPL")
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("AAPL", state.symbol)
            assertEquals("Apple Inc.", state.companyName)
            assertEquals(200.00, state.currentPrice, 0.001)
            assertEquals(178.50, state.previousPrice, 0.001)
            assertEquals(PriceDirection.UP, state.priceDirection)
            assertEquals(9000L, state.lastUpdatedTimestamp)
            assertFalse(state.isLoading)
            assertTrue(state.description.isNotEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `description comes from metadata repository`() = runTest {
        fakeRepository.setPrices(mapOf("NVDA" to stockData("NVDA", current = 900.00)))
        val viewModel = createViewModel("NVDA")
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("NVIDIA Corporation", state.companyName)
            assertTrue(state.description.contains("GPU"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `price updates flow through in real time`() = runTest {
        fakeRepository.setPrices(mapOf("AAPL" to stockData("AAPL", current = 180.00)))
        val viewModel = createViewModel("AAPL")
        viewModel.uiState.test {
            assertEquals(180.00, awaitItem().currentPrice, 0.001)

            fakeRepository.setPrices(
                mapOf("AAPL" to stockData("AAPL", current = 195.00, previous = 180.00))
            )
            val updated = awaitItem()
            assertEquals(195.00, updated.currentPrice, 0.001)
            assertEquals(180.00, updated.previousPrice, 0.001)
            assertEquals(PriceDirection.UP, updated.priceDirection)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `direction changes when price drops`() = runTest {
        fakeRepository.setPrices(mapOf("AAPL" to stockData("AAPL", current = 180.00)))
        val viewModel = createViewModel("AAPL")
        viewModel.uiState.test {
            awaitItem()
            fakeRepository.setPrices(
                mapOf("AAPL" to stockData("AAPL", current = 170.00, previous = 180.00))
            )
            assertEquals(PriceDirection.DOWN, awaitItem().priceDirection)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `changes to other symbols dont emit if target symbol unchanged`() = runTest {
        fakeRepository.setPrices(
            mapOf(
                "AAPL" to stockData("AAPL", current = 180.00),
                "GOOG" to stockData("GOOG", current = 140.00)
            )
        )
        val viewModel = createViewModel("AAPL")
        viewModel.uiState.test {
            awaitItem()
            fakeRepository.setPrices(
                mapOf(
                    "AAPL" to stockData("AAPL", current = 180.00),
                    "GOOG" to stockData("GOOG", current = 999.00)
                )
            )
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun stockData(
        symbol: String,
        current: Double,
        previous: Double = current,
        timestamp: Long = 1L
    ) = StockData(
        symbol = symbol,
        currentPrice = current,
        previousPrice = previous,
        lastUpdatedTimestamp = timestamp
    )
}