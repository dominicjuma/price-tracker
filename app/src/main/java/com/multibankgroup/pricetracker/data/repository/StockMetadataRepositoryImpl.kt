package com.multibankgroup.pricetracker.data.repository

import com.multibankgroup.pricetracker.data.model.StockInfo
import javax.inject.Inject
import javax.inject.Singleton

/** Hardcoded 25 symbols. In production, this would be backed by a local DB or network API. */
@Singleton
class StockMetadataRepositoryImpl @Inject constructor() : StockMetadataRepository {

    private val stocks: Map<String, StockInfo> = buildStockInfoMap()

    override fun getStockInfo(symbol: String): StockInfo {
        return stocks[symbol]
            ?: throw IllegalArgumentException("Unknown stock symbol: $symbol")
    }

    override fun getAllStockInfo(): List<StockInfo> = stocks.values.toList()

    override fun getAllSymbols(): List<String> = stocks.keys.toList()

    companion object {

        private fun buildStockInfoMap(): Map<String, StockInfo> = listOf(
            StockInfo("AAPL", "Apple Inc.", "Designs, manufactures, and markets smartphones, personal computers, tablets, wearables, and accessories worldwide.", 178.50),
            StockInfo("GOOG", "Alphabet Inc.", "Provides online advertising services, search engine technology, cloud computing, and software products globally.", 141.80),
            StockInfo("TSLA", "Tesla, Inc.", "Designs, develops, manufactures, and sells electric vehicles, energy generation and storage systems worldwide.", 245.20),
            StockInfo("AMZN", "Amazon.com, Inc.", "Engages in e-commerce, cloud computing (AWS), digital streaming, and artificial intelligence services.", 185.60),
            StockInfo("MSFT", "Microsoft Corporation", "Develops and licenses consumer and enterprise software, cloud solutions, and hardware devices.", 415.30),
            StockInfo("NVDA", "NVIDIA Corporation", "Designs GPU-accelerated computing platforms for gaming, professional visualization, data centers, and automotive markets.", 875.40),
            StockInfo("META", "Meta Platforms, Inc.", "Builds technologies for connecting people through social media, augmented reality, and virtual reality products.", 505.75),
            StockInfo("NFLX", "Netflix, Inc.", "Provides subscription-based streaming entertainment services with original and licensed content worldwide.", 628.90),
            StockInfo("BRK.B", "Berkshire Hathaway Inc.", "Diversified holding company engaged in insurance, freight rail, energy, manufacturing, and retail businesses.", 410.20),
            StockInfo("JPM", "JPMorgan Chase & Co.", "Provides global financial services including investment banking, asset management, and consumer banking.", 198.40),
            StockInfo("V", "Visa Inc.", "Operates a global digital payments network facilitating electronic funds transfers worldwide.", 276.50),
            StockInfo("JNJ", "Johnson & Johnson", "Develops and markets pharmaceutical products, medical devices, and consumer health products globally.", 155.80),
            StockInfo("WMT", "Walmart Inc.", "Operates retail and wholesale stores, e-commerce websites, and membership warehouse clubs worldwide.", 165.30),
            StockInfo("PG", "Procter & Gamble Co.", "Manufactures and sells branded consumer packaged goods including beauty, grooming, health, and home care products.", 158.90),
            StockInfo("MA", "Mastercard Incorporated", "Provides transaction processing and payment-related technology solutions connecting consumers, merchants, and institutions.", 460.70),
            StockInfo("UNH", "UnitedHealth Group Inc.", "Operates as a diversified healthcare company offering health benefits and technology-enabled health services.", 520.60),
            StockInfo("HD", "The Home Depot, Inc.", "Operates as a home improvement retailer selling building materials, home improvement products, and tools.", 345.20),
            StockInfo("DIS", "The Walt Disney Company", "Operates as a diversified entertainment company with theme parks, media networks, and streaming services.", 112.40),
            StockInfo("BAC", "Bank of America Corporation", "Provides banking and financial products and services for individual consumers, businesses, and institutions.", 35.80),
            StockInfo("XOM", "Exxon Mobil Corporation", "Engages in the exploration, production, and sale of crude oil and natural gas, and petroleum product manufacturing.", 108.50),
            StockInfo("ADBE", "Adobe Inc.", "Provides digital media and marketing solutions including creative, document, and experience cloud platforms.", 575.30),
            StockInfo("CRM", "Salesforce, Inc.", "Provides customer relationship management technology bringing companies and customers together with cloud-based services.", 265.40),
            StockInfo("INTC", "Intel Corporation", "Designs and manufactures semiconductor chips, computing and communications platforms and technologies worldwide.", 42.80),
            StockInfo("AMD", "Advanced Micro Devices, Inc.", "Designs and sells microprocessors, GPUs, and adaptive computing solutions for data centers and embedded systems.", 165.90),
            StockInfo("PYPL", "PayPal Holdings, Inc.", "Operates a digital payments platform enabling digital and mobile payments for consumers and merchants globally.", 62.50)
        ).associateBy { it.symbol }
    }
}