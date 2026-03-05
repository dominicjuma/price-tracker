package com.multibankgroup.pricetracker.core.util

/**
 * Fake [Clock] for unit tests.
 * Tests control time directly via [currentTime] and [advance].
 */
class FakeClock(var currentTime: Long = 0L) : Clock {
    override fun now(): Long = currentTime
    fun advance(millis: Long) { currentTime += millis }
}