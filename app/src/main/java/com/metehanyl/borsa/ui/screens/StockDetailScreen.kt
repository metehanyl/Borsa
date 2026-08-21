package com.metehanyl.borsa.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metehanyl.borsa.analysis.Analysis
import com.metehanyl.borsa.analysis.CommentarySynthesizer
import com.metehanyl.borsa.analysis.LongTermOutlook
import com.metehanyl.borsa.analysis.NewsAnalyzer
import com.metehanyl.borsa.analysis.NewsSentiment
import com.metehanyl.borsa.analysis.NewsTilt
import com.metehanyl.borsa.data.model.Holding
import com.metehanyl.borsa.data.model.NewsItem
import com.metehanyl.borsa.data.model.Quote
import com.metehanyl.borsa.data.remote.NewsService
import com.metehanyl.borsa.ui.PortfolioViewModel
import com.metehanyl.borsa.ui.components.ChartRange
import com.metehanyl.borsa.ui.components.PriceChart
import com.metehanyl.borsa.ui.components.ScoreBadge
import com.metehanyl.borsa.ui.components.colorFor
import com.metehanyl.borsa.ui.components.formatCompactNumber
import com.metehanyl.borsa.ui.components.formatPercent
import com.metehanyl.borsa.ui.components.formatPrice
import com.metehanyl.borsa.ui.holdings.HoldingsViewModel
import com.metehanyl.borsa.ui.theme.BuyGreen
import com.metehanyl.borsa.ui.theme.SellRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    viewModel: PortfolioViewModel,
    holdingsViewModel: HoldingsViewModel,
    symbol: String,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val holdings by holdingsViewModel.holdings.collectAsState()
    val entry = state.entries.firstOrNull { it.quote.info.symbol == symbol }
    val holding = holdings.firstOrNull { it.symbol == symbol }
    var selectedRange by remember { mutableStateOf(ChartRange.THREE_MONTHS) }

    var newsItems by remember(symbol) { mutableStateOf<List<NewsItem>>(emptyList()) }
    var newsLoading by remember(symbol) { mutableStateOf(true) }

    LaunchedEffect(symbol, entry?.quote?.info?.name) {
        newsLoading = true
        val query = entry?.quote?.info?.name ?: symbol
        newsItems = NewsService.search(query, language = "tr")
        newsLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(entry?.quote?.info?.symbol ?: symbol, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (entry == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)
            ) {
                Text("Veri yükleniyor veya bulunamadı.", color = MaterialTheme.colorScheme.onSurface)
            }
            return@Scaffold
        }

        val quote = entry.quote
        val analysis = entry.analysis
        val isPositive = quote.changePercent >= 0
        val newsTilt = if (newsItems.isNotEmpty()) NewsAnalyzer.analyze(newsItems) else null

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { HeaderSection(quote, isPositive) }
            if (holding != null) {
                item { HoldingPositionCard(holding, quote) }
            }
            item {
                PriceChart(
                    history = quote.history,
                    isPositive = isPositive,
                    selectedRange = selectedRange,
                    onRangeSelected = { selectedRange = it }
                )
            }
            item { KeyStatsGrid(quote) }
            if (analysis != null) {
                item { AnalysisSection(analysis) }
                item { LongTermOutlookCard(analysis) }
                item {
                    CommentaryCard(
                        text = CommentarySynthesizer.synthesize(quote.info.name, quote, analysis, newsTilt)
                    )
                }
            } else {
                item {
                    Text(
                        "Bu sembol için yeterli geçmiş veri bulunmadığından algoritmik analiz üretilemedi.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            item { NewsSection(loading = newsLoading, items = newsItems, tilt = newsTilt) }
            item { DisclaimerCard() }
        }
    }
}

@Composable
private fun HeaderSection(quote: Quote, isPositive: Boolean) {
    Column {
        Text(
            text = "${quote.info.market.countryFlag} ${quote.info.name}",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = "${quote.info.market.exchangeLabel} · ${quote.info.sector}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = formatPrice(quote.price, quote.currency),
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "${formatPercent(quote.changePercent)} (${formatPrice(quote.changeAmount, quote.currency)})",
                fontSize = 15.sp,
                color = if (isPositive) BuyGreen else SellRed,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

@Composable
private fun HoldingPositionCard(holding: Holding, quote: Quote) {
    val currentValue = quote.price * holding.quantity
    val costValue = holding.averageCost * holding.quantity
    val pnl = currentValue - costValue
    val pnlPct = if (costValue != 0.0) (pnl / costValue) * 100.0 else 0.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text("Pozisyonunuz", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Text(
            "${holding.quantity} adet · ${formatPrice(holding.averageCost, quote.currency)} ortalama maliyet · ${holding.purchaseDateLabel}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp)
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Güncel Değer", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Text(formatPrice(currentValue, quote.currency), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Kâr/Zarar", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Text(
                    "${formatPrice(pnl, quote.currency)} (${formatPercent(pnlPct)})",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = if (pnl >= 0) BuyGreen else SellRed
                )
            }
        }
        if (holding.note.isNotBlank()) {
            Text(
                "Not: ${holding.note}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun KeyStatsGrid(quote: Quote) {
    val stats = buildList {
        add("Önceki Kapanış" to formatPrice(quote.previousClose, quote.currency))
        quote.dayHigh?.let { add("Günlük Yüksek" to formatPrice(it, quote.currency)) }
        quote.dayLow?.let { add("Günlük Düşük" to formatPrice(it, quote.currency)) }
        quote.fiftyTwoWeekHigh?.let { add("52 Hafta Yüksek" to formatPrice(it, quote.currency)) }
        quote.fiftyTwoWeekLow?.let { add("52 Hafta Düşük" to formatPrice(it, quote.currency)) }
        quote.volume?.let { add("Hacim" to formatCompactNumber(it)) }
        quote.marketCap?.let { add("Piyasa Değeri" to "${formatCompactNumber(it)} ${quote.currency}") }
        quote.trailingPE?.let { add("F/K Oranı" to "%.1f".format(it)) }
        quote.dividendYieldPct?.let { add("Temettü Verimi" to "%.2f%%".format(it)) }
        quote.epsTrailingTwelveMonths?.let { add("Hisse Başı Kâr (EPS)" to formatPrice(it, quote.currency)) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text("Temel Bilgiler", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp))
        stats.chunked(2).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                rowItems.forEach { (label, value) ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalysisSection(analysis: Analysis) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Kısa Vadeli Teknik Analiz", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            ScoreBadge(recommendation = analysis.recommendation, score = analysis.score)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            analysis.recommendation.label,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = colorFor(analysis.recommendation)
        )
        Spacer(Modifier.height(12.dp))
        Text("Neden bu değerlendirme?", fontWeight = FontWeight.Medium, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
        analysis.reasons.forEach { reason ->
            Row(modifier = Modifier.padding(vertical = 3.dp)) {
                Text("•  ", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text(reason, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
            }
        }

        Spacer(Modifier.height(12.dp))
        val indicatorRows = buildList {
            analysis.rsi?.let { add("RSI (14)" to "%.1f".format(it)) }
            analysis.sma20?.let { add("SMA 20" to "%.2f".format(it)) }
            analysis.sma50?.let { add("SMA 50" to "%.2f".format(it)) }
            analysis.sma200?.let { add("SMA 200" to "%.2f".format(it)) }
            analysis.momentum1M?.let { add("1 Aylık Getiri" to formatPercent(it)) }
            analysis.momentum3M?.let { add("3 Aylık Getiri" to formatPercent(it)) }
            analysis.volatility?.let { add("Yıllıklandırılmış Volatilite" to "%.1f%%".format(it)) }
        }
        indicatorRows.chunked(2).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                rowItems.forEach { (label, value) ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun LongTermOutlookCard(analysis: Analysis) {
    val color = when (analysis.longTermOutlook) {
        LongTermOutlook.HIGH -> BuyGreen
        LongTermOutlook.MEDIUM -> MaterialTheme.colorScheme.onSurface
        LongTermOutlook.LOW -> SellRed
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text("Uzun Vadeli Değerlenme Potansiyeli", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Text(
            analysis.longTermOutlook.label,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = color,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            "Kısa vadeli teknik sinyalden bağımsız, fiyatın uzun vadeli yapısal konumuna dayanır. " +
                "Kısa vadede \"Sat\" görünse bile uzun vadede potansiyel taşıyabilir.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        analysis.longTermReasons.forEach { reason ->
            Row(modifier = Modifier.padding(vertical = 3.dp)) {
                Text("•  ", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text(reason, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
            }
        }
    }
}

@Composable
private fun CommentaryCard(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text("Otomatik Yorum", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp))
        Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
    }
}

@Composable
private fun NewsSection(loading: Boolean, items: List<NewsItem>, tilt: NewsTilt?) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("İlgili Haberler", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            if (tilt != null) {
                val (label, color) = when (tilt.sentiment) {
                    NewsSentiment.POSITIVE -> "Olumlu" to BuyGreen
                    NewsSentiment.NEGATIVE -> "Olumsuz" to SellRed
                    NewsSentiment.NEUTRAL -> "Nötr" to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                }
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color)
            }
        }
        Text(
            "Google News'ten çekilen güncel başlıklar; anahtar kelime taramasıyla kabaca olumlu/olumsuz " +
                "etiketlenir. Bir yapay zekanın makaleleri okuyup yorumlaması değildir.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        when {
            loading -> CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            items.isEmpty() -> Text(
                "Şu anda gösterilecek güncel haber bulunamadı.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            else -> items.forEach { newsItem ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (newsItem.link.isNotBlank()) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(newsItem.link)))
                            }
                        }
                        .padding(vertical = 8.dp)
                ) {
                    Text(newsItem.title, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    if (newsItem.source.isNotBlank()) {
                        Text(
                            newsItem.source,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DisclaimerCard() {
    Text(
        text = "Bu ekrandaki skor, uzun vadeli görünüm ve yorumlar; hareketli ortalama, RSI, MACD, momentum, " +
            "52 haftalık aralık, F/K oranı, temettü verimi ve haber başlıklarının kural tabanlı biçimde " +
            "ağırlıklandırılmasıyla cihazınızda otomatik üretilir. Bir yapay zekanın canlı yorumu değildir. " +
            "Yatırım danışmanlığı değildir; yatırım kararlarınızı kendi araştırmanız ve risk toleransınıza göre verin.",
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
