package com.metehanyl.borsa.data.remote

import com.metehanyl.borsa.data.model.YahooChartResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
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

    private suspend fun getChart(symbol: String, host: String): YahooChartResponse = withContext(Dispatchers.IO) {
        val url = "https://$host/v8/finance/chart/$symbol?range=1y&interval=1d&includePrePost=false"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            val bodyString = response.body?.string() ?: throw IOException("Boş yanıt: $symbol")
            // Yahoo, geçersiz sembollerde bile 200/4xx ile birlikte chart.error alanı taşıyan
            // bir JSON gövdesi döndürebilir; bu yüzden yanıt kodundan bağımsız parse ediyoruz.
            json.decodeFromString<YahooChartResponse>(bodyString)
        }
    }

    /** query1'i dener, başarısız olursa query2'ye düşer. */
    suspend fun getChartWithFallback(symbol: String): YahooChartResponse {
        return try {
            getChart(symbol, "query1.finance.yahoo.com")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            getChart(symbol, "query2.finance.yahoo.com")
        }
    }
}
