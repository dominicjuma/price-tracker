package com.multibankgroup.pricetracker.core.connectivity

import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber

/**
 * Bridges [NetworkCallback] into a [StateFlow] via [callbackFlow].
 * [SharingStarted.Eagerly] ensures tracking starts at app launch, not on first collector.
 * [checkCurrentConnection] seeds the initial value before the first callback fires.
 * [SecurityException] is caught on Android 11 and below (issuetracker.google.com/175055271).
 */
class AndroidConnectivityObserver(
    private val connectivityManager: ConnectivityManager,
    ioDispatcher: CoroutineDispatcher
) : ConnectivityObserver {

    override val isOnline: StateFlow<Boolean> by lazy { createFlow(ioDispatcher) }

    private fun createFlow(ioDispatcher: CoroutineDispatcher): StateFlow<Boolean> =
        callbackFlow {
            val callback = object : NetworkCallback() {
                override fun onCapabilitiesChanged(
                    network: Network,
                    capabilities: NetworkCapabilities
                ) {
                    launch { send(capabilities.hasInternetConnection()) }
                }

                override fun onLost(network: Network) {
                    launch { send(false) }
                }
            }

            safelyRegisterCallback(callback)
            awaitClose { safelyUnregisterCallback(callback) }
        }.stateIn(
            scope = CoroutineScope(ioDispatcher),
            started = SharingStarted.Eagerly,
            initialValue = checkCurrentConnection()
        )

    private fun safelyRegisterCallback(callback: NetworkCallback) {
        try {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } catch (e: SecurityException) {
            Timber.e(e, "Failed to register network callback")
        }
    }

    private fun safelyUnregisterCallback(callback: NetworkCallback) {
        try {
            connectivityManager.unregisterNetworkCallback(callback)
        } catch (e: SecurityException) {
            Timber.e(e, "Failed to unregister network callback")
        }
    }

    /**
     * Seeds [isOnline] initial value before the first [NetworkCallback] fires.
     * Falls back to `true` on [SecurityException] to avoid blocking the user.
     */
    private fun checkCurrentConnection(): Boolean {
        return try {
            val capabilities = connectivityManager
                .getNetworkCapabilities(connectivityManager.activeNetwork)
            capabilities?.hasInternetConnection() ?: false
        } catch (e: SecurityException) {
            Timber.e(e, "Failed to check connection — assuming online")
            true
        }
    }

    private fun NetworkCapabilities.hasInternetConnection(): Boolean {
        return hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}