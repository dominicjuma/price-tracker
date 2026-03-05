package com.multibankgroup.pricetracker.feature.feed

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.multibankgroup.pricetracker.domain.model.PriceDirection
import com.multibankgroup.pricetracker.feature.shared_ui.model.StockDisplayItem
import com.multibankgroup.pricetracker.feature.shared_ui.theme.PriceTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for the Feed screen.
 */
class FeedScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun displaysStockSymbolsAndCompanyNames() {
        val state = feedUiState(
            stocks = listOf(
                displayItem("AAPL", "Apple Inc.", currentPrice = 180.00),
                displayItem("GOOG", "Alphabet Inc.", currentPrice = 140.00)
            )
        )
        composeRule.setContent {
            PriceTrackerTheme {
                FeedContent(uiState = state, onToggleFeed = {}, onStockClick = {})
            }
        }
        composeRule.onNodeWithText("AAPL").assertIsDisplayed()
        composeRule.onNodeWithText("Apple Inc.").assertIsDisplayed()
        composeRule.onNodeWithText("GOOG").assertIsDisplayed()
        composeRule.onNodeWithText("Alphabet Inc.").assertIsDisplayed()
    }

    @Test
    fun displaysFormattedPrices() {
        val state = feedUiState(
            stocks = listOf(displayItem("AAPL", "Apple Inc.", currentPrice = 178.50))
        )
        composeRule.setContent {
            PriceTrackerTheme {
                FeedContent(uiState = state, onToggleFeed = {}, onStockClick = {})
            }
        }
        composeRule.onNodeWithText("$178.50").assertIsDisplayed()
    }

    @Test
    fun emptyStateShowsNoStockRows() {
        composeRule.setContent {
            PriceTrackerTheme {
                FeedContent(uiState = feedUiState(), onToggleFeed = {}, onStockClick = {})
            }
        }
        composeRule.onNodeWithText("AAPL").assertDoesNotExist()
    }

    @Test
    fun rowsDisplayedInGivenOrder() {
        val state = feedUiState(
            stocks = listOf(
                displayItem("NVDA", "NVIDIA Corporation", currentPrice = 900.00),
                displayItem("AAPL", "Apple Inc.", currentPrice = 180.00),
                displayItem("BAC", "Bank of America Corporation", currentPrice = 35.00)
            )
        )
        composeRule.setContent {
            PriceTrackerTheme {
                FeedContent(uiState = state, onToggleFeed = {}, onStockClick = {})
            }
        }
        composeRule.onNodeWithText("NVDA").assertIsDisplayed()
        composeRule.onNodeWithText("AAPL").assertIsDisplayed()
        composeRule.onNodeWithText("BAC").assertIsDisplayed()
    }

    @Test
    fun showsLiveWhenConnected() {
        composeRule.setContent {
            PriceTrackerTheme {
                FeedContent(uiState = feedUiState(isConnected = true), onToggleFeed = {}, onStockClick = {})
            }
        }
        composeRule.onNodeWithText("Live").assertIsDisplayed()
    }

    @Test
    fun showsOfflineWhenDisconnected() {
        composeRule.setContent {
            PriceTrackerTheme {
                FeedContent(uiState = feedUiState(isConnected = false), onToggleFeed = {}, onStockClick = {})
            }
        }
        composeRule.onNodeWithText("Offline").assertIsDisplayed()
    }

    @Test
    fun feedLabelIsDisplayed() {
        composeRule.setContent {
            PriceTrackerTheme {
                FeedContent(uiState = feedUiState(), onToggleFeed = {}, onStockClick = {})
            }
        }
        composeRule.onNodeWithText("Feed").assertIsDisplayed()
    }

    @Test
    fun toggleSwitchCallsOnToggleFeed() {
        var toggleCount = 0
        composeRule.setContent {
            PriceTrackerTheme {
                FeedContent(uiState = feedUiState(isFeedActive = true), onToggleFeed = { toggleCount++ }, onStockClick = {})
            }
        }
        composeRule.onNode(isToggleable()).performClick()
        assertEquals(1, toggleCount)
    }

    @Test
    fun clickingRowCallsOnStockClickWithSymbol() {
        var clickedSymbol = ""
        val state = feedUiState(
            stocks = listOf(
                displayItem("TSLA", "Tesla, Inc.", currentPrice = 245.00),
                displayItem("AAPL", "Apple Inc.", currentPrice = 180.00)
            )
        )
        composeRule.setContent {
            PriceTrackerTheme {
                FeedContent(uiState = state, onToggleFeed = {}, onStockClick = { clickedSymbol = it })
            }
        }
        composeRule.onNodeWithText("TSLA").performClick()
        assertEquals("TSLA", clickedSymbol)
    }

    @Test
    fun clickingSecondRowNavigatesToCorrectSymbol() {
        var clickedSymbol = ""
        val state = feedUiState(
            stocks = listOf(
                displayItem("AAPL", "Apple Inc.", currentPrice = 180.00),
                displayItem("GOOG", "Alphabet Inc.", currentPrice = 140.00)
            )
        )
        composeRule.setContent {
            PriceTrackerTheme {
                FeedContent(uiState = state, onToggleFeed = {}, onStockClick = { clickedSymbol = it })
            }
        }
        composeRule.onNodeWithText("GOOG").performClick()
        assertEquals("GOOG", clickedSymbol)
    }

    private fun feedUiState(
        stocks: List<StockDisplayItem> = emptyList(),
        isConnected: Boolean = false,
        isFeedActive: Boolean = true
    ) = FeedUiState(
        stocks = stocks,
        isConnected = isConnected,
        isFeedActive = isFeedActive
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