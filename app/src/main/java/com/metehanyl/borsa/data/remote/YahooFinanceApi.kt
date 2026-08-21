package com.metehanyl.borsa.data.remote

import com.metehanyl.borsa.data.model.YahooChartResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Yahoo Finance'in genel (anahtarsız) grafik uç noktası. Tek çağrı hem son fiyatı
 * (meta) hem de geçmiş fiyat serisini (timestamp + indicators) döndürür.
 */
interface YahooFinanceApi {
    @GET("v8/finance/chart/{symbol}")
    suspend fun getChart(
        @Path("symbol") symbol: String,
        @Query("range") range: String = "1y",
        @Query("interval") interval: String = "1d",
        @Query("includePrePost") includePrePost: Boolean = false
    ): YahooChartResponse
}
