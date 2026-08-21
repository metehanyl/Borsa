package com.metehanyl.borsa.analysis

import com.metehanyl.borsa.data.model.Quote

/**
 * Teknik analiz, uzun vadeli görünüm, temel değerleme (F/K, temettü) ve haber
 * tonunu tek bir Türkçe paragrafta birleştiren şablon tabanlı bir metin
 * üretici. Bir LLM'e canlı çağrı YAPMAZ; var olan hesaplanmış verilerden
 * kurallı bir özet kurar.
 */
object CommentarySynthesizer {

    fun synthesize(name: String, quote: Quote, analysis: Analysis, newsTilt: NewsTilt?): String {
        val sb = StringBuilder()

        sb.append("$name için kısa vadeli teknik görünüm: ${analysis.recommendation.label} (skor ${analysis.score}/100). ")

        val topShortReason = analysis.reasons.firstOrNull()
        if (topShortReason != null) sb.append("$topShortReason ")

        val mismatch = (analysis.recommendation == Recommendation.SELL || analysis.recommendation == Recommendation.STRONG_SELL) &&
            analysis.longTermOutlook == LongTermOutlook.HIGH
        val bothWeak = (analysis.recommendation == Recommendation.SELL || analysis.recommendation == Recommendation.STRONG_SELL) &&
            analysis.longTermOutlook == LongTermOutlook.LOW

        when {
            mismatch -> sb.append(
                "Buna karşın uzun vadeli yapısal görünüm ${analysis.longTermOutlook.label.lowercase()} — " +
                    "yani kısa vadede satış/temkin sinyali verse de, zaman içinde değerlenme potansiyeli taşıyabilir. " +
                    "${analysis.longTermReasons.firstOrNull().orEmpty()} "
            )
            bothWeak -> sb.append(
                "Uzun vadeli görünüm de zayıf (${analysis.longTermOutlook.label.lowercase()}) — hem kısa hem uzun vadede temkinli olmakta fayda var. "
            )
            else -> sb.append(
                "Uzun vadeli değerlenme potansiyeli ${analysis.longTermOutlook.label.lowercase()} olarak değerlendiriliyor. "
            )
        }

        valuationSentence(quote.trailingPE)?.let { sb.append("$it ") }
        dividendSentence(quote.dividendYieldPct)?.let { sb.append("$it ") }

        if (newsTilt != null) {
            when (newsTilt.sentiment) {
                NewsSentiment.POSITIVE -> sb.append(
                    "Güncel haber akışı genel olarak olumlu bir ton taşıyor (${newsTilt.positiveHits} olumlu başlık); " +
                        "bu da kısa vadeli tabloyu destekleyici bir unsur olabilir."
                )
                NewsSentiment.NEGATIVE -> sb.append(
                    "Güncel haber akışında olumsuz sinyaller öne çıkıyor (${newsTilt.negativeHits} olumsuz başlık); " +
                        "bu durum oynaklığı artırabilir, dikkatli olmakta fayda var."
                )
                NewsSentiment.NEUTRAL -> sb.append(
                    "Güncel haberlerde belirgin bir yön görülmüyor, piyasa dışı gelişmeler kararınızda etkili olmayabilir."
                )
            }
        }

        return sb.toString().trim()
    }

    /**
     * Genel/kaba F/K eşiklerine dayalıdır; sektöre göre ayarlanmış bir
     * karşılaştırma DEĞİLDİR (ör. teknoloji şirketleri doğası gereği bankalardan
     * daha yüksek F/K taşıyabilir).
     */
    private fun valuationSentence(trailingPE: Double?): String? {
        if (trailingPE == null || trailingPE <= 0) return null
        val pe = "%.1f".format(trailingPE)
        return when {
            trailingPE < 12 -> "F/K oranı $pe ile düşük seviyede — piyasa ucuz fiyatlıyor olabilir, ama bu bazen zayıf büyüme beklentisini de yansıtır."
            trailingPE <= 25 -> "F/K oranı $pe ile makul/dengeli bir seviyede."
            else -> "F/K oranı $pe ile yüksek seviyede — piyasa güçlü bir büyüme bekliyor olabilir, bu da fiyatı beklenti sapmalarına karşı hassaslaştırır."
        }
    }

    private fun dividendSentence(dividendYieldPct: Double?): String? {
        // Veri yoksa hiçbir şey söylemeyiz — bu, "temettü ödemiyor" anlamına
        // gelmez, sadece bu alan için veri çekilemediği anlamına gelebilir.
        if (dividendYieldPct == null) return null
        if (dividendYieldPct <= 0) return "Belirgin bir temettü ödemesi görünmüyor."
        val y = "%.1f".format(dividendYieldPct)
        return when {
            dividendYieldPct < 1 -> "Temettü verimi %$y ile sembolik düzeyde."
            dividendYieldPct <= 4 -> "Temettü verimi %$y ile ölçülü bir seviyede."
            else -> "Temettü verimi %$y ile yüksek — gelir odaklı yatırımcılar için dikkat çekici olabilir, ancak çok yüksek verim bazen fiyat düşüşünün bir yansıması da olabilir."
        }
    }
}
