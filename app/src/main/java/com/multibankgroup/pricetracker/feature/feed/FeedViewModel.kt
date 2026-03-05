package com.multibankgroup.pricetracker.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multibankgroup.pricetracker.data.model.DataError
import com.multibankgroup.pricetracker.data.repository.StockPriceRepository
import com.multibankgroup.pricetracker.data.websocket.ConnectionStatus
import com.multibankgroup.pricetracker.domain.ObserveStocksUseCase
import com.multibankgroup.pricetracker.domain.model.Stock
import com.multibankgroup.pricetracker.feature.shared_ui.model.StockDisplayItem
import com.multibankgroup.pricetracker.feature.shared_ui.model.UiError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Feed screen state holder. MVI-lite: single [FeedUiState] + function-based user actions. */
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val observeStocksUseCase: ObserveStocksUseCase,
    private val stockPriceRepository: StockPriceRepository
) : ViewModel() {

    /** Holds the current error to display. Cleared by [onDismissError]. */
    private val _currentError = MutableStateFlow<UiError?>(null)

    init {
        stockPriceRepository.connect()
        collectErrors()
    }

    /** Combines stocks, connection, feed, connectivity, and errors into single state. */
    val uiState: StateFlow<FeedUiState> = combine(
        observeStocksUseCase(),
        stockPriceRepository.connectionStatus,
        stockPriceRepository.isFeedActive,
        stockPriceRepository.isOnline,
        _currentError
    ) { stocks, connectionStatus, isFeedActive, isOnline, error ->

        FeedUiState(
            stocks = stocks
                .sortedByDescending { it.currentPrice }
                .map { it.toDisplayItem() },
            isConnected = connectionStatus == ConnectionStatus.CONNECTED,
            isFeedActive = isFeedActive,
            isOnline = isOnline,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FeedUiState()
    )

    // ──────────────────────────────────────────────────────────────────────
    // User actions
    // ──────────────────────────────────────────────────────────────────────

    fun onToggleFeed() {
        stockPriceRepository.toggleFeed()
    }

    /** Called when the user dismisses the error (e.g., Snackbar dismissed). */
    fun onDismissError() {
        _currentError.update { null }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Error collection
    // ──────────────────────────────────────────────────────────────────────

    /** Collects one-shot errors from repository and maps to [UiError]. */
    private fun collectErrors() {
        viewModelScope.launch {
            stockPriceRepository.errors.collect { dataError ->
                _currentError.update { dataError.toUiError() }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Mapping
    // ──────────────────────────────────────────────────────────────────────

    private fun DataError.toUiError(): UiError = when (this) {
        is DataError.ConnectionFailed -> UiError.CONNECTION_FAILED
        is DataError.ConnectionLost -> UiError.CONNECTION_LOST
        is DataError.ParseFailed -> UiError.PARSE_FAILED
        is DataError.SerializationFailed -> UiError.PARSE_FAILED
        DataError.NoInternet -> UiError.NO_INTERNET
    }

    private fun Stock.toDisplayItem(): StockDisplayItem {
        return StockDisplayItem(
            symbol = symbol,
            companyName = companyName,
            currentPrice = currentPrice,
            previousPrice = previousPrice,
            priceDirection = priceDirection,
            lastUpdatedTimestamp = lastUpdatedTimestamp
        )
    }
}