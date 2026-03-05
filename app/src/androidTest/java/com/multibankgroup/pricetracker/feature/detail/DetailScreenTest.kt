package com.multibankgroup.pricetracker.feature.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.multibankgroup.pricetracker.domain.model.PriceDirection
import com.multibankgroup.pricetracker.feature.shared_ui.theme.PriceTrackerTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for the Detail screen.
 */
class DetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun displaysSymbolInTopBar() {
        composeRule.setContent {
            PriceTrackerTheme {
                DetailContent(
                    uiState = detailUiState(symbol = "AAPL", companyName = "Apple Inc."),
                    onNavigateBack = {}
                )
            }
        }
        composeRule.onNodeWithText("AAPL").assertIsDisplayed()
    }

    @Test
    fun displaysCompanyNameInTopBar() {
        composeRule.setContent {
            PriceTrackerTheme {
                DetailContent(
                    uiState = detailUiState(symbol = "AAPL", companyName = "Apple Inc."),
                    onNavigateBack = {}
                )
            }
        }
        composeRule.onNodeWithText("Apple Inc.").assertIsDisplayed()
    }

    @Test
    fun hidesCompanyNameWhenEmpty() {
        composeRule.setContent {
            PriceTrackerTheme {
                DetailContent(
                    uiState = detailUiState(symbol = "AAPL", companyName = ""),
                    onNavigateBack = {}
                )
            }
        }
        composeRule.onNodeWithText("AAPL").assertIsDisplayed()
        composeRule.onNodeWithText("Apple Inc.").assertDoesNotExist()
    }

    @Test
    fun backButtonCallsOnNavigateBack() {
        var backClicked = false
        composeRule.setContent {
            PriceTrackerTheme {
                DetailContent(
                    uiState = detailUiState(symbol = "AAPL"),
                    onNavigateBack = { backClicked = true }
                )
            }
        }
        composeRule.onNodeWithContentDescription("Back").performClick()
        assertTrue(backClicked)
    }

    @Test
    fun displaysCurrentPriceSectionLabel() {
        composeRule.setContent {
            PriceTrackerTheme {
                DetailContent(uiState = detailUiState(currentPrice = 200.00), onNavigateBack = {})
            }
        }
        composeRule.onNodeWithText("CURRENT PRICE").assertIsDisplayed()
    }

    @Test
    fun displaysFormattedPrice() {
        composeRule.setContent {
            PriceTrackerTheme {
                DetailContent(uiState = detailUiState(currentPrice = 178.50), onNavigateBack = {})
            }
        }
        composeRule.onNodeWithText("$178.50").assertIsDisplayed()
    }

    @Test
    fun displaysPriceContext() {
        composeRule.setContent {
            PriceTrackerTheme {
                DetailContent(uiState = detailUiState(), onNavigateBack = {})
            }
        }
        composeRule.onNodeWithText("vs previous tick").assertIsDisplayed()
    }

    @Test
    fun displaysAboutSectionWithDescription() {
        val description = "Designs, manufactures, and markets smartphones."
        composeRule.setContent {
            PriceTrackerTheme {
                DetailContent(uiState = detailUiState(description = description), onNavigateBack = {})
            }
        }
        composeRule.onNodeWithText("ABOUT").assertIsDisplayed()
        composeRule.onNodeWithText(description).assertIsDisplayed()
    }

    @Test
    fun hidesAboutCardWhenDescriptionEmpty() {
        composeRule.setContent {
            PriceTrackerTheme {
                DetailContent(uiState = detailUiState(description = ""), onNavigateBack = {})
            }
        }
        composeRule.onNodeWithText("ABOUT").assertDoesNotExist()
    }

    @Test
    fun displaysDeepLinkUri() {
        composeRule.setContent {
            PriceTrackerTheme {
                DetailContent(uiState = detailUiState(symbol = "NVDA"), onNavigateBack = {})
            }
        }
        composeRule.onNodeWithText("stocks://symbol/NVDA").assertIsDisplayed()
    }

    @Test
    fun tappingDeepLinkShowsCopiedFeedback() {
        composeRule.setContent {
            PriceTrackerTheme {
                DetailContent(uiState = detailUiState(symbol = "AAPL"), onNavigateBack = {})
            }
        }
        composeRule.onNodeWithText("stocks://symbol/AAPL").performClick()
        composeRule.onNodeWithText("Copied to clipboard").assertIsDisplayed()
    }

    private fun detailUiState(
        symbol: String = "AAPL",
        companyName: String = "Apple Inc.",
        description: String = "A technology company.",
        currentPrice: Double = 180.00,
        previousPrice: Double = 178.50,
        direction: PriceDirection = PriceDirection.UP,
        timestamp: Long = 1L,
        isLoading: Boolean = false
    ) = DetailUiState(
        symbol = symbol,
        companyName = companyName,
        description = description,
        currentPrice = currentPrice,
        previousPrice = previousPrice,
        priceDirection = direction,
        lastUpdatedTimestamp = timestamp,
        isLoading = isLoading
    )
}