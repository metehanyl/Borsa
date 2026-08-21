package com.metehanyl.borsa.analysis

/**
 * Teknik analiz, uzun vadeli görünüm ve haber tonunu tek bir Türkçe paragrafta
 * birleştiren basit bir metin üretici. Bir LLM'e çağrı yapmaz; var olan
 * hesaplanmış verilerden şablon tabanlı bir özet kurar.
 */
object CommentarySynthesizer {

    fun synthesize(name: String, analysis: Analysis, newsTilt: NewsTilt?): String {
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
}
