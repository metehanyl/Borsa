package com.metehanyl.borsa.analysis

enum class Recommendation(val label: String, val shortLabel: String) {
    STRONG_BUY("Güçlü Alım Fırsatı", "Güçlü Al"),
    BUY("Alım Fırsatı", "Al"),
    HOLD("Nötr / İzlemede Kal", "Nötr"),
    SELL("Satış Sinyali", "Sat"),
    STRONG_SELL("Güçlü Satış Sinyali", "Güçlü Sat")
}

data class Analysis(
    val score: Int,
    val recommendation: Recommendation,
    val reasons: List<String>,
    val rsi: Double?,
    val sma20: Double?,
    val sma50: Double?,
    val sma200: Double?,
    val momentum1M: Double?,
    val momentum3M: Double?,
    val volatility: Double?,
    val distanceFrom52wHighPct: Double?,
    val distanceFrom52wLowPct: Double?
)
