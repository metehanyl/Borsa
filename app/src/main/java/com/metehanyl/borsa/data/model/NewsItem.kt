package com.metehanyl.borsa.data.model

/** Google News RSS'ten çekilen tek bir haber başlığı. */
data class NewsItem(
    val title: String,
    val source: String,
    val link: String,
    val publishedRaw: String?
)
