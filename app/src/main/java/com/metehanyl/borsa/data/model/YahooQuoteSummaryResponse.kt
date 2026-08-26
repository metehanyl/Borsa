package com.metehanyl.borsa.data.model

import kotlinx.serialization.Serializable

/**
 * Yahoo Finance'in toplu (birden çok sembolü tek istekte döndüren) "quote"
 * uç noktasının yanıtı. Fiyat grafiği yerine F/K oranı, piyasa değeri,
 * temettü verimi gibi temel finansal alanları taşır.
 */
@Serializable
data class YahooQuoteSummaryResponse(
    val quoteResponse: YahooQuoteResponseBody
)

@Serializable
data class YahooQuoteResponseBody(
    val result: List<YahooQuoteResult>? = null,
    val error: String? = null
)

@Serializable
data class YahooQuoteResult(
    val symbol: String,
    val marketCap: Long? = null,
    val trailingPE: Double? = null,
    val forwardPE: Double? = null,
    val dividendYield: Double? = null,
    val epsTrailingTwelveMonths: Double? = null,
    /** En iyi alış fiyatı ve bu fiyattaki lot (hisse adedi) kuyruğu — kişi sayısı değildir. */
    val bid: Double? = null,
    val bidSize: Long? = null,
    /** En iyi satış fiyatı ve bu fiyattaki lot (hisse adedi) kuyruğu — kişi sayısı değildir. */
    val ask: Double? = null,
    val askSize: Long? = null,
    /** Bir sonraki bilanço açıklama tarihi aralığının başlangıcı/bitişi (unix saniye). Yahoo çoğu sembol için verir; yoksa null. */
    val earningsTimestampStart: Long? = null,
    val earningsTimestampEnd: Long? = null
)
