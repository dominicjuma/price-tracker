package com.multibankgroup.pricetracker.data.model

/**
 * Structured error types for the data layer.
 *
 * Instead of catching generic [Exception] and logging, errors are modeled
 * as a sealed hierarchy so the UI layer can display context-appropriate
 * messages without inspecting exception types.
 *
 * Each error carries the original [cause] for logging/crash reporting
 * while exposing a clean type for the ViewModel to map to UI text.
 */
sealed interface DataError {

    val cause: Throwable?

    /** WebSocket connection failed or was rejected. */
    data class ConnectionFailed(override val cause: Throwable? = null) : DataError

    /** WebSocket disconnected unexpectedly after being connected. */
    data class ConnectionLost(override val cause: Throwable? = null) : DataError

    /** Failed to serialize price update for sending. */
    data class SerializationFailed(override val cause: Throwable? = null) : DataError

    /** Failed to parse echoed message from server. */
    data class ParseFailed(override val cause: Throwable? = null) : DataError

    /** Device has no internet connectivity. */
    data object NoInternet : DataError {
        override val cause: Throwable? = null
    }
}