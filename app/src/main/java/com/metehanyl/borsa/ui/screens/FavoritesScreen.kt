package com.metehanyl.borsa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metehanyl.borsa.data.StockCatalog
import com.metehanyl.borsa.data.model.StockInfo
import com.metehanyl.borsa.ui.PortfolioViewModel
import com.metehanyl.borsa.ui.components.StockListItem
import com.metehanyl.borsa.ui.favorites.FavoritesViewModel
import com.metehanyl.borsa.ui.holdings.AddHoldingDialog
import com.metehanyl.borsa.ui.holdings.HoldingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: PortfolioViewModel,
    holdingsViewModel: HoldingsViewModel,
    favoritesViewModel: FavoritesViewModel,
    onStockClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val favorites by favoritesViewModel.favorites.collectAsState()
    var buyTarget by remember { mutableStateOf<StockInfo?>(null) }

    // Uygulama yeniden açıldığında, kataloğun dışında yıldızlanmış bir kağıt varsa
    // (ör. Portföyüm'e eklenmeden sadece favorilere eklenmiş elle girilen bir sembol)
    // fiyatının da tekrar çekilmeye başlanmasını sağlar.
    LaunchedEffect(favorites) {
        favorites.filter { info -> StockCatalog.all.none { it.symbol == info.symbol } }
            .forEach { info -> viewModel.trackSymbol(info) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Favoriler", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (favorites.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp)) {
                Text(
                    "Henüz favori eklemediniz",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Piyasalar veya Önerilerim sekmesinde bir kağıdın yanındaki yıldız ikonuna " +
                        "dokunarak onu buraya ekleyebilirsiniz.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            return@Scaffold
        }

        val favoriteSymbols = favorites.map { it.symbol }.toSet()
        val entries = state.entries.filter { it.quote.info.symbol in favoriteSymbols }
        val loadedSymbols = entries.map { it.quote.info.symbol }.toSet()
        val stillLoadingCount = favoriteSymbols.count { it !in loadedSymbols }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { IntroCard() }
            items(entries, key = { it.quote.info.symbol }) { entry ->
                StockListItem(
                    entry = entry,
                    onClick = { onStockClick(entry.quote.info.symbol) },
                    onBuyClick = { buyTarget = entry.quote.info },
                    isFavorite = true,
                    onToggleFavorite = { favoritesViewModel.remove(entry.quote.info.symbol) }
                )
            }
            if (stillLoadingCount > 0) {
                item {
                    Text(
                        "$stillLoadingCount favori için fiyat yükleniyor…",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
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
        Text(
            "Yıldızladığınız kağıtlar burada, cihazınızda saklanır. Yıldıza tekrar dokunarak " +
                "listeden çıkarabilirsiniz.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
