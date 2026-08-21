package com.metehanyl.borsa.data.model

/** Katalogdaki statik hisse/fon tanımı (sembol + görünen ad + piyasa). */
data class StockInfo(
    val symbol: String,
    val name: String,
    val market: Market,
    val sector: String
)

/** Bir sembol için geçmiş fiyat serisindeki tek bir gün. */
data class PricePoint(
    val timestampMillis: Long,
    val close: Double,
    val open: Double?,
    val high: Double?,
    val low: Double?,
    val volume: Long?
)

/** Yahoo Finance'ten çekilen ve modele dönüştürülmüş tam kayıt. */
data class Quote(
    val info: StockInfo,
    val currency: String,
    val price: Double,
    val previousClose: Double,
    val dayHigh: Double?,
    val dayLow: Double?,
    val volume: Long?,
    val fiftyTwoWeekHigh: Double?,
    val fiftyTwoWeekLow: Double?,
    val history: List<PricePoint>
) {
    val changeAmount: Double get() = price - previousClose
    val changePercent: Double get() = if (previousClose != 0.0) (changeAmount / previousClose) * 100.0 else 0.0
}
