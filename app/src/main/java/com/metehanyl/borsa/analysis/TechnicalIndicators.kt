package com.metehanyl.borsa.analysis

import kotlin.math.sqrt

/** Basit, bağımsız teknik gösterge hesaplamaları. Girdi: kronolojik (eskiden yeniye) kapanış fiyatları. */
object TechnicalIndicators {

    fun sma(closes: List<Double>, period: Int): Double? {
        if (closes.size < period) return null
        return closes.takeLast(period).average()
    }

    /** Üstel hareketli ortalama serisi (her nokta için), en az `period` veri gerektirir. */
    fun emaSeries(closes: List<Double>, period: Int): List<Double> {
        if (closes.isEmpty()) return emptyList()
        val k = 2.0 / (period + 1)
        val result = ArrayList<Double>(closes.size)
        var prevEma = closes.first()
        result.add(prevEma)
        for (i in 1 until closes.size) {
            val ema = closes[i] * k + prevEma * (1 - k)
            result.add(ema)
            prevEma = ema
        }
        return result
    }

    /** Wilder'ın klasik RSI(14) yöntemi. Yeterli veri yoksa null döner. */
    fun rsi(closes: List<Double>, period: Int = 14): Double? {
        if (closes.size <= period) return null
        var avgGain = 0.0
        var avgLoss = 0.0
        for (i in 1..period) {
            val change = closes[i] - closes[i - 1]
            if (change >= 0) avgGain += change else avgLoss -= change
        }
        avgGain /= period
        avgLoss /= period

        for (i in (period + 1) until closes.size) {
            val change = closes[i] - closes[i - 1]
            val gain = if (change >= 0) change else 0.0
            val loss = if (change < 0) -change else 0.0
            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
        }

        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    data class Macd(val macdLine: Double, val signalLine: Double, val histogram: Double)

    fun macd(closes: List<Double>, fast: Int = 12, slow: Int = 26, signal: Int = 9): Macd? {
        if (closes.size < slow + signal) return null
        val emaFast = emaSeries(closes, fast)
        val emaSlow = emaSeries(closes, slow)
        val macdLineSeries = closes.indices.map { emaFast[it] - emaSlow[it] }
        val signalSeries = emaSeries(macdLineSeries, signal)
        val macdLine = macdLineSeries.last()
        val signalLine = signalSeries.last()
        return Macd(macdLine, signalLine, macdLine - signalLine)
    }

    /** Belirli işlem günü öncesine göre yüzde değişim (momentum). */
    fun momentumPercent(closes: List<Double>, tradingDaysAgo: Int): Double? {
        if (closes.size <= tradingDaysAgo) return null
        val past = closes[closes.size - 1 - tradingDaysAgo]
        val now = closes.last()
        if (past == 0.0) return null
        return ((now - past) / past) * 100.0
    }

    /** Son `window` günlük getirilerin standart sapmasından yıllıklandırılmış volatilite (%). */
    fun annualizedVolatilityPercent(closes: List<Double>, window: Int = 30): Double? {
        if (closes.size <= window) return null
        val returns = closes.takeLast(window + 1).zipWithNext { a, b -> if (a == 0.0) 0.0 else (b - a) / a }
        if (returns.size < 2) return null
        val mean = returns.average()
        val variance = returns.sumOf { (it - mean) * (it - mean) } / (returns.size - 1)
        val dailyStdDev = sqrt(variance)
        return dailyStdDev * sqrt(252.0) * 100.0
    }
}
