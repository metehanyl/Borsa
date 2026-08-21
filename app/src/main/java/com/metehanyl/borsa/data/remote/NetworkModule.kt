@file:OptIn(ExperimentalSerializationApi::class)

package com.metehanyl.borsa.data.remote

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Basit servis kurucusu. query1 ana uç noktadır; bazı semboller/bölgeler için
 * query2 yedek olarak denenir (chart.finance.yahoo.com altyapısı zaman zaman
 * bölgesel yönlendirme yapabildiğinden).
 */
object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private fun buildClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
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

    private fun buildApi(baseUrl: String): YahooFinanceApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(buildClient())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(YahooFinanceApi::class.java)
    }

    val primaryApi: YahooFinanceApi by lazy { buildApi("https://query1.finance.yahoo.com/") }
    val fallbackApi: YahooFinanceApi by lazy { buildApi("https://query2.finance.yahoo.com/") }
}
