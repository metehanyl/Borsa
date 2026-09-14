package com.metehanyl.borsa.analysis

import com.metehanyl.borsa.data.model.NewsItem

enum class NewsSentiment { POSITIVE, NEUTRAL, NEGATIVE }

data class NewsTilt(
    val sentiment: NewsSentiment,
    val positiveHits: Int,
    val negativeHits: Int,
    val matchedPositive: List<String>,
    val matchedNegative: List<String>
)

/**
 * @param severeConflict teknik görünüm olumluyken (Al/Güçlü Al) haberler ciddi
 * ölçüde olumsuzsa true — bu durumda arayüz belirgin bir uyarı göstermelidir.
 */
data class NewsNudgeResult(
    val recommendation: Recommendation,
    val note: String?,
    val severeConflict: Boolean
)

/**
 * Haber başlıklarını "okuyup anlayan" bir yapay zeka DEĞİLDİR. Piyasayı etkileme
 * ihtimali yüksek anahtar kelimeleri başlıklarda arayan basit, şeffaf ve
 * ücretsiz bir sezgisel (heuristic) tarama katmanıdır — sonuç kesinlik taşımaz,
 * sadece haber akışının genel tonu hakkında kaba bir fikir verir.
 */
object NewsAnalyzer {

    private val positiveKeywords = listOf(
        "büyüme", "büyüdü", "rekor", "kazanç", "kâr", "yükseliş", "yükseldi", "anlaşma",
        "faiz indirimi", "güçlü bilanço", "ralli", "iyimser", "toparlanma", "beklentileri aştı",
        "growth", "record", "profit", "rally", "upgrade", "deal", "rate cut", "strong earnings",
        "surge", "optimism", "recovery", "beat expectations", "soars", "jumps"
    )

    private val negativeKeywords = listOf(
        "kriz", "savaş", "gerileme", "düşüş", "düştü", "resesyon", "faiz artışı", "zam",
        "enflasyon şoku", "iflas", "kayıp", "dava", "soruşturma", "yaptırım", "grev", "kesinti",
        "war", "crisis", "recession", "sanctions", "lawsuit", "downgrade", "plunge", "sell-off",
        "layoffs", "inflation surge", "default", "bankruptcy", "conflict", "tariffs", "slump"
    )

    /**
     * Teknik skoru DEĞİŞTİRMEZ (o skor tüm listede — Piyasalar, Önerilerim —
     * her hisse için haber çekmenin pratik olmamasından dolayı haberden
     * bağımsız hesaplanır). Bunun yerine, o an bu hissenin detay ekranında
     * zaten çekilmiş olan haber tonuna göre, gösterilen tavsiyeyi yukarı ya da
     * aşağı "iter" ve nedenini açıklar. Haberler ne kadar tek yönlüyse (çok
     * sayıda olumlu ya da olumsuz başlık) itme o kadar güçlü olur — hafif bir
     * ton farkı 1 kademe, güçlü/net bir ton 2 kademe iter. Teknik görünüm
     * "Al/Güçlü Al" derken haberler ciddi ölçüde olumsuzsa, bu bir ÇELİŞKİ
     * olarak ayrıca işaretlenir: düşüşün sadece geçici bir dalgalanma değil,
     * gerçek bir sorunun işareti olabileceği açıkça belirtilir.
     */
    fun applyNewsNudge(recommendation: Recommendation, newsTilt: NewsTilt?): NewsNudgeResult {
        if (newsTilt == null) return NewsNudgeResult(recommendation, null, severeConflict = false)
        val net = newsTilt.positiveHits - newsTilt.negativeHits
        if (kotlin.math.abs(net) < 2) {
            return NewsNudgeResult(
                recommendation,
                "Güncel haber akışında net bir yön görülmüyor; karar büyük ölçüde teknik görünüme dayanıyor.",
                severeConflict = false
            )
        }
        val order = Recommendation.entries // STRONG_BUY..STRONG_SELL, düşük ordinal = daha olumlu
        val currentIndex = order.indexOf(recommendation)
        val steps = if (kotlin.math.abs(net) >= 3) 2 else 1
        val nudgedIndex = if (net > 0) (currentIndex - steps).coerceAtLeast(0) else (currentIndex + steps).coerceAtMost(order.size - 1)
        val nudged = order[nudgedIndex]

        val technicalIsBullish = recommendation == Recommendation.BUY || recommendation == Recommendation.STRONG_BUY
        val severeConflict = net <= -3 && technicalIsBullish

        val note = when {
            net > 0 -> "Son günlerdeki haberler genel olarak olumlu (${newsTilt.positiveHits} olumlu başlık) — bu, teknik görünümü destekleyici bir unsur olarak dikkate alındı."
            severeConflict -> "DİKKAT: Teknik gösterge 'ucuzlamış, fırsat olabilir' diyor ama haberler ciddi ölçüde olumsuz (${newsTilt.negativeHits} olumsuz başlık). Bu, fiyattaki düşüşün sadece geçici bir dalgalanma değil, şirketle ilgili gerçek bir sorunun işareti olabileceği anlamına gelebilir. Bu durumda teknik sinyale güvenmeden önce haberleri dikkatlice okuyup düşüşün asıl nedenini anlamanızı önemle öneririz."
            else -> "Son günlerdeki haberler genel olarak olumsuz (${newsTilt.negativeHits} olumsuz başlık) — bu, teknik görünüme rağmen temkinli olmayı gerektirebilir."
        }
        return NewsNudgeResult(nudged, note, severeConflict)
    }

    fun analyze(items: List<NewsItem>): NewsTilt {
        val matchedPositive = mutableListOf<String>()
        val matchedNegative = mutableListOf<String>()

        items.forEach { item ->
            val lower = item.title.lowercase()
            if (positiveKeywords.any { lower.contains(it) }) matchedPositive += item.title
            if (negativeKeywords.any { lower.contains(it) }) matchedNegative += item.title
        }

        val sentiment = when {
            matchedPositive.size > matchedNegative.size -> NewsSentiment.POSITIVE
            matchedNegative.size > matchedPositive.size -> NewsSentiment.NEGATIVE
            else -> NewsSentiment.NEUTRAL
        }

        return NewsTilt(
            sentiment = sentiment,
            positiveHits = matchedPositive.size,
            negativeHits = matchedNegative.size,
            matchedPositive = matchedPositive,
            matchedNegative = matchedNegative
        )
    }
}
