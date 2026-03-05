package com.multibankgroup.pricetracker.feature.feed

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.multibankgroup.pricetracker.domain.model.PriceDirection
import com.multibankgroup.pricetracker.feature.shared_ui.model.StockDisplayItem
import com.multibankgroup.pricetracker.feature.shared_ui.model.UiError
import com.multibankgroup.pricetracker.feature.shared_ui.theme.PriceTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Compose UI tests for the Feed screen. */
class FeedScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ── Stock rows ───────────────────────────────────────────────────

    @Test
    fun displaysStockSymbolsAndCompanyNames() {
        setFeedContent(
            feedUiState(
                stocks = listOf(
                    displayItem("AAPL", "Apple Inc.", currentPrice = 180.00),
                    displayItem("GOOG", "Alphabet Inc.", currentPrice = 140.00)
                )
            )
        )
        composeRule.onNodeWithText("AAPL").assertIsDisplayed()
        composeRule.onNodeWithText("Apple Inc.").assertIsDisplayed()
        composeRule.onNodeWithText("GOOG").assertIsDisplayed()
        composeRule.onNodeWithText("Alphabet Inc.").assertIsDisplayed()
    }

    @Test
    fun displaysFormattedPrices() {
        setFeedContent(
            feedUiState(stocks = listOf(displayItem("AAPL", "Apple Inc.", currentPrice = 178.50)))
        )
        composeRule.onNodeWithText("$178.50").assertIsDisplayed()
    }

    @Test
    fun emptyStateShowsNoStockRows() {
        setFeedContent(feedUiState())
        composeRule.onNodeWithText("AAPL").assertDoesNotExist()
    }

    @Test
    fun rowsDisplayedInGivenOrder() {
        setFeedContent(
            feedUiState(
                stocks = listOf(
                    displayItem("NVDA", "NVIDIA Corporation", currentPrice = 900.00),
                    displayItem("AAPL", "Apple Inc.", currentPrice = 180.00),
                    displayItem("BAC", "Bank of America Corporation", currentPrice = 35.00)
                )
            )
        )
        composeRule.onNodeWithText("NVDA").assertIsDisplayed()
        composeRule.onNodeWithText("AAPL").assertIsDisplayed()
        composeRule.onNodeWithText("BAC").assertIsDisplayed()
    }

    // ── Connection indicator ─────────────────────────────────────────

    @Test
    fun showsLiveWhenConnected() {
        setFeedContent(feedUiState(isConnected = true))
        composeRule.onNodeWithText("Live").assertIsDisplayed()
    }

    @Test
    fun showsOfflineWhenDisconnected() {
        setFeedContent(feedUiState(isConnected = false))
        composeRule.onNodeWithText("Offline").assertIsDisplayed()
    }

    // ── Feed toggle ──────────────────────────────────────────────────

    @Test
    fun feedLabelIsDisplayed() {
        setFeedContent(feedUiState())
        composeRule.onNodeWithText("Feed").assertIsDisplayed()
    }

    @Test
    fun toggleSwitchCallsOnToggleFeed() {
        var toggleCount = 0
        setFeedContent(
            uiState = feedUiState(isFeedActive = true),
            onToggleFeed = { toggleCount++ }
        )
        composeRule.onNode(isToggleable()).performClick()
        assertEquals(1, toggleCount)
    }

    // ── Row click → navigation ───────────────────────────────────────

    @Test
    fun clickingRowCallsOnStockClickWithSymbol() {
        var clickedSymbol = ""
        setFeedContent(
            uiState = feedUiState(
                stocks = listOf(
                    displayItem("TSLA", "Tesla, Inc.", currentPrice = 245.00),
                    displayItem("AAPL", "Apple Inc.", currentPrice = 180.00)
                )
            ),
            onStockClick = { clickedSymbol = it }
        )
        composeRule.onNodeWithText("TSLA").performClick()
        assertEquals("TSLA", clickedSymbol)
    }

    @Test
    fun clickingSecondRowNavigatesToCorrectSymbol() {
        var clickedSymbol = ""
        setFeedContent(
            uiState = feedUiState(
                stocks = listOf(
                    displayItem("AAPL", "Apple Inc.", currentPrice = 180.00),
                    displayItem("GOOG", "Alphabet Inc.", currentPrice = 140.00)
                )
            ),
            onStockClick = { clickedSymbol = it }
        )
        composeRule.onNodeWithText("GOOG").performClick()
        assertEquals("GOOG", clickedSymbol)
    }

    // ── Error Snackbar ───────────────────────────────────────────────

    @Test
    fun showsSnackbarWhenErrorPresent() {
        setFeedContent(feedUiState(error = UiError.CONNECTION_LOST))
        composeRule.onNodeWithText("Connection lost — reconnecting…").assertIsDisplayed()
    }

    @Test
    fun showsNoInternetSnackbar() {
        setFeedContent(feedUiState(error = UiError.NO_INTERNET))
        composeRule.onNodeWithText("No internet connection").assertIsDisplayed()
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private fun setFeedContent(
        uiState: FeedUiState,
        onToggleFeed: () -> Unit = {},
        onDismissError: () -> Unit = {},
        onStockClick: (String) -> Unit = {}
    ) {
        composeRule.setContent {
            PriceTrackerTheme {
                FeedContent(
                    uiState = uiState,
                    onToggleFeed = onToggleFeed,
                    onDismissError = onDismissError,
                    onStockClick = onStockClick
                )
            }
        }
    }

    private fun feedUiState(
        stocks: List<StockDisplayItem> = emptyList(),
        isConnected: Boolean = false,
        isFeedActive: Boolean = true,
        error: UiError? = null
    ) = FeedUiState(
        stocks = stocks,
        isConnected = isConnected,
        isFeedActive = isFeedActive,
        error = error
    )

    private fun displayItem(
        symbol: String,
        companyName: String,
        currentPrice: Double,
        previousPrice: Double = currentPrice,
        direction: PriceDirection = PriceDirection.NONE,
        timestamp: Long = 1L
    ) = StockDisplayItem(
        symbol = symbol,
        companyName = companyName,
        currentPrice = currentPrice,
        previousPrice = previousPrice,
        priceDirection = direction,
        lastUpdatedTimestamp = timestamp
    )
}