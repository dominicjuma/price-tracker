package com.multibankgroup.pricetracker.domain

import app.cash.turbine.test
import com.multibankgroup.pricetracker.data.model.StockData
import com.multibankgroup.pricetracker.data.repository.FakeStockPriceRepository
import com.multibankgroup.pricetracker.data.repository.StockMetadataRepositoryImpl
import com.multibankgroup.pricetracker.domain.model.PriceDirection
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ObserveStocksUseCase].
 *
 * Uses [Turbine] for sequential emission testing and [FakeStockPriceRepository]
 * for controlled state. The real [StockMetadataRepository] is used (static data).
 *
 * ## What's tested
 * 1. Combining prices with metadata produces complete Stock objects
 * 2. PriceDirection computation (UP/DOWN/NONE)
 * 3. Empty state handling
 * 4. Reactive updates — new prices produce new emissions
 */
class ObserveStocksUseCaseTest {

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

    // ──────────────────────────────────────────────────────────────────
    // Empty state
    // ──────────────────────────────────────────────────────────────────

    @Test
    fun `emits empty list when no prices available`() = runTest {
        useCase().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // Combining prices with metadata
    // ──────────────────────────────────────────────────────────────────

    @Test
    fun `combines price data with metadata into Stock objects`() = runTest {
        fakeRepository.setPrices(
            mapOf("AAPL" to stockData("AAPL", current = 180.00, previous = 178.50))
        )

        useCase().test {
            val stocks = awaitItem()
            assertEquals(1, stocks.size)

            val aapl = stocks.first()
            assertEquals("AAPL", aapl.symbol)
            assertEquals("Apple Inc.", aapl.companyName)
            assertEquals(180.00, aapl.currentPrice, 0.001)
            assertEquals(178.50, aapl.previousPrice, 0.001)
            assertTrue(aapl.description.isNotEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `each stock gets correct metadata from repository`() = runTest {
        fakeRepository.setPrices(
            mapOf(
                "AAPL" to stockData("AAPL", current = 180.00),
                "NVDA" to stockData("NVDA", current = 900.00)
            )
        )

        useCase().test {
            val stockMap = awaitItem().associateBy { it.symbol }
            assertEquals("Apple Inc.", stockMap["AAPL"]!!.companyName)
            assertEquals("NVIDIA Corporation", stockMap["NVDA"]!!.companyName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // Price direction computation
    // ──────────────────────────────────────────────────────────────────

    @Test
    fun `direction is UP when current greater than previous`() = runTest {
        fakeRepository.setPrices(
            mapOf("AAPL" to stockData("AAPL", current = 200.00, previous = 180.00))
        )

        useCase().test {
            assertEquals(PriceDirection.UP, awaitItem().first().priceDirection)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `direction is DOWN when current less than previous`() = runTest {
        fakeRepository.setPrices(
            mapOf("AAPL" to stockData("AAPL", current = 170.00, previous = 180.00))
        )

        useCase().test {
            assertEquals(PriceDirection.DOWN, awaitItem().first().priceDirection)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `direction is NONE when current equals previous`() = runTest {
        fakeRepository.setPrices(
            mapOf("AAPL" to stockData("AAPL", current = 180.00, previous = 180.00))
        )

        useCase().test {
            assertEquals(PriceDirection.NONE, awaitItem().first().priceDirection)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `handles multiple symbols with different directions`() = runTest {
        fakeRepository.setPrices(
            mapOf(
                "AAPL" to stockData("AAPL", current = 200.00, previous = 180.00),
                "GOOG" to stockData("GOOG", current = 130.00, previous = 141.80),
                "MSFT" to stockData("MSFT", current = 415.30, previous = 415.30)
            )
        )

        useCase().test {
            val directions = awaitItem().associate { it.symbol to it.priceDirection }
            assertEquals(PriceDirection.UP, directions["AAPL"])
            assertEquals(PriceDirection.DOWN, directions["GOOG"])
            assertEquals(PriceDirection.NONE, directions["MSFT"])
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // Reactive updates
    // ──────────────────────────────────────────────────────────────────

    @Test
    fun `emits new list when prices change`() = runTest {
        useCase().test {
            // Initial empty
            assertTrue(awaitItem().isEmpty())

            // First price update
            fakeRepository.setPrices(
                mapOf("AAPL" to stockData("AAPL", current = 180.00))
            )
            val first = awaitItem()
            assertEquals(1, first.size)
            assertEquals(180.00, first.first().currentPrice, 0.001)

            // Second price update
            fakeRepository.setPrices(
                mapOf("AAPL" to stockData("AAPL", current = 195.00, previous = 180.00))
            )
            val second = awaitItem()
            assertEquals(195.00, second.first().currentPrice, 0.001)
            assertEquals(PriceDirection.UP, second.first().priceDirection)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────────────────────────────

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