package com.metehanyl.borsa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metehanyl.borsa.data.model.Market
import com.metehanyl.borsa.ui.PortfolioViewModel
import com.metehanyl.borsa.ui.components.StockListItem

/** Bir kağıdın "önerilerim" listesine girmesi için gereken asgari skor (Al ve üzeri). */
private const val RECOMMENDATION_MIN_SCORE = 20
private const val RECOMMENDATION_MAX_PER_MARKET = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationsScreen(
    viewModel: PortfolioViewModel,
    onStockClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Önerilerim", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            return@Scaffold
        }

        val picksByMarket = Market.entries.map { market ->
            val picks = state.entries
                .filter { it.quote.info.market == market && (it.analysis?.score ?: Int.MIN_VALUE) >= RECOMMENDATION_MIN_SCORE }
                .sortedByDescending { it.analysis?.score ?: Int.MIN_VALUE }
                .take(RECOMMENDATION_MAX_PER_MARKET)
            market to picks
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { IntroCard() }
            picksByMarket.forEach { (market, picks) ->
                item { MarketHeader(market) }
                if (picks.isEmpty()) {
                    item { NoPickText() }
                } else {
                    items(picks, key = { it.quote.info.symbol }) { entry ->
                        StockListItem(entry = entry, onClick = { onStockClick(entry.quote.info.symbol) })
                    }
                }
            }
            item { Spacer(Modifier.height(60.dp)) }
        }
    }
}

@Composable
private fun IntroCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text("En Güçlü Alım Sinyalleri", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Text(
            "Her ülke için, algoritmik analiz motorunun skoru en yüksek (gerçek Al/Güçlü Al sinyali " +
                "taşıyan) en fazla ${RECOMMENDATION_MAX_PER_MARKET} kağıt listelenir. Bu, kişisel bir " +
                "canlı yapay zeka değerlendirmesi değildir — uygulamanın genelinde kullanılan aynı kural " +
                "tabanlı analiz motorunun sonucudur. Yatırım tavsiyesi değildir; kendi araştırmanızı yapın.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun MarketHeader(market: Market) {
    Text(
        text = "${market.countryFlag} ${market.displayName}",
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun NoPickText() {
    Text(
        text = "Şu anda bu piyasada güçlü bir alım sinyali görünmüyor.",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
