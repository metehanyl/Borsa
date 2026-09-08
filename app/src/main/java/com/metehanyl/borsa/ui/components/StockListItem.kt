package com.metehanyl.borsa.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metehanyl.borsa.ui.StockEntry
import com.metehanyl.borsa.ui.theme.BuyGreen
import com.metehanyl.borsa.ui.theme.SellRed

/**
 * @param onBuyClick verilirse satırın sonuna küçük bir "Satın Al" ikon butonu eklenir
 * (ör. Piyasalar sekmesinde); verilmezse (ör. Önerilerim/Portföyüm) buton gösterilmez.
 * @param onToggleFavorite verilirse bir yıldız butonu eklenir (favorilere ekle/çıkar).
 */
@Composable
fun StockListItem(
    entry: StockEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onBuyClick: (() -> Unit)? = null,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null
) {
    val quote = entry.quote
    val isPositive = quote.changePercent >= 0
    val changeColor = if (isPositive) BuyGreen else SellRed

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = quote.info.market.countryFlag, fontSize = 22.sp)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = quote.info.symbol,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = quote.info.name,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        MiniSparkline(
            closes = quote.history.takeLast(30).map { it.close },
            isPositive = isPositive,
            modifier = Modifier.width(44.dp).height(28.dp).padding(horizontal = 6.dp)
        )

        Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(96.dp)) {
            Text(
                text = formatPrice(quote.price, quote.currency),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatPercent(quote.changePercent),
                fontSize = 12.sp,
                color = changeColor
            )
        }

        Row(modifier = Modifier.padding(start = 8.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            entry.analysis?.let { analysis ->
                ScoreBadge(recommendation = analysis.recommendation, score = analysis.score)
            }
            if (onToggleFavorite != null) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (isFavorite) "Favorilerden çıkar" else "Favorilere ekle",
                        tint = if (isFavorite) androidx.compose.ui.graphics.Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            if (onBuyClick != null) {
                IconButton(onClick = onBuyClick) {
                    Icon(
                        Icons.Filled.ShoppingCart,
                        contentDescription = "Satın al",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/** Son ~30 kapanışı tek renkli, eksensiz küçük bir çizgi olarak çizer (liste satırları için hızlı bir görsel özet). */
@Composable
private fun MiniSparkline(closes: List<Double>, isPositive: Boolean, modifier: Modifier = Modifier) {
    if (closes.size < 2) return
    val color = if (isPositive) BuyGreen else SellRed
    val min = closes.min()
    val max = closes.max()
    val range = (max - min).let { if (it == 0.0) 1.0 else it }

    Canvas(modifier = modifier) {
        val stepX = size.width / (closes.size - 1)
        val path = androidx.compose.ui.graphics.Path()
        closes.forEachIndexed { index, value ->
            val x = index * stepX
            val normalized = ((value - min) / range).toFloat()
            val y = size.height - (normalized * size.height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = color, style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round))

        val lastNorm = ((closes.last() - min) / range).toFloat()
        val lastY = size.height - (lastNorm * size.height)
        drawCircle(color = color, radius = 2.dp.toPx(), center = Offset(size.width, lastY))
    }
}
