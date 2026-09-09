package com.metehanyl.borsa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metehanyl.borsa.analysis.LongTermOutlook
import com.metehanyl.borsa.data.StockCatalog
import com.metehanyl.borsa.data.model.Market
import com.metehanyl.borsa.data.model.StockInfo
import com.metehanyl.borsa.ui.PortfolioViewModel
import com.metehanyl.borsa.ui.StockEntry
import com.metehanyl.borsa.ui.components.StockListItem
import com.metehanyl.borsa.ui.favorites.FavoritesViewModel
import com.metehanyl.borsa.ui.holdings.AddHoldingDialog
import com.metehanyl.borsa.ui.holdings.HoldingsViewModel

/** Bir kağıdın "önerilerim" listesine girmesi için gereken asgari skor (Al ve üzeri). */
private const val RECOMMENDATION_MIN_SCORE = 20
private const val RECOMMENDATION_MAX_PER_MARKET = 5
private const val LONG_TERM_MAX = 20
private const val NEW_LISTINGS_MAX = 30

private enum class RecommendationTab { SHORT_TERM, LONG_TERM, NEW_LISTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationsScreen(
    viewModel: PortfolioViewModel,
    holdingsViewModel: HoldingsViewModel,
    favoritesViewModel: FavoritesViewModel,
    onStockClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val favorites by favoritesViewModel.favorites.collectAsState()
    val favoriteSymbols = remember(favorites) { favorites.map { it.symbol }.toSet() }
    var selectedTab by remember { mutableStateOf(RecommendationTab.SHORT_TERM) }
    var buyTarget by remember { mutableStateOf<StockInfo?>(null) }

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

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                FilterChip(
                    selected = selectedTab == RecommendationTab.SHORT_TERM,
                    onClick = { selectedTab = RecommendationTab.SHORT_TERM },
                    label = { Text("Kısa Vadeli Al Sinyalleri") }
                )
                FilterChip(
                    selected = selectedTab == RecommendationTab.LONG_TERM,
                    onClick = { selectedTab = RecommendationTab.LONG_TERM },
                    label = { Text("Uzun Vadeli Öneri") }
                )
                FilterChip(
                    selected = selectedTab == RecommendationTab.NEW_LISTINGS,
                    onClick = { selectedTab = RecommendationTab.NEW_LISTINGS },
                    label = { Text("Yeni Halka Arzlar") }
                )
            }

            when (selectedTab) {
                RecommendationTab.SHORT_TERM -> {
                    val picksByMarket = Market.entries.map { market ->
                        val picks = state.entries
                            .filter { it.quote.info.market == market && (it.analysis?.score ?: Int.MIN_VALUE) >= RECOMMENDATION_MIN_SCORE }
                            .sortedByDescending { it.analysis?.score ?: Int.MIN_VALUE }
                            .take(RECOMMENDATION_MAX_PER_MARKET)
                        market to picks
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
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
                                    StockListItem(
                                        entry = entry,
                                        onClick = { onStockClick(entry.quote.info.symbol) },
                                        onBuyClick = { buyTarget = entry.quote.info },
                                        isFavorite = entry.quote.info.symbol in favoriteSymbols,
                                        onToggleFavorite = { favoritesViewModel.toggle(entry.quote.info) }
                                    )
                                }
                            }
                        }
                        item { Spacer(Modifier.height(60.dp)) }
                    }
                }

                RecommendationTab.LONG_TERM -> {
                    val longTermPicks: List<StockEntry> = state.entries
                        .filter { it.analysis?.longTermOutlook == LongTermOutlook.HIGH }
                        .sortedByDescending { it.analysis?.score ?: Int.MIN_VALUE }
                        .take(LONG_TERM_MAX)

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { LongTermIntroCard() }
                        if (longTermPicks.isEmpty()) {
                            item {
                                Text(
                                    "Şu anda uzun vadeli potansiyeli 'Yüksek' olarak işaretlenen bir kağıt bulunmuyor.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        } else {
                            items(longTermPicks, key = { it.quote.info.symbol }) { entry ->
                                StockListItem(
                                    entry = entry,
                                    onClick = { onStockClick(entry.quote.info.symbol) },
                                    onBuyClick = { buyTarget = entry.quote.info },
                                    isFavorite = entry.quote.info.symbol in favoriteSymbols,
                                    onToggleFavorite = { favoritesViewModel.toggle(entry.quote.info) }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(60.dp)) }
                    }
                }

                RecommendationTab.NEW_LISTINGS -> {
                    val newListings: List<StockEntry> = state.entries
                        .filter { it.quote.isLikelyRecentListing }
                        .sortedByDescending { it.analysis?.score ?: Int.MIN_VALUE }
                        .take(NEW_LISTINGS_MAX)

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item { NewListingsIntroCard() }
                        if (newListings.isEmpty()) {
                            item {
                                Text(
                                    "Şu anda kataloğumuzdaki kağıtlar arasında işlem geçmişi ~1 yıldan kısa " +
                                        "(muhtemelen yakın zamanda halka arz olmuş) bir kağıt tespit edilmedi. " +
                                        "Bildiğiniz bir yeni halka arzı Piyasalar veya Portföyüm'den 'Sembolü elle " +
                                        "girin' ile eklerseniz, uygun olduğunda otomatik olarak burada görünür.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        } else {
                            items(newListings, key = { it.quote.info.symbol }) { entry ->
                                Column(modifier = Modifier.padding(bottom = 6.dp)) {
                                    StockListItem(
                                        entry = entry,
                                        onClick = { onStockClick(entry.quote.info.symbol) },
                                        onBuyClick = { buyTarget = entry.quote.info },
                                        isFavorite = entry.quote.info.symbol in favoriteSymbols,
                                        onToggleFavorite = { favoritesViewModel.toggle(entry.quote.info) }
                                    )
                                    Text(
                                        text = newListingVerdictText(entry),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                    )
                                }
                            }
                        }
                        item { Spacer(Modifier.height(60.dp)) }
                    }
                }
            }
        }
    }

    val target = buyTarget
    if (target != null) {
        AddHoldingDialog(
            marketViewModel = viewModel,
            preselectedStock = target,
            onDismiss = { buyTarget = null },
            onConfirm = { info, quantity, investedAmount, cost, date, note ->
                holdingsViewModel.addHolding(info, quantity, cost, date, note, investedAmount)
                if (StockCatalog.all.none { it.symbol == info.symbol }) viewModel.trackSymbol(info)
                buyTarget = null
            }
        )
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
private fun LongTermIntroCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text("Uzun Vadeli Potansiyel Sıralaması", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Text(
            "Bu liste, kısa vadeli Al/Sat sinyalinden BAĞIMSIZ olarak — fiyatın 200 günlük ortalamaya, " +
                "52 hafta aralığına ve RSI'a göre yapısal konumuna dayanarak — uzun vadeli değerlenme " +
                "potansiyeli 'Yüksek' olarak hesaplanan kağıtları sıralar. Kısa vadede 'Sat' sinyali " +
                "veren bir kağıt burada da görünebilir. Bu bir GARANTİ değildir — hiçbir algoritma " +
                "gelecekteki fiyatı garanti edemez; yatırım tavsiyesi yerine geçmez.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun NewListingsIntroCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text("Yeni Halka Arzlar", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Text(
            "Bu, elle tutulan güncel bir halka arz takvimi DEĞİLDİR. Kataloğumuzdaki kağıtlar arasında " +
                "gerçek işlem geçmişi ~1 yıldan kısa olanlar (Yahoo Finance verisine göre) otomatik olarak " +
                "burada listelenir — bir kağıt bir yılı doldurunca kendiliğinden bu listeden düşer. Kısıtlı " +
                "işlem geçmişi nedeniyle teknik göstergeler (özellikle uzun vadeli olanlar) daha az " +
                "güvenilirdir; 'gelecek vaadi' yorumu da dahil hiçbiri yatırım tavsiyesi ya da garanti değildir.",
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

/** Yeni halka arz olması muhtemel bir kağıt için kısa "gelecek vaadi" yorumu. */
private fun newListingVerdictText(entry: StockEntry): String {
    val analysis = entry.analysis
        ?: return "Henüz yeterli işlem geçmişi (30 günden az) olmadığından algoritmik bir değerlendirme yapılamıyor."
    val base = "Kısa vadeli sinyal: ${analysis.recommendation.label} · Uzun vadeli potansiyel: ${analysis.longTermOutlook.label}"
    return if (analysis.sma200 == null) {
        "$base (henüz 200 günlük tam geçmiş oluşmadığından uzun vadeli göstergeler sınırlı güvenilirlikte)"
    } else {
        base
    }
}
