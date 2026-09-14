package com.metehanyl.borsa.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metehanyl.borsa.data.model.PricePoint
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val Sma50Color = Color(0xFFFFA726) // turuncu
private val Sma200Color = Color(0xFF7E57C2) // mor

enum class ChartRange(val label: String, val tradingDays: Int) {
    ONE_MONTH("1A", 21),
    THREE_MONTHS("3A", 63),
    SIX_MONTHS("6A", 126),
    ONE_YEAR("1Y", 252)
}

/**
 * Bağımlılıksız (üçüncü parti kütüphane kullanmayan), Canvas tabanlı, detaylı
 * fiyat grafiği. Seçilen aralığa göre kapanış fiyatlarını çizgi + degrade
 * dolgu ile gösterir; 50 ve 200 günlük hareketli ortalamaları (turuncu/mor
 * çizgi), 52 haftalık en yüksek/en düşük seviyeleri (kesikli çizgi) ve
 * okunabilirlik için hafif ızgara çizgileri + fiyat etiketleri üst üste
 * çizer, altında günlük hacim çubukları gösterir. Parmakla basılı tutup
 * sürükleyerek o güne ait Tarih/Açılış/Kapanış/Düşük/Yüksek/Değişim
 * bilgisini gösteren bir ipucu kutusu açılır. Not: bu değerler GÜNLÜK
 * mumlardır (dakikalık/anlık değil) — Yahoo Finance'ten çekilen geçmiş veri
 * bu çözünürlüktedir.
 *
 * @param fillAvailableHeight true ise grafik sabit bir [chartHeight] yerine,
 * bulunduğu Column içinde (ör. ChartDetailScreen'de) kalan tüm dikey alanı
 * doldurur — gerçek "tam ekran" grafik görünümü için kullanılır.
 */
@Composable
fun PriceChart(
    history: List<PricePoint>,
    isPositive: Boolean,
    selectedRange: ChartRange,
    onRangeSelected: (ChartRange) -> Unit,
    fiftyTwoWeekHigh: Double? = null,
    fiftyTwoWeekLow: Double? = null,
    chartHeight: Dp = 200.dp,
    fillAvailableHeight: Boolean = false,
    onExpandClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val lineColor = if (isPositive) com.metehanyl.borsa.ui.theme.BuyGreen else com.metehanyl.borsa.ui.theme.SellRed
    val closesAll = remember(history) { history.map { it.close } }
    val sma50All = remember(closesAll) { rollingSma(closesAll, 50) }
    val sma200All = remember(closesAll) { rollingSma(closesAll, 200) }

    val windowSize = remember(history, selectedRange) { visibleWindowSize(history, selectedRange.tradingDays) }
    val points = remember(history, windowSize) { history.takeLast(windowSize) }
    val sma50 = remember(sma50All, windowSize) { sma50All.takeLast(windowSize) }
    val sma200 = remember(sma200All, windowSize) { sma200All.takeLast(windowSize) }

    var canvasWidthPx by remember { mutableStateOf(0f) }
    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }

    Column(modifier = modifier) {
        if (onExpandClick != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onExpandClick) {
                    Icon(
                        Icons.Filled.Fullscreen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Tam Ekran Grafik", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        val boxModifier = if (fillAvailableHeight) {
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                .padding(16.dp)
        } else {
            Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                .padding(16.dp)
        }

        Box(modifier = boxModifier) {
            if (points.size < 2) {
                Text(
                    "Grafik için yeterli veri yok",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                val closes = points.map { it.close }
                var minClose = closes.min()
                var maxClose = closes.max()
                sma50.filterNotNull().forEach { minClose = min(minClose, it); maxClose = max(maxClose, it) }
                sma200.filterNotNull().forEach { minClose = min(minClose, it); maxClose = max(maxClose, it) }
                fiftyTwoWeekHigh?.let { maxClose = max(maxClose, it) }
                fiftyTwoWeekLow?.let { minClose = min(minClose, it) }
                // Üstte/altta biraz nefes payı bırak, çizgi kenara yapışmasın.
                val paddingAmount = (maxClose - minClose) * 0.06
                minClose -= paddingAmount
                maxClose += paddingAmount
                val range = (maxClose - minClose).let { if (it == 0.0) 1.0 else it }

                // MaterialTheme.colorScheme bir @Composable okuyucudur; Canvas'ın çizim
                // bloğu (DrawScope) @Composable bir bağlam DEĞİLDİR — bu yüzden rengi
                // burada, Composable bağlamdayken önceden hesaplayıp yakalıyoruz.
                val referenceLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                val gridLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                val axisLabelColorArgb = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f).toArgb()
                val density = LocalDensity.current
                val axisLabelTextSizePx = with(density) { 11.sp.toPx() }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (fillAvailableHeight) Modifier.fillMaxHeight() else Modifier.height(chartHeight - 32.dp))
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
                    val leftMargin = 46.dp.toPx()
                    val w = size.width - leftMargin
                    val h = size.height
                    val stepX = if (points.size > 1) w / (points.size - 1) else w

                    fun yFor(value: Double): Float {
                        val normalized = ((value - minClose) / range).toFloat()
                        return h - (normalized * h)
                    }

                    // Okunabilirlik için hafif yatay ızgara çizgileri + sol tarafta fiyat etiketleri
                    val textPaint = android.graphics.Paint().apply {
                        color = axisLabelColorArgb
                        textSize = axisLabelTextSizePx
                        isAntiAlias = true
                    }
                    val gridSteps = 4
                    for (i in 0..gridSteps) {
                        val fraction = i / gridSteps.toFloat()
                        val value = minClose + (range * fraction)
                        val y = yFor(value)
                        drawLine(
                            color = gridLineColor,
                            start = Offset(leftMargin, y),
                            end = Offset(leftMargin + w, y),
                            strokeWidth = 1.dp.toPx()
                        )
                        val label = "%.2f".format(value)
                        val textY = when (i) {
                            gridSteps -> y + textPaint.textSize
                            0 -> y - 4.dp.toPx()
                            else -> y + (textPaint.textSize / 3f)
                        }
                        drawContext.canvas.nativeCanvas.drawText(label, 0f, textY, textPaint)
                    }

                    // 52 haftalık en yüksek/en düşük referans çizgileri
                    fiftyTwoWeekHigh?.let { high ->
                        drawLine(
                            color = referenceLineColor,
                            start = Offset(leftMargin, yFor(high)),
                            end = Offset(leftMargin + w, yFor(high)),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )
                    }
                    fiftyTwoWeekLow?.let { low ->
                        drawLine(
                            color = referenceLineColor,
                            start = Offset(leftMargin, yFor(low)),
                            end = Offset(leftMargin + w, yFor(low)),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )
                    }

                    val path = androidx.compose.ui.graphics.Path()
                    val fillPath = androidx.compose.ui.graphics.Path()
                    points.forEachIndexed { index, point ->
                        val x = leftMargin + index * stepX
                        val y = yFor(point.close)
                        if (index == 0) {
                            path.moveTo(x, y)
                            fillPath.moveTo(x, h)
                            fillPath.lineTo(x, y)
                        } else {
                            path.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }
                    }
                    fillPath.lineTo(leftMargin + w, h)
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(lineColor.copy(alpha = 0.30f), lineColor.copy(alpha = 0.0f))
                        )
                    )

                    // 50 ve 200 günlük hareketli ortalama çizgileri (varsa)
                    drawSmaLine(sma50, leftMargin, stepX, ::yFor, Sma50Color)
                    drawSmaLine(sma200, leftMargin, stepX, ::yFor, Sma200Color)

                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                    )

                    // Son fiyat noktası (hafif hâle efektiyle vurgulanmış)
                    val lastX = leftMargin + (points.size - 1) * stepX
                    val lastY = yFor(points.last().close)
                    drawCircle(color = lineColor.copy(alpha = 0.25f), radius = 8.dp.toPx(), center = Offset(lastX, lastY))
                    drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(lastX, lastY))

                    // Sürüklenerek seçilen nokta: kesikli dikey çizgi + vurgulu nokta
                    selectedIndex?.let { idx ->
                        val point = points.getOrNull(idx) ?: return@let
                        val x = leftMargin + idx * stepX
                        val y = yFor(point.close)
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

        if (points.size >= 2) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 46.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatShortDate(points.first().timestampMillis), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Text(formatShortDate(points.last().timestampMillis), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            VolumeStrip(points = points)
            ChartLegend(hasSma50 = sma50.any { it != null }, hasSma200 = sma200.any { it != null }, hasRange = fiftyTwoWeekHigh != null || fiftyTwoWeekLow != null)
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

private fun DrawScope.drawSmaLine(
    values: List<Double?>,
    leftMargin: Float,
    stepX: Float,
    yFor: (Double) -> Float,
    color: Color
) {
    val path = androidx.compose.ui.graphics.Path()
    var started = false
    values.forEachIndexed { index, value ->
        if (value == null) return@forEachIndexed
        val x = leftMargin + index * stepX
        val y = yFor(value)
        if (!started) {
            path.moveTo(x, y)
            started = true
        } else {
            path.lineTo(x, y)
        }
    }
    if (started) {
        drawPath(path = path, color = color, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** Fiyat grafiğinin altında, aynı genişlikte, günlük işlem hacmini çubuk olarak gösterir. */
@Composable
private fun VolumeStrip(points: List<PricePoint>) {
    val volumes = points.map { it.volume ?: 0L }
    val maxVolume = volumes.maxOrNull()?.takeIf { it > 0 } ?: return
    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Text(
            "Günlük İşlem Hacmi (lot)",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Canvas(modifier = Modifier.fillMaxWidth().height(36.dp).padding(top = 2.dp)) {
            val w = size.width
            val h = size.height
            val stepX = if (points.size > 1) w / points.size else w
            val barWidth = (stepX * 0.7f).coerceAtLeast(1f)
            points.forEachIndexed { index, point ->
                val vol = point.volume ?: 0L
                if (vol <= 0L) return@forEachIndexed
                val barHeight = (vol.toFloat() / maxVolume.toFloat()) * h
                val isUp = point.close >= (point.open ?: point.close)
                val color = if (isUp) com.metehanyl.borsa.ui.theme.BuyGreen else com.metehanyl.borsa.ui.theme.SellRed
                val x = index * stepX + (stepX - barWidth) / 2f
                drawRect(
                    color = color.copy(alpha = 0.55f),
                    topLeft = Offset(x, h - barHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                )
            }
        }
    }
}

@Composable
private fun ChartLegend(hasSma50: Boolean, hasSma200: Boolean, hasRange: Boolean) {
    if (!hasSma50 && !hasSma200 && !hasRange) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (hasSma50) LegendItem(color = Sma50Color, label = "50 günlük ortalama")
        if (hasSma200) LegendItem(color = Sma200Color, label = "200 günlük ortalama")
        if (hasRange) LegendItem(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), label = "52 hafta yüksek/düşük")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(3.dp)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
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

private fun visibleWindowSize(history: List<PricePoint>, n: Int): Int {
    if (history.isEmpty()) return 0
    return max(min(n, history.size), min(2, history.size))
}

/** Verilen kapanış serisi için, her noktada geriye dönük `period` günlük basit hareketli ortalama. Yeterli geçmiş olmayan noktalarda null. */
private fun rollingSma(closes: List<Double>, period: Int): List<Double?> {
    if (closes.isEmpty()) return emptyList()
    val result = ArrayList<Double?>(closes.size)
    var windowSum = 0.0
    for (i in closes.indices) {
        windowSum += closes[i]
        if (i >= period) windowSum -= closes[i - period]
        result.add(if (i >= period - 1) windowSum / period else null)
    }
    return result
}
