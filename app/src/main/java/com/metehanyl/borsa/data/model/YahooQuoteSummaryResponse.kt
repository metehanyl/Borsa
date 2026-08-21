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
    val epsTrailingTwelveMonths: Double? = null
)
