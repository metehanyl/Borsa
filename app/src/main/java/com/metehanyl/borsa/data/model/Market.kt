package com.metehanyl.borsa.data.model

import kotlinx.serialization.Serializable

/** Uygulamanın kapsadığı 7 ülke borsası. */
@Serializable
enum class Market(
    val displayName: String,
    val countryFlag: String,
    val exchangeLabel: String,
    val currencySymbol: String
) {
    US("Amerika Birleşik Devletleri", "🇺🇸", "NASDAQ / NYSE", "$"),
    GERMANY("Almanya", "🇩🇪", "XETRA (Frankfurt)", "€"),
    UK("İngiltere", "🇬🇧", "London Stock Exchange", "£"),
    TURKEY("Türkiye", "🇹🇷", "Borsa İstanbul (BIST)", "₺"),
    FRANCE("Fransa", "🇫🇷", "Euronext Paris", "€"),
    CHINA("Çin", "🇨🇳", "Shanghai / Shenzhen", "¥"),
    JAPAN("Japonya", "🇯🇵", "Tokyo Stock Exchange", "¥")
}
