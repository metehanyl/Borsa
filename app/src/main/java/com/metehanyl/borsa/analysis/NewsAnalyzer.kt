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
