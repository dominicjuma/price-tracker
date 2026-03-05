package com.multibankgroup.pricetracker.feature.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.multibankgroup.pricetracker.domain.model.PriceDirection
import com.multibankgroup.pricetracker.feature.shared_ui.model.UiError
import com.multibankgroup.pricetracker.feature.shared_ui.theme.PriceTrackerTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Compose UI tests for the Detail screen. */
class DetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ── Top bar ──────────────────────────────────────────────────────

    @Test
    fun displaysSymbolInTopBar() {
        setDetailContent(detailUiState(symbol = "AAPL", companyName = "Apple Inc."))
        composeRule.onNodeWithText("AAPL").assertIsDisplayed()
    }

    @Test
    fun displaysCompanyNameInTopBar() {
        setDetailContent(detailUiState(symbol = "AAPL", companyName = "Apple Inc."))
        composeRule.onNodeWithText("Apple Inc.").assertIsDisplayed()
    }

    @Test
    fun hidesCompanyNameWhenEmpty() {
        setDetailContent(detailUiState(symbol = "AAPL", companyName = ""))
        composeRule.onNodeWithText("AAPL").assertIsDisplayed()
        composeRule.onNodeWithText("Apple Inc.").assertDoesNotExist()
    }

    @Test
    fun backButtonCallsOnNavigateBack() {
        var backClicked = false
        setDetailContent(
            uiState = detailUiState(symbol = "AAPL"),
            onNavigateBack = { backClicked = true }
        )
        composeRule.onNodeWithContentDescription("Back").performClick()
        assertTrue(backClicked)
    }

    // ── Price card ───────────────────────────────────────────────────

    @Test
    fun displaysCurrentPriceSectionLabel() {
        setDetailContent(detailUiState(currentPrice = 200.00))
        composeRule.onNodeWithText("CURRENT PRICE").assertIsDisplayed()
    }

    @Test
    fun displaysFormattedPrice() {
        setDetailContent(detailUiState(currentPrice = 178.50))
        composeRule.onNodeWithText("$178.50").assertIsDisplayed()
    }

    @Test
    fun displaysPriceContext() {
        setDetailContent(detailUiState())
        composeRule.onNodeWithText("vs previous tick").assertIsDisplayed()
    }

    // ── About card ───────────────────────────────────────────────────

    @Test
    fun displaysAboutSectionWithDescription() {
        val description = "Designs, manufactures, and markets smartphones."
        setDetailContent(detailUiState(description = description))
        composeRule.onNodeWithText("ABOUT").assertIsDisplayed()
        composeRule.onNodeWithText(description).assertIsDisplayed()
    }

    @Test
    fun hidesAboutCardWhenDescriptionEmpty() {
        setDetailContent(detailUiState(description = ""))
        composeRule.onNodeWithText("ABOUT").assertDoesNotExist()
    }

    // ── Deep link ────────────────────────────────────────────────────

    @Test
    fun displaysDeepLinkUri() {
        setDetailContent(detailUiState(symbol = "NVDA"))
        composeRule.onNodeWithText("stocks://symbol/NVDA").assertIsDisplayed()
    }

    @Test
    fun tappingDeepLinkShowsCopiedFeedback() {
        setDetailContent(detailUiState(symbol = "AAPL"))
        composeRule.onNodeWithText("stocks://symbol/AAPL").performClick()
        composeRule.onNodeWithText("Copied to clipboard").assertIsDisplayed()
    }

    // ── Error Snackbar ───────────────────────────────────────────────

    @Test
    fun showsSnackbarWhenErrorPresent() {
        setDetailContent(detailUiState(error = UiError.CONNECTION_LOST))
        composeRule.onNodeWithText("Connection lost — reconnecting…").assertIsDisplayed()
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private fun setDetailContent(
        uiState: DetailUiState,
        onNavigateBack: () -> Unit = {},
        onDismissError: () -> Unit = {}
    ) {
        composeRule.setContent {
            PriceTrackerTheme {
                DetailContent(
                    uiState = uiState,
                    onNavigateBack = onNavigateBack,
                    onDismissError = onDismissError
                )
            }
        }
    }

    private fun detailUiState(
        symbol: String = "AAPL",
        companyName: String = "Apple Inc.",
        description: String = "A technology company.",
        currentPrice: Double = 180.00,
        previousPrice: Double = 178.50,
        direction: PriceDirection = PriceDirection.UP,
        timestamp: Long = 1L,
        isLoading: Boolean = false,
        error: UiError? = null
    ) = DetailUiState(
        symbol = symbol,
        companyName = companyName,
        description = description,
        currentPrice = currentPrice,
        previousPrice = previousPrice,
        priceDirection = direction,
        lastUpdatedTimestamp = timestamp,
        isLoading = isLoading,
        error = error
    )
}