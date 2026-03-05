package com.multibankgroup.pricetracker.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multibankgroup.pricetracker.domain.ObserveStocksUseCase
import com.multibankgroup.pricetracker.domain.model.Stock
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Detail screen state holder. Filters shared stock stream to one symbol via [SavedStateHandle].
 * No duplicate WebSocket connection — observes the same [ObserveStocksUseCase] as feed.
 */
@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeStocksUseCase: ObserveStocksUseCase
) : ViewModel() {

    private val symbol: String = requireNotNull(savedStateHandle["symbol"]) {
        "Symbol argument is required for DetailViewModel"
    }

    /** Filters all stocks to [symbol], maps to [DetailUiState]. Loading state if not found yet. */
    val uiState: StateFlow<DetailUiState> = observeStocksUseCase()
        .map { stocks ->
            val stock = stocks.firstOrNull { it.symbol == symbol }
            stock?.toDetailUiState() ?: DetailUiState(
                symbol = symbol,
                isLoading = true
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DetailUiState(symbol = symbol)
        )

    private fun Stock.toDetailUiState(): DetailUiState {
        return DetailUiState(
            symbol = symbol,
            companyName = companyName,
            description = description,
            currentPrice = currentPrice,
            previousPrice = previousPrice,
            priceDirection = priceDirection,
            lastUpdatedTimestamp = lastUpdatedTimestamp,
            isLoading = false
        )
    }
}