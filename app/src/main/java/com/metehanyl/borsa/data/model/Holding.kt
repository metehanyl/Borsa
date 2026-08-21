package com.metehanyl.borsa.data.model

import kotlinx.serialization.Serializable

/**
 * Kullanıcının başka bir yerden (ör. banka/aracı kurum uygulaması) satın aldığı ve
 * bu uygulamaya elle girdiği bir pozisyon. Sunucuya gönderilmez, yalnızca cihazda
 * saklanır.
 */
@Serializable
data class Holding(
    val id: String,
    val symbol: String,
    /** Kataloğun dışında elle eklenmiş bir kağıtsa görünen ad; uygulama yeniden açıldığında sembolü yeniden izleyebilmek için saklanır. */
    val name: String = symbol,
    val market: Market = Market.US,
    val quantity: Double,
    val averageCost: Double,
    /** Serbest metin olarak alış tarihi (ör. "16.03.2026"); ayrıştırma yapılmaz. */
    val purchaseDateLabel: String,
    val note: String = ""
)
