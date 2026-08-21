package com.metehanyl.borsa.data.model

/**
 * Gün içi (saatlik) bir zaman diliminde el değiştiren hisse adedi (hacim).
 * Bu, "kaç kişi bu hissede var/işlem yaptı" bilgisi DEĞİLDİR — böyle bir veri
 * hiçbir borsa veya Yahoo Finance tarafından yayımlanmaz. Burada gösterilen,
 * o saatte alınıp satılan hisse adedidir; kişi/işlem sayısı değildir.
 */
data class IntradayPoint(
    val timestampMillis: Long,
    val volume: Long,
    val close: Double
)
