package com.multibankgroup.pricetracker.test.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit4 rule that sets [Dispatchers.Main] to a [TestDispatcher] for the
 * duration of each test.
 *
 * This is the standard pattern recommended by the official Android testing docs
 * for testing ViewModels and any code that uses `Dispatchers.Main` (which
 * doesn't exist on the local JVM by default).
 *
 * Uses [UnconfinedTestDispatcher] by default so coroutines execute eagerly,
 * making tests deterministic without manual advancing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}