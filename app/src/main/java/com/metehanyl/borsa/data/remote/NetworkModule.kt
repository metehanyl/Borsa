package com.metehanyl.borsa.data.remote

import com.metehanyl.borsa.data.model.YahooChartResponse
import com.metehanyl.borsa.data.model.YahooQuoteResult
import com.metehanyl.borsa.data.model.YahooQuoteSummaryResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Yahoo Finance'in genel (anahtarsız) chart uç noktasına düz OkHttp isteği atıp
 * yanıtı kotlinx.serialization ile modele çeviren istemci. Retrofit yerine
 * doğrudan OkHttp kullanılır; böylece ek bir converter kütüphanesine bağımlı
 * olunmaz. query1 ana uç noktadır, başarısız olursa query2 denenir.
 */
object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android) KureselBorsa/1.0")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private suspend fun getChart(symbol: String, host: String, range: String, interval: String): YahooChartResponse =
        withContext(Dispatchers.IO) {
            val url = "https://$host/v8/finance/chart/$symbol?range=$range&interval=$interval&includePrePost=false"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: throw IOException("Boş yanıt: $symbol")
                // Yahoo, geçersiz sembollerde bile 200/4xx ile birlikte chart.error alanı taşıyan
                // bir JSON gövdesi döndürebilir; bu yüzden yanıt kodundan bağımsız parse ediyoruz.
                json.decodeFromString<YahooChartResponse>(bodyString)
            }
        }

    /**
     * query1'i dener, başarısız olursa query2'ye düşer.
     * @param range/interval varsayılan olarak 1 yıllık günlük mumlar (teknik analiz için);
     * gün içi hacim için ör. range="5d", interval="60m" geçilebilir.
     */
    suspend fun getChartWithFallback(symbol: String, range: String = "1y", interval: String = "1d"): YahooChartResponse {
        return try {
            getChart(symbol, "query1.finance.yahoo.com", range, interval)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            getChart(symbol, "query2.finance.yahoo.com", range, interval)
        }
    }

    private suspend fun getQuoteSummary(symbolsCsv: String, host: String): YahooQuoteSummaryResponse =
        withContext(Dispatchers.IO) {
            val url = "https://$host/v7/finance/quote?symbols=$symbolsCsv"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: throw IOException("Boş yanıt")
                json.decodeFromString<YahooQuoteSummaryResponse>(bodyString)
            }
        }

    /**
     * Birden çok sembol için tek istekte F/K oranı, piyasa değeri, temettü
     * verimi gibi temel finansal alanları çeker. Herhangi bir hata durumunda
     * (ör. beklenmedik alan adı/tipi) sessizce boş liste döner — bu, sadece
     * "Temel Bilgiler" zenginleştirmesini atlar, uygulamanın geri kalanını
     * etkilemez.
     */
    suspend fun getQuoteSummariesWithFallback(symbols: List<String>): List<YahooQuoteResult> {
        if (symbols.isEmpty()) return emptyList()
        val csv = symbols.joinToString(",") { URLEncoder.encode(it, "UTF-8") }
        return try {
            getQuoteSummary(csv, "query1.finance.yahoo.com").quoteResponse.result.orEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            try {
                getQuoteSummary(csv, "query2.finance.yahoo.com").quoteResponse.result.orEmpty()
            } catch (e2: CancellationException) {
                throw e2
            } catch (e2: Exception) {
                emptyList()
            }
        }
    }
}
