package com.metehanyl.borsa.analysis

/**
 * "Ne kadar süre tutmalıyım / ne kadar sürede satmayı düşünmeliyim" sorusuna
 * algoritmik bir referans. Kesin bir öngörü ya da garanti DEĞİLDİR — kısa
 * vadeli sinyalin gücü, oynaklık, uzun vadeli yapısal görünüm ve haber
 * tonundan türetilen kaba bir zaman çerçevesi önerisidir. Piyasalar
 * tahmin edilemez; bu sadece "bu koşullarda genelde nasıl yaklaşılır"
 * sorusuna kural tabanlı bir yanıttır.
 */
data class HoldingHorizon(
    val label: String,
    val explanation: String
)

fun computeHoldingHorizon(analysis: Analysis, newsTilt: NewsTilt?): HoldingHorizon {
    val bearishSignal = analysis.recommendation == Recommendation.SELL || analysis.recommendation == Recommendation.STRONG_SELL
    val bullishSignal = analysis.recommendation == Recommendation.BUY || analysis.recommendation == Recommendation.STRONG_BUY
    val highVolatility = (analysis.volatility ?: 0.0) >= 40.0
    val strongLongTerm = analysis.longTermOutlook == LongTermOutlook.HIGH
    val badNews = newsTilt?.sentiment == NewsSentiment.NEGATIVE && (newsTilt.negativeHits - newsTilt.positiveHits) >= 2
    val goodNews = newsTilt?.sentiment == NewsSentiment.POSITIVE && (newsTilt.positiveHits - newsTilt.negativeHits) >= 2

    return when {
        bearishSignal && badNews -> HoldingHorizon(
            label = "Çok kısa vade (günler içinde gözden geçirin)",
            explanation = "Kısa vadeli teknik sinyal de haber akışı da olumsuz — pozisyonu uzatmak yerine yakın günlerde tekrar değerlendirmeniz önerilir."
        )
        bearishSignal && strongLongTerm -> HoldingHorizon(
            label = "Kısa vadede gözden geçirin, uzun vadede tekrar bakın",
            explanation = "Şu an kısa vadeli sinyal satış yönünde olsa da yapısal görünüm güçlü; bazı yatırımcılar kısa vadede azaltıp uzun vadede yeniden değerlendirmeyi tercih eder."
        )
        bearishSignal -> HoldingHorizon(
            label = "Kısa vade (1-2 hafta içinde gözden geçirin)",
            explanation = "Teknik göstergeler zayıflığa işaret ediyor; pozisyonu uzun süre elde tutmadan yakından takip etmeniz önerilir."
        )
        bullishSignal && strongLongTerm && !badNews -> HoldingHorizon(
            label = "Uzun vade (6+ ay)",
            explanation = "Kısa vadeli sinyal de yapısal görünüm de olumlu; bu tür kurulumlarda pozisyonu daha uzun süre taşımak daha yaygın bir yaklaşımdır."
        )
        bullishSignal && highVolatility -> HoldingHorizon(
            label = "Kısa-orta vade (2-6 hafta, sık kontrol edin)",
            explanation = "Sinyal olumlu ama oynaklık yüksek — ani sert hareketler olabileceğinden pozisyonu sık aralıklarla gözden geçirmeniz faydalı olur."
        )
        bullishSignal && goodNews -> HoldingHorizon(
            label = "Orta vade (1-3 ay)",
            explanation = "Momentum ve haber akışı birlikte olumlu; orta vadeli bir pencerede tutup gelişmeleri takip etmek mantıklı olabilir."
        )
        bullishSignal -> HoldingHorizon(
            label = "Orta vade (1-3 ay)",
            explanation = "Momentum olumlu; düzenli aralıklarla (ör. haftalık) kontrol ederek pozisyonu değerlendirmeniz önerilir."
        )
        else -> HoldingHorizon(
            label = "Belirsiz — net bir zaman çerçevesi yok",
            explanation = "Şu an net bir kısa vadeli yön sinyali bulunmuyor; yeni bir haber ya da teknik gelişme (ör. bir sonraki bilanço) belirleyici olabilir."
        )
    }
}
