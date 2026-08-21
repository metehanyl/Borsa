package com.metehanyl.borsa.data

import com.metehanyl.borsa.data.model.PricePoint
import com.metehanyl.borsa.data.model.Quote
import com.metehanyl.borsa.data.model.StockInfo
import com.metehanyl.borsa.data.model.YahooChartResult
import com.metehanyl.borsa.data.remote.NetworkModule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Sonuç: ya bir Quote ya da kullanıcıya gösterilecek bir hata mesajı. */
sealed class QuoteResult {
    data class Success(val quote: Quote) : QuoteResult()
    data class Failure(val info: StockInfo, val message: String) : QuoteResult()
}

class StockRepository {

    private val cache = LinkedHashMap<String, Quote>()
    private val cacheMutex = Mutex()
    private val cacheTtlMillis = 5 * 60 * 1000L
    private var lastFetchAt = 0L

    /** Tüm katalogdaki sembolleri sınırlı eşzamanlılıkla çeker. */
    suspend fun fetchAll(forceRefresh: Boolean = false): List<QuoteResult> = coroutineScope {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cache.isNotEmpty() && (now - lastFetchAt) < cacheTtlMillis) {
            return@coroutineScope cache.values.map { QuoteResult.Success(it) }
        }

        val chunkSize = 8
        val results = ArrayList<QuoteResult>(StockCatalog.all.size)
        StockCatalog.all.chunked(chunkSize).forEach { chunk ->
            val deferred = chunk.map { info -> async { fetchOne(info) } }
            results += deferred.map { it.await() }
        }

        cacheMutex.withLock {
            results.filterIsInstance<QuoteResult.Success>().forEach { cache[it.quote.info.symbol] = it.quote }
            lastFetchAt = System.currentTimeMillis()
        }
        results
    }

    suspend fun fetchOne(info: StockInfo): QuoteResult {
        return try {
            val response = try {
                NetworkModule.primaryApi.getChart(info.symbol)
            } catch (primaryError: Exception) {
                if (primaryError is CancellationException) throw primaryError
                NetworkModule.fallbackApi.getChart(info.symbol)
            }

            val result = response.chart.result?.firstOrNull()
                ?: return QuoteResult.Failure(info, response.chart.error?.description ?: "Veri bulunamadı")

            val quote = toQuote(info, result)
                ?: return QuoteResult.Failure(info, "Fiyat verisi eksik")

            QuoteResult.Success(quote)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            QuoteResult.Failure(info, e.message ?: "Bilinmeyen ağ hatası")
        }
    }

    private fun toQuote(info: StockInfo, result: YahooChartResult): Quote? {
        val meta = result.meta
        val price = meta.regularMarketPrice ?: return null
        val previousClose = meta.chartPreviousClose ?: meta.previousClose ?: price

        val timestamps = result.timestamp.orEmpty()
        val quotes = result.indicators.quote?.firstOrNull()
        val closes = quotes?.close.orEmpty()
        val opens = quotes?.open.orEmpty()
        val highs = quotes?.high.orEmpty()
        val lows = quotes?.low.orEmpty()
        val volumes = quotes?.volume.orEmpty()

        val history = timestamps.indices.mapNotNull { i ->
            val close = closes.getOrNull(i) ?: return@mapNotNull null
            PricePoint(
                timestampMillis = timestamps[i] * 1000L,
                close = close,
                open = opens.getOrNull(i),
                high = highs.getOrNull(i),
                low = lows.getOrNull(i),
                volume = volumes.getOrNull(i)
            )
        }

        return Quote(
            info = info.copy(
                name = meta.longName ?: meta.shortName ?: info.name
            ),
            currency = meta.currency ?: "USD",
            price = price,
            previousClose = previousClose,
            dayHigh = meta.regularMarketDayHigh,
            dayLow = meta.regularMarketDayLow,
            volume = meta.regularMarketVolume,
            fiftyTwoWeekHigh = meta.fiftyTwoWeekHigh,
            fiftyTwoWeekLow = meta.fiftyTwoWeekLow,
            history = history
        )
    }
}
