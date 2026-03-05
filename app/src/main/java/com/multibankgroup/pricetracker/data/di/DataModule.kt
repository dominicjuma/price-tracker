package com.multibankgroup.pricetracker.data.di

import com.multibankgroup.pricetracker.common.connectivity.ConnectivityObserver
import com.multibankgroup.pricetracker.common.di.ApplicationScope
import com.multibankgroup.pricetracker.common.di.IoDispatcher
import com.multibankgroup.pricetracker.common.util.Clock
import com.multibankgroup.pricetracker.data.repository.StockMetadataRepository
import com.multibankgroup.pricetracker.data.repository.StockMetadataRepositoryImpl
import com.multibankgroup.pricetracker.data.repository.StockPriceRepository
import com.multibankgroup.pricetracker.data.repository.StockPriceRepositoryImpl
import com.multibankgroup.pricetracker.data.websocket.StockPriceDataSource
import com.multibankgroup.pricetracker.data.websocket.StockPriceWebSocketDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json

/**
 * Hilt module for data-layer specific bindings.
 *
 * This module provides:
 * - Json configuration
 * - WebSocket data source binding
 * - Repository wiring
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    @Provides
    @Singleton
    fun provideStockPriceDataSource(
        impl: StockPriceWebSocketDataSource
    ): StockPriceDataSource = impl

    @Provides
    @Singleton
    fun provideStockMetadataRepository(
        impl: StockMetadataRepositoryImpl
    ): StockMetadataRepository = impl

    @Provides
    @Singleton
    fun provideStockPriceRepository(
        dataSource: StockPriceDataSource,
        metadataRepository: StockMetadataRepository,
        connectivityObserver: ConnectivityObserver,
        @ApplicationScope externalScope: CoroutineScope,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        json: Json,
        clock: Clock
    ): StockPriceRepository {
        return StockPriceRepositoryImpl(
            dataSource = dataSource,
            metadataRepository = metadataRepository,
            connectivityObserver = connectivityObserver,
            externalScope = externalScope,
            ioDispatcher = ioDispatcher,
            json = json,
            clock = clock
        )
    }
}