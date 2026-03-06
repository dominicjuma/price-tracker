package com.multibankgroup.pricetracker.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multibankgroup.pricetracker.data.repository.StockPriceRepository
import com.multibankgroup.pricetracker.domain.ObserveStocksUseCase
import com.multibankgroup.pricetracker.domain.model.Stock
import com.multibankgroup.pricetracker.feature.shared_ui.model.UiError
import com.multibankgroup.pricetracker.feature.shared_ui.model.toUiError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Detail screen state holder. Filters shared stock stream to one symbol via [SavedStateHandle].
 * No duplicate WebSocket connection — observes the same [ObserveStocksUseCase] as feed.
 */
@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeStocksUseCase: ObserveStocksUseCase,
    private val stockPriceRepository: StockPriceRepository
) : ViewModel() {

    private val symbol: String = requireNotNull(savedStateHandle["symbol"]) {
        "Symbol argument is required for DetailViewModel"
    }

    private val _currentError = MutableStateFlow<UiError?>(null)

    init {
        stockPriceRepository.connect() //Idempotent — no-op if already connected.
        collectErrors()
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

    private fun collectErrors() {
        viewModelScope.launch {
            stockPriceRepository.errors.collect { dataError ->
                _currentError.update { dataError.toUiError() }
            }
        }
    }

    fun onDismissError() {
        _currentError.update { null }
    }

    private fun Stock.toDetailUiState(error: UiError? = null): DetailUiState {
        return DetailUiState(
            symbol = symbol,
            companyName = companyName,
            description = description,
            currentPrice = currentPrice,
            previousPrice = previousPrice,
            priceDirection = priceDirection,
            lastUpdatedTimestamp = lastUpdatedTimestamp,
            isLoading = false,
            error = error
        )
    }
}