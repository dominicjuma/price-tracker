package com.multibankgroup.pricetracker.common.connectivity

import kotlinx.coroutines.flow.StateFlow

/**
 * Abstracts device connectivity so production code uses [ConnectivityManager]
 * and tests swap in a fake — same pattern as [StockPriceDataSource] and [Clock].
 */
interface ConnectivityObserver {

    /** Emits `true` when online, `false` when offline. [StateFlow.value] always holds current status. */
    val isOnline: StateFlow<Boolean>
}