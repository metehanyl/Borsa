package com.metehanyl.borsa.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metehanyl.borsa.data.model.PricePoint
import kotlin.math.max
import kotlin.math.min

enum class ChartRange(val label: String, val tradingDays: Int) {
    ONE_MONTH("1A", 21),
    THREE_MONTHS("3A", 63),
    SIX_MONTHS("6A", 126),
    ONE_YEAR("1Y", 252)
}

/**
 * Bağımlılıksız (üçüncü parti kütüphane kullanmayan), Canvas tabanlı sade
 * fiyat grafiği. Seçilen aralığa göre kapanış fiyatlarını çizgi + degrade
 * dolgu ile gösterir; en düşük/en yüksek noktaları etiketler.
 */
@Composable
fun PriceChart(
    history: List<PricePoint>,
    isPositive: Boolean,
    selectedRange: ChartRange,
    onRangeSelected: (ChartRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val lineColor = if (isPositive) com.metehanyl.borsa.ui.theme.BuyGreen else com.metehanyl.borsa.ui.theme.SellRed
    val points = remember(history, selectedRange) { lastNPoints(history, selectedRange.tradingDays) }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            if (points.size < 2) {
                Text(
                    "Grafik için yeterli veri yok",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                val closes = points.map { it.close }
                val minClose = closes.min()
                val maxClose = closes.max()
                val range = (maxClose - minClose).let { if (it == 0.0) 1.0 else it }

                Canvas(modifier = Modifier.fillMaxWidth().height(168.dp)) {
                    val w = size.width
                    val h = size.height
                    val stepX = if (points.size > 1) w / (points.size - 1) else w

                    val path = androidx.compose.ui.graphics.Path()
                    val fillPath = androidx.compose.ui.graphics.Path()
                    points.forEachIndexed { index, point ->
                        val x = index * stepX
                        val normalized = ((point.close - minClose) / range).toFloat()
                        val y = h - (normalized * h)
                        if (index == 0) {
                            path.moveTo(x, y)
                            fillPath.moveTo(x, h)
                            fillPath.lineTo(x, y)
                        } else {
                            path.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }
                    }
                    fillPath.lineTo(w, h)
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.0f))
                        )
                    )
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Son fiyat noktası
                    val lastX = (points.size - 1) * stepX
                    val lastNorm = ((points.last().close - minClose) / range).toFloat()
                    val lastY = h - (lastNorm * h)
                    drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(lastX, lastY))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ChartRange.entries.forEach { range ->
                val selected = range == selectedRange
                Text(
                    text = range.label,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                        .clickable { onRangeSelected(range) }
                )
            }
        }
    }
}

private fun lastNPoints(history: List<PricePoint>, n: Int): List<PricePoint> {
    if (history.isEmpty()) return emptyList()
    val count = max(min(n, history.size), min(2, history.size))
    return history.takeLast(count)
}
