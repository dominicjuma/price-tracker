package com.multibankgroup.pricetracker.common.util

/**
 * Production [Clock] backed by [System.currentTimeMillis].
 * Injected as a singleton via Hilt to centralise real-time access and keep it testable.
 */
object SystemClock : Clock {
    override fun now(): Long = System.currentTimeMillis()
}