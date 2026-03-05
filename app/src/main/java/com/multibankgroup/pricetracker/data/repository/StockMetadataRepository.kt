package com.multibankgroup.pricetracker.data.repository

import com.multibankgroup.pricetracker.data.model.StockInfo

/** Static stock metadata — symbol, company name, description, initial price. */
interface StockMetadataRepository {

    /** @throws IllegalArgumentException if symbol not found. */
    fun getStockInfo(symbol: String): StockInfo

    fun getAllStockInfo(): List<StockInfo>

    fun getAllSymbols(): List<String>
}