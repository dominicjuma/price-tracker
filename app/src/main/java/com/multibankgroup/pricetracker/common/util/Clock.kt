package com.multibankgroup.pricetracker.common.util

/** Abstraction over system time. Tests inject [FakeClock] for deterministic timestamps. */
fun interface Clock {
    fun now(): Long
}