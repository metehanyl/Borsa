package com.metehanyl.borsa.ui.components

import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.abs

private val priceFormat = DecimalFormat("#,##0.00")
private val compactVolumeFormat = DecimalFormat("#,##0.0")

fun formatPrice(value: Double, currency: String): String = "${priceFormat.format(value)} $currency"

fun formatPercent(value: Double): String {
    val sign = if (value >= 0) "+" else ""
    return "$sign${priceFormat.format(value)}%"
}

fun formatCompactNumber(value: Long): String {
    val absValue = abs(value.toDouble())
    return when {
        absValue >= 1_000_000_000 -> "${compactVolumeFormat.format(value / 1_000_000_000.0)}B"
        absValue >= 1_000_000 -> "${compactVolumeFormat.format(value / 1_000_000.0)}M"
        absValue >= 1_000 -> "${compactVolumeFormat.format(value / 1_000.0)}K"
        else -> value.toString()
    }
}

fun formatDate(timestampMillis: Long): String {
    val date = java.util.Date(timestampMillis)
    val fmt = java.text.SimpleDateFormat("d MMM yyyy", Locale("tr", "TR"))
    return fmt.format(date)
}

/** Saat:dakika + kısa tarih (cihazın yerel saat dilimine göre). */
fun formatHourLabel(timestampMillis: Long): String {
    val date = java.util.Date(timestampMillis)
    val fmt = java.text.SimpleDateFormat("d MMM HH:mm", Locale("tr", "TR"))
    return fmt.format(date)
}
