package com.metehanyl.borsa.analysis

import com.metehanyl.borsa.data.model.Quote

/**
 * Portföydeki bir pozisyon için "ne zaman satmalıyım" sorusuna algoritmik bir
 * referans sunar. Bunlar kesin bir tahmin ya da garanti DEĞİLDİR — teknik
 * analizde yaygın kullanılan iki basit kural üzerinden hesaplanan referans
 * seviyelerdir:
 *  - Kâr al hedefi: 52 haftanın en yüksek seviyesi (henüz oraya ulaşılmadıysa)
 *    ya da güncel fiyatın biraz üzeri (zaten zirvedeyse/üzerindeyse).
 *  - Zarar durdur seviyesi: 50 (yoksa 20) günlük hareketli ortalamanın biraz
 *    altı — trend takip stratejilerinde sık kullanılan bir kural.
 * Bilanço tarihi ise Yahoo Finance'ten gelen gerçek bir veridir (varsa);
 * fiyatın o tarih civarında daha oynak olabileceğine dair bir hatırlatmadır,
 * "bu tarihte kesin satış sinyali" anlamına gelmez.
 */
data class SellGuidance(
    val takeProfitPrice: Double?,
    val takeProfitBasis: String,
    val stopLossPrice: Double?,
    val stopLossBasis: String,
    val nextEarningsDateMillis: Long?
)

fun computeSellGuidance(quote: Quote, analysis: Analysis): SellGuidance {
    val price = quote.price
    val high = quote.fiftyTwoWeekHigh

    val takeProfitPrice: Double?
    val takeProfitBasis: String
    if (high != null && high > price) {
        takeProfitPrice = high
        takeProfitBasis = "52 hafta zirvesi"
    } else {
        takeProfitPrice = price * 1.10
        takeProfitBasis = "güncel fiyatın %10 üzeri (zaten 52 hafta zirvesinde/üzerinde)"
    }

    val support = analysis.sma50 ?: analysis.sma20
    val supportLabel = if (analysis.sma50 != null) "50 günlük ortalama" else "20 günlük ortalama"
    val stopLossPrice: Double?
    val stopLossBasis: String
    if (support != null && support < price) {
        stopLossPrice = support * 0.95
        stopLossBasis = "$supportLabel seviyesinin %5 altı"
    } else {
        stopLossPrice = null
        stopLossBasis = "fiyat zaten hareketli ortalamaların altında — teknik görünümü yakından izleyin"
    }

    return SellGuidance(
        takeProfitPrice = takeProfitPrice,
        takeProfitBasis = takeProfitBasis,
        stopLossPrice = stopLossPrice,
        stopLossBasis = stopLossBasis,
        nextEarningsDateMillis = quote.nextEarningsDateMillis
    )
}
