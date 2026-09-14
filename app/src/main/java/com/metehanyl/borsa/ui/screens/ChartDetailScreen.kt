package com.metehanyl.borsa.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metehanyl.borsa.ui.PortfolioViewModel
import com.metehanyl.borsa.ui.components.ChartRange
import com.metehanyl.borsa.ui.components.PriceChart
import com.metehanyl.borsa.ui.components.formatPercent
import com.metehanyl.borsa.ui.components.formatPrice
import com.metehanyl.borsa.ui.theme.BuyGreen
import com.metehanyl.borsa.ui.theme.SellRed

/**
 * Fiyat grafiğinin, hisse detay ekranındaki küçük haliyle sığmayan, gerçekten
 * TAM EKRAN hâli. Aynı veriyi/aynı bileşeni (PriceChart) kullanır; grafik,
 * üstteki kısa başlık ve alttaki kısa açıklama dışında ekranın tamamını
 * kaplayacak şekilde büyütülür (fillAvailableHeight = true).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartDetailScreen(
    viewModel: PortfolioViewModel,
    symbol: String,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val entry = state.entries.firstOrNull { it.quote.info.symbol == symbol }
    var selectedRange by remember { mutableStateOf(ChartRange.ONE_YEAR) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$symbol — Detaylı Grafik", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
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
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            return@Scaffold
        }

        val quote = entry.quote
        val isPositive = quote.changePercent >= 0

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${quote.info.market.countryFlag} ${quote.info.name}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatPrice(quote.price, quote.currency),
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${formatPercent(quote.changePercent)} (${formatPrice(quote.changeAmount, quote.currency)})",
                    fontSize = 13.sp,
                    color = if (isPositive) BuyGreen else SellRed
                )
            }

            Spacer(Modifier.width(8.dp))

            PriceChart(
                history = quote.history,
                isPositive = isPositive,
                selectedRange = selectedRange,
                onRangeSelected = { selectedRange = it },
                fiftyTwoWeekHigh = quote.fiftyTwoWeekHigh,
                fiftyTwoWeekLow = quote.fiftyTwoWeekLow,
                fillAvailableHeight = true,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )

            Text(
                "Parmakla basılı tutup sürükleyerek herhangi bir günün Açılış/Kapanış/Düşük/Yüksek/Değişim " +
                    "bilgisini görebilirsiniz. Turuncu çizgi 50 günlük, mor çizgi 200 günlük ortalamadır; " +
                    "kesikli çizgiler 52 haftalık en yüksek/en düşük seviyelerdir.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
