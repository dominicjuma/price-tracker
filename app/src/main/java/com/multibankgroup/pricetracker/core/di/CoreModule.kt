package com.multibankgroup.pricetracker.core.di

import android.content.Context
import android.net.ConnectivityManager
import com.multibankgroup.pricetracker.core.connectivity.AndroidConnectivityObserver
import com.multibankgroup.pricetracker.core.connectivity.ConnectivityObserver
import com.multibankgroup.pricetracker.core.util.Clock
import com.multibankgroup.pricetracker.core.util.SystemClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Hilt module providing cross-cutting infrastructure shared across all layers.
 *
 * These are not data-layer concerns — they're app-wide utilities:
 * - [CoroutineScope] for long-lived operations
 * - [CoroutineDispatcher] for I/O work
 * - [Clock] for deterministic time
 * - [ConnectivityObserver] for network status
 */
@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideClock(): Clock = SystemClock

    @Provides
    @Singleton
    fun provideConnectivityObserver(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): ConnectivityObserver {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        return AndroidConnectivityObserver(connectivityManager, ioDispatcher)
    }
}