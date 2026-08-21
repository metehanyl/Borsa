package com.metehanyl.borsa.data.model

import kotlinx.serialization.Serializable

@Serializable
data class YahooChartResponse(
    val chart: YahooChart
)

@Serializable
data class YahooChart(
    val result: List<YahooChartResult>? = null,
    val error: YahooChartError? = null
)

@Serializable
data class YahooChartError(
    val code: String? = null,
    val description: String? = null
)

@Serializable
data class YahooChartResult(
    val meta: YahooMeta,
    val timestamp: List<Long>? = null,
    val indicators: YahooIndicators
)

@Serializable
data class YahooMeta(
    val currency: String? = null,
    val symbol: String,
    val exchangeName: String? = null,
    val fullExchangeName: String? = null,
    val instrumentType: String? = null,
    val regularMarketPrice: Double? = null,
    val chartPreviousClose: Double? = null,
    val previousClose: Double? = null,
    val regularMarketDayHigh: Double? = null,
    val regularMarketDayLow: Double? = null,
    val regularMarketVolume: Long? = null,
    val fiftyTwoWeekHigh: Double? = null,
    val fiftyTwoWeekLow: Double? = null,
    val longName: String? = null,
    val shortName: String? = null,
    val regularMarketTime: Long? = null
)

@Serializable
data class YahooIndicators(
    val quote: List<YahooQuote>? = null,
    val adjclose: List<YahooAdjClose>? = null
)

@Serializable
data class YahooQuote(
    val open: List<Double?>? = null,
    val high: List<Double?>? = null,
    val low: List<Double?>? = null,
    val close: List<Double?>? = null,
    val volume: List<Long?>? = null
)

@Serializable
data class YahooAdjClose(
    val adjclose: List<Double?>? = null
)
