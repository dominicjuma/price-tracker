package com.multibankgroup.pricetracker.domain

import com.multibankgroup.pricetracker.data.repository.StockMetadataRepository
import com.multibankgroup.pricetracker.data.repository.StockPriceRepository
import com.multibankgroup.pricetracker.domain.model.PriceDirection
import com.multibankgroup.pricetracker.domain.model.Stock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Combines real-time prices with static metadata and computes price direction.
 * Reused by both Feed and Detail ViewModels — avoids duplicating combine + direction logic.
 */
class ObserveStocksUseCase @Inject constructor(
    private val stockPriceRepository: StockPriceRepository,
    private val stockMetadataRepository: StockMetadataRepository
) {
    operator fun invoke(): Flow<List<Stock>> {
        return stockPriceRepository.stockPrices.map { priceMap ->
            priceMap.map { (symbol, priceData) ->
                val metadata = stockMetadataRepository.getStockInfo(symbol)

                Stock(
                    symbol = symbol,
                    companyName = metadata.companyName,
                    description = metadata.description,
                    currentPrice = priceData.currentPrice,
                    previousPrice = priceData.previousPrice,
                    priceDirection = computeDirection(
                        current = priceData.currentPrice,
                        previous = priceData.previousPrice
                    ),
                    lastUpdatedTimestamp = priceData.lastUpdatedTimestamp
                )
            }
        }
    }

    private fun computeDirection(current: Double, previous: Double): PriceDirection {
        return when {
            current > previous -> PriceDirection.UP
            current < previous -> PriceDirection.DOWN
            else -> PriceDirection.NONE
        }
    }
}