package com.multibankgroup.pricetracker.core.di

import javax.inject.Qualifier

/** App-scoped [CoroutineScope] for long-lived operations that survive screen transitions. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/** [Dispatchers.IO] injected into repositories; replaced with [UnconfinedTestDispatcher] in tests. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher