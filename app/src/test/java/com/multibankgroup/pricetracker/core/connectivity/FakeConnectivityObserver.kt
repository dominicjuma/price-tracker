package com.multibankgroup.pricetracker.core.connectivity

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake [ConnectivityObserver] for unit tests.
 * Tests control connectivity via [setOnline].
 */
class FakeConnectivityObserver(initialOnline: Boolean = true) : ConnectivityObserver {

    private val _isOnline = MutableStateFlow(initialOnline)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    fun setOnline(online: Boolean) {
        _isOnline.value = online
    }
}