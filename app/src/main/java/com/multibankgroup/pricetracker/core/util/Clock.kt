package com.multibankgroup.pricetracker.core.util

/** Abstraction over system time. Tests inject [FakeClock] for deterministic timestamps. */
fun interface Clock {
    fun now(): Long
}