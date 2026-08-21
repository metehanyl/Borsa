package com.metehanyl.borsa.analysis

import com.metehanyl.borsa.data.model.Quote
import kotlin.math.roundToInt

/**
 * Kural tabanlı (algoritmik) teknik analiz motoru. Bir yapay zeka modeline canlı
 * çağrı YAPMAZ — trend (SMA), momentum (RSI/MACD), kısa/orta vadeli getiri ve
 * 52 haftalık aralıktaki konum gibi standart, şeffaf teknik göstergeleri
 * ağırlıklandırarak -100..+100 arası bir "fırsat skoru" üretir. Sonuç kesin bir
 * yatırım tavsiyesi değil, eğitim amaçlı algoritmik bir değerlendirmedir.
 */
object AnalysisEngine {

    fun analyze(quote: Quote): Analysis? {
        val closes = quote.history.map { it.close }
        if (closes.size < 30) return null // anlamlı analiz için yeterli veri yok

        val sma20 = TechnicalIndicators.sma(closes, 20)
        val sma50 = TechnicalIndicators.sma(closes, 50)
        val sma200 = TechnicalIndicators.sma(closes, 200)
        val rsi = TechnicalIndicators.rsi(closes, 14)
        val macd = TechnicalIndicators.macd(closes)
        val momentum1M = TechnicalIndicators.momentumPercent(closes, 21)
        val momentum3M = TechnicalIndicators.momentumPercent(closes, 63)
        val volatility = TechnicalIndicators.annualizedVolatilityPercent(closes, 30)

        val price = quote.price
        val high52 = quote.fiftyTwoWeekHigh
        val low52 = quote.fiftyTwoWeekLow
        val distFromHigh = if (high52 != null && high52 != 0.0) ((price - high52) / high52) * 100.0 else null
        val distFromLow = if (low52 != null && low52 != 0.0) ((price - low52) / low52) * 100.0 else null

        var score = 0
        val reasons = mutableListOf<String>()

        // 1) Trend bileşeni (ağırlık: 30 puan) — SMA50 / SMA200 ilişkisi
        if (sma50 != null && sma200 != null) {
            when {
                price > sma50 && sma50 > sma200 -> {
                    score += 30
                    reasons += "Trend güçlü yukarı yönlü: fiyat, 50 ve 200 günlük ortalamaların üzerinde ve kısa vadeli ortalama uzun vadelinin üstünde (Golden Cross görünümü)."
                }
                price > sma50 && sma50 <= sma200 -> {
                    score += 10
                    reasons += "Fiyat 50 günlük ortalamanın üzerinde ancak uzun vadeli (200 günlük) trend henüz yukarı dönmedi."
                }
                price < sma50 && sma50 < sma200 -> {
                    score -= 30
                    reasons += "Trend aşağı yönlü: fiyat, 50 ve 200 günlük ortalamaların altında (Death Cross görünümü)."
                }
                else -> {
                    score -= 10
                    reasons += "Fiyat kısa vadeli ortalamanın altında, trend zayıflıyor."
                }
            }
        } else if (sma20 != null) {
            if (price > sma20) {
                score += 10
                reasons += "Fiyat 20 günlük ortalamanın üzerinde (sınırlı geçmiş veri)."
            } else {
                score -= 10
                reasons += "Fiyat 20 günlük ortalamanın altında (sınırlı geçmiş veri)."
            }
        }

        // 2) RSI bileşeni (ağırlık: 25 puan)
        if (rsi != null) {
            when {
                rsi < 30 -> {
                    score += 25
                    reasons += "RSI ${rsi.roundToInt()} ile aşırı satım bölgesinde — teknik tepki/alım fırsatı olabilir."
                }
                rsi < 45 -> {
                    score += 12
                    reasons += "RSI ${rsi.roundToInt()} ile satım bölgesine yakın, aşırı satılmış değil."
                }
                rsi <= 55 -> {
                    reasons += "RSI ${rsi.roundToInt()} ile nötr bölgede."
                }
                rsi <= 70 -> {
                    score -= 12
                    reasons += "RSI ${rsi.roundToInt()} ile alım bölgesine yakın, ısınma belirtisi var."
                }
                else -> {
                    score -= 25
                    reasons += "RSI ${rsi.roundToInt()} ile aşırı alım bölgesinde — kâr satışı/düzeltme riski yüksek."
                }
            }
        }

        // 3) MACD bileşeni (ağırlık: 20 puan)
        if (macd != null) {
            if (macd.histogram > 0) {
                score += 20
                reasons += "MACD sinyal çizgisinin üzerinde, kısa vadeli momentum pozitif."
            } else {
                score -= 20
                reasons += "MACD sinyal çizgisinin altında, kısa vadeli momentum negatif."
            }
        }

        // 4) Orta vadeli momentum bileşeni (ağırlık: 15 puan) — 3 aylık getiri
        if (momentum3M != null) {
            when {
                momentum3M > 15 -> { score += 15; reasons += "Son 3 ayda %${momentum3M.roundToInt()} değer kazandı — güçlü pozitif momentum." }
                momentum3M > 5 -> { score += 8; reasons += "Son 3 ayda %${momentum3M.roundToInt()} değer kazandı." }
                momentum3M > -5 -> { reasons += "Son 3 ayda yatay seyretti (%${momentum3M.roundToInt()})." }
                momentum3M > -15 -> { score -= 8; reasons += "Son 3 ayda %${(-momentum3M).roundToInt()} değer kaybetti." }
                else -> { score -= 15; reasons += "Son 3 ayda %${(-momentum3M).roundToInt()} sert değer kaybetti — zayıflık belirgin." }
            }
        }

        // 5) 52 haftalık aralıktaki konum (ağırlık: 10 puan)
        if (distFromLow != null && distFromHigh != null) {
            when {
                distFromLow <= 10 -> {
                    score += 10
                    reasons += "52 haftalık en düşük seviyeye yakın (+%${distFromLow.roundToInt()}), değerleme cazip olabilir."
                }
                distFromHigh >= -5 -> {
                    score -= 10
                    reasons += "52 haftalık en yüksek seviyeye yakın (%${distFromHigh.roundToInt()}), kâr satışı riski."
                }
                else -> {
                    reasons += "52 haftalık aralığın orta kesiminde işlem görüyor."
                }
            }
        }

        val clampedScore = score.coerceIn(-100, 100)
        val recommendation = when {
            clampedScore >= 50 -> Recommendation.STRONG_BUY
            clampedScore >= 20 -> Recommendation.BUY
            clampedScore > -20 -> Recommendation.HOLD
            clampedScore > -50 -> Recommendation.SELL
            else -> Recommendation.STRONG_SELL
        }

        val (longTermOutlook, longTermReasons) = computeLongTermOutlook(
            closes = closes,
            sma200 = sma200,
            rsi = rsi,
            distFromHigh = distFromHigh,
            distFromLow = distFromLow
        )

        return Analysis(
            score = clampedScore,
            recommendation = recommendation,
            reasons = reasons,
            rsi = rsi,
            sma20 = sma20,
            sma50 = sma50,
            sma200 = sma200,
            momentum1M = momentum1M,
            momentum3M = momentum3M,
            volatility = volatility,
            distanceFrom52wHighPct = distFromHigh,
            distanceFrom52wLowPct = distFromLow,
            longTermOutlook = longTermOutlook,
            longTermReasons = longTermReasons
        )
    }

    /**
     * Kısa vadeli teknik skordan bağımsız olarak, fiyatın uzun vadeli yapısal
     * konumuna (200 günlük ortalamanın eğimi, zirveden/dipten uzaklık) dayalı
     * kaba bir "ileride değerlenme potansiyeli" tahmini üretir. Bir kağıt kısa
     * vadede "Sat" derken uzun vadede "Yüksek" potansiyelli görünebilir.
     */
    private fun computeLongTermOutlook(
        closes: List<Double>,
        sma200: Double?,
        rsi: Double?,
        distFromHigh: Double?,
        distFromLow: Double?
    ): Pair<LongTermOutlook, List<String>> {
        var points = 0
        val reasons = mutableListOf<String>()

        val earlierSma200 = if (sma200 != null && closes.size >= 242) {
            TechnicalIndicators.sma(closes.dropLast(42), 200)
        } else null

        if (sma200 != null && earlierSma200 != null) {
            if (sma200 > earlierSma200) {
                points += 2
                reasons += "200 günlük ortalama yükseliş eğiminde — uzun vadeli yapı sağlam."
            } else {
                points -= 2
                reasons += "200 günlük ortalama düşüş eğiminde — uzun vadeli yapı zayıflıyor."
            }
        } else {
            reasons += "Uzun vadeli trend eğimi için yeterli geçmiş veri yok, temkinli değerlendirildi."
        }

        if (distFromHigh != null) {
            when {
                distFromHigh <= -25 -> {
                    points += 2
                    reasons += "Fiyat 52 haftalık zirveden %${(-distFromHigh).let { "%.0f".format(it) }} uzakta — belirgin bir indirim/toparlanma payı var."
                }
                distFromHigh <= -10 -> {
                    points += 1
                    reasons += "Fiyat zirveden bir miktar geri çekilmiş durumda."
                }
            }
        }

        if (distFromLow != null && sma200 != null && earlierSma200 != null && sma200 > earlierSma200) {
            if (distFromLow <= 15) {
                points += 1
                reasons += "Uzun vadeli trend hâlâ yukarı yönlüyken fiyat dip bölgesine yakın — yapısal fırsat olabilir."
            }
        }

        if (rsi != null && rsi < 35 && sma200 != null && earlierSma200 != null && sma200 > earlierSma200) {
            points += 1
            reasons += "Kısa vadede aşırı satım var ama uzun vadeli trend hâlâ yukarı yönlü — tepki alımı ihtimali."
        }

        val outlook = when {
            points >= 3 -> LongTermOutlook.HIGH
            points >= 0 -> LongTermOutlook.MEDIUM
            else -> LongTermOutlook.LOW
        }

        return outlook to reasons
    }
}
