package com.metehanyl.borsa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metehanyl.borsa.analysis.SellGuidance

/**
 * Bir pozisyon için "ne zaman satmalıyım" sorusuna algoritmik referans veren
 * kart. Fiyat hedefleri teknik analizde yaygın kurallardan (52 hafta zirvesi,
 * hareketli ortalama desteği) türetilir; bilanço tarihi ise Yahoo Finance'ten
 * gelen gerçek bir veridir. Hiçbiri kesin tahmin ya da yatırım tavsiyesi
 * DEĞİLDİR — kart bunu açıkça belirtir.
 */
@Composable
fun SellGuidanceCard(guidance: SellGuidance, currency: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text("Satış Rehberi", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

        guidance.takeProfitPrice?.let { price ->
            Text(
                "🎯 Kâr al hedefi: ${formatPrice(price, currency)} (${guidance.takeProfitBasis})",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (guidance.stopLossPrice != null) {
            Text(
                "🛑 Zarar durdur seviyesi: ${formatPrice(guidance.stopLossPrice, currency)} (${guidance.stopLossBasis})",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            Text(
                "⚠️ ${guidance.stopLossBasis}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        val earningsDate = guidance.nextEarningsDateMillis
        if (earningsDate != null && earningsDate > System.currentTimeMillis()) {
            Text(
                "📅 Sonraki bilanço tarihi: ${formatDate(earningsDate)} — bu tarihe yakın pozisyonunuzu " +
                    "gözden geçirmenizi öneririz (bilanço sonrası fiyat oynaklığı genelde artar).",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Text(
            "Bu seviyeler algoritmik bir referanstır; kesin bir tahmin, garanti ya da yatırım tavsiyesi değildir.",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
