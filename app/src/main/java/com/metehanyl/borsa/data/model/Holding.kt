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
    /**
     * Adet bazlı giriş yapıldıysa pay/lot sayısı; kullanıcı adet girmek yerine
     * yalnızca yatırdığı tutarı biliyorsa 0 (veya boş) bırakılır ve [investedAmount]
     * kullanılır — [effectiveQuantity] ikisini tek bir sayıya indirger.
     */
    val quantity: Double,
    val averageCost: Double,
    /**
     * Adet yerine "şu kadar tutar yatırdım" şeklinde girildiyse toplam yatırım
     * tutarı (kağıdın para biriminde). Adet bazlı girişlerde null kalır.
     */
    val investedAmount: Double? = null,
    /** Serbest metin olarak alış tarihi (ör. "16.03.2026"); ayrıştırma yapılmaz. */
    val purchaseDateLabel: String,
    val note: String = ""
) {
    /** Tutar bazlı girilmiş pozisyonlar için ortalama maliyetten türetilen efektif adet. */
    val effectiveQuantity: Double
        get() = when {
            quantity > 0 -> quantity
            investedAmount != null && averageCost > 0 -> investedAmount / averageCost
            else -> 0.0
        }

    /** Bu pozisyonun adet yerine tutar bazlı girilip girilmediği. */
    val isAmountBased: Boolean get() = quantity <= 0 && investedAmount != null
}
