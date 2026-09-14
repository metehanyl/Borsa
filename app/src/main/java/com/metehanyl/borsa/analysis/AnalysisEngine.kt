package com.metehanyl.borsa.analysis

import com.metehanyl.borsa.data.model.Quote
import kotlin.math.roundToInt

/**
 * Kural tabanlı (algoritmik) teknik analiz motoru. Bir yapay zeka modeline canlı
 * çağrı YAPMAZ — standart, şeffaf teknik göstergeleri ağırlıklandırarak -100..+100
 * arası bir "fırsat skoru" üretir. Sonuç kesin bir yatırım tavsiyesi değil,
 * eğitim amaçlı algoritmik bir değerlendirmedir.
 *
 * FELSEFE (önemli): Bu motor kasıtlı olarak "zaten yükselmiş, trendi onaylanmış"
 * kağıtları değil, "henüz ucuzken/aşırı satılmışken erken yakalanan, tepki
 * verme ihtimali olan" kağıtları öne çıkaracak şekilde ağırlıklandırılmıştır.
 * Bir kağıt son aylarda çok hızlı yükseldiyse ve/veya 52 haftalık zirvesine çok
 * yaklaştıysa, bu artık "geç kalınmış" sayılır ve skoru düşürür — "zaten
 * yükselmiş" bir kağıda "Güçlü Al" denmez. Tersine, sert düşmüş ama dönüş
 * belirtisi gösteren bir kağıt, hâlâ net bir aşağı trendde olsa bile daha
 * yüksek puan alabilir. Bu YİNE DE bir garanti değildir: "ucuz" bir kağıt
 * daha da ucuzlayabilir; hiçbir gösterge geleceği kesin bilemez.
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
        val macdPrev = if (closes.size > 35) TechnicalIndicators.macd(closes.dropLast(1)) else null
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

        // 1) Değerleme / konum bileşeni (ağırlık: 25 puan) — hissenin son 1 yılın
        // en düşük mü en yüksek seviyesine mi yakın olduğu. Bu motorun EN ÖNEMLİ
        // bileşenlerinden biridir: dipteki bir kağıt "ucuz olabilir" diye puan
        // alır, zirvedeki bir kağıt ise "geç kalınmış olabilir" diye puan kaybeder.
        if (distFromLow != null && distFromHigh != null) {
            when {
                distFromLow <= 10 -> {
                    score += 25
                    reasons += "Hisse, son 1 yılın en düşük seviyelerine çok yakın (sadece %${distFromLow.roundToInt()} üzerinde) — bu, ucuz kalmış bir fırsat olabilir. Ama unutmayın: bazen bir hisse ucuzken daha da ucuzlayabilir, düşüşün nedeni araştırılmalı."
                }
                distFromLow <= 25 -> {
                    score += 12
                    reasons += "Hisse, son 1 yılın en düşük seviyesine bir miktar yakın (%${distFromLow.roundToInt()} üzerinde) — dip bölgesine göre ucuz sayılabilir."
                }
                distFromHigh >= -5 -> {
                    score -= 25
                    reasons += "Hisse, son 1 yılın en yüksek seviyesine çok yakın (zirveye sadece %${(-distFromHigh).roundToInt()} uzaklıkta) — büyük olasılıkla en iyi alım zamanı geçmiş olabilir, şimdi almak riskli görünüyor."
                }
                distFromHigh >= -15 -> {
                    score -= 12
                    reasons += "Hisse, son 1 yılın en yüksek seviyesine yakın (%${(-distFromHigh).roundToInt()} altında) — yükselişin çoğu muhtemelen gerçekleşmiş olabilir."
                }
                else -> {
                    reasons += "Hisse, son 1 yıllık en düşük ve en yüksek seviyeleri arasında, ortalarda bir yerde işlem görüyor — ne çok ucuz ne çok pahalı görünüyor."
                }
            }
        }

        // 2) RSI bileşeni (ağırlık: 30 puan) — hissenin ne kadar hızlı yükselip
        // düştüğünü 0-100 arasında ölçen bir gösterge. Düşükse ("aşırı satım")
        // kısa vadeli bir tepki/toparlanma ihtimali artar; yüksekse ("aşırı
        // alım") hissenin çok hızlı yükseldiği ve durup dinlenebileceği anlamına
        // gelir.
        if (rsi != null) {
            when {
                rsi < 30 -> {
                    score += 30
                    reasons += "RSI göstergesi ${rsi.roundToInt()} (100 üzerinden) — hisse çok hızlı düştüğü için 'aşırı satılmış' durumda. Bu tür durumlarda genelde kısa vadeli bir tepki/toparlanma görülür, bu yüzden dikkat çekici bir giriş noktası olabilir."
                }
                rsi < 45 -> {
                    score += 15
                    reasons += "RSI göstergesi ${rsi.roundToInt()} — satım bölgesine yakın ama henüz aşırıya kaçmamış."
                }
                rsi <= 55 -> {
                    reasons += "RSI göstergesi ${rsi.roundToInt()} — nötr bölgede, ne aşırı alım ne aşırı satım var."
                }
                rsi <= 70 -> {
                    score -= 15
                    reasons += "RSI göstergesi ${rsi.roundToInt()} — hisse hızlı yükseliyor, 'aşırı alım' bölgesine yaklaşıyor. Yeni alım için acele etmemek daha güvenli olabilir."
                }
                else -> {
                    score -= 30
                    reasons += "RSI göstergesi ${rsi.roundToInt()} — hisse çok hızlı yükseldiği için 'aşırı alım' bölgesinde. Bu seviyeden yeni alım yapmak, tam tepede kalma riski taşır."
                }
            }
        }

        // 3) Erken dönüş sinyali (ağırlık: 20 puan) — MACD göstergesinin YENİ mi
        // yoksa uzun süredir mi pozitif/negatif olduğuna bakar. Amaç: "yükseliş
        // yeni mi başladı" (henüz geç kalınmamış) ile "yükseliş uzun süredir
        // devam ediyor" (muhtemelen geç kalınmış) arasındaki farkı yakalamak.
        if (macd != null) {
            val wasPositive = macdPrev != null && macdPrev.histogram > 0
            val wasNegative = macdPrev != null && macdPrev.histogram <= 0
            when {
                macd.histogram > 0 && macdPrev != null && wasNegative -> {
                    score += 20
                    reasons += "Kısa vadeli momentum göstergesi (MACD) az önce pozitife döndü — bu, düşüşün durup yukarı dönüşün YENİ başlamış olabileceğine işaret ediyor, henüz geç kalınmamış bir sinyal olabilir."
                }
                macd.histogram > 0 -> {
                    score += 5
                    reasons += "Kısa vadeli momentum hâlâ pozitif, ama bu yükseliş bir süredir devam ediyor — en taze fırsat bu olmayabilir."
                }
                macdPrev != null && wasPositive -> {
                    score -= 20
                    reasons += "Kısa vadeli momentum göstergesi (MACD) az önce negatife döndü — yükselişin ivme kaybettiğine ve olası bir gerilemenin başladığına işaret edebilir."
                }
                else -> {
                    score -= 5
                    reasons += "Kısa vadeli momentum hâlâ negatif; düşüş eğilimi bir süredir devam ediyor."
                }
            }
        }

        // 4) Genel eğilim bağlamı (ağırlık: +5 / -15) — SMA50/SMA200 (50 ve 200
        // günlük ortalama fiyat) ilişkisi. Bu artık ana "al" sürücüsü DEĞİL,
        // sadece bir risk bağlamı: net bir aşağı trendde "ucuz" görünen bir
        // hisseye girmek daha risklidir ('düşen bıçağı yakalamak' denir), bu
        // yüzden ciddi bir aşağı trend puanı düşürür; ama sırf trend yukarıysa
        // bu tek başına büyük bir "al" nedeni sayılmaz (çünkü genelde o kağıt
        // zaten epey yükselmiş olur).
        if (sma50 != null && sma200 != null) {
            when {
                price > sma50 && sma50 > sma200 -> {
                    score += 5
                    reasons += "Genel eğilim (fiyatın 50 ve 200 günlük ortalamalarına göre konumu) hâlâ yukarı yönlü — bu tek başına güçlü bir alım nedeni değil ama olumlu bir arka plan sayılabilir."
                }
                price < sma50 && sma50 < sma200 -> {
                    score -= 15
                    reasons += "Genel eğilim aşağı yönlü (fiyat, 50 ve 200 günlük ortalamaların altında) — hisse zayıf bir dönemden geçiyor. Diğer göstergeler 'fırsat' dese bile, düşüşün devam etme riski var, temkinli olun."
                }
                else -> {
                    score -= 3
                    reasons += "Genel eğilim karışık/belirsiz görünüyor — kısa ve uzun vadeli ortalamalar farklı yönler gösteriyor."
                }
            }
        }

        // 5) Son 3 aylık fiyat değişimi (ağırlık: -8 / +5) — yumuşatılmış ve
        // KISMEN TERS ÇEVRİLMİŞTİR: çok hızlı bir yükseliş artık ödüllendirilmez,
        // çünkü bu genelde "geç kalınmış" anlamına gelir. Sert bir düşüş de tek
        // başına ödüllendirilmez çünkü bunun nedeni gerçek bir sorun da olabilir
        // — bu belirsizlik açıkça belirtilir.
        if (momentum3M != null) {
            when {
                momentum3M > 25 -> {
                    score -= 8
                    reasons += "Son 3 ayda çok hızlı bir yükseliş yaşadı (%${momentum3M.roundToInt()}) — bu tempo genelde sürdürülemez, şu an yeni girmek riskli olabilir; en ucuz dönem geride kalmış olabilir."
                }
                momentum3M > 10 -> {
                    score += 3
                    reasons += "Son 3 ayda ılımlı bir yükseliş var (%${momentum3M.roundToInt()}), henüz aşırıya kaçmamış görünüyor."
                }
                momentum3M > -10 -> {
                    reasons += "Son 3 ayda fiyat büyük bir değişim göstermedi (%${momentum3M.roundToInt()})."
                }
                momentum3M > -25 -> {
                    score += 5
                    reasons += "Son 3 ayda bir miktar gerileme yaşadı (%${momentum3M.roundToInt()}) — diğer göstergelerle birlikte değerlendirildiğinde bu bir fırsat olabilir."
                }
                else -> {
                    reasons += "Son 3 ayda sert bir düşüş yaşadı (%${momentum3M.roundToInt()}). Bu, bazen bir fırsat, bazen de şirketle ilgili gerçek bir sorunun işaretidir — sadece bu göstergeye bakarak karar vermeyin, düşüşün nedenini araştırın."
                }
            }
        }

        // 6) Hacim bileşeni (ağırlık: ±8 puan) — son işlem gününün hacmi 20 günlük
        // ortalamaya göre ne kadar yüksek; bu, "kaç kişi işlem yaptı" değil, o gün
        // el değiştiren hisse adedinin (kaç kişi olduğunu göstermez) ortalamaya oranıdır.
        val volumes = quote.history.mapNotNull { it.volume }
        if (volumes.size >= 21) {
            val latestVolume = volumes.last()
            val avgVolume20 = volumes.dropLast(1).takeLast(20).average()
            if (avgVolume20 > 0) {
                val ratio = latestVolume / avgVolume20
                val risingPrice = momentum1M != null && momentum1M > 0
                when {
                    ratio >= 2.0 && !risingPrice -> {
                        score += 8
                        reasons += "İşlem hacmi, 20 günlük ortalamanın ${"%.1f".format(ratio)} katı ama fiyat düşüyor — bu, satışların yoğunlaştığı ve dip bölgesinde olunabileceği bir 'panik satışı' anı olabilir."
                    }
                    ratio >= 2.0 && risingPrice -> {
                        score -= 8
                        reasons += "İşlem hacmi, 20 günlük ortalamanın ${"%.1f".format(ratio)} katı ve fiyat zaten yükseliyor — bu, yükselişin son (coşku) evresi olabilir, dikkatli olun."
                    }
                    ratio >= 1.3 -> {
                        reasons += "İşlem hacmi ortalamanın üzerinde (${"%.1f".format(ratio)}×), ilgi artıyor."
                    }
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
