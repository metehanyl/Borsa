package com.metehanyl.borsa.data

import com.metehanyl.borsa.data.model.IntradayPoint
import com.metehanyl.borsa.data.model.PricePoint
import com.metehanyl.borsa.data.model.Quote
import com.metehanyl.borsa.data.model.StockInfo
import com.metehanyl.borsa.data.model.YahooChartResult
import com.metehanyl.borsa.data.model.YahooQuoteResult
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

    /**
     * Verilen katalogdaki sembolleri sınırlı eşzamanlılıkla çeker. Önbellek hâlâ
     * tazeyse ve daha önce çekilmiş sembollerse yeniden ağ isteği atmaz; kataloğa
     * sonradan eklenen (ör. kullanıcının elle izlemeye başladığı) semboller varsa
     * onları TTL'den bağımsız olarak çeker.
     */
    suspend fun fetchAll(catalog: List<StockInfo>, forceRefresh: Boolean = false): List<QuoteResult> = coroutineScope {
        val now = System.currentTimeMillis()
        val cacheFresh = !forceRefresh && (now - lastFetchAt) < cacheTtlMillis
        val toFetch = if (cacheFresh) catalog.filter { it.symbol !in cache } else catalog

        if (toFetch.isEmpty()) {
            return@coroutineScope catalog.mapNotNull { info -> cache[info.symbol]?.let { QuoteResult.Success(it) } }
        }

        val chunkSize = 12
        val fetched = ArrayList<QuoteResult>(toFetch.size)
        toFetch.chunked(chunkSize).forEach { chunk ->
            val deferred = chunk.map { info -> async { fetchOne(info) } }
            fetched += deferred.map { it.await() }
        }

        val enrichedFetched = enrichWithFundamentals(fetched)

        cacheMutex.withLock {
            enrichedFetched.filterIsInstance<QuoteResult.Success>().forEach { cache[it.quote.info.symbol] = it.quote }
            lastFetchAt = System.currentTimeMillis()
        }

        val fetchedSymbols = toFetch.map { it.symbol }.toSet()
        val fromCache = catalog.filter { it.symbol !in fetchedSymbols }
            .mapNotNull { info -> cache[info.symbol]?.let { QuoteResult.Success(it) } }
        enrichedFetched + fromCache
    }

    /**
     * Fiyat/grafik verisi başarıyla çekilen semboller için F/K oranı, piyasa
     * değeri, temettü verimi gibi temel finansal alanları toplu istekle çekip
     * Quote'lara ekler. Bu adım tamamen isteğe bağlıdır — herhangi bir hata
     * sessizce yutulur ve girdi olduğu gibi döndürülür (uygulamanın geri
     * kalanını etkilemez).
     */
    private suspend fun enrichWithFundamentals(results: List<QuoteResult>): List<QuoteResult> {
        val successes = results.filterIsInstance<QuoteResult.Success>()
        if (successes.isEmpty()) return results

        val fundamentalsBySymbol = HashMap<String, YahooQuoteResult>()
        successes.map { it.quote.info.symbol }.chunked(50).forEach { chunk ->
            val batch = try {
                NetworkModule.getQuoteSummariesWithFallback(chunk)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
            batch.forEach { fundamentalsBySymbol[it.symbol] = it }
        }
        if (fundamentalsBySymbol.isEmpty()) return results

        return results.map { result ->
            if (result !is QuoteResult.Success) return@map result
            val f = fundamentalsBySymbol[result.quote.info.symbol] ?: return@map result
            QuoteResult.Success(
                result.quote.copy(
                    marketCap = f.marketCap,
                    trailingPE = f.trailingPE,
                    dividendYieldPct = f.dividendYield,
                    epsTrailingTwelveMonths = f.epsTrailingTwelveMonths,
                    bid = f.bid,
                    bidSize = f.bidSize,
                    ask = f.ask,
                    askSize = f.askSize,
                    nextEarningsDateMillis = (f.earningsTimestampStart ?: f.earningsTimestampEnd)?.times(1000L)
                )
            )
        }
    }

    /**
     * Son birkaç günün saatlik (60 dakikalık) mumlarını çeker — "o saatte kaç
     * hisse el değiştirdi" sorusuna gerçek bir cevap vermek için. Bu, "o saatte
     * kaç kişi vardı" DEĞİLDİR; hiçbir borsa ya da Yahoo Finance bu bilgiyi
     * yayımlamaz. Hata durumunda boş liste döner.
     */
    suspend fun fetchIntradayVolume(symbol: String): List<IntradayPoint> {
        return try {
            val response = NetworkModule.getChartWithFallback(symbol, range = "5d", interval = "60m")
            val result = response.chart.result?.firstOrNull() ?: return emptyList()
            val timestamps = result.timestamp.orEmpty()
            val quotes = result.indicators.quote?.firstOrNull()
            val volumes = quotes?.volume.orEmpty()
            val closes = quotes?.close.orEmpty()
            timestamps.indices.mapNotNull { i ->
                val vol = volumes.getOrNull(i) ?: return@mapNotNull null
                val close = closes.getOrNull(i) ?: return@mapNotNull null
                IntradayPoint(timestampMillis = timestamps[i] * 1000L, volume = vol, close = close)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Verilen sembolün, belirtilen tarihte veya o tarihten önceki son işlem
     * gününde oluşan kapanış fiyatını çeker. Portföye pozisyon eklerken
     * "alış tarihindeki fiyatı otomatik getir" özelliği için kullanılır.
     * Sembolün geçmişi hedef tarihten sonra başlıyorsa (ör. yeni halka arz),
     * bulunan ilk kapanış fiyatı döner. Veri çekilemezse null döner —
     * kullanıcı bu durumda fiyatı elle girer.
     */
    suspend fun fetchHistoricalClose(symbol: String, dateMillis: Long): Double? {
        return try {
            val response = NetworkModule.getChartWithFallback(symbol, range = "max", interval = "1d")
            val result = response.chart.result?.firstOrNull() ?: return null
            val timestamps = result.timestamp.orEmpty()
            val closes = result.indicators.quote?.firstOrNull()?.close.orEmpty()
            if (timestamps.isEmpty() || closes.isEmpty()) return null

            val targetSeconds = dateMillis / 1000L
            var bestIndex = 0
            for (i in timestamps.indices) {
                if (timestamps[i] <= targetSeconds) bestIndex = i else break
            }
            closes.getOrNull(bestIndex)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchOne(info: StockInfo): QuoteResult {
        return try {
            val response = NetworkModule.getChartWithFallback(info.symbol)

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
            history = history,
            open = history.lastOrNull()?.open
        )
    }
}
