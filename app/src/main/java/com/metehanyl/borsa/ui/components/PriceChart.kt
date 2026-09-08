package com.metehanyl.borsa.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metehanyl.borsa.data.model.PricePoint
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class ChartRange(val label: String, val tradingDays: Int) {
    ONE_MONTH("1A", 21),
    THREE_MONTHS("3A", 63),
    SIX_MONTHS("6A", 126),
    ONE_YEAR("1Y", 252)
}

/**
 * Bağımlılıksız (üçüncü parti kütüphane kullanmayan), Canvas tabanlı fiyat
 * grafiği. Seçilen aralığa göre kapanış fiyatlarını çizgi + degrade dolgu ile
 * gösterir. Parmakla basılı tutup sürükleyerek (borsa terminallerindeki gibi)
 * o güne ait Tarih/Açılış/Kapanış/Düşük/Yüksek/Değişim bilgisini gösteren bir
 * ipucu kutusu açılır. Not: bu değerler GÜNLÜK mumlardır (dakikalık/anlık
 * değil) — Yahoo Finance'ten çekilen geçmiş veri bu çözünürlüktedir.
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
    var canvasWidthPx by remember { mutableStateOf(0f) }
    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }

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

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp)
                        .onSizeChanged { canvasWidthPx = it.width.toFloat() }
                        .pointerInput(points) {
                            detectDragGestures(
                                onDragStart = { offset -> selectedIndex = indexForX(offset.x, canvasWidthPx, points.size) },
                                onDragEnd = { selectedIndex = null },
                                onDragCancel = { selectedIndex = null },
                                onDrag = { change, _ -> selectedIndex = indexForX(change.position.x, canvasWidthPx, points.size) }
                            )
                        }
                ) {
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

                    // Sürüklenerek seçilen nokta: kesikli dikey çizgi + vurgulu nokta
                    selectedIndex?.let { idx ->
                        val point = points.getOrNull(idx) ?: return@let
                        val x = idx * stepX
                        val normalized = ((point.close - minClose) / range).toFloat()
                        val y = h - (normalized * h)
                        drawLine(
                            color = lineColor.copy(alpha = 0.6f),
                            start = Offset(x, 0f),
                            end = Offset(x, h),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                        )
                        drawCircle(color = lineColor, radius = 5.dp.toPx(), center = Offset(x, y))
                    }
                }

                selectedIndex?.let { idx ->
                    points.getOrNull(idx)?.let { point ->
                        val prevClose = points.getOrNull(idx - 1)?.close
                        SelectedPointTooltip(point = point, prevClose = prevClose)
                    }
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

@Composable
private fun SelectedPointTooltip(point: PricePoint, prevClose: Double?) {
    val changePct = prevClose?.takeIf { it != 0.0 }?.let { ((point.close - it) / it) * 100.0 }
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        TooltipRow("Tarih", formatDate(point.timestampMillis))
        TooltipRow("Açılış", point.open?.let { "%.2f".format(it) } ?: "-")
        TooltipRow("Kapanış", "%.2f".format(point.close))
        TooltipRow("Düşük", point.low?.let { "%.2f".format(it) } ?: "-")
        TooltipRow("Yüksek", point.high?.let { "%.2f".format(it) } ?: "-")
        TooltipRow("Değişim", changePct?.let { formatPercent(it) } ?: "-")
    }
}

@Composable
private fun TooltipRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 1.dp)) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), modifier = Modifier.padding(end = 10.dp))
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun indexForX(x: Float, canvasWidthPx: Float, count: Int): Int {
    if (count <= 1 || canvasWidthPx <= 0f) return 0
    val stepX = canvasWidthPx / (count - 1)
    return (x / stepX).roundToInt().coerceIn(0, count - 1)
}

private fun lastNPoints(history: List<PricePoint>, n: Int): List<PricePoint> {
    if (history.isEmpty()) return emptyList()
    val count = max(min(n, history.size), min(2, history.size))
    return history.takeLast(count)
}
